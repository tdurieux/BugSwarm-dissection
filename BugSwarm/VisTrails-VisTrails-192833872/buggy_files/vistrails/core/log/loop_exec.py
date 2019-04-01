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
from __future__ import division

from vistrails.db.domain import DBLoopExec, DBLoopIteration


class LoopExec(DBLoopExec):
    """Class that stores info for logging a loop.
    """

    def __copy__(self):
        return self.do_copy()

    def do_copy(self, new_ids=False, id_scope=None, id_remap=None):
        cp = DBLoopExec.do_copy(self, new_ids, id_scope, id_remap)
        cp.__class__ = LoopExec
        return cp

    @staticmethod
    def convert(_loop_exec):
        if _loop_exec.__class__ == LoopExec:
            return
        _loop_exec.__class__ = LoopExec
        for iteration in _loop_exec.loop_iterations:
            LoopIteration.convert(iteration)

    ##########################################################################
    # Properties

    id = DBLoopExec.db_id
    ts_start = DBLoopIteration.db_ts_start
    ts_end = DBLoopIteration.db_ts_end

    def _get_loop_iterations(self):
        return self.db_loop_iterations
    def _set_loop_iterations(self, iterations):
        self.db_loop_iterations = iterations
    loop_iterations = DBLoopExec.db_loop_iterations
    def add_loop_iteration(self, iteration):
        self.db_add_loop_iteration(iteration)


class LoopIteration(DBLoopIteration):
    """Class that stores info for logging a single iteration of a loop.
    """

    def __copy__(self):
        return self.do_copy()

    def do_copy(self, new_ids=False, id_scope=None, id_remap=None):
        cp = DBLoopIteration.do_copy(self, new_ids, id_scope, id_remap)
        cp.__class__ = LoopIteration
        return cp

    @staticmethod
    def convert(_loop_iteration):
        from vistrails.core.log.module_exec import ModuleExec
        from vistrails.core.log.group_exec import GroupExec

        if _loop_iteration.__class__ == LoopIteration:
            return
        _loop_iteration.__class__ = LoopIteration
        for item_exec in _loop_iteration.item_execs:
            if item_exec.vtType == ModuleExec.vtType:
                ModuleExec.convert(item_exec)
            elif item_exec.vtType == GroupExec.vtType:
                GroupExec.convert(item_exec)
            elif item_exec.vtType == LoopExec.vtType:
                LoopExec.convert(item_exec)

    ##########################################################################
    # Properties

    id = DBLoopIteration.db_id
    ts_start = DBLoopIteration.db_ts_start
    ts_end = DBLoopIteration.db_ts_end
    completed = DBLoopIteration.db_completed
    error = DBLoopIteration.db_error
    iteration = DBLoopIteration.db_iteration

    def _get_duration(self):
        if self.db_ts_end is not None:
            return self.db_ts_end - self.db_ts_start
        return None
    duration = property(_get_duration)

    item_execs = DBLoopIteration.db_item_execs
    def add_item_exec(self, item_exec):
        self.db_add_item_exec(item_exec)
