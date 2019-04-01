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
from traits.api import Property, Instance, List, Either, Int, Float, HasTraits, \
    Str, Bool
from traitsui.api import View, Item, UItem, VGroup, HGroup, spring
from traitsui.tabular_adapter import TabularAdapter
# ============= standard library imports ========================
# ============= local library imports  ==========================
from uncertainties import nominal_value, std_dev
from pychron.core.ui.tabular_editor import myTabularEditor
from pychron.envisage.tasks.base_editor import BaseTraitsEditor
from pychron.core.helpers.formatting import floatfmt
from pychron.mass_spec.mass_spec_recaller import MassSpecRecaller
from pychron.pychron_constants import PLUSMINUS_ONE_SIGMA

DIFF_TOLERANCE_PERCENT = 0.01


class ValueTabularAdapter(TabularAdapter):
    columns = [('Name', 'name'),
               ('Pychron', 'lvalue'),
               ('Diff', 'diff'),
               ('MassSpec', 'rvalue'),
               ('% Dff', 'percent_diff')]

    lvalue_width = Int(100)
    diff_width = Int(100)
    rvalue_width = Int(100)
    # name_width = Int(100)
    name_width = Int(120)

    # name_text = Property
    lvalue_text = Property
    diff_text = Property
    rvalue_text = Property
    percent_diff_text = Property

    font = '12'
    use_bg_color = Bool(True)

    def get_bg_color(self, object, trait, row, column=0):
        color = 'white'
        if self.use_bg_color:
            v = self.item.diff
            if abs(v) > 1e-8:
                color = '#FFCCCC'
        return color

    # def _get_name_text(self):
    #     return '<b>{}</b'.format(self.item.name)

    def _get_percent_diff_text(self):
        v = self.item.percent_diff
        return self._get_value_text(v, n=5)

    def _get_lvalue_text(self):
        v = self.item.lvalue
        return self._get_value_text(v)

    def _get_rvalue_text(self):
        v = self.item.rvalue
        return self._get_value_text(v)

    def _get_value_text(self, v, n=6):
        if isinstance(v, float):
            v = floatfmt(v, n=n, s=5, use_scientific=True)
        return v

    def _get_diff_text(self):
        v = self.item.diff
        if isinstance(v, float):
            v = floatfmt(v, n=8, use_scientific=True)
        elif isinstance(v, bool):
            v = '---' if v else ''

        return v


class Value(HasTraits):
    name = Str
    lvalue = Either(Int, Float)
    rvalue = Either(Int, Float)
    diff = Property(depends_on='lvalue,rvalue')
    enabled = Property(depends_on='lvalue,rvalue')
    percent_diff = Property(depends_on='lvalue,rvalue')

    def _get_percent_diff(self):
        try:
            return self.diff / self.lvalue * 100
        except ZeroDivisionError:
            return 'NaN'

    def _get_diff(self):
        return self.lvalue - self.rvalue

    def _get_enabled(self):
        t = True
        d = self.percent_diff
        if d != 'NaN':
            t = abs(d) > DIFF_TOLERANCE_PERCENT
        return t


class StrValue(Value):
    lvalue = Str
    rvalue = Str

    def _get_diff(self):
        return self.lvalue != self.rvalue

    def _get_enabled(self):
        return self.diff

    def _get_percent_diff(self):
        return ''


