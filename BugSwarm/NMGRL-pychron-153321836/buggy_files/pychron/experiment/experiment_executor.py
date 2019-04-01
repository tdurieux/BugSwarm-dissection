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
from apptools.preferences.preference_binding import bind_preference
from pyface.constant import CANCEL, YES, NO
from pyface.timer.do_later import do_after
from traits.api import Event, Button, String, Bool, Enum, Property, Instance, Int, List, Any, Color, Dict, \
    on_trait_change, Long, Float, Str
from traits.trait_errors import TraitError

# ============= standard library imports ========================
from threading import Thread, Event as Flag, Lock, currentThread
from datetime import datetime
from itertools import groupby
import time
import os
import yaml
# ============= local library imports  ==========================
from pychron.consumer_mixin import consumable
from pychron.core.codetools.memory_usage import mem_available
from pychron.core.helpers.filetools import add_extension, get_path
from pychron.core.notification_manager import NotificationManager
from pychron.core.progress import open_progress
from pychron.core.ui.gui import invoke_in_main_thread
from pychron.core.ui.led_editor import LED
from pychron.envisage.consoleable import Consoleable
from pychron.envisage.preference_mixin import PreferenceMixin
from pychron.envisage.view_util import open_view
from pychron.experiment.automated_run.persistence import ExcelPersister
from pychron.experiment.conditional.conditional import conditionals_from_file
from pychron.experiment.conflict_resolver import ConflictResolver
from pychron.experiment.datahub import Datahub
from pychron.experiment.health.series import SystemHealthSeries
from pychron.experiment.notifier.user_notifier import UserNotifier
from pychron.experiment.stats import StatsGroup
from pychron.experiment.utilities.conditionals import test_queue_conditionals_name, SYSTEM, QUEUE, RUN, \
    CONDITIONAL_GROUP_TAGS
from pychron.experiment.utilities.conditionals_results import reset_conditional_results
from pychron.experiment.utilities.repository_identifier import retroactive_repository_identifiers, \
    populate_repository_identifiers, get_curtag
from pychron.experiment.utilities.identifier import convert_extract_device, is_special
from pychron.extraction_line.ipyscript_runner import IPyScriptRunner
from pychron.globals import globalv
from pychron.paths import paths
from pychron.pychron_constants import DEFAULT_INTEGRATION_TIME, LINE_STR
from pychron.wait.wait_group import WaitGroup


def remove_backup(uuid_str):
    """
        remove uuid from backup recovery file
    """
    with open(paths.backup_recovery_file, 'r') as rfile:
        r = rfile.read()

    r = r.replace('{}\n'.format(uuid_str), '')
    with open(paths.backup_recovery_file, 'w') as wfile:
        wfile.write(r)


