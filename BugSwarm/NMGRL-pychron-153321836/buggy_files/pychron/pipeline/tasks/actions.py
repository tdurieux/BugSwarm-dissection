# ===============================================================================
# Copyright 2015 Jake Ross
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
from pyface.confirmation_dialog import confirm
from pyface.tasks.action.task_action import TaskAction
from traitsui.menu import Action

from pychron.envisage.resources import icon


class GitRollbackAction(TaskAction):
    name = 'Git Undo'
    method = 'git_rollback'


class SavePipelineTemplateAction(TaskAction):
    name = 'Save Pipeline Template'
    method = 'save_pipeline_template'


class RunAction(TaskAction):
    name = 'Run'
    method = 'run'
    image = icon('start')
    visible_name = 'run_enabled'
    accelerator = 'Ctrl+R'


class ResumeAction(TaskAction):
    name = 'Resume'
    method = 'resume'
    image = icon('resume')
    visible_name = 'resume_enabled'


class RunFromAction(TaskAction):
    name = 'Run From'
    method = 'run_from'


class ResetAction(TaskAction):
    name = 'Reset'
    method = 'reset'
    image = icon('resume')
    # enabled_name = 'resume_enabled'


class ClearAction(TaskAction):
    name = 'Clear'
    method = 'clear'
    image = icon('clear')


class SwitchToBrowserAction(TaskAction):
    name = 'To Browser'
    method = 'switch_to_browser'
    image = icon('start')


class ConfigureRecallAction(TaskAction):
    name = 'Configure Recall'
    dname = 'Configure Recall'
    method = 'configure_recall'
    image = icon('cog')


class ConfigureAnalysesTableAction(TaskAction):
    name = 'Configure Analyses Table'
    dname = 'Configure Analyses Table'
    method = 'configure_analyses_table'
    image = icon('cog')


class LoadReviewStatusAction(TaskAction):
    name = 'Review Status'
    method = 'load_review_status'


class EditAnalysisAction(TaskAction):
    name = 'Edit Analysis'
    method = 'edit_analysis'
    image = icon('application-form-edit')


class DiffViewAction(TaskAction):
    name = 'Diff View'
    method = 'diff_analysis'
    image = icon('edit_diff')
    enabled_name = 'diff_enabled'


class TabularViewAction(TaskAction):
    name = 'Tabular View'
    method = 'tabular_view'
    image = icon('table')


class PipelineAction(Action):
    def perform(self, event):
        app = event.task.window.application
        task = app.get_task('pychron.pipeline.task')
        if hasattr(task, self.action):
            getattr(task, self.action)()


class BrowserAction(Action):
    def perform(self, event):
        app = event.task.window.application
        task = app.get_task('pychron.browser.task')
        if hasattr(task, self.action):
            getattr(task, self.action)()


class TimeViewBrowserAction(BrowserAction):
    name = 'Time View Recall...'
    action = 'open_time_view_browser'


class ReductionAction(PipelineAction):
    pass


class IsoEvolutionAction(PipelineAction):
    name = 'Isotope Evolutions'
    dname = 'Isotope Evolutions'
    action = 'set_isotope_evolutions_template'


class BlanksAction(PipelineAction):
    name = 'Blanks'
    dname = 'Blanks'
    action = 'set_blanks_template'


class ICFactorAction(PipelineAction):
    name = 'ICFactor'
    dname = 'ICFactor'
    action = 'set_icfactor_template'


class FluxAction(PipelineAction):
    name = 'Flux'
    dname = 'Flux'
    action = 'set_flux_template'


class FreezeProductionRatios(PipelineAction):
    name = 'Freeze Production Ratios'
    dname = 'Freeze Production Ratios'
    action = 'freeze_production_ratios'


class FreezeFlux(PipelineAction):
    name = 'Freeze Flux'
    dname = 'Freeze Flux'
    action = 'freeze_flux'


# ============= Plotting Actions =============================================
class ResetFactoryDefaultsAction(Action):
    name = 'Reset Factory Defaults'

    def perform(self, event):
        from pychron.paths import paths
        if confirm(None, 'Are you sure you want to reset to Factory Default settings'):
            paths.reset_plot_factory_defaults()


class PlotAction(PipelineAction):
    pass


class IdeogramAction(PlotAction):
    name = 'Ideogram'
    action = 'set_ideogram_template'
    image = icon('histogram')
    accelerator = 'Ctrl+i'


class SpectrumAction(PlotAction):
    name = 'Spectrum'
    action = 'set_spectrum_template'
    accelerator = 'Ctrl+D'
    # image = icon('histogram')


class IsochronAction(PlotAction):
    name = 'Isochron'
    action = 'set_isochron_template'
    # image = icon('histogram')


class InverseIsochronAction(PlotAction):
    name = 'InverseIsochron'
    action = 'set_inverse_isochron_template'


class SeriesAction(PlotAction):
    name = 'Series'
    dname = 'Series'
    action = 'set_series_template'
    id = 'pychron.series'


class VerticalFluxAction(PipelineAction):
    name = 'Vertical Flux'
    action = 'set_vertical_flux_template'


class ExtractionAction(Action):
    name = 'Extraction Results...'

    def perform(self, event):
        app = event.task.window.application
        windows = app.windows
        for tid in ('pychron.browser.task', 'pychron.pipeline.task'):
            for win in windows:
                task = win.active_task
                if task and task.id == tid:
                    getattr(task, 'show_extraction_graph')()
                    break

# ============= Quick Series ====================================
class LastNAnalysesSeriesAction(PipelineAction):
    name = 'Last N...'
    action = 'set_last_n_analyses_template'


class LastNHoursSeriesAction(PipelineAction):
    name = 'Last N Hours...'
    action = 'set_last_n_hours_template'


class LastDaySeriesAction(PipelineAction):
    name = 'Last Day'
    action = 'set_last_day_template'


class LastWeekSeriesAction(PipelineAction):
    name = 'Last Week'
    action = 'set_last_week_template'


class LastMonthSeriesAction(PipelineAction):
    name = 'Last Month'
    action = 'set_last_month_template'


# ============= tag =============================================
class TagAction(TaskAction):
    name = 'Tag...'
    dname = 'Tag'
    # accelerator = 'Ctrl+Shift+t'
    method = 'set_tag'
    image = icon('tag-blue-add')
    id = 'pychron.tag'


class SetInvalidAction(TaskAction):
    name = 'Set Invalid'
    method = 'set_invalid'


class SetFilteringTagAction(TaskAction):
    name = 'Set Filtering Tag'
    method = 'set_filtering_tag'


# ============= Interperted Age =================================
class SetInterpretedAgeAction(TaskAction):
    name = 'Set Interpreted Age'
    method = 'set_interpreted_age'
    enabled_name = 'set_interpreted_enabled'


class SavePDFAction(TaskAction):
    name = 'Save PDF'
    method = 'save_figure_pdf'
    image = icon('file_pdf')


class SaveFigureAction(TaskAction):
    name = 'Save Figure'
    method = 'save_figure'

# ============= EOF =============================================