class DiffEditor(BaseTraitsEditor):
    values = List

    recaller = Instance(MassSpecRecaller)
    selected_row = Int

    # left_baselines = Dict
    # right_baselines = Dict
    _right = None
    basename = Str

    diffs_only = Bool(True)
    adapter = None

    record_id = ''
    is_blank = False

    def _diffs_only_changed(self, new):
        if new:
            self.values = [vi for vi in self.ovalues if vi.enabled]
            self.adapter.use_bg_color = False
        else:
            self.adapter.use_bg_color = True
            self.values = self.ovalues

    def setup(self, left):
        self.record_id = left.record_id
        self.is_blank = self.record_id.startswith('b')

        right = self._find_right(left)
        self.adapter = ValueTabularAdapter()
        if right:
            self._right = right
            return True

    def set_diff(self, left):
        self.name = '{} Diff'.format(left.record_id)
        self.basename = left.record_id

        right = self._right

        isotopes = ['Ar40', 'Ar39', 'Ar38', 'Ar37', 'Ar36']
        self._set_values(left, right, isotopes)

    def _find_right(self, left):
        """
            find corresponding analysis in secondary database
        """
        recaller = self.recaller

        ln = left.labnumber
        aliquot = left.aliquot
        if ln.startswith('b'):
            aliquot = '-'.join(left.record_id.split('-')[1:])
            ln = -1
        elif ln.startswith('a'):
            aliquot = '-'.join(left.record_id.split('-')[1:])
            ln = -2

        return recaller.find_analysis(ln, aliquot,
                                      left.step)

    def _set_values(self, left, right, isotopes):
        vs = []
        pfunc = lambda x: lambda n: u'{} {}'.format(x, n)

        if not self.is_blank:
            vs.append(Value(name='J',
                            lvalue=nominal_value(left.j or 0),
                            rvalue=nominal_value(right.j or 0)))
            vs.append(Value(name=u'J {}'.format(PLUSMINUS_ONE_SIGMA),
                            lvalue=std_dev(left.j or 0),
                            rvalue=std_dev(right.j or 0)))
            vs.append(Value(name='Age',
                            lvalue=left.age or 0,
                            rvalue=right.age or 0))
            vs.append(Value(name=u'Age {}'.format(PLUSMINUS_ONE_SIGMA),
                            lvalue=left.age_err or 0,
                            rvalue=right.age_err or 0))
            vs.append(Value(name='40Ar* %',
                            lvalue=nominal_value(left.rad40_percent or 0),
                            rvalue=nominal_value(right.rad40_percent or 0)))
            vs.append(Value(name='Rad4039',
                            lvalue=nominal_value(left.uF),
                            rvalue=nominal_value(right.rad4039)))
            vs.append(Value(name=u'Rad4039 {}'.format(PLUSMINUS_ONE_SIGMA),
                            lvalue=std_dev(left.uF),
                            rvalue=std_dev(right.rad4039)))

            constants = left.arar_constants
            vv = [Value(name=n, lvalue=nominal_value(getattr(constants, k)),
                        rvalue=nominal_value(getattr(right, k)))
                  for n, k in (('Lambda K', 'lambda_k'),
                               ('Lambda Ar37', 'lambda_Ar37'),
                               ('Lambda Ar37', 'lambda_Ar37'),
                               ('Lambda Cl36', 'lambda_Cl36'))]
            vs.extend(vv)

        def filter_str(ii):
            fd = ii.filter_outliers_dict.get('filter_outliers')
            return 'yes' if fd else 'no'

        for a in isotopes:
            iso = left.isotopes[a]
            riso = right.isotopes[a]
            func = pfunc(a)

            # mass spec only has baseline corrected intercepts
            # mass spec does not propagate baseline error
            i = iso.get_baseline_corrected_value(include_baseline_error=False)
            ri = riso.baseline_corrected
            vs.append(Value(name=func('Bs Corrected'),
                            lvalue=nominal_value(i),
                            rvalue=nominal_value(ri)))
            vs.append(Value(name=func(PLUSMINUS_ONE_SIGMA), lvalue=std_dev(i), rvalue=std_dev(ri)))

            if not self.is_blank:
                if iso.decay_corrected:
                    # baseline, blank corrected, ic_corrected, decay_corrected
                    i = iso.decay_corrected
                else:
                    # baseline, blank corrected, ic_corrected
                    i = iso.get_intensity()

                ri = riso.total_value
                vs.append(Value(name=func('Total'),
                                lvalue=nominal_value(i),
                                rvalue=nominal_value(ri)))
                vs.append(
                    Value(name=func(u'Total {}'.format(PLUSMINUS_ONE_SIGMA)), lvalue=std_dev(i), rvalue=std_dev(ri)))

            vs.append(Value(name=func('N'), lvalue=iso.n, rvalue=riso.n))
            vs.append(Value(name=func('fN'), lvalue=iso.fn, rvalue=riso.fn))

            vs.append(StrValue(name=func('Fit'), lvalue=iso.fit.lower(), rvalue=riso.fit.lower()))
            vs.append(StrValue(name=func('Filter'), lvalue=filter_str(iso), rvalue=filter_str(iso)))
            vs.append(Value(name=func('Filter Iter'), lvalue=iso.filter_outliers_dict.get('iterations'),
                            rvalue=riso.filter_outliers_dict.get('iterations')))
            vs.append(Value(name=func('Filter SD'), lvalue=iso.filter_outliers_dict.get('std_devs'),
                            rvalue=riso.filter_outliers_dict.get('std_devs')))
            vs.append(Value(name=func('IC'), lvalue=nominal_value(iso.ic_factor),
                            rvalue=nominal_value(riso.ic_factor)))
            vs.append(Value(name=func(u'IC {}'.format(PLUSMINUS_ONE_SIGMA)), lvalue=std_dev(iso.ic_factor),
                            rvalue=std_dev(riso.ic_factor)))
        for a in isotopes:
            func = pfunc(a)
            baseline = left.isotopes[a].baseline
            rbaseline = right.isotopes[a].baseline

            vs.append(Value(name=func('Bs'), lvalue=baseline.value, rvalue=rbaseline.value))
            vs.append(Value(name=func(u'Bs {}'.format(PLUSMINUS_ONE_SIGMA)), lvalue=baseline.error,
                            rvalue=rbaseline.error))
            vs.append(Value(name=func('Bs N'), lvalue=baseline.n, rvalue=rbaseline.n))
            vs.append(Value(name=func('Bs fN'), lvalue=baseline.fn, rvalue=rbaseline.fn))

            fv = StrValue(name=func('Bs Filter'), lvalue=filter_str(iso), rvalue=filter_str(iso))
            vs.append(fv)
            if not (fv.lvalue == 'no' and fv.rvalue == 'no'):
                vs.append(Value(name=func('Bs Filter Iter'), lvalue=baseline.filter_outliers_dict.get('iterations'),
                                rvalue=rbaseline.filter_outliers_dict.get('iterations')))
                vs.append(Value(name=func('Bs Filter SD'), lvalue=baseline.filter_outliers_dict.get('std_devs'),
                                rvalue=rbaseline.filter_outliers_dict.get('std_devs')))

        if not self.is_blank:
            for a in isotopes:
                func = pfunc(a)
                iso = left.isotopes[a]
                riso = right.isotopes[a]
                vs.append(Value(name=func('Blank'), lvalue=iso.blank.value, rvalue=riso.blank.value))
                vs.append(Value(name=func(u'Blank {}'.format(PLUSMINUS_ONE_SIGMA)), lvalue=iso.blank.error,
                                rvalue=riso.blank.error))

            rpr = right.production_ratios
            for k, v in left.production_ratios.iteritems():
                vs.append(Value(name=k, lvalue=nominal_value(v),
                                rvalue=nominal_value(rpr.get(k, 0))))

            rifc = right.interference_corrections
            for k, v in left.interference_corrections.iteritems():
                vs.append(Value(name=k, lvalue=nominal_value(v),
                                rvalue=nominal_value(rifc.get(k.lower(), 0))))

        # self.values = vs
        self.ovalues = vs[:]
        self._diffs_only_changed(self.diffs_only)

    def traits_view(self):
        v = View(VGroup(
            HGroup(Item('diffs_only'), spring, UItem('record_id', style='readonly'), spring),
            UItem('values', editor=myTabularEditor(adapter=self.adapter,
                                                   editable=False,
                                                   selected_row='selected_row'))))
        return v

# ============= EOF =============================================
