#!/usr/bin/env python
# pragma: no testimport
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
"""Main file for the VisTrails distribution."""

from __future__ import division

import os
import sys

# Allows the userpackages directory to be overridden through an environment
# variable
# As this variable is set by the package manager, this also allows
# multiprocessing to work correctly on Windows, where fork is not used and thus
# 'userpackages' needs to be available in processes spawned from a
# userpackage's code
try:
    userpackages_dir = os.environ['VISTRAILS_USERPACKAGES_DIR']
except KeyError:
    pass
else:
    old_sys_path = list(sys.path)
    sys.path.insert(0, os.path.join(userpackages_dir, os.path.pardir))
    try:
        import userpackages
    except ImportError:
        sys.stderr.write("Couldn't import VISTRAILS_USERPACKAGES_DIR (%s), "
                         "continuing\n" % userpackages_dir)

def fix_site():
    # py2app ships a stripped version of site.py
    # USER_BASE and USER_SITE is not set,
    # this is needed by at least scipy.weave
    import platform
    if platform.system()!='Darwin': return
    import site
    if hasattr(site, "USER_BASE"): return
    from vistrails.core.system import mac_site
    site.USER_BASE = mac_site.getuserbase()
    site.USER_SITE = mac_site.getusersitepackages()
    site._Helper = mac_site._Helper

def fix_paths():
    import site
    if not hasattr(site, "USER_BASE"): return  # We are running py2app
    if os.path.basename(__file__) != 'run.py': return  # Not running from source

    # Fix import path: add parent directory(so that we can
    # import vistrails.[gui|...] and remove other paths below it (we might have
    # been started from a subdir)
    # A better solution is probably to move run.py up a
    # directory in the repo
    old_dir = os.path.realpath(os.path.dirname(__file__))
    vistrails_dir = os.path.realpath(os.path.join(os.path.dirname(__file__), '..'))
    i = 0
    while i < len(sys.path):
        rpath = os.path.realpath(sys.path[i])
        if rpath.startswith(old_dir):
            del sys.path[i]
        else:
            i += 1
    if vistrails_dir not in sys.path:
        sys.path.insert(0, vistrails_dir)

def main():
    fix_paths()
    fix_site()

    # Load the default locale (from environment)
    import locale
    locale.setlocale(locale.LC_ALL, '')

    # Log to the console
    from vistrails.core import debug
    debug.DebugPrint.getInstance().log_to_console()

    # Setup usage reporting
    from vistrails.core import reportusage
    reportusage.setup_usage_report()

    from vistrails.gui.requirements import require_pyqt4_api2
    require_pyqt4_api2()

    import vistrails.gui.application
    from vistrails.core.application import APP_SUCCESS, APP_FAIL, APP_DONE
    try:
        v = vistrails.gui.application.start_application(args=sys.argv[1:])
        if v != APP_SUCCESS:
            app = vistrails.gui.application.get_vistrails_application()
            if app:
                app.finishSession()
            sys.exit(APP_SUCCESS if v == APP_DONE else APP_FAIL)
        app = vistrails.gui.application.get_vistrails_application()()
    except SystemExit, e:
        app = vistrails.gui.application.get_vistrails_application()
        if app:
            app.finishSession()
        reportusage.submit_usage_report(result='init exit %s' %
                                               getattr(e, 'code', '(unknown)'))
        sys.exit(e)
    except Exception, e:
        app = vistrails.gui.application.get_vistrails_application()
        if app:
            app.finishSession()
        import traceback
        print >>sys.stderr, "Uncaught exception on initialization: %s" % (
                traceback._format_final_exc_line(type(e).__name__, e).strip())
        traceback.print_exc(None, sys.stderr)
        reportusage.submit_usage_report(result='init %s' % type(e).__name__)
        sys.exit(255)

    try:
        if not app.temp_configuration.batch:
            v = app.exec_()
        vistrails.gui.application.stop_application()
    except BaseException, e:
        reportusage.submit_usage_report(result=type(e).__name__)
        raise
    reportusage.submit_usage_report(result='success %s' % v)
    sys.exit(v)

if __name__ == '__main__':
    main()
