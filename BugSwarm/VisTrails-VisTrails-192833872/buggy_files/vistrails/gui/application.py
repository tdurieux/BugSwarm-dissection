###############################################################################
##
## Copyright (C) 2014-2016, New York University.
## Copyright (C) 2011-2014, NYU-Poly.
## Copyright (C) 2006-2011, University of Utah.
## All rights reserved.
## Contact: contact@vistrails.org
##
## This file is part of VisTrails.
##
## "Redistribution and use in source and binary forms, with or without
## modification, are permitted provided that the following conditions are met:
##
##  - Redistributions of source code must retain the above copyright notice,
##    this list of conditions and the following disclaimer.
##  - Redistributions in binary form must reproduce the above copyright
##    notice, this list of conditions and the following disclaimer in the
##    documentation and/or other materials provided with the distribution.
##  - Neither the name of the New York University nor the names of its
##    contributors may be used to endorse or promote products derived from
##    this software without specific prior written permission.
##
## THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
## AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
## THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
## PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
## CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
## EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
## PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
## OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
## WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
## OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
## ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."
##
###############################################################################
""" This is the main application of vistrail, it will calls for
initializations to the theme, packages and the builder...

"""
from __future__ import division

from ast import literal_eval
import copy
import os.path
import getpass
import sys
import StringIO
import usagestats

from PyQt4 import QtGui, QtCore, QtNetwork

from vistrails.core.application import VistrailsApplicationInterface, \
    get_vistrails_application, set_vistrails_application
from vistrails.core import debug
from vistrails.core import system
from vistrails.core.application import APP_SUCCESS, APP_FAIL, APP_DONE
from vistrails.core.db.io import load_vistrail
from vistrails.core.db.locator import FileLocator, DBLocator
from vistrails.core import reportusage
import vistrails.core.requirements
from vistrails.core.vistrail.controller import VistrailController
from vistrails.db import VistrailsDBException
from vistrails.db.services.io import test_db_connection
from vistrails.gui import qt
import vistrails.gui.theme


def global_ui_fixes():
    # Prevent Mac OS 10.7 from restoring windows state since it would make Qt
    # 4.7.3 unstable due to its lack of handling Cocoa's Main Window.
    if platform.system() == 'Darwin':
        release = platform.mac_ver()[0].split('.')
        if len(release) >= 2 and (int(release[0]), int(release[1])) >= (10, 7):
            ss_path = os.path.expanduser('~/Library/Saved Application State/'
                                         'org.vistrails.savedState')
            if os.path.exists(ss_path):
                os.system('rm -rf "%s"' % ss_path)
            os.system('defaults write org.vistrails NSQuitAlwaysKeepsWindows '
                      '-bool false')

    # font bugfix for Qt 4.8 and OS X 10.9
    if platform.system() == 'Darwin':
        release = platform.mac_ver()[0].split('.')
        if len(release) >= 2 and (int(release[0]), int(release[1])) >= (1, 9):
            QtGui.QFont.insertSubstitution(".Lucida Grande UI", "Lucida Grande")


