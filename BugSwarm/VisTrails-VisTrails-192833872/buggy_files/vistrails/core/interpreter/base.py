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

from vistrails.core import debug

##############################################################################

class InternalTuple(object):
    """Tuple used internally for constant tuples."""

    list_depth = 0

    def _get_length(self):
        return len(self._values)
    def _set_length(self, length):
        self._values = [None] * length
    length = property(_get_length, _set_length)

    def compute(self):
        return

    def set_input_port(self, index, connector):
        self._values[index] = connector()

    def get_output(self, port):
        return tuple(self._values)

    def update(self):
        pass

##############################################################################

class AbortExecution(Exception):
    """Internal exception raised to signal the interpreter it should stop.
    """

##############################################################################

class BaseInterpreter(object):

    def __init__(self):
        """ BaseInterpreter() -> BaseInterpreter
        Initialize class members
        
        """
        self.done_summon_hook = None
        self.done_update_hook = None

    def resolve_aliases(self, pipeline,
                        customAliases=None):
        # We don't build the alias dictionary anymore because as we don't 
        # perform expression evaluation anymore, the values won't change.
        # We only care for custom aliases because they might have a value 
        # different from what it's stored.
        
        aliases = {}
        if customAliases:
            #customAliases can be only a subset of the aliases
            #so we need to build the Alias Dictionary always
            for k,v in customAliases.iteritems():
                aliases[k] = v
            # no support for expression evaluation. The code that does that is
            # ugly and dangerous.
        for alias in aliases:
            try:
                info = pipeline.aliases[alias]
                param = pipeline.db_get_object(info[0],info[1])
                param.strValue = str(aliases[alias])
            except KeyError:
                pass
                    
        return aliases
    
    def update_params(self, pipeline,
                        customParams=None):
        """update_params(pipeline: Pipeline, 
                         customParams=[(vttype, oId, strval)] -> None
        This will set the new parameter values in the pipeline before
        execution 
        
        """
        if customParams:
            for (vttype, oId, strval) in customParams:
                try:
                    param = pipeline.db_get_object(vttype,oId)
                    param.strValue = str(strval)
                except Exception, e:
                    debug.debug("Problem when updating params", e)

    def resolve_variables(self, vistrail_variables, pipeline):
        for m in pipeline.module_list:
            if m.is_vistrail_var():
                vistrail_var = vistrail_variables(m.get_vistrail_var())
                if vistrail_var is None: # assume set in parameter exploration
                    continue
                strValue = vistrail_var.value
                for func in m.functions:
                    if func.name == 'value':
                        func.params[0].strValue = strValue

    def set_done_summon_hook(self, hook):
        """ set_done_summon_hook(hook: function(pipeline, objects)) -> None
        Assign a function to call right after every objects has been
        summoned during execution
        
        """
        self.done_summon_hook = hook

    def set_done_update_hook(self, hook):
        """ set_done_update_hook(hook: function(pipeline, objects)) -> None
        Assign a function to call right after every objects has been
        updated
        
        """
        self.done_update_hook = hook

##############################################################################
