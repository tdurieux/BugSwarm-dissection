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
from traits.api import Instance, Bool, Str
# ============= standard library imports ========================
import base64
import hashlib
import os
import struct
from datetime import datetime
from uncertainties import std_dev, nominal_value
from git.exc import GitCommandError
# ============= local library imports  ==========================
from pychron.dvc import dvc_dump
from pychron.dvc.dvc_analysis import META_ATTRS, EXTRACTION_ATTRS, analysis_path, PATH_MODIFIERS
from pychron.experiment.automated_run.persistence import BasePersister
from pychron.experiment.classifier.isotope_classifier import IsotopeClassifier
from pychron.git_archive.repo_manager import GitRepoManager
from pychron.paths import paths
from pychron.pychron_constants import DVC_PROTOCOL, LINE_STR, NULL_STR


def format_repository_identifier(project):
    return project.replace('/', '_').replace('\\', '_')


def spectrometer_sha(src, defl, gains):
    sha = hashlib.sha1()
    for d in (src, defl, gains):
        for k, v in sorted(d.items()):
            sha.update(k)
            sha.update(str(v))

    return sha.hexdigest()


class DVCPersister(BasePersister):
    active_repository = Instance(GitRepoManager)
    dvc = Instance(DVC_PROTOCOL)
    use_isotope_classifier = Bool(False)
    isotope_classifier = Instance(IsotopeClassifier, ())
    stage_files = Bool(True)
    default_principal_investigator = Str

    def per_spec_save(self, pr, repository_identifier=None, commit=False, msg_prefix=None):
        self.per_spec = pr

        if repository_identifier:
            self.initialize(repository_identifier, False)

        self.pre_extraction_save()
        self.pre_measurement_save()
        self.post_extraction_save()
        self.post_measurement_save(commit=commit, msg_prefix=msg_prefix)

    def initialize(self, repository, pull=True):
        """
        setup git repos.

        repositories are guaranteed to exist. The automated run factory clones the required projects
        on demand.

        :return:
        """
        self.debug('^^^^^^^^^^^^^ Initialize DVCPersister {} pull={}'.format(repository, pull))

        self.dvc.initialize()

        repository = format_repository_identifier(repository)
        self.active_repository = repo = GitRepoManager()

        root = os.path.join(paths.repository_dataset_dir, repository)
        repo.open_repo(root)

        remote = 'origin'
        if repo.has_remote(remote) and pull:
            self.info('pulling changes from repo: {}'.format(repository))
            self.active_repository.pull(remote=remote, use_progress=False)

    def pre_extraction_save(self):
        pass

    def post_extraction_save(self):

        rblob = self.per_spec.response_blob
        oblob = self.per_spec.output_blob
        sblob = self.per_spec.setpoint_blob

        if rblob:
            rblob = base64.b64encode(rblob)
        if oblob:
            oblob = base64.b64encode(oblob)
        if sblob:
            sblob = base64.b64encode(sblob)

        obj = {'request': rblob,
               'response': oblob,
               'sblob': sblob}

        pid = self.per_spec.pid
        if pid:
            obj['pid'] = pid

        for e in EXTRACTION_ATTRS:
            v = getattr(self.per_spec.run_spec, e)
            obj[e] = v

        ps = []
        for i, pp in enumerate(self.per_spec.positions):
            pos, x, y, z = None, None, None, None
            if isinstance(pp, tuple):
                if len(pp) == 2:
                    x, y = pp
                elif len(pp) == 3:
                    x, y, z = pp
            else:
                pos = pp
                try:
                    ep = self.per_spec.extraction_positions[i]
                    x = ep[0]
                    y = ep[1]
                    if len(ep) == 3:
                        z = ep[2]
                except IndexError:
                    self.debug('no extraction position for {}'.format(pp))
            pd = {'x': x, 'y': y, 'z': z, 'position': pos, 'is_degas': self.per_spec.run_spec.identifier == 'dg'}
            ps.append(pd)

        db = self.dvc.db
        load_name = self.per_spec.load_name
        for p in ps:
            db.add_measured_position(load=load_name, **p)

        obj['positions'] = ps
        hexsha = self.dvc.get_meta_head()
        obj['commit'] = str(hexsha)

        path = self._make_path(modifier='extraction')
        dvc_dump(obj, path)

    def pre_measurement_save(self):
        pass

    def post_measurement_save(self, commit=True, msg_prefix='Collection'):
        """
        save
            - analysis.json
            - analysis.monitor.json

        check if unique spectrometer.json
        commit changes
        push changes
        :return:
        """
        self.debug('================= post measurement started')
        ret = True

        # save spectrometer
        spec_sha = self._get_spectrometer_sha()
        spec_path = os.path.join(self.active_repository.path, '{}.json'.format(spec_sha))
        if not os.path.isfile(spec_path):
            self._save_spectrometer_file(spec_path)

        # self.dvc.meta_repo.save_gains(self.per_spec.run_spec.mass_spectrometer,
        #                               self.per_spec.gains)

        # save analysis

        if not self.per_spec.timestamp:
            timestamp = datetime.now()
        else:
            timestamp = self.per_spec.timestamp

        # check repository identifier before saving
        # will modify repository to NoRepo if repository_identifier does not exist
        self._check_repository_identifier()

        self._save_analysis(timestamp)

        with self.dvc.session_ctx():
            self._save_analysis_db(timestamp)

        # save monitor
        self._save_monitor()

        # save peak center
        self._save_peak_center(self.per_spec.peak_center)

        # stage files
        if self.stage_files:
            paths = [spec_path, ] + [self._make_path(modifier=m) for m in PATH_MODIFIERS]

            for p in paths:
                if os.path.isfile(p):
                    self.active_repository.add(p, commit=False, msg_prefix=msg_prefix)
                else:
                    self.debug('not at valid file {}'.format(p))

            if commit:
                try:
                    self.active_repository.smart_pull(accept_their=True)

                    # commit files
                    self.active_repository.commit('<COLLECTION>')
                    # self.active_repository.push()
                    self.dvc.push_repository(self.active_repository)

                    # update meta
                    self.dvc.meta_pull(accept_our=True)

                    self.dvc.meta_commit('repo updated for analysis {}'.format(self.per_spec.run_spec.runid))

                    # push commit
                    self.dvc.meta_push()
                except GitCommandError, e:
                    self.warning(e)
                    if not self.confirmation_dialog('DVC/Git Failed. Do you want to continue the experiment?',
                                                    timeout_ret=True,
                                                    timeout=30):
                        ret = False

        self.debug('================= post measurement finished')
        return ret

    # private
    def _check_repository_identifier(self):
        repo_id = self.per_spec.run_spec.repository_identifier
        db = self.dvc.db
        repo = db.get_repository(repo_id)
        if repo is None:
            self.warning('No repository named ="{}" changing to NoRepo'.format(repo_id))
            self.per_spec.run_spec.repository_identifier = 'NoRepo'
            repo = db.get_repository('NoRepo')
            if repo is None:
                db.add_repository('NoRepo', self.default_principal_investigator)

    def _save_analysis_db(self, timestamp):
        rs = self.per_spec.run_spec
        d = {k: getattr(rs, k) for k in ('uuid', 'analysis_type', 'aliquot',
                                         'increment', 'mass_spectrometer', 'weight', 'comment',
                                         'cleanup', 'duration', 'extract_value', 'extract_units')}

        ed = self.per_spec.run_spec.extract_device
        if ed in (None, '', NULL_STR, LINE_STR, 'Extract Device'):
            d['extract_device'] = 'No Extract Device'
        else:
            d['extract_device'] = ed

        d['timestamp'] = timestamp

        # save script names
        d['measurementName'] = self.per_spec.measurement_name
        d['extractionName'] = self.per_spec.extraction_name

        db = self.dvc.db
        an = db.add_analysis(**d)

        # all associations are handled by the ExperimentExecutor._retroactive_experiment_identifiers
        # *** _retroactive_experiment_identifiers is currently disabled ***

        if self.per_spec.use_repository_association:
            db.add_repository_association(rs.repository_identifier, an)

        self.debug('get identifier "{}"'.format(rs.identifier))
        pos = db.get_identifier(rs.identifier)
        self.debug('setting analysis irradiation position={}'.format(pos))
        an.irradiation_position = pos

        t = self.per_spec.tag

        db.flush()

        change = db.add_analysis_change(tag=t)
        an.change = change

        change = db.add_analysis_change(tag=t)
        an.change = change
        db.commit()

    def _save_analysis(self, timestamp):

        isos = {}
        dets = {}
        signals = []
        baselines = []
        sniffs = []
        blanks = {}
        intercepts = {}
        cbaselines = {}
        icfactors = {}

        if self.use_isotope_classifier:
            clf = self.isotope_classifier

        endianness = '>'
        for iso in self.per_spec.isotope_group.isotopes.values():

            sblob = base64.b64encode(iso.pack(endianness, as_hex=False))
            snblob = base64.b64encode(iso.sniff.pack(endianness, as_hex=False))
            signals.append({'isotope': iso.name, 'detector': iso.detector, 'blob': sblob})
            sniffs.append({'isotope': iso.name, 'detector': iso.detector, 'blob': snblob})

            isod = {'detector': iso.detector}
            if self.use_isotope_classifier:
                klass, prob = clf.predict_isotope(iso)
                isod.update(classification=klass,
                            classification_probability=prob)

            isos[iso.name] = isod
            if iso.detector not in dets:
                bblob = base64.b64encode(iso.baseline.pack(endianness, as_hex=False))
                baselines.append({'detector': iso.detector, 'blob': bblob})
                dets[iso.detector] = {'deflection': self.per_spec.defl_dict.get(iso.detector),
                                      'gain': self.per_spec.gains.get(iso.detector)}

                icfactors[iso.detector] = {'value': float(nominal_value(iso.ic_factor or 1)),
                                           'error': float(std_dev(iso.ic_factor or 0)),
                                           'fit': 'default',
                                           'references': []}
                cbaselines[iso.detector] = {'fit': iso.baseline.fit,
                                            'filter_outliers_dict': iso.baseline.filter_outliers_dict,
                                            'value': float(nominal_value(iso.baseline.uvalue)),
                                            'error': float(std_dev(iso.baseline.uvalue))}

            intercepts[iso.name] = {'fit': iso.fit,
                                    'filter_outliers_dict': iso.filter_outliers_dict,
                                    'value': float(nominal_value(iso.uvalue)),
                                    'error': float(std_dev(iso.uvalue))}
            blanks[iso.name] = {'fit': 'previous',
                                'references': [{'runid': self.per_spec.previous_blank_runid,
                                                'exclude': False}],
                                'value': float(nominal_value(iso.blank.uvalue)),
                                'error': float(std_dev(iso.blank.uvalue))}

        obj = self._make_analysis_dict()

        from pychron.experiment import __version__ as eversion
        from pychron.dvc import __version__ as dversion

        obj['timestamp'] = timestamp.isoformat()
        obj['collection_version'] = '{}:{}'.format(eversion, dversion)
        obj['detectors'] = dets
        obj['isotopes'] = isos
        obj['spec_sha'] = self._get_spectrometer_sha()
        obj['intensity_scalar'] = self.per_spec.intensity_scalar

        # save the conditionals
        obj['conditionals'] = [c.to_dict() for c in self.per_spec.conditionals] if \
            self.per_spec.conditionals else None
        obj['tripped_conditional'] = self.per_spec.tripped_conditional.result_dict() if \
            self.per_spec.tripped_conditional else None

        # save the scripts
        ms = self.per_spec.run_spec.mass_spectrometer
        for si in ('measurement', 'extraction'):
            name = getattr(self.per_spec, '{}_name'.format(si))
            blob = getattr(self.per_spec, '{}_blob'.format(si))
            self.dvc.meta_repo.update_script(ms, name, blob)
            obj[si] = name

        # save experiment
        self.debug('---------------- Experiment Queue saving disabled')
        # self.dvc.update_experiment_queue(ms, self.per_spec.experiment_queue_name,
        #                                  self.per_spec.experiment_queue_blob)

        hexsha = str(self.dvc.get_meta_head())
        obj['commit'] = hexsha

        # dump runid.json
        p = self._make_path()
        dvc_dump(obj, p)

        p = self._make_path(modifier='intercepts')
        dvc_dump(intercepts, p)

        # dump runid.blank.json
        p = self._make_path(modifier='blanks')
        dvc_dump(blanks, p)

        p = self._make_path(modifier='baselines')
        dvc_dump(cbaselines, p)

        p = self._make_path(modifier='icfactors')
        dvc_dump(icfactors, p)

        # dump runid.data.json
        p = self._make_path(modifier='.data')
        data = {'commit': hexsha,
                'encoding': 'base64',
                'format': '{}ff'.format(endianness),
                'signals': signals, 'baselines': baselines, 'sniffs': sniffs}
        dvc_dump(data, p)

    def _save_monitor(self):
        if self.per_spec.monitor:
            p = self._make_path(modifier='monitor')
            checks = []
            for ci in self.per_spec.monitor.checks:
                data = ''.join([struct.pack('>ff', x, y) for x, y in ci.data])
                params = dict(name=ci.name,
                              parameter=ci.parameter, criterion=ci.criterion,
                              comparator=ci.comparator, tripped=ci.tripped,
                              data=data)
                checks.append(params)

            dvc_dump(checks, p)

    def _save_spectrometer_file(self, path):
        obj = dict(spectrometer=dict(self.per_spec.spec_dict),
                   gains=dict(self.per_spec.gains),
                   deflections=dict(self.per_spec.defl_dict))
        # hexsha = self.dvc.get_meta_head()
        # obj['commit'] = str(hexsha)

        dvc_dump(obj, path)

    def _save_peak_center(self, pc):
        self.info('DVC saving peakcenter')
        p = self._make_path(modifier='peakcenter')

        obj = {}
        if pc:
            obj['reference_detector'] = pc.reference_detector.name
            obj['reference_isotope'] = pc.reference_isotope
            fmt = '>ff'
            obj['fmt'] = fmt
            results = pc.get_results()
            if results:
                for result in results:
                    obj[result.detector] = {'low_dac': result.low_dac,
                                            'center_dac': result.center_dac,
                                            'high_dac': result.high_dac,
                                            'low_signal': result.low_signal,
                                            'center_signal': result.center_signal,
                                            'high_signal': result.high_signal,
                                            'points': base64.b64encode(''.join([struct.pack(fmt, *di)
                                                                                for di in result.points]))}

            # if pc.result:
            #     xs, ys, _mx, _my = pc.result
            #     obj.update({'low_dac': xs[0],
            #                 'center_dac': xs[1],
            #                 'high_dac': xs[2],
            #                 'low_signal': ys[0],
            #                 'center_signal': ys[1],
            #                 'high_signal': ys[2]})
            #
            # data = pc.get_data()
            # if data:
            #     fmt = '>ff'
            #     obj['fmt'] = fmt
            #     for det, pts in data:
            #         obj[det] = base64.b64encode(''.join([struct.pack(fmt, *di) for di in pts]))

        dvc_dump(obj, p)

    def _make_path(self, modifier=None, extension='.json'):
        runid = self.per_spec.run_spec.runid
        repository_identifier = self.per_spec.run_spec.repository_identifier
        return analysis_path(runid, repository_identifier, modifier, extension, mode='w')

    def _make_analysis_dict(self, keys=None):
        rs = self.per_spec.run_spec
        if keys is None:
            keys = META_ATTRS

        d = {k: getattr(rs, k) for k in keys}
        return d

    def _get_spectrometer_sha(self):
        """
        return a sha-1 hash.

        generate using spec_dict, defl_dict, and gains
        spec_dict: source parameters, cdd operating voltage
        defl_dict: detector deflections
        gains: detector gains

        make hash using
        for key,value in dictionary:
            sha1.update(key)
            sha1.update(value)

        to ensure consistence, dictionaries are sorted by key
        for key,value in sorted(dictionary)
        :return:
        """
        return spectrometer_sha(self.per_spec.spec_dict, self.per_spec.defl_dict, self.per_spec.gains)

# ============= EOF =============================================
        #         self._save_measured_positions()
        #
        #
        # def _save_measured_positions(self):
        #     dvc = self.dvc
        #
        #     load_name = self.per_spec.load_name
        #     for i, pp in enumerate(self.per_spec.positions):
        #         if isinstance(pp, tuple):
        #             if len(pp) > 1:
        #                 if len(pp) == 3:
        #                     dvc.add_measured_position('', load_name, x=pp[0], y=pp[1], z=pp[2])
        #                 else:
        #                     dvc.add_measured_position('', load_name, x=pp[0], y=pp[1])
        #             else:
        #                 dvc.add_measured_position(pp[0], load_name)
        #
        #         else:
        #             dbpos = dvc.add_measured_position(pp, load_name)
        #             try:
        #                 ep = self.per_spec.extraction_positions[i]
        #                 dbpos.x = ep[0]
        #                 dbpos.y = ep[1]
        #                 if len(ep) == 3:
        #                     dbpos.z = ep[2]
        #             except IndexError:
        #                 self.debug('no extraction position for {}'.format(pp))