class VistrailsApplicationSingleton(VistrailsApplicationInterface,
                                    QtGui.QApplication):
    """
    VistrailsApplicationSingleton is the singleton of the application,
    there will be only one instance of the application during VisTrails
    
    """

    use_event_filter = system.systemType in ['Darwin']
    timeout = 15000
    execution_timeout = 600000

    def __call__(self):
        """ __call__() -> VistrailsApplicationSingleton
        Return self for calling method
        
        """
        if not self._initialized and not self._is_running:
            self.init()
        return self

    def __init__(self):
        global_ui_fixes()

        QtGui.QApplication.__init__(self, sys.argv)
        VistrailsApplicationInterface.__init__(self)

        if self.use_event_filter:
            self.installEventFilter(self)
        self.builderWindow = None
        # local notifications
        self.window_notifications = {}
        self.view_notifications = {}

        if QtCore.QT_VERSION < 0x40200: # 0x40200 = 4.2.0
            raise vistrails.core.requirements.MissingRequirement("Qt version >= 4.2")
        self._is_running = False
        self.shared_memory = None
        self.local_server = None
        self.setAttribute(QtCore.Qt.AA_DontShowIconsInMenus)
        qt.allowQObjects()

    def run_single_instance(self, args):
        # code for single instance of the application
        # based on the C++ solution available at
        # http://wiki.qtcentre.org/index.php?title=SingleApplication
        if QtCore.QT_VERSION >= 0x40400:
            self._unique_key = os.path.join(system.home_directory(),
                       "vistrails-single-instance-check-%s"%getpass.getuser())
            self.shared_memory = QtCore.QSharedMemory(self._unique_key)
            self.local_server = None
            if self.shared_memory.attach():
                self._is_running = True

                local_socket = QtNetwork.QLocalSocket(self)
                local_socket.connectToServer(self._unique_key)
                if not local_socket.waitForConnected(self.timeout):
                    debug.critical(
                            "Connection failed: %s\n"
                            "Removing socket" % (local_socket.errorString()))
                    try:
                        os.remove(self._unique_key)
                    except OSError, e:
                        debug.critical("Couldn't remove socket: %s" %
                                       self._unique_key, e)

                else:
                    if self.found_another_instance_running(local_socket, args):
                        return APP_DONE # success, we should shut down
                    else:  # This is bad, but not fatal. Let's keep going...
                        debug.critical("Failed to communicate with existing "
                                       "instance")
                        return

            if not self.shared_memory.create(1):
                debug.critical("Unable to create single instance "
                               "of vistrails application")
                return
            self.local_server = QtNetwork.QLocalServer(self)
            self.connect(self.local_server, QtCore.SIGNAL("newConnection()"),
                         self.message_received)
            if self.local_server.listen(self._unique_key):
                debug.log("Listening on %s"%self.local_server.fullServerName())
            else:
                # This usually happens when vistrails have crashed
                # Delete the key and try again
                self.shared_memory.detach()
                self.local_server.close()
                if os.path.exists(self._unique_key):
                    os.remove(self._unique_key)

                self.shared_memory = QtCore.QSharedMemory(self._unique_key)
                self.local_server = None

                if not self.shared_memory.create(1):
                    debug.critical("Unable to create single instance "
                                   "of vistrails application")
                    return
                self.local_server = QtNetwork.QLocalServer(self)
                self.connect(self.local_server, QtCore.SIGNAL("newConnection()"),
                             self.message_received)
                if self.local_server.listen(self._unique_key):
                    debug.log("Listening on %s"%self.local_server.fullServerName())
                else:
                    debug.warning(
                            "Server is not listening. This means it "
                            "will not accept parameters from other "
                            "instances")

        return None

    def found_another_instance_running(self, local_socket, args):
        debug.critical("Found another instance of VisTrails running")
        msg = bytes(args)
        debug.critical("Will send parameters to main instance %s" % msg)
        res = self.send_message(local_socket, msg)
        if res is True:
            debug.critical("Main instance succeeded")
            return True
        elif res is False:
            return False
        else:
            debug.critical("Main instance reports: %s" % res)
            return False

    def init(self, optionsDict=None, args=[]):
        """ VistrailsApplicationSingleton(optionDict: dict)
                                          -> VistrailsApplicationSingleton
        Create the application with a dict of settings
        
        """
        reportusage.record_usage(gui=True)

        vistrails.gui.theme.initializeCurrentTheme()
        VistrailsApplicationInterface.init(self, optionsDict, args)
        
        if self.temp_configuration.check('jobInfo') or \
           self.temp_configuration.check('jobList'):
            self.temp_configuration.batch = True

        # singleInstance configuration
        singleInstance = self.temp_configuration.check('singleInstance')
        if singleInstance:
            finished = self.run_single_instance(args)
            if finished is not None:
                return finished

        interactive = not self.temp_configuration.check('batch')
        if interactive:
            self.setIcon()
            self.createWindows()
            self.processEvents()
            
        # self.vistrailsStartup.init()
        self.package_manager.initialize_packages(
                report_missing_dependencies=not self.startup.first_run)

        # ugly workaround for configuration initialization order issue
        # If we go through the configuration too late,
        # The window does not get maximized. If we do it too early,
        # there are no created windows during spreadsheet initialization.
        if interactive:
            reportusage.record_usage(gui_interactive=True)
            if  self.temp_configuration.check('maximizeWindows'):
                self.builderWindow.showMaximized()
            if self.temp_configuration.check('dbDefault'):
                self.builderWindow.setDBDefault(True)

        self._initialized = True

        # usage statistics
        if reportusage.usage_report.status is usagestats.Stats.UNSET:
            self.ask_enable_usage_report()
        # news
        elif self.temp_configuration.check('showVistrailsNews'):
            self.show_news()

        # default handler installation
        if system.systemType == 'Linux':
            if not (self.temp_configuration.check('handlerDontAsk') or
                    self.configuration.check('handlerDontAsk')):
                if not linux_default_application_set():
                    self.ask_update_default_application()

        if interactive:
            self.interactiveMode()
        else:
            r = self.noninteractiveMode()
            return APP_SUCCESS if r is True else APP_FAIL
        return APP_SUCCESS

    def ask_enable_usage_report(self):
        news = reportusage.get_server_news()
        if hasattr(self, 'splashScreen') and self.splashScreen:
            self.splashScreen.hide()
        dialog = QtGui.QDialog()
        dialog.setWindowTitle(u"Anonymous usage statistics")
        layout = QtGui.QVBoxLayout()
        dialog.setLayout(layout)
        descr = QtGui.QTextBrowser()
        descr.setOpenExternalLinks(True)
        descr.setHtml(news['usage_report_prompt_html'])
        layout.addWidget(descr)
        layout.addWidget(QtGui.QLabel(
            u"Send anonymous reports to the developers?"))
        dont_ask = QtGui.QCheckBox(u"Don't ask again")
        layout.addWidget(dont_ask)
        buttons = QtGui.QDialogButtonBox(
                QtGui.QDialogButtonBox.Yes | QtGui.QDialogButtonBox.No)
        layout.addWidget(buttons)
        QtCore.QObject.connect(buttons, QtCore.SIGNAL('accepted()'),
                     dialog, QtCore.SLOT('accept()'))
        QtCore.QObject.connect(buttons, QtCore.SIGNAL('rejected()'),
                     dialog, QtCore.SLOT('reject()'))

        res = dialog.exec_()
        if res == QtGui.QDialog.Accepted:
            reportusage.usage_report.enable_reporting()
        else:
            if dont_ask.isChecked():
                reportusage.usage_report.disable_reporting()
        self.configuration.lastShownNews = news['version']

    def show_news(self):
        news = reportusage.get_server_news()
        if (getattr(self.temp_configuration, 'lastShownNews', None) ==
                news['version']):
            return

        if news['news_html']:
            if hasattr(self, 'splashScreen') and self.splashScreen:
                self.splashScreen.hide()
            dialog = QtGui.QDialog()
            dialog.setWindowTitle(u"VisTrails News")
            layout = QtGui.QVBoxLayout()
            dialog.setLayout(layout)
            descr = QtGui.QTextBrowser()
            descr.setOpenExternalLinks(True)
            descr.setHtml(news['news_html'])
            layout.addWidget(descr)

            hlayout = QtGui.QHBoxLayout()
            button = QtGui.QPushButton('&Close')
            hlayout.addStretch(1)
            hlayout.addWidget(button)
            hlayout.addStretch(1)
            layout.addLayout(hlayout)
            button.clicked.connect(dialog.close)

            dialog.exec_()
        self.configuration.lastShownNews = news['version']

    def ask_update_default_application(self, dont_ask_checkbox=True):
        if hasattr(self, 'splashScreen') and self.splashScreen:
            self.splashScreen.hide()
        dialog = QtGui.QDialog()
        dialog.setWindowTitle(u"Install .vt .vtl handler")
        layout = QtGui.QVBoxLayout()
        dialog.setLayout(layout)
        layout.addWidget(QtGui.QLabel(u"Install VisTrails as default handler "
                                      u"to open .vt and .vtl files?"))
        if dont_ask_checkbox:
            dont_ask = QtGui.QCheckBox(u"Don't ask on startup")
            dont_ask_setting = self.configuration.check('handlerDontAsk')
            dont_ask.setChecked(dont_ask_setting)
            layout.addWidget(dont_ask)
        buttons = QtGui.QDialogButtonBox(
                QtGui.QDialogButtonBox.Yes | QtGui.QDialogButtonBox.No)
        layout.addWidget(buttons)
        QtCore.QObject.connect(buttons, QtCore.SIGNAL('accepted()'),
                     dialog, QtCore.SLOT('accept()'))
        QtCore.QObject.connect(buttons, QtCore.SIGNAL('rejected()'),
                     dialog, QtCore.SLOT('reject()'))

        res = dialog.exec_()
        if dont_ask_checkbox:
            if dont_ask.isChecked() != dont_ask_setting:
                self.configuration.handlerDontAsk = dont_ask.isChecked()
                self.configuration.handlerDontAsk = dont_ask.isChecked()
        if res != QtGui.QDialog.Accepted:
            return False
        if system.systemType == 'Linux':
            if not linux_update_default_application():
                QtGui.QMessageBox.warning(
                        None,
                        u"Install .vt .vtl handler",
                        u"Couldn't set VisTrails as default handler "
                        u"to open .vt and .vtl files")
                return False
        else:
            QtGui.QMessageBox.warning(
                    None,
                    u"Install .vt .vtl handler",
                    u"Can't install a default handler on this platform")
            return False
        return True

    def is_running_gui(self):
        return True

    def get_current_controller(self):
        return self.builderWindow.get_current_controller()
    get_controller = get_current_controller

    def get_vistrail(self):
        if self.get_controller():
            return self.get_controller().vistrail
        return None

    def ensure_vistrail(self, locator):
        view = self.builderWindow.ensureVistrail(locator)
        if view is not None:
            return view.controller
        return None

    def add_vistrail(self, *objs):
        return self.builderWindow.add_vistrail(*objs)

    def remove_vistrail(self, locator=None):
        return self.builderWindow.remove_vistrail(locator)

    def select_version(self, version):
        return self.builderWindow.select_version(version)

    def update_locator(self, old_locator, new_locator):
        pass

    def create_notification(self, notification_id, window=None, view=None):
        if view is not None:
            if view not in self.view_notifications:
                self.view_notifications[view] = {}
            notifications = self.view_notifications[view]
        elif window is not None:
            if window not in self.window_notifications:
                self.window_notifications[window] = {}
            notifications = self.window_notifications[window]
        else:
            notifications = self.notifications
        if notification_id not in notifications:
            notifications[notification_id] = set()
        # else:
        #     print "already added notification", notification_id

    def register_notification(self, notification_id, method, window=None,
                              view=None):
        if view is not None:
            if view not in self.view_notifications:
                self.view_notifications[view] = {}
            notifications = self.view_notifications[view]
            #print '>>> LOCAL adding notification', notification_id, view, method
            #print id(notifications), notifications
            #for n, o in notifications.iteritems():
            #    print "    ", n , "(%s)"%len(o)
            #    for m in o:
            #        print "        ", m
        elif window is not None:
            if window not in self.window_notifications:
                self.window_notifications[window] = {}
            notifications = self.window_notifications[window]
        else:
            notifications = self.notifications     
            #print '>>> GLOBAL adding notification', notification_id, method  
            #print id(notifications), notifications
        if notification_id not in notifications:
            self.create_notification(notification_id, window, view)
        notifications[notification_id].add(method)

    def unregister_notification(self, notification_id, method, window=None,
                                view=None):
        if view is not None:
            if view in self.view_notifications:
                notifications = self.view_notifications[view]
            else:
                notifications = {}
                #print '>>> LOCAL remove notification', notification_id, view
            
            #print id(notifications), notifications
