#! /usr/bin/env python
"""
Unit tests for landlab.io.netcdf module.
"""

import os
import numpy as np
from nose.tools import assert_equal, assert_true, assert_raises
from nose import SkipTest
try:
    from nose import assert_list_equal
except ImportError:
    from landlab.testing.tools import assert_list_equal
from numpy.testing import assert_array_equal

from landlab import RasterModelGrid
from landlab.io.netcdf import write_netcdf, NotRasterGridError, WITH_NETCDF4
from landlab.io.netcdf.read import _get_raster_spacing
from landlab.testing.tools import cdtemp

try:
    import netCDF4 as nc
except ImportError:
    pass


def test_netcdf_write_int64_field_netcdf4():
    field = RasterModelGrid(4, 3)
    field.add_field('node', 'topographic__elevation',
                    np.arange(12, dtype=np.int64))

    with cdtemp() as _:
        write_netcdf('test.nc', field, format='NETCDF4')

        root = nc.Dataset('test.nc', 'r', format='NETCDF4')

        for name in ['topographic__elevation']:
            assert_true(name in root.variables)
            assert_array_equal(root.variables[name][:].flat,
                               field.at_node[name])
            assert_equal(root.variables[name][:].dtype, 'int64')

        root.close()


def test_netcdf_write_uint8_field_netcdf4():
    field = RasterModelGrid(4, 3)
    field.add_field('node', 'topographic__elevation',
                    np.arange(12, dtype=np.uint8))

    with cdtemp() as _:
        write_netcdf('test.nc', field, format='NETCDF4')

        root = nc.Dataset('test.nc', 'r', format='NETCDF4')

        for name in ['topographic__elevation']:
            assert_true(name in root.variables)
            assert_array_equal(root.variables[name][:].flat,
                               field.at_node[name])
            assert_equal(root.variables[name][:].dtype, 'uint8')

        root.close()


def test_netcdf_write_as_netcdf3_64bit():
    from scipy.io import netcdf

    field = RasterModelGrid(4, 3)
    field.add_field('node', 'topographic__elevation', np.arange(12.))
    field.add_field('node', 'uplift_rate', 2. * np.arange(12.))

    with cdtemp() as _:
        write_netcdf('test.nc', field, format='NETCDF3_64BIT')

        f = netcdf.netcdf_file('test.nc', 'r')

        for name in ['topographic__elevation', 'uplift_rate']:
            assert_true(name in f.variables)
            assert_array_equal(f.variables[name][:].flat, field.at_node[name])

        f.close()


def test_netcdf_write_as_netcdf3_classic():
    from scipy.io import netcdf

    field = RasterModelGrid(4, 3)
    field.add_field('node', 'topographic__elevation', np.arange(12.))
    field.add_field('node', 'uplift_rate', 2. * np.arange(12.))

    with cdtemp() as _:
        write_netcdf('test.nc', field, format='NETCDF3_CLASSIC')

        f = netcdf.netcdf_file('test.nc', 'r')

        for name in ['topographic__elevation', 'uplift_rate']:
            assert_true(name in f.variables)
            assert_array_equal(f.variables[name][:].flat, field.at_node[name])

        f.close()


def test_netcdf_write():
    if not WITH_NETCDF4:
        raise SkipTest('netCDF4 package not installed')

    field = RasterModelGrid(4, 3)
    field.add_field('node', 'topographic__elevation', np.arange(12.))

    with cdtemp() as _:
        write_netcdf('test.nc', field, format='NETCDF4')
        root = nc.Dataset('test.nc', 'r', format='NETCDF4')

        assert_equal(set(root.dimensions), set(['ni', 'nj', 'nt']))
        assert_equal(len(root.dimensions['ni']), 3)
        assert_equal(len(root.dimensions['nj']), 4)
        assert_true(len(root.dimensions['nt']), 1)
        assert_true(root.dimensions['nt'].isunlimited())

        assert_equal(set(root.variables),
                     set(['x', 'y', 'topographic__elevation']))

        assert_array_equal(root.variables['x'][:].flat,
                           np.array([0., 1., 2., 0., 1., 2., 0., 1., 2.,
                                     0., 1., 2., ]))
        assert_array_equal(root.variables['y'][:].flat,
                           np.array([0., 0., 0., 1., 1., 1., 2., 2., 2.,
                                     3., 3., 3., ]))
        assert_array_equal(root.variables['topographic__elevation'][:].flat,
                           field.at_node['topographic__elevation'])

        root.close()


