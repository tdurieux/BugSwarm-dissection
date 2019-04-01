# ===============================================================================
# Copyright 2013 Jake Ross
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ===============================================================================

# ============= enthought library imports =======================
from pyface.constant import CANCEL, NO
from pyface.tasks.task_layout import PaneItem, TaskLayout, Splitter, Tabbed
from pyface.timer.do_later import do_after
from traits.api import Int, on_trait_change, Bool, Instance, Event, Color

# ============= standard library imports ========================
import shutil
import time
import os
import xlrd
# ============= local library imports  ==========================
from pychron.core.helpers.filetools import add_extension, backup
from pychron.core.ui.preference_binding import color_bind_preference, toTuple
from pychron.envisage.tasks.editor_task import EditorTask
from pychron.envisage.tasks.pane_helpers import ConsolePane
from pychron.envisage.view_util import open_view
from pychron.experiment.experiment_launch_history import update_launch_history
from pychron.experiment.experimentor import Experimentor
from pychron.experiment.queue.base_queue import extract_meta
from pychron.experiment.tasks.experiment_editor import ExperimentEditor, UVExperimentEditor
from pychron.experiment.utilities.save_dialog import ExperimentSaveDialog
from pychron.experiment.utilities.identifier import convert_extract_device, is_special
from pychron.furnace.ifurnace_manager import IFurnaceManager
from pychron.lasers.laser_managers.ilaser_manager import ILaserManager
from pychron.paths import paths
from pychron.pipeline.plot.editors.figure_editor import FigureEditor
from pychron.pychron_constants import SPECTROMETER_PROTOCOL
from pychron.experiment.tasks.experiment_panes import ExperimentFactoryPane, StatsPane, \
    ControlsPane, IsotopeEvolutionPane, ConnectionStatusPane, LoggerPane, ExplanationPane
from pychron.envisage.tasks.wait_pane import WaitPane


