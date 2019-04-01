# ===============================================================================
# Copyright 2014 Jake Ross
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ===============================================================================

# ============= enthought library imports =======================
from envisage.ui.tasks.task_factory import TaskFactory
from traits.api import List

# ============= standard library imports ========================
# ============= local library imports  ==========================
from pychron.envisage.tasks.base_task_plugin import BaseTaskPlugin
from pychron.external_pipette.protocol import IPipetteManager
from pychron.external_pipette.tasks.external_pipette_task import ExternalPipetteTask


class ExternalPipettePlugin(BaseTaskPlugin):
    managers = List(contributes_to='pychron.hardware.managers')
    id = 'pychron.external_pipette'

    _manager = None

    def _manager_factory(self, **kw):
        if self._manager:
            return self._manager
        else:
            pkg = 'pychron.external_pipette.apis_manager'
            klass = 'SimpleApisManager'
            factory = __import__(pkg, fromlist=[klass])
            m = getattr(factory, klass)(name='externalpipette')
            m.bootstrap()
            m.plugin_id = self.id
            m.bind_preferences(self.id)
            self._manager = m
            return m

    def _service_offers_default(self):
        so = self.service_offer_factory(protocol=IPipetteManager,
                                        factory=self._manager_factory)
        return [so, ]

    def _managers_default(self):
        return [dict(name='ExternalPipette',
                     plugin_name='ExternalPipette',
                     manager=self._manager_factory())]

    def _tasks_default(self):
        return [TaskFactory(id=self.id,
                            task_group='hardware',
                            factory=self._task_factory,
                            name='External Pipette',
                            image='pipette',
                            accelerator='Ctrl+Shift+0')]

    def _task_factory(self):
        t = ExternalPipetteTask(manager=self._manager)
        return t

# ============= EOF =============================================