def test_netcdf_write_as_netcdf4_classic():
    if not WITH_NETCDF4:
        raise SkipTest('netCDF4 package not installed')

    field = RasterModelGrid(4, 3)
    field.add_field('node', 'topographic__elevation', np.arange(12.))
    field.add_field('node', 'uplift_rate', np.arange(12.))

    with cdtemp() as _:
        write_netcdf('test.nc', field, format='NETCDF4_CLASSIC')
        root = nc.Dataset('test.nc', 'r', format='NETCDF4_CLASSIC')

        for name in ['topographic__elevation', 'uplift_rate']:
            assert_true(name in root.variables)
            assert_array_equal(root.variables[name][:].flat,
                               field.at_node[name])

        root.close()


def test_netcdf_write_names_keyword_as_list():
    if not WITH_NETCDF4:
        raise SkipTest('netCDF4 package not installed')

    field = RasterModelGrid(4, 3)
    field.add_field('node', 'topographic__elevation', np.arange(12.))
    field.add_field('node', 'uplift_rate', np.arange(12.))

    with cdtemp() as _:
        write_netcdf('test.nc', field, names=['topographic__elevation'],
                     format='NETCDF4')
        root = nc.Dataset('test.nc', 'r', format='NETCDF4')

        assert_true('topographic__elevation' in root.variables)
        assert_true('uplift_rate' not in root.variables)
        assert_array_equal(root.variables['topographic__elevation'][:].flat,
                           field.at_node['topographic__elevation'])

        root.close()


def test_netcdf_write_names_keyword_as_str():
    if not WITH_NETCDF4:
        raise SkipTest('netCDF4 package not installed')

    field = RasterModelGrid(4, 3)
    field.add_field('node', 'topographic__elevation', np.arange(12.))
    field.add_field('node', 'uplift_rate', np.arange(12.))

    with cdtemp() as _:
        write_netcdf('test.nc', field, names='uplift_rate', format='NETCDF4')
        root = nc.Dataset('test.nc', 'r', format='NETCDF4')

        assert_true('topographic__elevation' not in root.variables)
        assert_true('uplift_rate' in root.variables)
        assert_array_equal(root.variables['uplift_rate'][:].flat,
                           field.at_node['uplift_rate'])

        root.close()


def test_netcdf_write_names_keyword_as_none():
    if not WITH_NETCDF4:
        raise SkipTest('netCDF4 package not installed')

    field = RasterModelGrid(4, 3)
    field.add_field('node', 'topographic__elevation', np.arange(12.))
    field.add_field('node', 'uplift_rate', np.arange(12.))

    with cdtemp() as _:
        write_netcdf('test.nc', field, names=None, format='NETCDF4')
        root = nc.Dataset('test.nc', 'r', format='NETCDF4')

        for name in ['topographic__elevation', 'uplift_rate']:
            assert_true(name in root.variables)
            assert_array_equal(root.variables[name][:].flat,
                               field.at_node[name])

        root.close()


def test_2d_unit_spacing():
    (x, y) = np.meshgrid(np.arange(5.), np.arange(4.))

    spacing = _get_raster_spacing((y, x))
    assert_equal(spacing, 1.)


def test_2d_non_unit_spacing():
    (x, y) = np.meshgrid(np.arange(5.) * 2, np.arange(4.) * 2)

    spacing = _get_raster_spacing((y, x))
    assert_equal(spacing, 2.)


def test_2d_uneven_spacing_axis_0():
    (x, y) = np.meshgrid(np.logspace(0., 2., num=5), np.arange(4.))

    assert_raises(NotRasterGridError, _get_raster_spacing, (y, x))


def test_2d_uneven_spacing_axis_1():
    (x, y) = np.meshgrid(np.arange(4.), np.logspace(0., 2., num=5))

    assert_raises(NotRasterGridError, _get_raster_spacing, (y, x))


def test_2d_switched_coords():
    (x, y) = np.meshgrid(np.arange(5.), np.arange(4.))

    spacing = _get_raster_spacing((x, y))
    assert_equal(spacing, 0.)


def test_1d__unit_spacing():
    spacing = _get_raster_spacing((np.arange(5.), ))
    assert_equal(spacing, 1.)


def test_1d_non_unit_spacing():
    spacing = _get_raster_spacing((np.arange(5.) * 2, ))
    assert_equal(spacing, 2.)


def test_1d_uneven_spacing():
    (x, y) = np.meshgrid(np.logspace(0., 2., num=5), np.arange(4.))

    assert_raises(NotRasterGridError, _get_raster_spacing,
                  (np.logspace(0., 2., num=5), ))
