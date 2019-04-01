from pychron.core.ui import set_qt

set_qt()
from pychron.globals import globalv

globalv.use_warning_display = False
globalv.use_logger_display = False

from pychron.pyscripts.extraction_line_pyscript import ExtractionPyScript
from pychron.external_pipette.apis_manager import SimpleApisManager

__author__ = 'ross'
import unittest


class DummyApp(object):
    _man = SimpleApisManager(_timeout_flag=True)
    _man.available_pipettes = ['A1', 'A2']
    _man.available_blanks = ['B1', 'B2']

    def get_service(self, *args, **kw):
        return self._man


class DummyManager(object):
    application = DummyApp()

    def set_extract_state(self, state):
        pass

    def info(self, *args, **kw):
        pass


class ExternalPipetteTestCase(unittest.TestCase):
    def setUp(self):
        self.script = e = ExtractionPyScript(manager=DummyManager())
        e.setup_context(extract_device='')

    def test_extract_pipette_explicit_invalid_name(self):
        e = self.script
        ret = e.extract_pipette('A4', timeout=1)
        self.assertEqual(ret, 'Invalid Pipette name=A4 av=A1\nA2')

    def test_extract_pipette_explicit(self):
        e = self.script
        ret = e.extract_pipette('A1', timeout=1)
        self.assertEqual(ret, True)

    def test_extract_pipette_implicit(self):
        e = self.script
        e.setup_context(extract_value='A1', )
        ret = e.extract_pipette(timeout=1)
        self.assertEqual(ret, True)

    def test_extract_pipette_timeout(self):
        apis_man = self.script.manager.application.get_service()
        apis_man._timeout_flag = False
        e = self.script
        e.setup_context(extract_value='A1', )
        ret = e.extract_pipette(timeout=1)
        self.assertEqual(ret, 'TimeoutError func=loading_started, timeout=1')


if __name__ == '__main__':
    unittest.main()
