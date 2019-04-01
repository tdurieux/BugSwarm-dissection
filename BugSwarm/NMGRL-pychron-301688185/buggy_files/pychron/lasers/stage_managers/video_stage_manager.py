# ===============================================================================
# Copyright 2011 Jake Ross
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
import os
import shutil
import time
from threading import Thread, Timer, Event as TEvent

from apptools.preferences.preference_binding import bind_preference
from numpy import copy, array
from traits.api import Instance, String, Property, Button, Bool, Event, on_trait_change, Str, Float

from pychron.canvas.canvas2D.camera import Camera
from pychron.core.helpers.filetools import unique_path, unique_path_from_manifest
from pychron.core.ui.stage_component_editor import VideoComponentEditor
from pychron.image.video import Video, pil_save
from pychron.mv.lumen_detector import LumenDetector
from pychron.paths import paths
from stage_manager import StageManager

try:
    from pychron.canvas.canvas2D.video_laser_tray_canvas import \
        VideoLaserTrayCanvas
except ImportError:
    from pychron.canvas.canvas2D.laser_tray_canvas import \
        LaserTrayCanvas as VideoLaserTrayCanvas


class VideoStageManager(StageManager):
    """
    """
    video = Instance(Video)
    canvas_editor_klass = VideoComponentEditor

    camera_zoom_coefficients = Property(String(enter_set=True, auto_set=False),
                                        depends_on='_camera_zoom_coefficients')
    _camera_zoom_coefficients = String

    use_auto_center_interpolation = Bool(False)

    autocenter_button = Button('AutoCenter')
    configure_autocenter_button = Button('Configure')

    autocenter_manager = Instance(
        'pychron.mv.autocenter_manager.AutoCenterManager')
    autofocus_manager = Instance(
        'pychron.mv.focus.autofocus_manager.AutoFocusManager')

    # zoom_calibration_manager = Instance(
    #     'pychron.mv.zoom.zoom_calibration.ZoomCalibrationManager')

    snapshot_button = Button('Snapshot')
    auto_save_snapshot = Bool(True)

    record = Event
    record_label = Property(depends_on='is_recording')
    is_recording = Bool

    use_db = False

    use_video_archiver = Bool(True)
    video_archiver = Instance('pychron.core.helpers.archiver.Archiver')
    video_identifier = Str

    # use_video_server = Bool(False)
    # video_server_port = Int
    # video_server_quality = Int
    # video_server = Instance('pychron.image.video_server.VideoServer')

    use_media_storage = Bool(False)
    auto_upload = Bool(False)
    keep_local_copy = Bool(False)

    camera = Instance(Camera)
    lumen_detector = Instance(LumenDetector, ())

    render_with_markup = Bool(False)

    _auto_correcting = False
    stop_timer = Event

    pxpermm = Float(23)

    _measure_grain_t = None
    _measure_grain_evt = None

    def motor_event_hook(self, name, value, *args, **kw):
        if name == 'zoom':
            self._update_zoom(value)

    def bind_preferences(self, pref_id):
        self.debug('binding preferences')
        super(VideoStageManager, self).bind_preferences(pref_id)
        if self.autocenter_manager:
            bind_preference(self.autocenter_manager, 'use_autocenter',
                            '{}.use_autocenter'.format(pref_id))

        bind_preference(self, 'render_with_markup',
                        '{}.render_with_markup'.format(pref_id))
        bind_preference(self, 'auto_upload', '{}.auto_upload'.format(pref_id))
        bind_preference(self, 'use_media_storage', '{}.use_media_storage'.format(pref_id))
        bind_preference(self, 'keep_local_copy', '{}.keep_local_copy'.format(pref_id))

        bind_preference(self, 'use_video_archiver',
                        '{}.use_video_archiver'.format(pref_id))

        bind_preference(self, 'video_identifier',
                        '{}.video_identifier'.format(pref_id))

        bind_preference(self, 'use_video_server',
                        '{}.use_video_server'.format(pref_id))

        bind_preference(self.video_archiver, 'archive_months',
                        '{}.video_archive_months'.format(pref_id))
        bind_preference(self.video_archiver, 'archive_days',
                        '{}.video_archive_days'.format(pref_id))
        bind_preference(self.video_archiver, 'archive_hours',
                        '{}.video_archive_hours'.format(pref_id))
        bind_preference(self.video_archiver, 'root',
                        '{}.video_directory'.format(pref_id))

        bind_preference(self.video, 'output_mode',
                        '{}.video_output_mode'.format(pref_id))
        bind_preference(self.video, 'ffmpeg_path',
                        '{}.ffmpeg_path'.format(pref_id))

    def get_grain_mask(self):
        ld = self.lumen_detector
        l, m = ld.lum()
        return m.tostring()

    def get_grain_masks_blob(self):
        return array(self.grain_masks).tostring()

    def stop_measure_grain_mask(self):
        if self._measure_grain_evt:
            self._measure_grain_evt.set()

    def start_measure_grain_mask(self):
        self._measure_grain_evt = evt = TEvent()

        def _measure_grain_mask():
            ld = self.lumen_detector

            masks = []
            while not evt.is_set():
                l, m = ld.lum(copy(self.video.get_cached_frame()))
                masks.append(l)
                evt.wait(1)
            self.grain_masks = masks

        self._measure_grain_t = Thread(target=_measure_grain_mask)
        self._measure_grain_t.start()

    def start_recording(self, new_thread=True, path=None, use_dialog=False, basename='vm_recording', **kw):
        """
        """

        if path is None:
            if use_dialog:
                path = self.save_file_dialog()
            else:
                vd = self.video_archiver.root
                self.debug('video archiver root {}'.format(vd))
                if not vd:
                    vd = paths.video_dir

                path = unique_path_from_manifest(vd, basename, extension='avi')

        kw['path'] = path
        kw['basename'] = basename
        if new_thread:
            t = Thread(target=self._start_recording, kwargs=kw)
            t.start()
        else:
            self._start_recording(**kw)
        self.is_recording = True
        return path

    def stop_recording(self, user='remote', delay=None):
        """
        """

        def close():
            self.is_recording = False
            self.info('stop video recording')
            p = self.video.output_path
            if self.video.stop_recording(wait=True):
                if self.auto_upload:
                    p = self._upload(p, inform=False)
            return p

        if self.video.is_recording():
            if delay:
                t = Timer(delay, close)
                t.start()
            else:
                return close()

    def initialize_video(self):
        if self.video:
            self.video.open(
                identifier=self.video_identifier)

    def initialize_stage(self):
        super(VideoStageManager, self).initialize_stage()
        self.initialize_video()

        # s = self.stage_controller
        # if s.axes:
        #     xa = s.axes['x'].drive_ratio
        #     ya = s.axes['y'].drive_ratio

        # self._drive_xratio = xa
        # self._drive_yratio = ya

        self._update_zoom(0)

    def autocenter(self, *args, **kw):
        return self._autocenter(*args, **kw)

    def snapshot(self, path=None, name=None, auto=False,
                 inform=True, return_blob=False, pic_format='.jpg'):
        """
            path: abs path to use
            name: base name to use if auto saving in default dir
            auto: force auto save

            returns:
                    path: local abs path
                    upath: remote abs path
        """

        if path is None:
            if self.auto_save_snapshot or auto:

                if name is None:
                    name = 'snapshot'
                path = unique_path_from_manifest(paths.snapshot_dir, name, pic_format)
            elif name is not None:
                if not os.path.isdir(os.path.dirname(name)):
                    path = unique_path_from_manifest(paths.snapshot_dir, name, pic_format)
                else:
                    path = name

            else:
                path = self.save_file_dialog()

        if path:
            self.info('saving snapshot {}'.format(path))
            # play camera shutter sound
            # play_sound('shutter')

            self._render_snapshot(path)
            if self.auto_upload:
                upath = self._upload(path, inform=inform)
                if upath is None:
                    upath = ''

                if inform:
                    if self.keep_local_copy:
                        self.information_dialog('Snapshot saved: "{}".\nUploaded  : "{}"'.format(path, upath))
                    else:
                        self.information_dialog('Snapshot uploaded to "{}"'.format(upath))
            else:
                upath = None
                if inform:
                    self.information_dialog('Snapshot saved to "{}"'.format(path))

            if return_blob:
                with open(path, 'rb') as rfile:
                    im = rfile.read()
                    return path, upath, im
            else:
                return path, upath

    def kill(self):
        """
        """

        super(VideoStageManager, self).kill()
        if self.camera:
            self.camera.save_calibration()

        self.stop_timer = True

        self.canvas.close_video()
        if self.video:
            self.video.close(force=True)

        # if self.use_video_server:
        #            self.video_server.stop()
        # if self._stage_maps:
        #     for s in self._stage_maps:
        #         s.dump_correction_file()

        self.clean_video_archive()

    def clean_video_archive(self):
        if self.use_video_archiver:
            self.info('Cleaning video directory')
            self.video_archiver.clean(('manifest.yaml',))

    def is_auto_correcting(self):
        return self._auto_correcting

    crop_width = 5
    crop_height = 5

    def get_scores(self, **kw):
        ld = self.lumen_detector

        src = self.video.get_cached_frame()

        csrc = copy(src)
        return ld.get_scores(csrc, **kw)

    def get_brightness(self, **kw):
        ld = self.lumen_detector

        src = self.video.get_cached_frame()

        csrc = copy(src)
        src, v = ld.get_value(csrc, **kw)
        return csrc, src, v

    def get_frame_size(self):
        cw = 2 * self.crop_width * self.pxpermm
        ch = 2 * self.crop_height * self.pxpermm
        return cw, ch

    def close_open_images(self):
        if self.autocenter_manager:
            self.autocenter_manager.close_open_images()

    def finish_move_to_hole(self, user_entry):
        self.debug('finish move to hole')
        # if user_entry and not self.keep_images_open:
        #     self.close_open_images()

    # private
    def _stage_map_changed_hook(self):
        self.lumen_detector.hole_radius = self.stage_map.g_dimension

    def _upload(self, src, inform=True):
        if not self.use_media_storage:
            msg = 'Use Media Storage not enabled in Laser preferences'
            if inform:
                self.warning_dialog(msg)
            else:
                self.warning(msg)
        else:
            srv = 'pychron.media_storage.manager.MediaStorageManager'
            msm = self.parent.application.get_service(srv)
            if msm is not None:
                dest = os.path.join(self.parent.name, os.path.basename(src))
                msm.put(src, dest)

                if not self.keep_local_copy:
                    self.debug('removing {}'.format(src))
                    if src.endswith('.avi'):
                        head, ext = os.path.splitext(src)
                        vd = '{}-images'.format(head)
                        self.debug('removing video build directory {}'.format(vd))
                        shutil.rmtree(vd)

                    os.remove(src)
                dest = '{}/{}'.format(msm.get_base_url(), dest)
                return dest
            else:
                msg = 'Media Storage Plugin not enabled'
                if inform:
                    self.warning_dialog(msg)
                else:
                    self.warning(msg)

    def _render_snapshot(self, path):
        from chaco.plot_graphics_context import PlotGraphicsContext

        c = self.canvas
        p = None
        was_visible = False
        if not self.render_with_markup:
            p = c.show_laser_position
            c.show_laser_position = False
            if self.points_programmer.is_visible:
                c.hide_all()
                was_visible = True

        gc = PlotGraphicsContext((int(c.outer_width), int(c.outer_height)))
        c.do_layout()
        gc.render_component(c)
        # gc.save(path)
        from pychron.core.helpers import save_gc
        save_gc.save(gc, path)

        if p is not None:
            c.show_laser_position = p

        if was_visible:
            c.show_all()

    def _start_recording(self, path, basename):

        self.info('start video recording {}'.format(path))
        d = os.path.dirname(path)
        if not os.path.isdir(d):
            self.warning('invalid directory {}'.format(d))
            self.warning('using default directory')
            path, _ = unique_path(paths.video_dir, basename,
                                  extension='avi')

        self.info('saving recording to path {}'.format(path))

        # if self.use_db:
        # db = self.get_video_database()
        #     db.connect()
        #
        #     v = db.add_video_record(rid=basename)
        #     db.add_path(v, path)
        #     self.info('saving {} to database'.format(basename))
        #     db.commit()

        video = self.video

        def renderer(p):
            cw, ch = self.get_frame_size()
            frame = video.get_cached_frame()
            frame = video.crop(frame, 0, 0, cw, ch)
            if frame is not None:
                pil_save(frame, p)

        if self.render_with_markup:
            renderer = self._render_snapshot

        self.video.start_recording(path, renderer)

    def _move_to_hole_hook(self, holenum, correct, autocentered_position):
        args = holenum, correct, autocentered_position
        self.debug('move to hole hook holenum={}, '
                   'correct={}, autocentered_position={}'.format(*args))
        if correct:
            ntries = 1 if autocentered_position else 3

            self._auto_correcting = True
            self._autocenter(holenum=holenum, ntries=ntries, save=True)
            self._auto_correcting = False

    def find_center(self):
        ox, oy = self.canvas.get_screen_offset()
        rpos, src = self.autocenter_manager.calculate_new_center(
            self.stage_controller.x,
            self.stage_controller.y,
            ox, oy,
            dim=self.stage_map.g_dimension, open_image=False)

        return rpos, src

    def find_target(self):
        if self.video:
            ox, oy = self.canvas.get_screen_offset()
            src = self.video.get_cached_frame()

            ch = cw = self.pxpermm * self.stage_map.g_dimension * 2.5
            src = self.video.crop(src, ox, oy, cw, ch)
            return self.lumen_detector.find_target(src)

    def find_best_target(self):
        if self.video:
            src = self.video.get_cached_frame()
            src = self.autocenter_manager.crop(src)
            return self.lumen_detector.find_best_target(src)

    def _autocenter(self, holenum=None, ntries=3, save=False,
                    use_interpolation=False, inform=False,
                    alpha_enabled=True,
                    auto_close_image=True):
        self.debug('do autocenter')
        rpos = None
        interp = False
        sm = self.stage_map
        st = time.time()
        if self.autocenter_manager.use_autocenter:
            time.sleep(0.1)
            ox, oy = self.canvas.get_screen_offset()
            for ti in xrange(max(1, ntries)):
                # use machine vision to calculate positioning error
                args = self.autocenter_manager.calculate_new_center(
                    self.stage_controller.x,
                    self.stage_controller.y,
                    ox, oy,
                    dim=self.stage_map.g_dimension,
                    alpha_enabled=alpha_enabled,
                    auto_close_image=auto_close_image)

                if args is not None:
                    rpos, _ = args
                    self.linear_move(*rpos, block=True,
                                     use_calibration=False,
                                     update_hole=False,
                                     velocity_scalar=0.1)
                    time.sleep(0.1)
                else:
                    self.snapshot(auto=True,
                                  name='pos_err_{}_{}'.format(holenum, ti),
                                  inform=inform)
                    break

                    # if use_interpolation and rpos is None:
                    #     self.info('trying to get interpolated position')
                    #     rpos = sm.get_interpolated_position(holenum)
                    #     if rpos:
                    #         s = '{:0.3f},{:0.3f}'
                    #         interp = True
                    #     else:
                    #         s = 'None'
                    #     self.info('interpolated position= {}'.format(s))

        if rpos:
            corrected = True
            # add an adjustment value to the stage map
            if save and holenum is not None:
                sm.set_hole_correction(holenum, *rpos)
                sm.dump_correction_file()
                #            f = 'interpolation' if interp else 'correction'
        else:
            #            f = 'uncorrected'
            corrected = False
            if holenum is not None:
                rpos = sm.get_hole(holenum).nominal_position
        self.debug('Autocenter duration ={}'.format(time.time() - st))
        return rpos, corrected, interp

    # ===============================================================================
    # views
    # ===============================================================================
    # ===============================================================================
    # view groups
    # ===============================================================================

    # ===============================================================================
    # handlers
    # ===============================================================================
    def _update_zoom(self, v):
        if self.canvas.camera:
            self._update_xy_limits()

    @on_trait_change('parent:motor_event')
    def _update_motor(self, new):
        print 'motor event', new, self.canvas, self.canvas.camera
        # s = self.stage_controller
        if self.canvas.camera:
            if not isinstance(new, (int, float)):
                args, _ = new
                name, v = args[:2]
            else:
                name = 'zoom'
                v = new

            if name == 'zoom':
                self._update_xy_limits()
                # pxpermm = self.canvas.camera.set_limits_by_zoom(v, s.x, s.y)
                # self.pxpermm = pxpermm
            elif name == 'beam':
                self.lumen_detector.beam_radius = v / 2.0

    def _pxpermm_changed(self, new):
        if self.autocenter_manager:
            self.autocenter_manager.pxpermm = new
            self.lumen_detector.pxpermm = new
            # self.lumen_detector.mask_radius = new*self.stage_map.g_dimension

    def _autocenter_button_fired(self):
        self.goto_position(self.calibrated_position_entry, autocenter_only=True)

    #     def _configure_autocenter_button_fired(self):
    #         info = self.autocenter_manager.edit_traits(view='configure_view',
    #                                                 kind='livemodal')
    #         if info.result:
    #             self.autocenter_manager.dump_detector()

    def _snapshot_button_fired(self):
        self.snapshot()

    def _record_fired(self):
        #            time.sleep(4)
        #            self.stop_recording()
        if self.is_recording:

            self.stop_recording()
        else:

            self.start_recording()

    def _use_video_server_changed(self):
        if self.use_video_server:
            self.video_server.start()
        else:
            self.video_server.stop()

    def _get_camera_zoom_coefficients(self):
        return self.canvas.camera.zoom_coefficients

    def _set_camera_zoom_coefficients(self, v):
        self.canvas.camera.zoom_coefficients = ','.join(map(str, v))
        self._update_xy_limits()

    def _validate_camera_zoom_coefficients(self, v):
        try:
            return map(float, v.split(','))
        except ValueError:
            pass

    def _update_xy_limits(self):
        z = 0
        if self.parent is not None:
            zoom = self.parent.get_motor('zoom')
            if zoom is not None:
                z = zoom.data_position

        x = self.stage_controller.get_current_position('x')
        y = self.stage_controller.get_current_position('y')
        pxpermm = self.canvas.camera.set_limits_by_zoom(z, x, y)
        self.canvas.request_redraw()
        self.pxpermm = pxpermm

        self.debug('updated xy limits zoom={}, pxpermm={}'.format(z, pxpermm))

    def _get_record_label(self):
        return 'Start Recording' if not self.is_recording else 'Stop'

    # ===============================================================================
    # factories
    # ===============================================================================
    def _canvas_factory(self):
        """
        """
        try:
            video = self.video
        except AttributeError:
            self.warning('Video not Available')
            video = None

        v = VideoLaserTrayCanvas(stage_manager=self,
                                 padding=30,
                                 video=video,
                                 camera=self.camera)
        self.camera.parent = v
        return v

    def _canvas_editor_factory(self):
        e = super(VideoStageManager, self)._canvas_editor_factory()
        e.stop_timer = 'stop_timer'
        return e

    # ===============================================================================
    # defaults
    # ===============================================================================
    def _camera_default(self):
        camera = Camera()

        p = os.path.join(paths.canvas2D_dir, 'camera.cfg')
        camera.load(p)

        #        camera.current_position = (0, 0)
        camera.set_limits_by_zoom(0, 0, 0)

        vid = self.video
        if vid:
            # swap red blue channels True or False
            vid.swap_rb = camera.swap_rb

            vid.vflip = camera.vflip
            vid.hflip = camera.hflip

        self._camera_zoom_coefficients = camera.zoom_coefficients

        return camera

    def _video_default(self):
        v = Video()
        return v

    def _video_server_default(self):
        from pychron.image.video_server import VideoServer

        return VideoServer(video=self.video)

    def _video_archiver_default(self):
        from pychron.core.helpers.archiver import Archiver

        return Archiver()

    def _autocenter_manager_default(self):
        if self.parent.mode != 'client':
            # from pychron.mv.autocenter_manager import AutoCenterManager
            if 'co2' in self.parent.name.lower():
                from pychron.mv.autocenter_manager import CO2AutocenterManager
                klass = CO2AutocenterManager
            else:
                from pychron.mv.autocenter_manager import DiodeAutocenterManager
                klass = DiodeAutocenterManager

            return klass(video=self.video,
                         canvas=self.canvas,
                         application=self.application)

    def _autofocus_manager_default(self):
        if self.parent.mode != 'client':
            from pychron.mv.focus.autofocus_manager import AutoFocusManager

            return AutoFocusManager(video=self.video,
                                    laser_manager=self.parent,
                                    stage_controller=self.stage_controller,
                                    canvas=self.canvas,
                                    application=self.application)

            # def _zoom_calibration_manager_default(self):
            #     if self.parent.mode != 'client':
            #         from pychron.mv.zoom.zoom_calibration import ZoomCalibrationManager
            #         return ZoomCalibrationManager(laser_manager=self.parent)

