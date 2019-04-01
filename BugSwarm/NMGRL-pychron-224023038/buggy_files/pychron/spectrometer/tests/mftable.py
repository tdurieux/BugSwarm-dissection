import os
import unittest

from pychron.spectrometer.mftable import MagnetFieldTable


class Argon2CDDMFTableTestCase(unittest.TestCase):
    def setUp(self):
        self.mftable = MagnetFieldTable(bind=False)
        self.mftable.molweights = {'Ar40': 40, 'Ar39': 39, 'Ar36': 36, 'Ar38':38, 'PHHCbs': 1}

        p = './data/argon_2CDD.csv'
        if not os.path.isfile(p):
            p = 'spectrometer/tests/data/argon_2CDD.csv'

        self.mftable._test_path = p
        self.mftable.load_mftable(path=p)

    def test_update(self):
        self.mftable.update_field_table('L2(CDD)', 'Ar36', 3.76439824048, report=True, save=False)
        dac = self.mftable.get_dac('L2(CDD)', 36)
        self.assertEqual(dac, 3.76439824048)

    def test_update2(self):
        self.mftable.update_field_table('H2', 'Ar40', 3.76439824048, report=True, save=False)
        dac = self.mftable.get_dac('AX(CDD)', 38)
        self.assertEqual(dac, 3.79185704008)


class DiscreteMFTableTestCase(unittest.TestCase):
    def setUp(self):
        self.mftable = MagnetFieldTable(bind=False)
        self.mftable.molweights = {'Ar40': 40, 'Ar39': 39, 'Ar36': 36, 'Foo': 1}

        p = './data/discrete_mftable.csv'
        if not os.path.isfile(p):
            p = 'pychron/spectrometer/tests/data/discrete_mftable.csv'

        self.mftable._test_path = p
        self.mftable.load_mftable(path=p)

    def test_mass_func(self):
        self.assertEqual(self.mftable.mass_cal_func, 'discrete')

    def test_missing(self):
        self.assertEqual(self.mftable._mftable['L2(CDD)'], (['Ar40', 'Ar39', 'Ar36', 'Foo'], [1], (12.34,), None))

    def test_discrete1(self):
        dac = self.mftable.map_mass_to_dac('Ar40', 'H2')
        self.assertEqual(dac, 5.8955)


class MFTableTestCase(unittest.TestCase):
    def setUp(self):
        self.mftable = MagnetFieldTable(bind=False)
        self.mftable.molweights = {'Ar40': 40, 'Ar39': 39, 'Ar36': 36, 'Foo': 1}

        p = './data/mftable.csv'
        if not os.path.isfile(p):
            p = 'pychron/spectrometer/tests/data/mftable.csv'

        self.mftable._test_path = p
        self.mftable.load_mftable(path=p)

    def test_mass_func(self):
        self.assertEqual(self.mftable.mass_cal_func, 'parabolic')

    #
    # def test_missing(self):
    #     self.assertEqual(self.mftable._mftable['L2(CDD)'], (['Ar40', 'Ar39', 'Ar36', 'Foo'], [1], (12.34,), None))

    def test_map_mass_to_dac(self):
        dac = self.mftable.map_mass_to_dac('Ar40', 'H2')
        self.assertNotEqual(dac, 5.8955)


if __name__ == '__main__':
    unittest.main()