class ExperimentEditorTask(EditorTask):
    id = 'pychron.experiment.task'
    name = 'Experiment'

    default_filename = 'Experiment Current.txt'
    default_directory = paths.experiment_dir
    default_open_action = 'open files'
    wildcard = '*.txt'

    use_notifications = Bool
    use_syslogger = Bool
    notifications_port = Int
    notifier = Instance('pychron.messaging.notify.notifier.Notifier', ())

    loading_manager = Instance('pychron.loading.loading_manager.LoadingManager')

    # use_syslogger = Bool
    # syslogger = Instance('pychron.experiment.sys_log.SysLogger')

    # analysis_health = Instance(AnalysisHealth)
    last_experiment_changed = Event

    bgcolor = Color
    even_bgcolor = Color

    automated_runs_editable = Bool

    isotope_evolution_pane = Instance(IsotopeEvolutionPane)
    experiment_factory_pane = Instance(ExperimentFactoryPane)
    # wait_pane = Instance(WaitPane)
    load_pane = Instance('pychron.loading.tasks.panes.LoadDockPane')
    load_table_pane = Instance('pychron.loading.tasks.panes.LoadTablePane')
    laser_control_client_pane = None

    def save_as_current_experiment(self):
        self.debug('save as current experiment')
        if self.has_active_editor():
            path = os.path.join(paths.experiment_dir, 'Current Experiment.txt')
            self.save(path=path)

    def configure_experiment_table(self):
        if self.has_active_editor():
            self.active_editor.show_table_configurer()

    def new_pattern(self):
        pm = self._pattern_maker_view_factory()
        open_view(pm)

    def open_pattern(self):
        pm = self._pattern_maker_view_factory()
        if pm.load_pattern():
            open_view(pm)

    def send_test_notification(self):
        self.debug('sending test notification')
        db = self.manager.db
        # an=db.get_last_analysis('bu-FD-o')
        an = db.get_last_analysis('ba-01-o')
        an = self.manager.make_analysis(an)
        if an:
            self.debug('test push {}'.format(an.record_id))
            self._publish_notification(an)
        else:
            self.debug('problem recalling last analysis')

    def deselect(self):
        if self.active_editor:
            self.active_editor.queue.selected = []
            self.active_editor.queue.executed_selected = []

    def undo(self):
        if self.has_active_editor():
            self.manager.experiment_factory.undo()

    def edit_queue_conditionals(self):
        if self.has_active_editor():
            from pychron.experiment.conditional.conditionals_edit_view import edit_conditionals

            dnames = None
            spec = self.application.get_service(
                'pychron.spectrometer.base_spectrometer_manager.BaseSpectrometerManager')
            if spec:
                dnames = spec.spectrometer.detector_names

            edit_conditionals(self.manager.experiment_factory.queue_factory.queue_conditionals_name,
                              detectors=dnames,
                              app=self.application)

    def reset_queues(self):
        for editor in self.editor_area.editors:
            editor.queue.reset()

        man = self.manager
        ex = man.executor
        man.update_info()
        man.stats.reset()

        ex.end_at_run_completion = False
        ex.set_extract_state('')

    def sync_queue(self):
        """
        sync queue to database
        """
        if not self.has_active_editor():
            return
        queue = self.active_editor.queue
        ms = queue.mass_spectrometer
        ed = queue.extract_device
        for i, ai in enumerate(queue.automated_runs):
            if ai.skip or ai.is_special():
                continue

            kw = {'identifier': ai.identifier, 'position': ai.position,
                  'mass_spectrometer': ms,
                  'extract_device': ed}
            if ai.is_step_heat():
                kw['aliquot'] = ai.aliquot
                kw['extract_value'] = ai.extract_value

            self.debug('checking {}/{}. attr={}'.format(i, ai.runid, kw))
            aa = self.manager.get_analysis(**kw)
            if aa is None:
                self.debug('----- not found')
                break

        if i:
            if i == len(queue.automated_runs) - 1:
                self.information_dialog('All Analyses from this experiment have been run')
            else:
                queue.automated_runs = queue.automated_runs[i:]
        else:
            self.information_dialog('No Analyses from this experiment have been run')

    def _assemble_state_colors(self):
        colors = {}
        for c in ('success', 'extraction', 'measurement', 'canceled', 'truncated',
                  'failed', 'end_after', 'invalid'):
            v = self.application.preferences.get('pychron.experiment.{}_color'.format(c))
            # tt = toTuple(v)
            # print 'fff',v, tt, type(tt[0]), type(tt[1]), type(tt[2])
            colors[c] = '#{:02X}{:02X}{:02X}'.format(*toTuple(v)[:3])
        return colors

    def new(self):
        manager = self.manager
        if manager.verify_database_connection(inform=True):

            if manager.load():
                self.manager.experiment_factory.activate(load_persistence=True)

                editor = ExperimentEditor(application=self.application)
                editor.setup_tabular_adapters(self.bgcolor, self.even_bgcolor, self._assemble_state_colors())
                editor.new_queue()

                self._open_editor(editor)
                if self.loading_manager:
                    self.loading_manager.clear()

                if not self.manager.executor.is_alive():
                    self.manager.executor.executable = False
                return True

    def execute(self):
        if not self.manager.executor.is_alive():
            self._execute()

    # ===============================================================================
    # tasks protocol
    # ===============================================================================
    def prepare_destroy(self):
        super(ExperimentEditorTask, self).prepare_destroy()

        self.manager.experiment_factory.destroy()
        self.manager.executor.notification_manager.parent = None

        if self.use_notifications:
            self.notifier.close()

        manager = self.application.get_service(IFurnaceManager)
        if manager:
            for window in self.application.windows:
                if 'furnace' in window.active_task.id:
                    break
            else:
                manager.stop_update()

                # del manager. fixes problem of multiple experiments being started
                # closed tasks were still receiving execute_event(s)
                # del self.manager
                # self.manager = None

    def bind_preferences(self):
        # notifications

        self._preference_binder('pychron.experiment',
                                ('use_notifications',
                                 'notifications_port',
                                 'automated_runs_editable'))

        # force notifier setup
        if self.use_notifications:
            self.notifier.setup(self.notifications_port)

        # sys logger
        # bind_preference(self, 'use_syslogger', 'pychron.use_syslogger')
        # if self.use_syslogger:
        # self._use_syslogger_changed()

        color_bind_preference(self, 'bgcolor', 'pychron.experiment.bg_color')
        color_bind_preference(self, 'even_bgcolor', 'pychron.experiment.even_bg_color')

    def activated(self):
        self.bind_preferences()
        super(ExperimentEditorTask, self).activated()

        self.manager.dvc.create_session()

        manager = self.application.get_service(IFurnaceManager)
        if manager:
            manager.start_update()

    def prepare_destory(self):
        self.manager.prepare_destroy()
        self.manager.dvc.close_session()

    def create_dock_panes(self):

        name = 'Isotope Evolutions'
        man = self.application.get_service(SPECTROMETER_PROTOCOL)
        if not man or man.simulation:
            name = '{}(Simulation)'.format(name)

        self.isotope_evolution_pane = IsotopeEvolutionPane(name=name)

        self.experiment_factory_pane = ExperimentFactoryPane(model=self.manager.experiment_factory)
        wait_pane = WaitPane(model=self.manager.executor.wait_group)

        explanation_pane = ExplanationPane()
        explanation_pane.set_colors(self._assemble_state_colors())

        ex = self.manager.executor
        panes = [StatsPane(model=self.manager.stats),
                 ControlsPane(model=ex),
                 ConsolePane(model=ex),
                 LoggerPane(),
                 # AnalysisHealthPane(model=self.analysis_health),
                 ConnectionStatusPane(model=ex),
                 self.experiment_factory_pane,
                 self.isotope_evolution_pane,
                 explanation_pane,
                 wait_pane]

        if self.loading_manager:
            self.load_pane = self.window.application.get_service('pychron.loading.tasks.panes.LoadDockPane')
            self.load_table_pane = self.window.application.get_service('pychron.loading.tasks.panes.LoadTablePane')

            self.load_pane.model = self.loading_manager
            self.load_table_pane.model = self.loading_manager

            panes.extend([self.load_pane,
                          self.load_table_pane])

        panes = self._add_canvas_pane(panes)

        manager = self.application.get_service(IFurnaceManager)
        if manager:
            from pychron.experiment.tasks.experiment_panes import ExperimentFurnacePane
            fpane = ExperimentFurnacePane(model=manager)
            panes.append(fpane)
        # app = self.window.application
        # man = app.get_service('pychron.lasers.laser_managers.ilaser_manager.ILaserManager')
        # if man:
        #     if hasattr(man.stage_manager, 'video'):
        #         from pychron.image.tasks.video_pane import VideoDockPane
        #
        #         video = man.stage_manager.video
        #         man.initialize_video()
        #         pane = VideoDockPane(video=video)
        #         panes.append(pane)
        #
        #     from pychron.lasers.tasks.laser_panes import ClientDockPane
        #     lc = ClientDockPane(model=man)
        #     self.laser_control_client_pane = lc
        #     panes.append(lc)

        return panes

    # private
    def _open_abort(self):
        try:
            self.notifier.close()
        except AttributeError:
            pass

    def _open_file(self, path, **kw):
        if not isinstance(path, (tuple, list)):
            path = (path,)

        manager = self.manager
        # print 'asdfa', manager
        if manager.verify_database_connection(inform=True):
            if manager.load():
                manager.experiment_factory.activate(load_persistence=False)
                for p in path:
                    self.manager.info('Opening experiment {}'.format(p))
                    self._open_experiment(p)

                manager.path = path
                # manager.update_info()
                do_after(1000, manager.update_info)
                return True

    def _open_experiment(self, path):
        name = os.path.basename(path)
        self.info('------------------------------ Open Experiment {} -------------------------------'.format(name))

        reopen_editor = False
        if name.endswith('.rem.txt') or name.endswith('.ex.txt'):
            ps = name.split('.')
            nname = '{}.txt'.format('.'.join(ps[:-2]))
            msg = 'Rename {} as {}'.format(name, nname)
            if self.confirmation_dialog(msg):
                reopen_editor = True
                npath = os.path.join(paths.experiment_dir, nname)

                shutil.copy(path, npath)
                path = npath

        editor = self._check_opened(path)
        if reopen_editor and editor:
            self.close_editor(editor)
            editor = None

        if not editor:
            if path.endswith('.xls'):
                txt, is_uv = self._open_xls(path)
            else:
                txt, is_uv = self._open_txt(path)

            klass = UVExperimentEditor if is_uv else ExperimentEditor
            editor = klass(path=path,
                           application=self.application,
                           automated_runs_editable=self.automated_runs_editable)
            editor.setup_tabular_adapters(self.bgcolor, self.even_bgcolor, self._assemble_state_colors())
            editor.new_queue(txt)
            self._open_editor(editor)
        else:
            self.debug('{} already open. using existing editor'.format(name))
            editor.application = self.application
            self.activate_editor(editor)

        # loading queue editor set dirty
        # clear dirty flag
        editor.dirty = False
        self._show_pane(self.experiment_factory_pane)

    def _open_xls(self, path):
        """
            open the workbook and convert it to text
            construct the text to mimic a normal experiment file
        """
        wb = xlrd.open_workbook(path)
        sh = wb.sheet_by_index(0)
        # write meta
        meta_rows = 7
        rows = []
        is_uv = False
        for r in range(meta_rows):
            attr = sh.cell_value(r, 0)
            v = sh.cell_value(r, 1)
            if attr == 'extract_device':
                is_uv = v == 'Fusions UV'

            rows.append('{}: {}'.format(attr,
                                        v))
        rows.append('#{}'.format('=' * 80))

        header = sh.row_values(meta_rows)
        rows.append('\t'.join(header))
        for r in range(meta_rows + 2, sh.nrows):
            t = '\t'.join(map(str, sh.row_values(r)))
            rows.append(t)

        txt = '\n'.join(map(str, rows))
        return txt, is_uv

    def _open_txt(self, path):
        with open(path, 'r') as rfile:
            txt = rfile.read()

            f = (l for l in txt.split('\n'))
            meta, metastr = extract_meta(f)
            is_uv = False
            if 'extract_device' in meta:
                is_uv = meta['extract_device'] in ('Fusions UV',)

        return txt, is_uv

    def _check_opened(self, path):
        return next((e for e in self.editor_area.editors if e.path == path), None)

    def _set_last_experiment(self, p):
        with open(paths.last_experiment, 'w') as wfile:
            wfile.write(p)
        self.last_experiment_changed = True

        update_launch_history(p)

    def _save_file(self, path):
        if self.active_editor.save(path):
            self.manager.refresh_executable()
            self.debug('queues saved')
            self.manager.reset_run_generator()
            return True

    def _get_save_path(self, default_filename=None, **kw):
        sd = ExperimentSaveDialog(root=paths.experiment_dir,
                                  name=default_filename or 'Untitled')
        info = sd.edit_traits()
        if info.result:
            return sd.path

    def _generate_default_filename(self):
        name = self.active_editor.queue.load_name
        if name:
            if name.startswith('Load'):
                name = name[4:].strip()

            name = name.replace(' ', '_')

            return 'Load{}'.format(name)

    def _publish_notification(self, run):
        if self.use_notifications:
            # msg = 'RunAdded {}'.format(run.uuid)
            # self.info('pushing notification {}'.format(msg))
            self.notifier.send_notification(run.uuid)

    def _prompt_for_save(self):
        """
            Prompt the user to save when exiting.
        """
        if self.manager.executor.is_alive():
            name = self.manager.executor.experiment_queue.name
            result = self._confirmation('{} is running. Are you sure you want to quit?'.format(name))
            if result in (CANCEL, NO):
                return False
            else:
                ret = super(ExperimentEditorTask, self)._prompt_for_save()
                if ret:
                    self.manager.executor.cancel(confirm=False)
                return ret
        else:
            return super(ExperimentEditorTask, self)._prompt_for_save()

    def _backup_editor(self, editor):
        p = editor.path
        p = add_extension(p, '.txt')

        if os.path.isfile(p):
            # make a backup copy of the original experiment file
            bp, pp = backup(p, paths.backup_experiment_dir)
            self.info('{} - saving a backup copy to {}'.format(bp, pp))

    def _close_external_windows(self):
        """
            ask user if ok to close open spectrometer and extraction line windows
        """
        if not self.window:
            return

        # ask user if ok to close windows
        windows = []
        names = []
        # print self.window, self.application
        self.debug('{} calling close_external_windows'.format(id(self)))
        if self.application:
            for wi in self.application.windows:
                wid = wi.active_task.id
                if wid == 'pychron.spectrometer':
                    windows.append(wi)
                    names.append('Spectrometer')
                elif wid == 'pychron.extraction_line':
                    windows.append(wi)
                    names.append('Extraction Line')

        if windows:
            is_are, them = 'is', 'it'
            if len(windows) > 1:
                is_are, them = 'are', 'them'
            msg = '{} {} open. Is it ok to close {}?'.format(','.join(names), is_are, them)

            if self.confirmation_dialog(msg):
                for wi in windows:
                    wi.close()

    # ===============================================================================
    # handlers
    # ===============================================================================
    def _active_editor_changed(self):
        if self.active_editor:
            self.manager.experiment_queue = self.active_editor.queue
            self.manager.executor.active_editor = self.active_editor
            self._show_pane(self.experiment_factory_pane)

    @on_trait_change('manager:executor:auto_save_event')
    def _auto_save(self):
        self.save()

    @on_trait_change('loading_manager:group_positions')
    def _update_group_positions(self, new):
        # if not new:
        # ef = self.manager.experiment_factory
        # rf = ef.run_factory
        #
        #     rf.position = ''
        #
        # else:
        pos = self.loading_manager.selected_positions
        self._update_selected_positions(pos)

    @on_trait_change('loading_manager:selected_positions')
    def _update_selected_positions(self, new):
        ef = self.manager.experiment_factory
        if new:
            ef.selected_positions = new
            rf = ef.run_factory

            nn = new[0]

            rf.selected_irradiation = nn.irradiation
            rf.selected_level = nn.level
            rf.labnumber = nn.labnumber

            # filter rows that dont match the first rows labnumber
            ns = [str(ni.positions[0]) for ni in new
                  if ni.labnumber == nn.labnumber]

            group_positions = self.loading_manager.group_positions
            # group_positions = False
            if group_positions:
                rf.position = ','.join(ns)
            else:
                n = len(ns)
                if n > 1 and abs(int(ns[0]) - int(ns[-1])) == n - 1:
                    rf.position = '{}-{}'.format(ns[0], ns[-1])
                else:
                    rf.position = str(ns[0])

    @on_trait_change('manager:experiment_factory:extract_device')
    def _handle_extract_device(self, new):
        if new:
            if self.window:
                app = self.window.application
                ed = convert_extract_device(new)
                man = app.get_service(ILaserManager, 'name=="{}"'.format(ed))
                if man:
                    if self.laser_control_client_pane:
                        self.laser_control_client_pane.model = man

        if new == 'Fusions UV':
            if self.active_editor and not isinstance(self.active_editor, UVExperimentEditor):
                editor = UVExperimentEditor()

                ms = self.manager.experiment_factory.queue_factory.mass_spectrometer
                editor.new_queue(mass_spectrometer=ms)
                editor.dirty = False

                # print self.active_editor
                # ask user to copy runs into the new editor
                ans = self.active_editor.queue.cleaned_automated_runs
                if ans:
                    if self.confirmation_dialog('Copy runs to the new UV Editor?'):
                        # editor.queue.executed_runs=self.active_editor.queue.executed_runs
                        editor.queue.automated_runs = self.active_editor.queue.automated_runs

                        # self.warning_dialog('Copying runs not yet implemented')

                self.active_editor.close()

                self._open_editor(editor)
                if not self.manager.executor.is_alive():
                    self.manager.executor.executable = False

    @on_trait_change('manager:experiment_factory:queue_factory:load_name')
    def _update_load(self, new):
        lm = self.loading_manager
        if lm is not None:
            lm.suppress_update = True
            lm.load_name = ''
            lm.load_name = new
            lm.suppress_update = False

            lm.load_load_by_name(new, group_labnumbers=False)
            lm.canvas.editable = False

    @on_trait_change('active_editor:queue:refresh_blocks_needed')
    def _update_blocks(self):
        self.manager.experiment_factory.run_factory.load_run_blocks()

    @on_trait_change('editor_area:editors[]')
    def _update_editors(self, new):
        self.manager.experiment_queues = [ei.queue for ei in new]

    @on_trait_change('manager:executor:measuring_run:plot_panel')
    def _update_plot_panel(self, new):
        if new is not None:
            self.isotope_evolution_pane.plot_panel = new

    @on_trait_change('manager:executor:console_updated')
    def _update_console(self, new):
        if self.use_notifications:
            self.notifier.send_console_message(new)

            # if self.use_syslogger:
            # self.syslogger.executor = self.manager.executor
            #     self.syslogger.trigger(new)

    @on_trait_change('manager:executor:run_completed')
    def _update_run_completed(self, new):
        self._publish_notification(new)

        load_name = self.manager.executor.experiment_queue.load_name
        if load_name:
            self._update_load(load_name)

    @on_trait_change('manager:add_queues_flag')
    def _add_queues(self, new):
        self.debug('add_queues_flag trigger n={}'.format(self.manager.add_queues_count))
        for _i in range(new):
            self.new()

    @on_trait_change('manager:activate_editor_event')
    def _set_active_editor(self, new):
        for ei in self.editor_area.editors:
            if id(ei.queue) == new:
                try:
                    self.editor_area.activate_editor(ei)
                except AttributeError:
                    pass
                break

    @on_trait_change('manager:execute_event')
    def _execute(self, obj, name, old, new):
        self.debug('execute event {} {}'.format(id(self), id(obj)))

        if self.editor_area.editors:
            try:
                # this method is error prone. just wrap in a try statement for now
                self._close_external_windows()
            except AttributeError:
                pass

            # bind the window control to the notification manager
            # if self.window:
            #     self.manager.executor.notification_manager.parent = self.window.control

            for ei in self.editor_area.editors:
                self._backup_editor(ei)

            qs = [ei.queue for ei in self.editor_area.editors
                  if ei != self.active_editor]

            if self.active_editor:
                qs.insert(0, self.active_editor.queue)

            if self.manager.execute_queues(qs):
                # self._show_pane(self.wait_pane)
                self._set_last_experiment(self.active_editor.path)
            else:
                self.warning('experiment queue did not start properly')

    @on_trait_change('manager:executor:autoplot_event')
    def _handle_autoplot(self, new):
        if new:
            editor = self._new_autoplot_editor(new)
            ans = self._get_autoplot_analyses(new)
            editor.set_items(ans)

            self._open_editor(editor)

            fs = [e for e in self.iter_editors(FigureEditor)]

            # close the oldest editor
            if len(fs) > 5:
                fs = sorted(fs, key=lambda x: x.last_update)
                self.close_editor(fs[0])

    def _get_autoplot_analyses(self, new):
        dvc = self.window.application.get_service('pychron.dvc.dvc.DVC')
        db = dvc.db
        ans, _ = db.get_labnumber_analyses(new.identifier)
        return dvc.make_analyses(ans)

    def _new_autoplot_editor(self, new):
        from pychron.pipeline.plot.editors.figure_editor import FigureEditor

        for editor in self.editor_area.editors:
            if isinstance(editor, FigureEditor):
                if new.identifier == editor.identifier:
                    break
        else:
            if is_special(new.identifier):
                from pychron.pipeline.plot.editors.series_editor import SeriesEditor

                editor = SeriesEditor()
            elif new.step:
                from pychron.pipeline.plot.editors.spectrum_editor import SpectrumEditor

                editor = SpectrumEditor()
            else:
                from pychron.pipeline.plot.editors.ideogram_editor import IdeogramEditor

                editor = IdeogramEditor()

            editor.identifier = new.identifier

        editor.last_update = time.time()
        return editor

    @on_trait_change('manager:executor:[measuring,extracting]')
    def _handle_measuring(self, name, new):
        if new:
            if name == 'measuring':
                self._show_pane(self.isotope_evolution_pane)
                # elif name == 'extracting':
                #     self._show_pane(self.wait_pane)

    @on_trait_change('active_editor:queue:dclicked')
    def _edit_event(self):
        p = self.experiment_factory_pane
        self._show_pane(p)

    @on_trait_change('manager:[save_event, executor:auto_save_event]')
    def _save_queue(self):
        self.save()

    @on_trait_change('active_editor:dirty')
    def _update_active_editor_dirty(self, new):
        if new and self.manager:
            self.manager.executor.executable = False
            # if self.active_editor:
            #     if self.active_editor.dirty:
            #         if self.manager:
            #             self.manager.executor.executable = False

    # ===============================================================================
    # default/factory
    # ===============================================================================
    def _manager_default(self):
        from pychron.envisage.initialization.initialization_parser import \
            InitializationParser

        ip = InitializationParser()
        plugin = ip.get_plugin('Experiment', category='general')
        mode = ip.get_parameter(plugin, 'mode')

        proto = 'pychron.database.isotope_database_manager.IsotopeDatabaseManager'
        iso_db_man = self.application.get_service(proto)
        # experimentor.iso_db_man = iso_db_man

        proto = 'pychron.dvc.dvc.DVC'
        dvc = self.application.get_service(proto)
        # experimentor.dvc = dvc

        experimentor = Experimentor(application=self.application,
                                    mode=mode, dvc=dvc, iso_db_man=iso_db_man)

        experimentor.executor.set_managers()
        experimentor.executor.bind_preferences()

        return experimentor

    def _pattern_maker_view_factory(self):
        from pychron.lasers.pattern.pattern_maker_view import PatternMakerView

        return PatternMakerView()

    def _loading_manager_default(self):
        lm = self.window.application.get_service('pychron.loading.loading_manager.LoadingManager')
        if lm:
            dvc = self.window.application.get_service('pychron.dvc.dvc.DVC')
            lm.trait_set(db=dvc.db,
                         show_group_positions=True)
            return lm

    def _default_directory_default(self):
        return paths.experiment_dir

    def _default_layout_default(self):
        return TaskLayout(
            left=Splitter(
                PaneItem('pychron.wait', height=100),
                Tabbed(
                    PaneItem('pychron.experiment.factory'),
                    PaneItem('pychron.experiment.isotope_evolution')),
                orientation='vertical'),
            right=Splitter(
                Tabbed(
                    PaneItem('pychron.experiment.stats'),
                    PaneItem('pychron.console', height=425),
                    PaneItem('pychron.experiment.explanation', height=425),
                    PaneItem('pychron.experiment.connection_status')),
                PaneItem('pychron.extraction_line.canvas_dock'),
                orientation='vertical'),
            top=PaneItem('pychron.experiment.controls'))

        # ============= EOF =============================================
        # def _use_syslogger_changed(self):
        # if self.use_syslogger:
        # from pychron.experiment.sys_log import SysLogger
        #
        # prefid = 'pychron.syslogger'
        # self.syslogger = SysLogger()
        # for attr in ('username', 'password', 'host'):
        # bind_preference(self.syslogger, attr, '{}.{}'.format(prefid, attr))
        # @on_trait_change('source_pane:[selected_connection, source:+]')
        # def _update_source(self, name, new):
        # from pychron.image.video_source import parse_url
        #
        # if name == 'selected_connection':
        # islocal, r = parse_url(new)
        # if islocal:
        #             pass
        #         else:
        #             self.source_pane.source.host = r[0]
        #             self.source_pane.source.port = r[1]
        #     else:
        #         url = self.source_pane.source.url()
        #
        #         self.video_source.set_url(url)

        # self._testing()
        #
        # def _testing(self):
        #     editor=self.active_editor
        #     queue = editor.queue
        #     editor.executed = True
        #     queue.executed_runs = queue.automated_runs[:]
        #     for i,ei in enumerate(queue.executed_runs):
        #         ei.aliquot = i+1
        #
        #     queue.automated_runs = []#queue.automated_runs[2:]

        # def open(self, path=None):
        # self.manager.experiment_factory.activate(load_persistence=False)
        #
        # if not os.path.isfile(path):
        # path = None
        #
        # if path is None:
        #     ps = self.open_file_dialog(action='open files',
        #                                default_filename='Current Experiment.txt')
        # else:
        #     ps = (path,)
        #
        # if ps:
        #     manager = self.manager
        #     if manager.verify_database_connection(inform=True):
        #         if manager.load():
        #             for path in ps:
        #                 self.manager.info('Opening experiment {}'.format(path))
        #                 self._open_experiment(path)
        #
        #             manager.path = path
        #             manager.update_info()
        #
        #     return True
        # else:
        #     self.notifier.close()