# ===============================================================================
# calcualte camera params
# ===============================================================================
# def _calculate_indicator_positions(self, shift=None):
#        ccm = self.camera_calibration_manager
#
#        zoom = self.parent.zoom
#        pychron, name = self.video_manager.snapshot(identifier=zoom)
#        ccm.image_factory(pychron=pychron)
#
#        ccm.process_image()
#        ccm.title = name
#
#        cond = Condition()
#        ccm.cond = cond
#        cond.acquire()
#        do_later(ccm.edit_traits, view='snapshot_view')
#        if shift:
#            self.stage_controller.linear_move(*shift, block=False)
#
#        cond.wait()
#        cond.release()
#
#    def _calculate_camera_parameters(self):
#        ccm = self.camera_calibration_manager
#        self._calculate_indicator_positions()
#        if ccm.result:
#            if self.calculate_offsets:
#                rdxmm = 5
#                rdymm = 5
#
#                x = self.stage_controller.x + rdxmm
#                y = self.stage_controller.y + rdymm
#                self.stage_controller.linear_move(x, y, block=True)
#
#                time.sleep(2)
#
#                polygons1 = ccm.polygons
#                x = self.stage_controller.x - rdxmm
#                y = self.stage_controller.y - rdymm
#                self._calculate_indicator_positions(shift=(x, y))
#
#                polygons2 = ccm.polygons
#
#                # compare polygon sets
#                # calculate pixel displacement
#                dxpx = sum([sum([(pts1.x - pts2.x)
#                                for pts1, pts2 in zip(p1.points, p2.points)]) / len(p1.points)
#                                    for p1, p2 in zip(polygons1, polygons2)]) / len(polygons1)
#                dypx = sum([sum([(pts1.y - pts2.y)
#                                for pts1, pts2 in zip(p1.points, p2.points)]) / len(p1.points)
#                                    for p1, p2 in zip(polygons1, polygons2)]) / len(polygons1)
#
#                # convert pixel displacement to mm using defined mapping
#                dxmm = dxpx / self.pxpercmx
#                dymm = dypx / self.pxpercmy
#
#                # calculate drive offset. ratio of request/actual
#                try:
#                    self.drive_xratio = rdxmm / dxmm
#                    self.drive_yratio = rdymm / dymm
#                except ZeroDivisionError:
#                    self.drive_xratio = 100
#
#    def _calibration_manager_default(self):
#
# #        self.video.open(user = 'calibration')
#        return CalibrationManager(parent = self,
#                                  laser_manager = self.parent,
#                               video_manager = self.video_manager,
#                               )
# ============= EOF ====================================
#                adxs = []
#                adys = []
#                for p1, p2 in zip(polygons, polygons2):
# #                    dxs = []
# #                    dys = []
# #                    for pts1, pts2 in zip(p1.points, p2.points):
# #
# #                        dx = pts1.x - pts2.x
# #                        dy = pts1.y - pts2.y
# #                        dxs.append(dx)
# #                        dys.append(dy)
# #                    dxs = [(pts1.x - pts2.x) for pts1, pts2 in zip(p1.points, p2.points)]
# #                    dys = [(pts1.y - pts2.y) for pts1, pts2 in zip(p1.points, p2.points)]
# #
#                    adx = sum([(pts1.x - pts2.x) for pts1, pts2 in zip(p1.points, p2.points)]) / len(p1.points)
#                    ady = sum([(pts1.y - pts2.y) for pts1, pts2 in zip(p1.points, p2.points)]) / len(p1.points)
#
# #                    adx = sum(dxs) / len(dxs)
# #                    ady = sum(dys) / len(dys)
#                    adxs.append(adx)
#                    adys.append(ady)
#                print 'xffset', sum(adxs) / len(adxs)
#                print 'yffset', sum(adys) / len(adys)
