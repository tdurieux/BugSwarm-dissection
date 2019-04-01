"""Test cases for AST merge (used for fine-grained incremental checking)"""

import os
import shutil
from typing import List, Tuple, Dict, Optional

from mypy import build
from mypy.build import BuildManager, BuildSource, State
from mypy.errors import Errors, CompileError
from mypy.nodes import (
    Node, MypyFile, SymbolTable, SymbolTableNode, TypeInfo, Expression, Var, UNBOUND_IMPORTED
)
from mypy.options import Options
from mypy.server.astmerge import merge_asts
from mypy.server.subexpr import get_subexpressions
from mypy.server.update import FineGrainedBuildManager
from mypy.strconv import StrConv, indent
from mypy.test.config import test_temp_dir
from mypy.test.data import DataDrivenTestCase, DataSuite
from mypy.test.helpers import assert_string_arrays_equal, normalize_error_messages
from mypy.test.testtypegen import ignore_node
from mypy.types import TypeStrVisitor, Type
from mypy.util import short_type, IdMapper


# Which data structures to dump in a test case?
SYMTABLE = 'SYMTABLE'
TYPEINFO = ' TYPEINFO'
TYPES = 'TYPES'
AST = 'AST'


NOT_DUMPED_MODULES = (
    'builtins',
    'typing',
    'abc',
    'contextlib',
    'sys',
    'mypy_extensions',
)