#            for n, o in notifications.iteritems():
#                print "    ", n , "(%s)"%len(o)
#                for m in o:
#                    print "        ", m
        elif window is not None:
            if window in self.window_notifications:
                notifications = self.window_notifications[window]
            else:
                notifications = {}
        else:
            notifications = self.notifications    
            #print '>>> GLOBAL remove notification', notification_id, method   
            #print id(notifications), notifications           
        if notification_id in notifications:
            notifications[notification_id].remove(method)

    def send_notification(self, notification_id, *args):
        # do global notifications
        if notification_id in self.notifications:
            for m in self.notifications[notification_id]:
                try:
                    m(*args)
                except Exception, e:
                    debug.unexpected_exception(e)
                    debug.print_exc()
        notifications = {}

        current_window = self.builderWindow

        # do window notifications
        if current_window in self.window_notifications:
            notifications = self.window_notifications[current_window]

            if notification_id in notifications:
                for m in notifications[notification_id]:
                    try:
                        m(*args)
                    except Exception, e:
                        debug.unexpected_exception(e)
                        debug.print_exc()

        if current_window is not None:
            current_view = current_window.current_view
        else:
            current_view = None
        # do local notifications
        if current_view in self.view_notifications:
            notifications = self.view_notifications[current_view]

            if notification_id in notifications:
                for m in notifications[notification_id]:
                    try:
                        m(*args)
                    except Exception, e:
                        debug.unexpected_exception(e)
                        debug.print_exc()

    def showBuilderWindow(self):
        # in some systems (Linux and Tiger) we need to make both calls
        # so builderWindow is activated
        self.setActiveWindow(self.builderWindow)
        self.builderWindow.activateWindow()
        self.builderWindow.show()
        self.builderWindow.raise_()
    
    def interactiveMode(self):
        """ interactiveMode() -> None
        Instantiate the GUI for interactive mode
        
        """
        if self.temp_configuration.check('showSplash'):
            self.splashScreen.finish(self.builderWindow)
            debug.DebugPrint.getInstance().register_splash(None)
            self.splashScreen = None
        # self.builderWindow.modulePalette.updateFromModuleRegistry()
        # self.builderWindow.modulePalette.connect_registry_signals()
        self.builderWindow.link_registry()
        
        self.process_interactive_input()
        if self.temp_configuration.showWindow:
            self.showBuilderWindow()
        else:
            self.builderWindow.hide()
        self.builderWindow.create_first_vistrail()

    def noninteractiveMode(self):
        """ noninteractiveMode() -> None
        Run the console in non-interactive mode
        
        """
        usedb = False
        passwd = ''
        if self.temp_configuration.check('host'):
            usedb = True
        if usedb and self.temp_configuration.check('user'):
            db_config = dict((x, self.temp_configuration.check(x))
                             for x in ['host', 'port', 
                                       'db', 'user'])
            try:
                test_db_connection(db_config)
            except VistrailsDBException:
                passwd = \
                    getpass.getpass("Connecting to %s:%s. Password for user '%s':" % (
                                    db_config['host'],
                                    db_config['db'],
                                    db_config['user']))
                db_config['passwd'] = passwd
                try:
                    test_db_connection(db_config)
                except VistrailsDBException:
                    debug.critical("Cannot login to database")
                    return False
    
        if self.input:
            w_list = []
            vt_list = []
            for filename in self.input:
                f_name, version = self._parse_vtinfo(filename, not usedb)
                if not f_name:
                    debug.critical("File not found: %s" % filename)
                    return False
                if not usedb:
                    locator = FileLocator(os.path.abspath(f_name))
                else:
                    locator = DBLocator(
                           host=self.temp_configuration.check('host'),
                           port=self.temp_configuration.check('port') or 3306,
                           database=self.temp_configuration.check('db'),
                           user=self.temp_configuration.check('user'),
                           passwd=passwd,
                           obj_id=f_name,
                           obj_type=None,
                           connection_id=None)
                    if not locator.is_valid():
                        #here there is a problem: as we allow execution from 
                        #command line with VisTrails already running, we need
                        #to update from the gui
                        if hasattr(self, 'builderWindow'):
                            ok = locator.update_from_gui(self.builderWindow)
                        else:
                            ok = locator.update_from_console()
                        if not ok:
                            debug.critical("Cannot login to database")
                if self.temp_configuration.check('jobList'):
                    return self.printJobs(locator)
                if self.temp_configuration.check('jobInfo'):
                    return self.printJob(locator, version)

                w_list.append((locator, version))
                vt_list.append(locator)
            import vistrails.core.console_mode

            if self.temp_configuration.check('outputDirectory'):
                output_dir = self.temp_configuration.outputDirectory
            else:
                output_dir = None

            errs = []
            if self.temp_configuration.check('outputPipelineGraph'):
                results = vistrails.core.console_mode.get_wf_graph(w_list, output_dir or '',
                                                                   self.temp_configuration.graphsAsPdf)
                for r in results:
                    if r[0] is False:
                        errs.append("Error generating workflow graph: %s" % \
                                    r[1])
                        debug.critical("*** Error in get_wf_graph: %s" % r[1])
            
            if self.temp_configuration.check('outputVersionTree'):
                results = vistrails.core.console_mode.get_vt_graph(vt_list, output_dir or '',
                                                                   self.temp_configuration.graphsAsPdf)
                for r in results:
                    if r[0] is False:
                        errs.append("Error generating vistrail graph: %s" % \
                                    r[1])
                        debug.critical("*** Error in get_vt_graph: %s" % r[1])

            if not self.temp_configuration.check('noExecute'):
                if self.temp_configuration.check('parameterExploration'):
                    errs.extend(
                        vistrails.core.console_mode.run_parameter_explorations(
                            w_list))
                else:
                    errs.extend(vistrails.core.console_mode.run(
                            w_list,
                            self.temp_configuration.check('parameters') or '',
                            update_vistrail=True))
                if len(errs) > 0:
                    for err in errs:
                        print err
                        debug.critical("*** Error in %s:%s:%s -- %s" % err)
                    return [False, ["*** Error in %s:%s:%s -- %s" % err for err in errs]]
            return True
        else:
            debug.warning("no input vistrails provided")
            return True

    def printJobs(self, locator):
        (v, abstractions, thumbnails, mashups)  = load_vistrail(locator)
        controller = VistrailController(v, locator, abstractions, thumbnails,
                                        mashups, auto_save=False)
        text = "### Workflows with jobs ###\n"
        text += "workflow | start date | status\n"
        text += '\n'.join(
            ["%s %s %s" %(j.version,
                          j.start,
                          "FINISHED" if j.completed() else "RUNNING")
             for i, j in controller.jobMonitor.workflows.iteritems()])
        print text
        return text

    def printJob(self, locator, version):
        (v, abstractions, thumbnails, mashups)  = load_vistrail(locator)
        controller = VistrailController(v, locator, abstractions, thumbnails,
                                        mashups, auto_save=False)
        text = "### Jobs in workflow ###\n"
        text += "name | start date | status\n"
        workflow = [wf for wf in controller.jobMonitor.workflows.itervalues()
                    if wf.version == int(version)]
        if len(workflow) < 1:
            text = "No job for workflow with id %s" % version
            print text
            return text
        workflow = workflow[0]
        text += '\n'.join(
            ["%s %s %s" %(i.name,
                          i.start,
                          "FINISHED" if i.finished else "RUNNING")
             for i in workflow.jobs.values()])
        print text
        return text

    def setIcon(self):
        """ setIcon() -> None
        Setup Vistrail Icon
        """
        self.setWindowIcon(vistrails.gui.theme.CurrentTheme.APPLICATION_ICON)
        
    def setupSplashScreen(self):
        """ setupSplashScreen() -> None
        Create the splash-screen at startup
        
        """
        if self.temp_configuration.check('showSplash'):
            splashPath = (system.vistrails_root_directory() +
                          "/gui/resources/images/vistrails_splash.png")
            pixmap = QtGui.QPixmap(splashPath)
            self.splashScreen = QtGui.QSplashScreen(pixmap, QtCore.Qt.WindowStaysOnTopHint)
            self.splashScreen.setFont(vistrails.gui.theme.CurrentTheme.SPLASH_SCREEN_FONT)
            debug.DebugPrint.getInstance().register_splash(self)
            self.splashScreen.show()
            
    def splashMessage(self, msg):
        if hasattr(self, "splashScreen"):
            self.splashScreen.showMessage(msg,
                        QtCore.Qt.AlignBottom|QtCore.Qt.AlignLeft,
                        QtCore.Qt.white)
            self.processEvents()

    def createWindows(self):
        """ createWindows() -> None
        Create and configure all GUI widgets including the builder
        
        """
        self.setupSplashScreen()

        # This is so that we don't import too many things before we
        # have to. Otherwise, requirements are checked too late.
        # from gui.builder_window import QBuilderWindow
        from vistrails.gui.vistrails_window import QVistrailsWindow

        # self.builderWindow = QBuilderWindow()
        self.builderWindow = QVistrailsWindow()
        if self.temp_configuration.showWindow:
            # self.builderWindow.show()
            # self.setActiveWindow(self.builderWindow)
            pass

    def finishSession(self):
        if QtCore.QT_VERSION >= 0x40400 and self.shared_memory is not None:
            self.shared_memory.detach()
            if self.local_server:
                self.local_server.close()
        if system.systemType in ['Darwin']:
            self.removeEventFilter(self)
        VistrailsApplicationInterface.finishSession(self)
   
    def eventFilter(self, o, event):
        """eventFilter(obj,event)-> boolean
        This will filter all create events and will set on the WA_MacMetalStyle
        attribute of a QWidget. It will also filter the FileOpen events on Mac.
        
        """
        metalstyle = hasattr(self, 'temp_configuration') and \
                     self.temp_configuration.check('useMacBrushedMetalStyle')
        if metalstyle:
            if QtCore.QT_VERSION < 0x40500:
                create_event = QtCore.QEvent.Create
                mac_attribute = QtCore.Qt.WA_MacMetalStyle
            else:
                create_event = 15
                mac_attribute = QtCore.Qt.WA_MacBrushedMetal
            if (event.type() == create_event and
                    isinstance(o, QtGui.QWidget) and
                    not isinstance(o, QtGui.QSplashScreen) and
                    not (o.windowFlags() & QtCore.Qt.Popup)):
                o.setAttribute(mac_attribute)
        if event.type() == QtCore.QEvent.FileOpen:
            self.input = [str(event.file())]
            self.process_interactive_input()
        return QtGui.QApplication.eventFilter(self,o,event)
    
    def is_running(self):
        return self._is_running

    def message_received(self):
        if QtCore.QT_VERSION >= 0x40400:
            local_socket = self.local_server.nextPendingConnection()
            if not local_socket.waitForReadyRead(self.timeout):
                debug.critical("Read error: %s" %
                               local_socket.errorString())
                return
            byte_array = local_socket.readAll()

            output = None
            try:
                # redirect stdout
                old_stdout = sys.stdout
                sys.stdout = StringIO.StringIO()
                result = self.parse_input_args_from_other_instance(str(byte_array))
                output = sys.stdout.getvalue()
                sys.stdout.close()
                sys.stdout = old_stdout
            except Exception, e:
                debug.unexpected_exception(e)
                debug.critical("Unknown error", e)
                result = debug.format_exc()

            if None == result:
                result = True
            if True == result:
                result = "Command Completed"
            elif False == result:
                result = "Command Failed"
            elif type(result) == list:
                result = '\n'.join(result[1])
            if result == "Command Completed" and output:
                result += '\n' + output
            self.shared_memory.lock()
            local_socket.write(bytes(result))
            self.shared_memory.unlock()
            if not local_socket.waitForBytesWritten(self.timeout):
                debug.critical("Writing failed: %s" %
                            local_socket.errorString())
                return
            local_socket.disconnectFromServer()

    def send_message(self, local_socket, message):
        self.shared_memory.lock()
        local_socket.write(message)
        self.shared_memory.unlock()
        if not local_socket.waitForBytesWritten(self.timeout):
            debug.critical("Writing failed: %s" %
                           local_socket.errorString())
            return False
        if not local_socket.waitForReadyRead(self.execution_timeout):
            debug.critical("Read error: %s" %
                           local_socket.errorString())
            return False
        byte_array = local_socket.readAll()
        result = str(byte_array)
        print "Other instance processed input (%s)" % result
        if not result.startswith('Command Completed'):
            debug.critical(result)
        else:
            local_socket.disconnectFromServer()
            return True
        local_socket.disconnectFromServer()
        return False

    def parse_input_args_from_other_instance(self, msg):
        reportusage.record_feature('args_from_other_instance')
        try:
            args = literal_eval(msg)
        except Exception as e:
            debug.critical("Invalid input: %s" % msg, e)
        else:
            if isinstance(args, list):
                try:
                    conf_options = self.read_options(args)
                except SystemExit:
                    debug.critical("Invalid options: %s" % ' '.join(args))
                    return False
                try:
                    # Execute using persistent configuration + new temp configuration
                    old_temp_conf = self.temp_configuration
                    self.startup.temp_configuration = copy.copy(self.configuration)
                    self.temp_configuration.update(conf_options)

                    interactive = not self.temp_configuration.check('batch')
                    if interactive:
                        result = self.process_interactive_input()
                        if self.temp_configuration.showWindow:
                            # in some systems (Linux and Tiger) we need to make both calls
                            # so builderWindow is activated
                            self.builderWindow.raise_()
                            self.builderWindow.activateWindow()
                        return result
                    else:
                        return self.noninteractiveMode()
                finally:
                    self.startup.temp_configuration = old_temp_conf
            else:
                debug.critical("Invalid input: %s" % msg)
        return False