class ExperimentExecutor(Consoleable, PreferenceMixin):
    """
    ExperimentExecutor coordinates execution of an experiment queue

    """
    experiment_queues = List
    experiment_queue = Any
    user_notifier = Instance(UserNotifier, ())
    connectables = List
    active_editor = Any
    console_bgcolor = 'black'
    selected_run = Instance('pychron.experiment.automated_run.spec.AutomatedRunSpec', )
    autoplot_event = Event
    run_completed = Event

    # ===========================================================================
    # control
    # ===========================================================================
    show_conditionals_button = Button('Show Conditionals')
    start_button = Event
    stop_button = Event
    can_start = Property(depends_on='executable, _alive')
    executing_led = Instance(LED, ())
    delaying_between_runs = Bool

    extraction_state_label = String
    extraction_state_color = Color

    end_at_run_completion = Bool(False)
    abort_run_button = Button('Abort Run')

    truncate_button = Button('Truncate Run')
    truncate_style = Enum('Normal', 'Quick')
    '''
        immediate 0= measure_iteration stopped at current step, script continues
        quick     1= measure_iteration stopped at current step, script continues using 0.25*counts

        old-style
            immediate 0= is the standard truncation, measure_iteration stopped at current step and measurement_script
                         truncated
            quick     1= the current measure_iteration is truncated and a quick baseline is collected, peak center?
            next_int. 2= same as setting ncounts to < current step. measure_iteration is truncated but script continues
    '''
    # ===========================================================================
    #
    # ===========================================================================

    wait_group = Instance(WaitGroup, ())
    stats = Instance(StatsGroup)

    spectrometer_manager = Any
    extraction_line_manager = Any
    ion_optics_manager = Any

    pyscript_runner = Instance(IPyScriptRunner)
    monitor = Instance('pychron.monitors.automated_run_monitor.AutomatedRunMonitor')
    system_health = Instance(SystemHealthSeries)

    measuring_run = Instance('pychron.experiment.automated_run.automated_run.AutomatedRun')
    extracting_run = Instance('pychron.experiment.automated_run.automated_run.AutomatedRun')

    datahub = Instance(Datahub)
    labspy_client = Instance('pychron.labspy.client.LabspyClient')
    dashboard_client = Instance('pychron.dashboard.client.DashboardClient')
    # ===========================================================================
    #
    # ===========================================================================
    queue_modified = False

    executable = Bool
    measuring = Bool(False)
    extracting = Bool(False)

    mode = 'normal'
    # ===========================================================================
    # preferences
    # ===========================================================================
    auto_save_delay = Int(30)
    use_auto_save = Bool(True)
    use_labspy = Bool
    use_dashboard_client = Bool
    min_ms_pumptime = Int(30)
    use_automated_run_monitor = Bool(False)
    use_system_health = Bool(False)
    set_integration_time_on_start = Bool(False)
    send_config_before_run = Bool(False)
    default_integration_time = Float(DEFAULT_INTEGRATION_TIME)
    use_memory_check = Bool(True)
    memory_threshold = Int
    use_dvc = Bool(False)
    use_autoplot = Bool(False)
    monitor_name = 'FC-2'

    use_xls_persistence = Bool(False)
    use_db_persistence = Bool(True)

    # dvc
    use_dvc_persistence = Bool(False)
    dvc_username = Str
    dvc_password = Str
    dvc_organization = Str
    default_principal_investigator = Str

    baseline_color = Color
    sniff_color = Color
    signal_color = Color

    alive = Bool(False)
    _canceled = False
    _state_thread = None
    _aborted = False

    _end_flag = None
    _complete_flag = None

    _prev_blanks = Dict
    _prev_baselines = Dict
    _prev_blank_runid = String
    _err_message = String
    _prev_blank_id = Long

    _cv_info = None
    _cached_runs = List
    _active_repository_identifier = Str

    def __init__(self, *args, **kw):
        super(ExperimentExecutor, self).__init__(*args, **kw)
        self.wait_control_lock = Lock()
        # self.set_managers()
        self.notification_manager = NotificationManager()

    def set_managers(self, prog=None):
        p1 = 'pychron.extraction_line.extraction_line_manager.ExtractionLineManager'
        p2 = 'pychron.spectrometer.base_spectrometer_manager.BaseSpectrometerManager'
        p3 = 'pychron.spectrometer.ion_optics.ion_optics_manager.IonOpticsManager'
        if self.application:
            if prog:
                prog.change_message('Setting Spectrometer')
            self.spectrometer_manager = self.application.get_service(p2)
            if self.spectrometer_manager is None:
                self.warning_dialog('Spectrometer Plugin is required for Experiment')
                return
            self.ion_optics_manager = self.application.get_service(p3)

            if prog:
                prog.change_message('Setting Extraction Line')
            self.extraction_line_manager = self.application.get_service(p1)
            if self.extraction_line_manager is None:
                self.warning_dialog('Extraction Line Plugin is required for Experiment')
                return

        dh = self.datahub
        dh.mainstore = self.application.get_service('pychron.dvc.dvc.DVC')
        dh.mainstore.precedence = 1

        return True

    def bind_preferences(self):
        self.datahub.bind_preferences()

        prefid = 'pychron.experiment'

        attrs = ('use_auto_save',
                 'use_autoplot',
                 'auto_save_delay',
                 'use_labspy',
                 'min_ms_pumptime',
                 'set_integration_time_on_start',
                 'send_config_before_run',
                 'default_integration_time',
                 'use_dvc_persistence',
                 'use_xls_persistence',
                 'use_db_persistence')
        self._preference_binder(prefid, attrs)

        if self.use_dvc_persistence:
            bind_preference(self, 'dvc_organization', 'pychron.dvc.organization')
            bind_preference(self, 'dvc_password', 'pychron.dvc.github_password')
            bind_preference(self, 'dvc_username', 'pychron.dvc.github_username')

        if self.use_labspy:
            client = self.application.get_service('pychron.labspy.client.LabspyClient')
            self.labspy_client = client

        # system health
        self._preference_binder(prefid, ('use_system_health',))

        # colors
        attrs = ('signal_color', 'sniff_color', 'baseline_color')
        self._preference_binder(prefid, attrs, mod='color')

        # user_notifier
        attrs = ('include_log',)
        self._preference_binder(prefid, attrs, obj=self.user_notifier)

        emailer = self.application.get_service('pychron.social.email.emailer.Emailer')
        self.user_notifier.emailer = emailer

        # memory
        attrs = ('use_memory_check', 'memory_threshold')
        self._preference_binder(prefid, attrs)

        # console
        self.console_bind_preferences(prefid)
        self._preference_binder(prefid, ('use_message_colormapping',))

        # dashboard
        self._preference_binder('pychron.dashboard.client', ('use_dashboard_client',))
        if self.use_dashboard_client:
            self.dashboard_client = self.application.get_service('pychron.dashboard.client.DashboardClient')

        # general
        self._preference_binder('pychron.general', ('default_principal_investigator',))

    def execute(self):

        if self.user_notifier.emailer is None:
            if any((eq.use_email or eq.use_group_email for eq in self.experiment_queues)):
                if not self.confirmation_dialog('Email Plugin not initialized. '
                                                'Required for sending email notifications. '
                                                'Are you sure you want to continue?'):
                    return
        prog = open_progress(100, position=(100, 100))

        if self._pre_execute_check(prog):
            self.info('pre execute check successful')
            prog.close()
            # reset executor
            self._reset()

            name = self.experiment_queue.name

            msg = 'Starting Execution "{}"'.format(name)
            self.heading(msg)

            self._aborted = False
            self._canceled = False
            self.extraction_state_label = ''

            self.experiment_queue.executed = True
            self.alive = True
            t = Thread(name='execute_exp',
                       target=self._execute)
            t.start()
            return t
        else:
            prog.close()
            self.alive = False
            self.info('pre execute check failed')

    def set_queue_modified(self):
        self.queue_modified = True

    def get_prev_baselines(self):
        return self._prev_baselines

    def get_prev_blanks(self):
        return self._prev_blank_id, self._prev_blanks, self._prev_blank_runid

    def is_alive(self):
        return self.alive

    def continued(self):
        self.stats.continue_run()

    def cancel(self, *args, **kw):
        self._cancel(*args, **kw)

    def set_extract_state(self, state, flash=0.75, color='green', period=1.5):
        self._set_extract_state(state, flash, color, period)

    def wait(self, t, msg=''):
        self._wait(t, msg)

    def get_wait_control(self):
        with self.wait_control_lock:
            wd = self.wait_group.active_control
            if wd.is_active():
                wd = self.wait_group.add_control()
        return wd

    def stop(self):
        if self.delaying_between_runs:
            self.alive = False
            self.stats.stop_timer()
            self.wait_group.stop()
            # self.wait_group.active_control.stop
            # self.active_wait_control.stop()
            # self.wait_dialog.stop()

            msg = '{} Stopped'.format(self.experiment_queue.name)
            self._set_message(msg, color='orange')
        else:
            self.cancel()

    def experiment_blob(self):
        path = self.experiment_queue.path
        path = add_extension(path, '.txt')
        if os.path.isfile(path):
            with open(path, 'r') as rfile:
                return '{}\n{}'.format(path,
                                       rfile.read())
        else:
            self.warning('{} is not a valid file'.format(path))

    def show_conditionals(self, *args, **kw):
        invoke_in_main_thread(self._show_conditionals, *args, **kw)

    # ===============================================================================
    # private
    # ===============================================================================
    def _reset(self):
        self.alive = True
        self._canceled = False
        self._aborted = False

        self._err_message = ''
        self.end_at_run_completion = False
        self.extraction_state_label = ''
        self.experiment_queue.executed = True

        if self.stats:
            self.stats.reset()
            self.stats.start_timer()

    def _wait_for_save(self):
        """
            wait for experiment queue to be saved.

            actually wait until time out or self.executable==True
            executable set higher up by the Experimentor

            if timed out auto save or cancel

        """
        st = time.time()
        delay = self.auto_save_delay
        auto_save = self.use_auto_save

        if not self.executable:
            self.info('Waiting for save')
            cnt = 0

            while not self.executable:
                time.sleep(1)
                if time.time() - st < delay and self.is_alive():
                    self.set_extract_state('Waiting for save. Autosave in {} s'.format(delay - cnt),
                                           flash=False)
                    cnt += 1
                else:
                    break

            if not self.executable:
                self.info('Timed out waiting for user input')
                if auto_save:
                    self.info('autosaving experiment queues')
                    self.set_extract_state('')
                    self.auto_save_event = True
                else:
                    self.info('canceling experiment queues')
                    self.cancel(confirm=False)

    def _execute(self):
        """
            execute opened experiment queues
        """
        # delay before starting
        exp = self.experiment_queue
        delay = exp.delay_before_analyses
        self._delay(delay, message='before')

        for i, exp in enumerate(self.experiment_queues):
            if self.is_alive():
                if self._pre_queue_check(exp):
                    break

                self._execute_queue(i, exp)
            else:
                self.debug('No alive. not starting {},{}'.format(i, exp.name))

            if self.end_at_run_completion:
                self.debug('Previous queue ended at completion. Not continuing to other opened experiments')
                break

        self.alive = False

    def _execute_queue(self, i, exp):
        """
            i: int
            exp: ExperimentQueue

            execute experiment queue ``exp``
        """
        self.experiment_queue = exp
        self.info('Starting automated runs set={:02d} {}'.format(i, exp.name))

        # save experiment to database
        self.info('saving experiment "{}" to database'.format(exp.name))
        exp.start_timestamp = datetime.now()  # .strftime('%m-%d-%Y %H:%M:%S')
        if self.labspy_enabled:
            self.labspy_client.add_experiment(exp)

        # self.datahub.add_experiment(exp)

        # reset conditionals result file
        reset_conditional_results()

        exp.executed = True
        # scroll to the first run
        exp.automated_runs_scroll_to_row = 0

        last_runid = None

        rgen, nruns = exp.new_runs_generator()

        cnt = 0
        total_cnt = 0
        is_first_flag = True
        is_first_analysis = True

        # from pympler import classtracker
        # tr = classtracker.ClassTracker()
        # from pychron.experiment.automated_run.automated_run import AutomatedRun
        # tr.track_class(AutomatedRun)
        # tr.track_class(AutomatedRunPersister)
        # tr.create_snapshot()
        # self.tracker = tr

        with consumable(func=self._overlapped_run) as con:
            while 1:
                if not self.is_alive():
                    self.debug('executor not alive')
                    break

                if self.queue_modified:
                    self.debug('Queue modified. making new run generator')
                    rgen, nruns = exp.new_runs_generator()
                    cnt = 0
                    self.queue_modified = False

                try:
                    spec = rgen.next()
                except StopIteration:
                    self.debug('stop iteration')
                    break

                if spec.skip:
                    self.debug('caught a skipped run {}'.format(spec.runid))
                    continue

                if self._pre_run_check(spec):
                    self.warning('pre run check failed')
                    break

                self.ms_pumptime_start = None
                # overlapping = self.current_run and self.current_run.isAlive()
                overlapping = self.measuring_run and self.measuring_run.is_alive()
                if not overlapping:
                    if self.is_alive() and cnt < nruns and not is_first_analysis:
                        # delay between runs
                        self._delay(exp.delay_between_analyses)
                        if not self.is_alive():
                            self.debug('User Cancel between runs')
                            break

                    else:
                        self.debug('not delaying between runs isAlive={}, '
                                   'cnts<nruns={}, is_first_analysis={}'.format(self.is_alive(),
                                                                                cnt < nruns, is_first_analysis))

                run = self._make_run(spec)
                if run is None:
                    self.debug('failed to make run')
                    break

                self.wait_group.active_control.page_name = run.runid
                run.is_first = is_first_flag

                if not run.is_last and run.spec.analysis_type == 'unknown' and spec.overlap[0]:
                    self.debug('waiting for extracting_run to finish')
                    self._wait_for(lambda x: self.extracting_run)

                    self.info('overlaping')

                    t = Thread(target=self._do_run, args=(run,),
                               name=run.runid)
                    t.start()

                    run.wait_for_overlap()
                    is_first_flag = False

                    self.debug('overlap finished. starting next run')

                    con.add_consumable((t, run))
                else:
                    is_first_flag = True
                    last_runid = run.runid
                    self._join_run(spec, run)

                # self.tracker.stats.print_summary()

                cnt += 1
                total_cnt += 1
                is_first_analysis = False
                if self.end_at_run_completion:
                    self.debug('end at run completion')
                    break

            self.debug('run loop exited. end at completion:{}'.format(self.end_at_run_completion))
            if self.end_at_run_completion:
                # if overlapping run is a special labnumber cancel it and finish experiment
                if self.extracting_run:
                    if not self.extracting_run.spec.is_special():
                        self._wait_for(lambda x: self.extracting_run)
                    else:
                        self.extracting_run.cancel_run()

                # wait for the measurement run to finish
                self._wait_for(lambda x: self.measuring_run)

            else:
                # wait for overlapped runs to finish.
                self._wait_for(lambda x: self.extracting_run or self.measuring_run)

        if self._err_message:
            self.warning('automated runs did not complete successfully')
            self.warning('error: {}'.format(self._err_message))

        self._end_runs()
        if last_runid:
            self.info('Automated runs ended at {}, runs executed={}'.format(last_runid, total_cnt))

        self.heading('experiment queue {} finished'.format(exp.name))

        if not self._err_message and self.end_at_run_completion:
            self._err_message = 'User terminated'

        if exp.use_email:
            self.info('Notifying user={} email={}'.format(exp.username, exp.email))
            self.user_notifier.notify(exp, last_runid, self._err_message)

        if exp.use_group_email:
            names, addrs = self._get_group_emails(exp.email)
            if names:
                self.info('Notifying user group names={}'.format(','.join(names)))
                self.user_notifier.notify_group(exp, last_runid, self._err_message, addrs)

        if self.labspy_enabled:
            self.labspy_client.update_experiment(exp, self._err_message)

    def _get_group_emails(self, email):
        names, addrs = None, None
        path = os.path.join(paths.setup_dir, 'users.yaml')
        if os.path.isfile(path):
            with open(path, 'r') as rfile:
                yl = yaml.load(rfile)

                items = [(i['name'], i['email']) for i in yl if i['enabled'] and i['email'] != email]
            if items:
                names, addrs = zip(*items)
        return names, addrs

    def _wait_for(self, predicate, period=1, invert=False):
        """
            predicate: callable. func(x)
            period: evaluate predicate every ``period`` seconds
            invert: bool invert predicate logic

            wait until predicate evaluates to False
            if invert is True wait until predicate evaluates to True
        """
        self.debug('waiting for')
        st = time.time()
        if invert:
            predicate = lambda x: not predicate(x)

        while 1:
            et = time.time() - st
            if not self.alive:
                break
            if not predicate(et):
                break
            time.sleep(period)

    def _join_run(self, spec, run):
        # def _join_run(self, spec, t, run):
        # t.join()
        self.debug('Changing Thread name to {}'.format(run.runid))
        ct = currentThread()
        ct.name = run.runid

        self.debug('join run')
        self._do_run(run)

        self.debug('{} finished'.format(run.runid))
        if self.is_alive():
            self.debug('spec analysis type {}'.format(spec.analysis_type))
            if spec.analysis_type.startswith('blank'):
                pb = run.get_baseline_corrected_signals()
                if pb is not None:
                    self._prev_blank_runid = run.spec.runid
                    # self._prev_blank_id = run.spec.analysis_dbid
                    self._prev_blanks = pb
                    self.debug('previous blanks ={}'.format(pb))

        self._report_execution_state(run)

        invoke_in_main_thread(run.teardown)

        # do_after(1000, run.teardown)
        # run.teardown()
        # t = Timer(1, run.teardown)
        # t.start()

        self.measuring_run = None
        self.debug('join run finished')

    def _do_run(self, run):
        st = time.time()

        self.debug('do run')

        if self.stats:
            self.stats.start_run(run)

        run.spec.state = 'not run'

        q = self.experiment_queue
        # is this the last run in the queue. queue is not empty until _start runs so n==1 means last run
        run.is_last = len(q.cleaned_automated_runs) == 1

        self.extracting_run = run

        for step in ('_start',
                     '_extraction',
                     '_measurement',
                     '_post_measurement'):

            if not self.is_alive():
                break

            if self._aborted:
                break

            if self.monitor and self.monitor.has_fatal_error():
                run.cancel_run()
                run.spec.state = 'failed'
                break

            f = getattr(self, step)
            if not f(run):
                self.warning('{} did not complete successfully'.format(step[1:]))
                run.spec.state = 'failed'
                break
        else:
            self.debug('$$$$$$$$$$$$$$$$$$$$ state at run end {}'.format(run.spec.state))
            if run.spec.state not in ('truncated', 'canceled', 'failed'):
                run.spec.state = 'success'

        if run.spec.state in ('success', 'truncated'):
            run.save()
            self.run_completed = run

        remove_backup(run.uuid)

        # check to see if action should be taken
        if run.spec.state not in ('canceled', 'failed'):
            if self._post_run_check(run):
                self._err_message = 'Post Run Check Failed'
                self.warning('post run check failed')
            else:
                self.heading('Post Run Check Passed')

        t = time.time() - st
        self.info('Automated run {} {} duration: {:0.3f} s'.format(run.runid, run.spec.state, t))

        run.finish()
        if run.spec.state not in ('canceled', 'failed', 'aborted'):
            self._retroactive_repository_identifiers(run.spec)

        if self.use_autoplot:
            self.autoplot_event = run

        self.wait_group.pop()
        if self.labspy_enabled:
            self.labspy_client.add_run(run, self.experiment_queue)

        if self.use_system_health:
            self._add_system_health(run)

        # mem_log('end run')
        if self.stats:
            self.stats.finish_run()
            if run.spec.state == 'success':
                self.stats.update_run_duration(run, t)
                self.stats.recalculate_etf()

        # write rem and ex queues
        self._write_rem_ex_experiment_queues()

        # close conditionals view
        self._close_cv()

    def _close_cv(self):
        if self._cv_info:
            try:
                self._cv_info.control.close()
            except (AttributeError, ValueError, TypeError):
                pass
                # window could already be closed

    def _write_rem_ex_experiment_queues(self):
        self.debug('write rem/ex queues')
        q = self.experiment_queue
        for runs, tag in ((q.automated_runs, 'rem'),
                          (q.executed_runs, 'ex')):
            path = os.path.join(paths.experiment_rem_dir, '{}.{}.txt'.format(q.name, tag))
            self.debug(path)
            with open(path, 'w') as wfile:
                q.dump(wfile, runs=runs)

    def _overlapped_run(self, v):
        self._overlapping = True
        t, run = v
        # while t.is_alive():
        # time.sleep(1)
        self.debug('OVERLAPPING. waiting for run to finish')
        t.join()

        self.debug('{} finished'.format(run.runid))
        if run.analysis_type.startswith('blank'):
            pb = run.get_baseline_corrected_signals()
            if pb is not None:
                # self._prev_blank_id = run.spec.analysis_dbid
                self._prev_blanks = pb
        self._report_execution_state(run)
        # run.teardown()
        do_after(1000, run.teardown)

    def _abort_run(self):
        self.debug('Abort Run')
        self.set_extract_state(False)
        self.wait_group.stop()

        self._aborted = True
        for arun in (self.measuring_run, self.extracting_run):
            if arun:
                arun.abort_run()

    def _cancel(self, style='queue', cancel_run=False, msg=None, confirm=True, err=None):
        self.debug('_cancel')
        aruns = (self.measuring_run, self.extracting_run)

        if style == 'queue':
            name = os.path.basename(self.experiment_queue.path)
            name, _ = os.path.splitext(name)
        else:
            name = aruns[0].runid

        if name:
            ret = YES
            if confirm:
                m = '"{}" is in progress. Are you sure you want to cancel'.format(name)
                if msg:
                    m = '{}\n{}'.format(m, msg)

                ret = self.confirmation_dialog(m,
                                               title='Confirm Cancel',
                                               return_retval=True,
                                               timeout=30)

            if ret == YES:
                # stop queue
                if style == 'queue':
                    self.alive = False
                    self.debug('Queue cancel. stop timer')
                    self.stats.stop_timer()

                self.set_extract_state(False)
                self.wait_group.stop()
                self._canceled = True
                for arun in aruns:
                    if arun:
                        if style == 'queue':
                            state = None
                            if cancel_run:
                                state = 'canceled'
                        else:
                            state = 'canceled'
                            # arun.aliquot = 0

                        arun.cancel_run(state=state)

                # self.debug('&&&&&&& Clearing runs')
                # self.measuring_run = None
                # self.extracting_run = None
                if err is None:
                    err = 'User Canceled'
                self._err_message = err

    def _end_runs(self):
        self.debug('End Runs. stats={}'.format(self.stats))
        # self._last_ran = None
        if self.stats:
            self.stats.stop_timer()

        # self.db.close()
        self.set_extract_state(False)
        # self.extraction_state = False
        # def _set_extraction_state():
        if self.end_at_run_completion:
            c = 'orange'
            msg = 'Stopped'
        else:
            if self._canceled:
                c = 'red'
                msg = 'Canceled'
            else:
                c = 'green'
                msg = 'Finished'

        n = self.experiment_queue.name
        msg = '{} {}'.format(n, msg)
        self._set_message(msg, c)

        invoke_in_main_thread(self._show_shareables)

    def _show_shareables(self):
        if self.use_dvc_persistence:
            from pychron.dvc.share import PushExperimentsModel
            from pychron.dvc.share import PushExperimentsView
            username = self.dvc_username
            password = self.dvc_password
            org = self.dvc_organization
            pm = PushExperimentsModel(org, username, password)
            if pm.shareables:
                if self.confirmation_dialog('You have shareable Experiments. Would you like to examine them?'):
                    pv = PushExperimentsView(model=pm)
                    open_view(pv)

    def _show_conditionals(self, active_run=None, tripped=None, kind='livemodal'):
        try:
            # if self._cv_info:
            #     if self._cv_info.control:
            #         self._cv_info.control.raise_()
            #         return

            from pychron.experiment.conditional.conditionals_view import ConditionalsView

            v = ConditionalsView()

            v.add_pre_run_terminations(self._load_system_conditionals('pre_run_terminations'))
            v.add_pre_run_terminations(self._load_queue_conditionals('pre_run_terminations'))

            v.add_system_conditionals(self._load_system_conditionals(None))
            v.add_conditionals(self._load_queue_conditionals(None))

            v.add_post_run_terminations(self._load_system_conditionals('post_run_terminations'))
            v.add_post_run_terminations(self._load_queue_conditionals('post_run_terminations'))
            self.debug('Show conditionals active run: {}'.format(active_run))
            self.debug('Show conditionals measuring run: {}'.format(self.measuring_run))
            self.debug('active_run same as measuring_run: {}'.format(self.measuring_run == active_run))
            if active_run:
                v.add_conditionals({'{}s'.format(tag): getattr(active_run, '{}_conditionals'.format(tag))
                                    for tag in CONDITIONAL_GROUP_TAGS}, level=RUN)
                v.title = '{} ({})'.format(v.title, active_run.runid)
            else:
                run = self.selected_run
                if run:
                    # in this case run is an instance of AutomatedRunSpec
                    p = get_path(paths.conditionals_dir, self.selected_run.conditionals, ['.yaml', '.yml'])
                    if p:
                        v.add_conditionals(conditionals_from_file(p, level=RUN))

                    if run.aliquot:
                        runid = run.runid
                    else:
                        runid = run.identifier

                    if run.position:
                        id2 = 'position={}'.format(run.position)
                    else:
                        idx = self.active_editor.queue.automated_runs.index(run) + 1
                        id2 = 'RowIdx={}'.format(idx)

                    v.title = '{} ({}, {})'.format(v.title, runid, id2)

            if tripped:
                v.select_conditional(tripped, tripped=True)

            if self._cv_info:
                self._close_cv()

            self._cv_info = open_view(v, kind=kind)

        except BaseException:
            import traceback

            self.warning('******** Exception trying to open conditionals. Notify developer ********')
            self.debug(traceback.format_exc())

    def _add_system_health(self, run):
        # save analysis. don't cancel immediately
        ret = None
        if self.system_health:
            ret = self.system_health.add_analysis(self)

        # cancel the experiment if failed to save to the secondary database
        # cancel/terminate if system health returns a value

        if ret == 'cancel':
            self.cancel(cancel_run=True, msg=self.system_health.error_msg)
        elif ret == 'terminate':
            self.cancel('run', cancel_run=True, msg=self.system_health.error_msg)
        else:
            return True

    # ===============================================================================
    # execution steps
    # ===============================================================================
    def _start(self, run):
        ret = True

        if self.set_integration_time_on_start:
            dit = self.default_integration_time
            self.info('Setting default integration. t={}'.format(dit))
            run.set_integration_time(dit)

        if not run.start():
            self.alive = False
            ret = False
            run.spec.state = 'failed'

            msg = 'Run {} did not start properly'.format(run.runid)
            self._err_message = msg
            self._canceled = True
            self.information_dialog(msg)
        else:
            self.experiment_queue.set_run_inprogress(run.runid)

        return ret

    def _extraction(self, ai):
        """
            ai: AutomatedRun
            extraction step
        """
        if self._pre_extraction_check(ai):
            self.heading('Pre Extraction Check Failed')
            self._err_message = 'Pre Extraction Check Failed'
            return

        # self.extracting_run = ai
        ret = True
        if ai.start_extraction():
            self.extracting = True
            if not ai.do_extraction():
                ret = self._failed_execution_step('Extraction Failed')
        else:
            ret = ai.is_alive()

        self.trait_set(extraction_state_label='', extracting=False)
        self.extracting_run = None
        return ret

    def _measurement(self, ai):
        """
            ai: AutomatedRun
            measurement step
        """
        if self.send_config_before_run:
            self.info('Sending spectrometer configuration')
            man = self.spectrometer_manager
            man.send_configuration()

        ret = True
        self.measuring_run = ai
        if ai.start_measurement():
            # only set to measuring (e.g switch to iso evo pane) if
            # automated run has a measurement_script
            self.measuring = True

            if not ai.do_measurement():
                ret = self._failed_execution_step('Measurement Failed')
        else:
            ret = ai.is_alive()

        # self.debug('^^^^^^^^ Clear measuring run')
        # self.measuring_run = None
        self.measuring = False
        return ret

    def _post_measurement(self, ai):
        """
            ai: AutomatedRun
            post measurement step
        """
        if not ai.do_post_measurement():
            self._failed_execution_step('Post Measurement Failed')
        else:
            # self._retroactive_experiment_identifiers(ai.spec)
            return True

    def _failed_execution_step(self, msg):
        self.debug('failed execution step {}'.format(msg))
        if not self._canceled:
            self._err_message = msg
            self.alive = False
        return False

    # ===============================================================================
    # utilities
    # ===============================================================================
    def _report_execution_state(self, run):
        pass

    def _make_run(self, spec):
        """
            spec: AutomatedRunSpec
            return AutomatedRun

            generate an AutomatedRun for this ``spec``.

        """
        exp = self.experiment_queue

        if not self._set_run_aliquot(spec):
            return

        # reuse run if not overlap
        # run = self.current_run if not spec.overlap[0] else None

        run = None
        arun = spec.make_run(run=run)
        arun.logger_name = 'AutomatedRun {}'.format(arun.runid)

        if spec.end_after:
            self.end_at_run_completion = True
            arun.is_last = True

        '''
            save this runs uuid to a hidden file
            used for analysis recovery
        '''
        self._add_backup(arun.uuid)

        arun.set_preferences(self.application.preferences)

        arun.integration_time = 1.04

        arun.experiment_executor = self
        arun.spectrometer_manager = self.spectrometer_manager
        arun.extraction_line_manager = self.extraction_line_manager
        arun.ion_optics_manager = self.ion_optics_manager
        arun.runner = self.pyscript_runner
        arun.extract_device = exp.extract_device

        arun.persister.datahub = self.datahub
        arun.persister.load_name = exp.load_name
        arun.persister.dbexperiment_identifier = exp.database_identifier

        arun.use_syn_extraction = False

        arun.use_db_persistence = self.use_db_persistence
        arun.use_dvc_persistence = self.use_dvc_persistence
        arun.use_xls_persistence = self.use_xls_persistence

        if self.use_dvc_persistence:
            dvcp = self.application.get_service('pychron.dvc.dvc_persister.DVCPersister')
            if dvcp:
                dvcp.load_name = exp.load_name
                dvcp.default_principal_investigator = self.default_principal_investigator
                arun.dvc_persister = dvcp

                repid = spec.repository_identifier
                self.datahub.mainstore.add_repository(repid, self.default_principal_investigator, inform=False)

                arun.dvc_persister.initialize(repid)

        mon = self.monitor
        if mon is not None:
            mon.automated_run = arun
            arun.monitor = mon
            arun.persister.monitor = mon

        if self.use_system_health:
            arun.system_health = self.system_health

        if self.use_xls_persistence:
            xls_persister = ExcelPersister()
            xls_persister.load_name = exp.load_name
            if mon is not None:
                xls_persister.monitor = mon
            arun.xls_persister = xls_persister

        return arun

    def _set_run_aliquot(self, spec):
        """
            spec: AutomatedRunSpec

            set the aliquot/step for this ``spec``
            check for conflicts between primary and secondary databases

        """

        if spec.conflicts_checked:
            return True

        # if a run in executed runs is in extraction or measurement state
        # we are in overlap mode
        dh = self.datahub

        ens = self.experiment_queue.executed_runs
        step_offset, aliquot_offset = 0, 0

        exs = [ai for ai in ens if ai.state in ('measurement', 'extraction')]
        if exs:
            if spec.is_step_heat():
                eruns = [(ei.labnumber, ei.aliquot) for ei in exs]
                step_offset = 1 if (spec.labnumber, spec.aliquot) in eruns else 0
            else:
                eruns = [ei.labnumber for ei in exs]
                aliquot_offset = 1 if spec.labnumber in eruns else 0

            conflict = dh.is_conflict(spec)
            if conflict:
                ret = self._in_conflict(spec, aliquot_offset, step_offset)
            else:
                dh.update_spec(spec, aliquot_offset, step_offset)
                ret = True
        else:
            conflict = dh.is_conflict(spec)
            if conflict:
                ret = self._in_conflict(spec, conflict)
            else:
                dh.update_spec(spec)
                ret = True

        return ret

    def _in_conflict(self, spec, conflict, aoffset=0, soffset=0):
        """
            handle databases in conflict
        """
        dh = self.datahub

        ret = self.confirmation_dialog('Databases are in conflict. '
                                       'Do you want to modify the Run Identifier to {}'.format(dh.new_runid),
                                       timeout_ret=True,
                                       timeout=30)
        if ret:
            dh.update_spec(spec, aoffset, soffset)
            ret = True
            self._canceled = False
            self._err_message = ''
        else:
            spec.conflicts_checked = False
            self._canceled = True
            self._err_message = 'Databases are in conflict. {}'.format(conflict)
            self.message(self._err_message)
            # self.info('No response from user. Canceling run')
            # do_later(self.information_dialog,
            # 'Databases are in conflict. No response from user. Canceling experiment')

        if self._canceled:
            self.cancel()

        return ret

    def _delay(self, delay, message='between'):
        """
            delay: float
            message: str

            sleep for ``delay`` seconds
        """
        # self.delaying_between_runs = True
        msg = 'Delay {} runs {} sec'.format(message, delay)
        self.info(msg)
        self._wait(delay, msg)
        self.delaying_between_runs = False

    def _wait(self, delay, msg):
        """
            delay: float
            message: str

            sleep for ``delay`` seconds using a WaitControl
        """
        wg = self.wait_group
        wc = self.get_wait_control()

        wc.message = msg
        wc.start(duration=delay)
        wg.pop(wc)

        # if wc.is_continued():
        # self.stats.continue_clock()

    def _set_extract_state(self, state, *args):
        """
            state: str
        """
        self.debug('set extraction state {} {}'.format(state, args))
        if state:
            self._extraction_state_on(state, *args)
        else:
            self._extraction_state_off()

    def _extraction_state_on(self, state, flash, color, period):
        """
            flash: float (0.0 - 1.0) percent of period to be on. e.g if flash=0.75 and period=4,
                    state displayed for 3 secs, then off for 1 sec
            color: str
            period: float
        """
        label = state.upper()
        if flash:
            if self._end_flag:
                self._end_flag.set()

                # wait until previous loop finished.
                cf = self._complete_flag
                while not cf.is_set():
                    time.sleep(0.05)

            else:
                self._end_flag = Flag()
                self._complete_flag = Flag()

            def pattern_gen():
                """
                    infinite generator
                """
                pattern = ((flash * period, True), ((1 - flash) * period, False))
                i = 0
                while 1:
                    try:
                        yield pattern[i]
                        i += 1
                    except IndexError:
                        yield pattern[0]
                        i = 1

            self._end_flag.clear()
            self._complete_flag.clear()

            invoke_in_main_thread(self._extraction_state_iter, pattern_gen(), label, color)
        else:
            invoke_in_main_thread(self.trait_set, extraction_state_label=label,
                                  extraction_state_color=color)

    def _extraction_state_off(self):
        """
            clear extraction state label
        """
        if self._end_flag:
            self._end_flag.set()

        invoke_in_main_thread(self.trait_set, extraction_state_label='')

    def _extraction_state_iter(self, gen, label, color):
        """
            iterator for extraction state label.
            used to flash label
        """
        t, state = gen.next()
        if state:
            self.debug('set state label={}, color={}'.format(label, color))
            self.trait_set(extraction_state_label=label,
                           extraction_state_color=color)
        else:
            self.debug('clear extraction_state_label')
            self.trait_set(extraction_state_label='')

        if not self._end_flag.is_set():
            do_after(t * 1000, self._extraction_state_iter, gen, label, color)
        else:
            self.debug('extract state complete')
            self._complete_flag.set()
            self.trait_set(extraction_state_label='')

    def _add_backup(self, uuid_str):
        """
            add uuid to backup recovery file
        """

        with open(paths.backup_recovery_file, 'a') as rfile:
            rfile.write('{}\n'.format(uuid_str))

    # ===============================================================================
    # checks
    # ===============================================================================
    def _check_dashboard(self, prog=None):
        """
        return True if dashboard has an error
        :return: boolean
        """
        if self.use_dashboard_client:
            if self.dashboard_client:
                ef = self.dashboard_client.error_flag
                if prog:
                    prog.change_message('Checking Dashboard client for errors')

                if ef:
                    self.warning('Canceling experiment. Dashboard client reports an error\n {}'.format(ef))
                    return ef

    def _check_memory(self, prog=None, threshold=None):
        """
            if avaliable memory is less than threshold  (MB)
            stop the experiment
            issue a warning

            return True if out of memory
            otherwise None
        """
        if self.use_memory_check:
            if prog:
                prog.change_message('Checking available memory')
            if threshold is None:
                threshold = self.memory_threshold

            # return amem in MB
            amem = mem_available()
            self.debug('Available memory {}. mem-threshold= {}'.format(amem, threshold))
            if amem < threshold:
                msg = 'Memory limit exceeded. Only {} MB available. Stopping Experiment'.format(amem)
                invoke_in_main_thread(self.warning_dialog, msg)
                return True

    def _check_managers(self, inform=True):
        self.debug('checking for managers')
        if globalv.experiment_debug:
            self.debug('********************** NOT DOING  managers check')
            return True

        nonfound = self._check_for_managers()
        if nonfound:
            self.info('experiment canceled because could connect to managers {}'.format(nonfound))
            if inform:
                invoke_in_main_thread(self.warning_dialog,
                                      'Canceled! Could not connect to managers {}. '
                                      'Check that these instances are running.'.format(','.join(nonfound)))
            return

        return True

    def _check_for_managers(self):
        """
            determine the necessary managers based on the ExperimentQueue and
            check that they exist and are connectable
        """
        from pychron.experiment.connectable import Connectable

        exp = self.experiment_queue
        nonfound = []
        elm_connectable = Connectable(name='Extraction Line',
                                      manager=self.extraction_line_manager)
        self.connectables = [elm_connectable]

        print self.extraction_line_manager

        if self.extraction_line_manager is None:
            nonfound.append('extraction_line')
        else:
            if not self.extraction_line_manager.test_connection():
                nonfound.append('extraction_line')
            else:
                elm_connectable.connected = True

        if exp.extract_device and exp.extract_device not in ('Extract Device', LINE_STR):
            # extract_device = convert_extract_device(exp.extract_device)
            extract_device = exp.extract_device.replace(' ', '')
            ed_connectable = Connectable(name=extract_device)
            man = None
            if self.application:
                self.debug('get service name={}'.format(extract_device))
                for protocol in ('pychron.lasers.laser_managers.ilaser_manager.ILaserManager',
                                 'pychron.furnace.ifurnace_manager.IFurnaceManager',
                                 'pychron.external_pipette.protocol.IPipetteManager'):

                    man = self.application.get_service(protocol, 'name=="{}"'.format(extract_device))
                    if man:
                        ed_connectable.protocol = protocol
                        break

            self.connectables.append(ed_connectable)
            if not man:
                nonfound.append(extract_device)
            else:
                if not man.test_connection():
                    nonfound.append(extract_device)
                else:
                    ed_connectable.set_connection_parameters(man)
                    ed_connectable.connected = True

        needs_spec_man = any([ai.measurement_script
                              for ai in exp.cleaned_automated_runs
                              if ai.state == 'not run'])

        if needs_spec_man:
            s_connectable = Connectable(name='Spectrometer', manager=self.spectrometer_manager)
            self.connectables.append(s_connectable)
            if self.spectrometer_manager is None:
                nonfound.append('spectrometer')
            else:
                if not self.spectrometer_manager.test_connection():
                    nonfound.append('spectrometer')
                else:
                    s_connectable.connected = True

        return nonfound

    def _pre_extraction_check(self, run):
        """
            do pre_run_terminations
        """

        if not self.alive:
            return

        self.debug('============================= Pre Extraction Check =============================')

        conditionals = self._load_queue_conditionals('pre_run_terminations')
        default_conditionals = self._load_system_conditionals('pre_run_terminations')
        if default_conditionals or conditionals:
            self.heading('Pre Extraction Check')

            self.debug('Get a measurement from the spectrometer')
            data = self.spectrometer_manager.spectrometer.get_intensities()
            ks = ','.join(data[0])
            ss = ','.join(['{:0.5f}'.format(d) for d in data[1]])
            self.debug('Pre Extraction Termination data. keys={}, signals={}'.format(ks, ss))

            if conditionals:
                self.info('testing user defined conditionals')
                if self._test_conditionals(run, conditionals,
                                           'Checking user defined pre extraction terminations',
                                           'Pre Extraction Termination',
                                           data=data):
                    return True

            if default_conditionals:
                self.info('testing system defined conditionals')
                if self._test_conditionals(run, default_conditionals,
                                           'Checking default pre extraction terminations',
                                           'Pre Extraction Termination',
                                           data=data):
                    return True

            self.heading('Pre Extraction Check Passed')
        self.debug('=================================================================================')

    def _pre_queue_check(self, exp):
        """
            return True to stop execution loop
        """
        if exp.tray:
            ed = next((ci for ci in self.connectables if ci.name == exp.extract_device), None)
            if ed and ed.connected:
                name = convert_extract_device(ed.name)
                man = self.application.get_service(ed.protocol, 'name=="{}"'.format(name))
                self.debug('Get service {}. name=="{}"'.format(ed.protocol, name))
                if man:
                    self.debug('{} service found {}'.format(name, man))
                    ed_tray = man.get_tray()
                    return ed_tray != exp.tray

    def _pre_run_check(self, spec):
        """
            return True to stop execution loop
        """
        self.heading('Pre Run Check')

        ef = self._check_dashboard()
        if ef:
            self._err_message = 'Dashboard error. {}'.format(ef)

        if self._check_memory():
            self._err_message = 'Not enough memory'
            return True

        if not self._check_managers():
            self._err_message = 'Not all managers available'
            return True

        if self._check_for_errors():
            return True

        if self.monitor:
            if not self.monitor.check():
                self._err_message = 'Automated Run Monitor Failed'
                self.warning('automated run monitor failed')
            return True

        # if the experiment queue has been modified wait until saved or
        # timed out. if timed out autosave.
        self._wait_for_save()
        self.heading('Pre Run Check Passed')

    def _retroactive_repository_identifiers(self, spec):
        self.warning('retroactive repository identifiers disabled')
        return

        db = self.datahub.mainstore
        crun, expid = retroactive_repository_identifiers(spec, self._cached_runs, self._active_repository_identifier)
        self._cached_runs, self._active_repository_identifier = crun, expid

        db.add_repository_association(spec.repository_identifier, spec)
        if not is_special(spec.identifier) and self._cached_runs:
            for c in self._cached_runs:
                db.add_repository_association(expid, c)
            self._cached_runs = []
            # if is_special(spec.identifier):
            #     self._cached_runs.append(spec)
            #     if self._active_experiment_identifier:
            #         spec.experiment_identifier = self._active_experiment_identifier
            # else:
            #     exp_id = spec.experiment_identifier
            #     if self._cached_runs:
            #         for c in self._cached_runs:
            #             self.datahub.maintstore.add_experiment_association(c, exp_id)
            #         self._cached_runs = []
            #     self._active_experiment_identifier = exp_id

    def _check_repository_identifiers(self):
        db = self.datahub.mainstore.db

        cr = ConflictResolver()
        for ei in self.experiment_queues:
            identifiers = {ai.identifier for ai in ei.cleaned_automated_runs}
            identifiers = [idn for idn in identifiers if not is_special(idn)]

            repositories = {}
            eas = db.get_associated_repositories(identifiers)
            for idn, exps in groupby(eas, key=lambda x: x[1]):
                repositories[idn] = [e[0] for e in exps]

            conflicts = []
            for ai in ei.cleaned_automated_runs:
                identifier = ai.identifier
                if not is_special(identifier):
                    try:
                        es = repositories[identifier]
                        if ai.repository_identifier not in es:
                            if ai.sample == self.monitor_name:
                                ai.repository_identifier = 'Irradiation-{}'.format(ai.irradiation)

                            else:

                                self.debug('Experiment association conflict. '
                                           'experimentID={} '
                                           'previous_associations={}'.format(ai.repository_identifier,
                                                                             ','.join(es)))
                                conflicts.append((ai, es))
                    except KeyError:
                        pass

            if conflicts:
                self.debug('Experiment association warning')
                cr.add_conflicts(ei.name, conflicts)

        if cr.conflicts:
            cr.available_ids = db.get_repository_identifiers()

            info = cr.edit_traits(kind='livemodal')
            if info.result:
                cr.apply()
                self.experiment_queue.refresh_table_needed = True
                return True
        else:
            return True

    def _sync_repositories(self, prog):
        experiment_ids = {a.repository_identifier for q in self.experiment_queues for a in q.cleaned_automated_runs}
        for e in experiment_ids:
            if prog:
                prog.change_message('Syncing {}'.format(e))
                if not self.datahub.mainstore.sync_repo(e, use_progress=False):
                    break
        else:
            return True

    def _pre_execute_check(self, prog=None, inform=True):
        if not self.use_db_persistence and not self.use_xls_persistence and not self.use_dvc_persistence:
            if not self.confirmation_dialog('You do not have any Database or XLS saving enabled. '
                                            'Are you sure you want to continue?\n\n'
                                            'Enable analysis saving in Preferences>>Experiment>>Automated Run'):
                return

        if self.use_db_persistence:
            if self.datahub.massspec_enabled:
                if not self.datahub.store_connect('massspec'):
                    if not self.confirmation_dialog(
                            'Not connected to a Mass Spec database. Do you want to continue with pychron only?'):
                        return

        if prog:
            prog.change_message('Checking queue length')

        exp = self.experiment_queue
        runs = exp.cleaned_automated_runs
        if not len(runs):
            if inform:
                self.warning_dialog('No analysis in the queue')
            return

        if self.user_notifier.emailer is None:
            if any((eq.use_email or eq.use_group_email for eq in self.experiment_queues)):
                if not self.confirmation_dialog('Email Plugin not initialized. '
                                                'Required for sending email notifications. '
                                                'Are you sure you want to continue?'):
                    return

        if self.datahub.massspec_enabled:
            if not self.datahub.store_connect('massspec'):
                if not self.confirmation_dialog(
                        'Not connected to a Mass Spec database. Do you want to continue with pychron only?'):
                    return

        if prog:
            prog.change_message('Setting aliquot for first analysis')

        # check the first aliquot before delaying
        arv = runs[0]
        if not self._set_run_aliquot(arv):
            if inform:
                self.warning_dialog('Failed setting aliquot')
            return

        if self.use_dvc_persistence:
            # create dated references repos

            curtag = get_curtag()

            dvc = self.datahub.stores['dvc']
            ms = self.active_editor.queue.mass_spectrometer
            for tag in ('air', 'cocktail', 'blank'):
                dvc.add_repository('{}_{}{}'.format(ms, tag, curtag), self.default_principal_investigator, inform=False)

            no_repo = []
            for i, ai in enumerate(runs):
                if not ai.repository_identifier:
                    self.warning('No repository identifier for i={}, {}'.format(i + 1, ai.runid))
                    no_repo.append(ai)

            if no_repo:
                if not self.confirmation_dialog('Missing repository identifiers. Automatically populate?'):
                    return

                populate_repository_identifiers(no_repo, ms, curtag, debug=self.debug)

        if globalv.experiment_debug:
            self.debug('********************** NOT DOING PRE EXECUTE CHECK ')
            return True

        if prog:
            prog.change_message('Checking Experiment Identifiers')

        if not self._check_repository_identifiers():
            return

        if prog:
            prog.change_message('Syncing repositories')
        if not self._sync_repositories(prog):
            return

        if self._check_dashboard(prog):
            return

        if self._check_memory(prog):
            return

        if not self._check_managers(inform=inform):
            return

        if self.use_automated_run_monitor:
            self.monitor = self._monitor_factory()
            if self.monitor:
                if prog:
                    prog.change_message('Checking Automated Run Monitor')
                self.monitor.set_additional_connections(self.connectables)
                self.monitor.clear_errors()
                if not self.monitor.check():
                    if inform:
                        self.warning_dialog('Automated Run Monitor Failed')
                    return

        if prog:
            prog.change_message('Get preceding blank')

        an = self._get_preceding_blank_or_background(inform=inform)
        if an is not True:
            if an is None:
                return
            else:
                self.info('using {} as the previous blank'.format(an.record_id))
                try:
                    # self._prev_blank_id = an.meas_analysis_id
                    self._prev_blanks = an.get_baseline_corrected_signal_dict()
                    self._prev_baselines = an.get_baseline_dict()
                except TraitError:
                    self.debug_exception()
                    self.warning('failed loading previous blank')
                    return
        if prog:
            prog.change_message('Checking PyScript Runner')
        if not self.pyscript_runner.connect():
            self.info('Failed connecting to pyscript_runner')
            msg = 'Failed connecting to a pyscript_runner. Is the extraction line computer running?'
            invoke_in_main_thread(self.warning_dialog, msg)
            return

        if prog:
            prog.change_message('Pre execute check complete')

        self.debug('pre execute check complete')
        return True

    def _post_run_check(self, run):
        """
            1. check post run termination conditionals.
            2. check to see if an action should be taken

            if runs  are overlapping this will be a problem.
            dont overlap onto blanks
            execute the action and continue the queue
        """
        if not self.alive:
            return
        self.heading('Post Run Check')

        # check user defined post run actions
        # conditionals = self._load_queue_conditionals('post_run_actions', klass='ActionConditional')
        conditionals = self._load_queue_conditionals('post_run_actions')
        if self._action_conditionals(run, conditionals, 'Checking user defined post run actions',
                                     'Post Run Action'):
            return True

        # check default post run actions
        # conditionals = self._load_default_conditionals('post_run_actions', klass='ActionConditional')
        conditionals = self._load_system_conditionals('post_run_actions')
        if self._action_conditionals(run, conditionals, 'Checking default post run actions',
                                     'Post Run Action'):
            return True

        # check queue defined terminations
        conditionals = self._load_queue_conditionals('post_run_terminations')
        if self._test_conditionals(run, conditionals, 'Checking user defined post run terminations',
                                   'Post Run Termination'):
            return True

        # check default terminations
        conditionals = self._load_system_conditionals('post_run_terminations')
        if self._test_conditionals(run, conditionals, 'Checking default post run terminations',
                                   'Post Run Termination'):
            return True

    def _check_for_errors(self):
        self.debug('checking for connectable errors')
        for c in self.connectables:
            self.debug('check connectable name: {} manager: {}'.format(c.name, c.manager))
            man = c.manager
            if man is None:
                man = self.application.get_service(c.protocol, 'name=="{}"'.format(c.name))

            self.debug('connectable manager: {}'.format(man))
            if man:
                e = man.get_error()
                self.debug('connectable get error {}'.format(e))
                if e and e.lower() != 'ok':
                    self._err_message = e
                    break

    def _load_system_conditionals(self, term_name, **kw):
        self.debug('loading system conditionals {}'.format(term_name))
        # p = paths.system_conditionals
        p = get_path(paths.spectrometer_dir, '.*conditionals', ['.yaml', '.yml'])
        if p:
            return self._extract_conditionals(p, term_name, level=SYSTEM, **kw)
        else:
            # pp = os.path.join(paths.spectrometer_dir, 'default_conditionals.yaml')
            self.warning('no system conditionals file located at {}'.format(p))

    def _load_queue_conditionals(self, term_name, **kw):
        self.debug('loading queue conditionals {}'.format(term_name))
        exp = self.experiment_queue
        if not exp and self.active_editor:
            exp = self.active_editor.queue

        if exp:
            name = exp.queue_conditionals_name
            if test_queue_conditionals_name(name):
                p = get_path(paths.queue_conditionals_dir, name, ['.yaml', '.yml'])
                self.debug('queue conditionals path {}'.format(p))
                return self._extract_conditionals(p, term_name, level=QUEUE, **kw)

    def _extract_conditionals(self, p, term_name, level=RUN, **kw):
        if p and os.path.isfile(p):
            self.debug('loading condiitonals from {}'.format(p))
            return conditionals_from_file(p, name=term_name, level=level, **kw)

    def _action_conditionals(self, run, conditionals, message1, message2):
        if conditionals:
            self.debug('{} n={}'.format(message1, len(conditionals)))
            for ci in conditionals:
                if ci.check(run, None, True):
                    self.info('{}. {}'.format(message2, ci.to_string()), color='yellow')
                    self._show_conditionals(active_run=run, tripped=ci, kind='live')
                    self._do_action(ci)

                    if self._cv_info:
                        do_after(2000, self._cv_info.control.close)

                    return True

    def _test_conditionals(self, run, conditionals, message1, message2,
                           data=None, cnt=True):
        if not self.alive:
            return True

        if conditionals:
            self.debug('{} n={}'.format(message1, len(conditionals)))
            for ci in conditionals:
                if ci.check(run, data, cnt):
                    self.warning('!!!!!!!!!! Conditional Tripped !!!!!!!!!!')
                    self.warning('{}. {}'.format(message2, ci.to_string()))

                    # self.notification_manager.add_notification('Conditional Tripped. {}. {}'.format(message2,
                    # ci.to_string()))

                    self.cancel(confirm=False)

                    self.show_conditionals(active_run=run, tripped=ci)
                    return True

    def _do_action(self, action):
        self.info('Do queue action {}'.format(action.action))
        if action.action == 'repeat':
            if action.count < action.nrepeat:
                self.debug('repeating last run')
                action.count += 1
                exp = self.experiment_queue

                run = exp.executed_runs[0]
                exp.automated_runs.insert(0, run)

                # experimentor handles the queue modified
                # resets the database and updates info
                self.queue_modified = True

            else:
                self.info('executed N {} {}s'.format(action.count + 1,
                                                     action.action))
                self.cancel(confirm=False)

        elif action.action == 'cancel':
            self.cancel(confirm=False)

    def _get_preceding_blank_or_background(self, inform=True):
        exp = self.experiment_queue

        types = ['air', 'unknown', 'cocktail']
        # get first air, unknown or cocktail
        aruns = exp.cleaned_automated_runs

        if aruns[0].analysis_type.startswith('blank'):
            return True

        msg = '''First "{}" not preceded by a blank.
Use Last "blank_{}"= {}
'''
        an = next((a for a in aruns if a.analysis_type in types), None)
        if an:
            anidx = aruns.index(an)

            # find first blank_
            # if idx > than an idx need a blank
            nopreceding = True
            ban = next((a for a in aruns if a.analysis_type == 'blank_{}'.format(an.analysis_type)), None)

            if ban:
                nopreceding = aruns.index(ban) > anidx

            if nopreceding:
                self.debug('no preceding blank')
            if anidx == 0:
                self.debug('first analysis is not a blank')

            if anidx == 0 or nopreceding:
                pdbr, selected = self._get_blank(an.analysis_type, exp.mass_spectrometer,
                                                 exp.extract_device,
                                                 last=True,
                                                 repository=an.repository_identifier if an.is_special() else None)

                if pdbr:
                    if selected:
                        self.debug('use user selected blank {}'.format(pdbr.record_id))
                        return pdbr
                    else:
                        msg = msg.format(an.analysis_type,
                                         an.analysis_type,
                                         pdbr.record_id)

                        retval = NO
                        if inform:
                            retval = self.confirmation_dialog(msg,
                                                              no_label='Select From Database',
                                                              cancel=True,
                                                              return_retval=True)

                        if retval == CANCEL:
                            return
                        elif retval == YES:
                            self.debug('use default blank {}'.format(pdbr.record_id))
                            return pdbr
                        else:
                            self.debug('get blank from database')
                            pdbr, _ = self._get_blank(an.analysis_type, exp.mass_spectrometer,
                                                      exp.extract_device)
                            return pdbr
                else:
                    self.warning_dialog('No blank for {} is in the database. Run a blank!!'.format(an.analysis_type))
                    return

        return True

    def _get_blank(self, kind, ms, ed, last=False, repository=None):
        mainstore = self.datahub.mainstore
        db = mainstore.db
        selected = False
        dbr = None
        if last:
            dbr = db.retrieve_blank(kind, ms, ed, last, repository)

        if dbr is None:
            selected = True
            from pychron.experiment.utilities.reference_analysis_selector import ReferenceAnalysisSelector
            selector = ReferenceAnalysisSelector(title='Select Default Blank')
            info = selector.edit_traits(kind='livemodal')
            dbs = db.get_blanks(ms)
            selector.init(dbs)
            if info.result:
                dbr = selector.selected
        if dbr:
            dbr = mainstore.make_analysis(dbr.make_record_view(repository))

        return dbr, selected

    def _set_message(self, msg, color='black'):
        self.heading(msg)
        invoke_in_main_thread(self.trait_set, extraction_state_label=msg,
                              extraction_state_color=color)

    # ===============================================================================
    # handlers
    # ===============================================================================
    def _measuring_run_changed(self):
        if self.measuring_run:
            self.measuring_run.is_last = self.end_at_run_completion

    def _extracting_run_changed(self):
        if self.extracting_run:
            self.extracting_run.is_last = self.end_at_run_completion

    def _end_at_run_completion_changed(self):
        if self.end_at_run_completion:
            if self.measuring_run:
                self.measuring_run.is_last = True
            if self.extracting_run:
                self.extracting_run.is_last = True
        else:
            self._update_automated_runs()

    @on_trait_change('experiment_queue:automated_runs[]')
    def _update_automated_runs(self):
        if self.is_alive():
            is_last = len(self.experiment_queue.cleaned_automated_runs) == 0
            if self.extracting_run:
                self.extracting_run.is_last = is_last

    def _stop_button_fired(self):
        self.debug('%%%%%%%%%%%%%%%%%% Stop fired alive={}'.format(self.is_alive()))
        if self.is_alive():
            self.info('stop execution')
            self.stop()

    def _abort_run_button_fired(self):
        self.debug('abort run. Executor.isAlive={}'.format(self.is_alive()))
        if self.is_alive():
            for crun, kind in ((self.measuring_run, 'measuring'),
                               (self.extracting_run, 'extracting')):
                if crun:
                    self.debug('abort {} run {}'.format(kind, crun.runid))
                    self._abort_run()
                    # do_after(50, self._cancel_run)
                    # t = Thread(target=self._cancel_run)
                    # t.start()
                    break

    def _truncate_button_fired(self):
        if self.measuring_run:
            self.measuring_run.truncate_run(self.truncate_style)

    def _show_conditionals_button_fired(self):
        self._show_conditionals()

    @on_trait_change('experiment_queue:selected, active_editor:queue:selected')
    def _handle_selection(self, new):
        if new:
            self.selected_run = new[0]
        else:
            self.selected_run = None

    def _alive_changed(self, new):
        self.executing_led.state = 2 if new else 0

    # ===============================================================================
    # property get/set
    # ===============================================================================
    def _get_can_start(self):
        return self.executable and not self.is_alive()

    # ===============================================================================
    # defaults
    # ===============================================================================
    def _system_health_default(self):
        sh = SystemHealthSeries()
        return sh

    def _datahub_default(self):
        dh = Datahub()
        return dh

    def _pyscript_runner_default(self):
        runner = self.application.get_service('pychron.extraction_line.ipyscript_runner.IPyScriptRunner')
        return runner

    def _monitor_factory(self):
        self.debug('Experiment Executor mode={}'.format(self.mode))
        if self.mode == 'client':
            from pychron.monitors.automated_run_monitor import RemoteAutomatedRunMonitor

            mon = RemoteAutomatedRunMonitor(name='automated_run_monitor')
        else:
            from pychron.monitors.automated_run_monitor import AutomatedRunMonitor

            mon = AutomatedRunMonitor()

        self.debug('Automated run monitor {}'.format(mon))
        if mon is not None:
            isok = mon.load()
            if isok:
                return mon
            else:
                self.warning('no automated run monitor available. '
                             'Make sure config file is located at setupfiles/monitors/automated_run_monitor.cfg')

    @property
    def labspy_enabled(self):
        if self.use_labspy:
            return self.labspy_client is not None

# ============= EOF =============================================