class ASTMergeSuite(DataSuite):
    files = ['merge.test']
    base_path = test_temp_dir
    optional_out = True

    def __init__(self, *, update_data: bool) -> None:
        super().__init__(update_data=update_data)
        self.str_conv = StrConv(show_ids=True)
        assert self.str_conv.id_mapper is not None
        self.id_mapper = self.str_conv.id_mapper  # type: IdMapper
        self.type_str_conv = TypeStrVisitor(self.id_mapper)

    def run_case(self, testcase: DataDrivenTestCase) -> None:
        name = testcase.name
        # We use the test case name to decide which data structures to dump.
        # Dumping everything would result in very verbose test cases.
        if name.endswith('_symtable'):
            kind = SYMTABLE
        elif name.endswith('_typeinfo'):
            kind = TYPEINFO
        elif name.endswith('_types'):
            kind = TYPES
        else:
            kind = AST

        main_src = '\n'.join(testcase.input)
        messages, manager, graph = self.build(main_src)
        assert manager is not None, 'cases where CompileError occurred should not be run'
        fine_grained_manager = FineGrainedBuildManager(manager, graph)

        a = []
        if messages:
            a.extend(messages)

        target_path = os.path.join(test_temp_dir, 'target.py')
        shutil.copy(os.path.join(test_temp_dir, 'target.py.next'), target_path)

        a.extend(self.dump(manager, kind))
        old_subexpr = get_subexpressions(manager.modules['target'])

        a.append('==>')

        new_file, new_types = self.build_increment(fine_grained_manager, 'target', target_path)
        a.extend(self.dump(manager, kind))

        for expr in old_subexpr:
            # Verify that old AST nodes are removed from the expression type map.
            assert expr not in new_types

        a = normalize_error_messages(a)

        assert_string_arrays_equal(
            testcase.output, a,
            'Invalid output ({}, line {})'.format(testcase.file,
                                                  testcase.line))

    def build(self, source: str) -> Tuple[List[str], Optional[BuildManager], Dict[str, State]]:
        options = Options()
        options.incremental = True
        options.use_builtins_fixtures = True
        options.show_traceback = True
        main_path = os.path.join(test_temp_dir, 'main')
        with open(main_path, 'w') as f:
            f.write(source)
        try:
            result = build.build(sources=[BuildSource(main_path, None, None)],
                                 options=options,
                                 alt_lib_path=test_temp_dir)
        except CompileError as e:
            # TODO: Is it okay to return None?
            return e.messages, None, {}
        return result.errors, result.manager, result.graph

    def build_increment(self, manager: FineGrainedBuildManager,
                        module_id: str, path: str) -> Tuple[MypyFile,
                                                            Dict[Expression, Type]]:
        manager.update([(module_id, path)])
        module = manager.manager.modules[module_id]
        type_map = manager.type_maps[module_id]
        return module, type_map

    def dump(self,
             manager: BuildManager,
             kind: str) -> List[str]:
        modules = manager.modules
        if kind == AST:
            return self.dump_asts(modules)
        elif kind == TYPEINFO:
            return self.dump_typeinfos(modules)
        elif kind == SYMTABLE:
            return self.dump_symbol_tables(modules)
        elif kind == TYPES:
            return self.dump_types(manager)
        assert False, 'Invalid kind %s' % kind

    def dump_asts(self, modules: Dict[str, MypyFile]) -> List[str]:
        a = []
        for m in sorted(modules):
            if m in NOT_DUMPED_MODULES:
                # We don't support incremental checking of changes to builtins, etc.
                continue
            s = modules[m].accept(self.str_conv)
            a.extend(s.splitlines())
        return a

    def dump_symbol_tables(self, modules: Dict[str, MypyFile]) -> List[str]:
        a = []
        for id in sorted(modules):
            if not is_dumped_module(id):
                # We don't support incremental checking of changes to builtins, etc.
                continue
            a.extend(self.dump_symbol_table(id, modules[id].names))
        return a

    def dump_symbol_table(self, module_id: str, symtable: SymbolTable) -> List[str]:
        a = ['{}:'.format(module_id)]
        for name in sorted(symtable):
            if name.startswith('__'):
                continue
            a.append('    {}: {}'.format(name, self.format_symbol_table_node(symtable[name])))
        return a

    def format_symbol_table_node(self, node: SymbolTableNode) -> str:
        if node.node is None:
            if node.kind == UNBOUND_IMPORTED:
                return 'UNBOUND_IMPORTED'
            return 'None'
        if isinstance(node.node, Node):
            s = '{}<{}>'.format(str(type(node.node).__name__),
                                self.id_mapper.id(node.node))
        else:
            s = '? ({})'.format(type(node.node))
        if node.type_override:
            override = self.format_type(node.type_override)
            s += '(type_override={})'.format(override)
        if (isinstance(node.node, Var) and node.node.type and
                not node.node.fullname().startswith('typing.')):
            typestr = self.format_type(node.node.type)
            s += '({})'.format(typestr)
        return s

    def dump_typeinfos(self, modules: Dict[str, MypyFile]) -> List[str]:
        a = []
        for id in sorted(modules):
            if not is_dumped_module(id):
                continue
            a.extend(self.dump_typeinfos_recursive(modules[id].names))
        return a

    def dump_typeinfos_recursive(self, names: SymbolTable) -> List[str]:
        a = []
        for name, node in sorted(names.items(), key=lambda x: x[0]):
            if isinstance(node.node, TypeInfo):
                a.extend(self.dump_typeinfo(node.node))
                a.extend(self.dump_typeinfos_recursive(node.node.names))
        return a

    def dump_typeinfo(self, info: TypeInfo) -> List[str]:
        s = info.dump(str_conv=self.str_conv,
                      type_str_conv=self.type_str_conv)
        return s.splitlines()

    def dump_types(self, manager: BuildManager) -> List[str]:
        a = []
        # To make the results repeatable, we try to generate unique and
        # deterministic sort keys.
        for module_id in sorted(manager.modules):
            if not is_dumped_module(module_id):
                continue
            type_map = manager.saved_cache[module_id][2]
            if type_map:
                a.append('## {}'.format(module_id))
                for expr in sorted(type_map, key=lambda n: (n.line, short_type(n),
                                                            str(n) + str(type_map[n]))):
                    typ = type_map[expr]
                    a.append('{}:{}: {}'.format(short_type(expr),
                                                expr.line,
                                                self.format_type(typ)))
        return a

    def format_type(self, typ: Type) -> str:
        return typ.accept(self.type_str_conv)


def is_dumped_module(id: str) -> bool:
    return id not in NOT_DUMPED_MODULES and (not id.startswith('_') or id == '__main__')