def linux_default_application_set():
    """linux_default_application_set() -> True|False|None
    For Linux - checks if a handler is set for .vt and .vtl files.
    """
    command = ['xdg-mime', 'query', 'filetype',
               os.path.join(system.vistrails_root_directory(),
                            'tests', 'resources', 'terminator.vt')]
    try:
        output = []
        result = system.execute_cmdline(command, output)
        if result != 0:
            # something is wrong, abort
            debug.warning("Error checking mimetypes: %s" % output[0])
            return None
    except OSError, e:
        debug.warning("Error checking mimetypes: %s" % e.message)
        return None
    if 'application/x-vistrails' == output[0].strip():
        return True
    return False

def linux_update_default_application():
    """ update_default_application() -> None
    For Linux - checks if we should install vistrails as the default
    application for .vt and .vtl files.
    If replace is False, don't replace an existing handler.

    Returns True if installation succeeded.
    """
    root = system.vistrails_root_directory()
    home = os.path.expanduser('~')

    # install mime type
    command = ['xdg-mime', 'install', 
               os.path.join(system.vistrails_root_directory(),
                            'gui/resources/vistrails-mime.xml')]
    output = []
    try:
        result = system.execute_cmdline(command, output)
    except OSError:
        result = None
    if result != 0:
        debug.warning("Error running xdg-mime")
        return False

    command = ['update-mime-database', home + '/.local/share/mime']
    output = []
    try:
        result = system.execute_cmdline(command, output)
    except OSError:
        result = None
    if result != 0:
        debug.warning("Error running update-mime-database")
        return False

    # install icon
    command = ['xdg-icon-resource', 'install',
               '--context', 'mimetypes',
               '--size', '48',
               os.path.join(system.vistrails_root_directory(),
                            'gui/resources/images/vistrails_icon_small.png'),
               'application-x-vistrails']
    output = []
    try:
        result = system.execute_cmdline(command, output)
    except OSError:
        result = None
    if result != 0:
        debug.warning("Error running xdg-icon-resource")
        return True # the handler is set anyway

    # install desktop file
    dirs = [home + '/.local', home + '/.local/share',
            home + '/.local/share/applications']

    for d in dirs:
        if not os.path.isdir(d):
            os.mkdir(d)
    desktop = """[Desktop Entry]
Name=VisTrails
Exec=python {root}/run.py %f
Icon={root}/gui/resources/images/vistrails_icon_small.png
Type=Application
MimeType=application/x-vistrails
""".format(root=root)
    f = open(os.path.join(dirs[2], 'vistrails.desktop'), 'w')
    f.write(desktop)
    f.close()

    command = ['update-desktop-database', dirs[2]]
    output = []
    try:
        result = system.execute_cmdline(command, output)
    except OSError:
        result = None
    if result != 0:
        debug.warning("Error running update-desktop-database")
    return True

# The initialization must be explicitly signalled. Otherwise, any
# modules importing vis_application will try to initialize the entire
# app.
def start_application(optionsDict=None, args=[]):
    """Initializes the application singleton."""
    VistrailsApplication = get_vistrails_application()
    if VistrailsApplication:
        debug.critical("Application already started.")
        return
    VistrailsApplication = VistrailsApplicationSingleton()
    set_vistrails_application(VistrailsApplication)
    x = VistrailsApplication.init(optionsDict, args)
    return x

def stop_application():
    """Stop and finalize the application singleton."""
    VistrailsApplication = get_vistrails_application()
    VistrailsApplication.finishSession()
    VistrailsApplication.save_configuration()
    VistrailsApplication.destroy()
    VistrailsApplication.deleteLater()

