"""Facilities and constants for generating error messages during type checking.

The type checker itself does not deal with message string literals to
improve code clarity and to simplify localization (in the future)."""

import re
import difflib

from typing import cast, List, Dict, Any, Sequence, Iterable, Tuple

from mypy.errors import Errors
from mypy.types import (
    Type, CallableType, Instance, TypeVarType, TupleType, TypedDictType,
    UnionType, NoneTyp, AnyType, Overloaded, FunctionLike, DeletedType, TypeType,
    UninhabitedType
)
from mypy.nodes import (
    TypeInfo, Context, MypyFile, op_methods, FuncDef, reverse_type_aliases,
    ARG_POS, ARG_OPT, ARG_NAMED, ARG_NAMED_OPT, ARG_STAR, ARG_STAR2
)


# Constants that represent simple type checker error message, i.e. messages
# that do not have any parameters.

NO_RETURN_VALUE_EXPECTED = 'No return value expected'
MISSING_RETURN_STATEMENT = 'Missing return statement'
INVALID_IMPLICIT_RETURN = 'Implicit return in function which does not return'
INCOMPATIBLE_RETURN_VALUE_TYPE = 'Incompatible return value type'
RETURN_ANY = 'Returning Any from function with declared return type "{}"'
RETURN_VALUE_EXPECTED = 'Return value expected'
NO_RETURN_EXPECTED = 'Return statement in function which does not return'
INVALID_EXCEPTION = 'Exception must be derived from BaseException'
INVALID_EXCEPTION_TYPE = 'Exception type must be derived from BaseException'
INVALID_RETURN_TYPE_FOR_GENERATOR = \
    'The return type of a generator function should be "Generator" or one of its supertypes'
INVALID_RETURN_TYPE_FOR_ASYNC_GENERATOR = \
    'The return type of an async generator function should be "AsyncGenerator" or one of its ' \
    'supertypes'
INVALID_GENERATOR_RETURN_ITEM_TYPE = \
    'The return type of a generator function must be None in its third type parameter in Python 2'
YIELD_VALUE_EXPECTED = 'Yield value expected'
INCOMPATIBLE_TYPES = 'Incompatible types'
INCOMPATIBLE_TYPES_IN_ASSIGNMENT = 'Incompatible types in assignment'
INCOMPATIBLE_REDEFINITION = 'Incompatible redefinition'
INCOMPATIBLE_TYPES_IN_AWAIT = 'Incompatible types in await'
INCOMPATIBLE_TYPES_IN_ASYNC_WITH_AENTER = 'Incompatible types in "async with" for __aenter__'
INCOMPATIBLE_TYPES_IN_ASYNC_WITH_AEXIT = 'Incompatible types in "async with" for __aexit__'
INCOMPATIBLE_TYPES_IN_ASYNC_FOR = 'Incompatible types in "async for"'

INCOMPATIBLE_TYPES_IN_YIELD = 'Incompatible types in yield'
INCOMPATIBLE_TYPES_IN_YIELD_FROM = 'Incompatible types in "yield from"'
INCOMPATIBLE_TYPES_IN_STR_INTERPOLATION = 'Incompatible types in string interpolation'
MUST_HAVE_NONE_RETURN_TYPE = 'The return type of "{}" must be None'
TUPLE_INDEX_MUST_BE_AN_INT_LITERAL = 'Tuple index must be an integer literal'
TUPLE_SLICE_MUST_BE_AN_INT_LITERAL = 'Tuple slice must be an integer literal'
TUPLE_INDEX_OUT_OF_RANGE = 'Tuple index out of range'
NEED_ANNOTATION_FOR_VAR = 'Need type annotation for variable'
ITERABLE_EXPECTED = 'Iterable expected'
ASYNC_ITERABLE_EXPECTED = 'AsyncIterable expected'
INVALID_SLICE_INDEX = 'Slice index must be an integer or None'
CANNOT_INFER_LAMBDA_TYPE = 'Cannot infer type of lambda'
CANNOT_INFER_ITEM_TYPE = 'Cannot infer iterable item type'
CANNOT_ACCESS_INIT = 'Cannot access "__init__" directly'
CANNOT_ASSIGN_TO_METHOD = 'Cannot assign to a method'
CANNOT_ASSIGN_TO_TYPE = 'Cannot assign to a type'
INCONSISTENT_ABSTRACT_OVERLOAD = \
    'Overloaded method has both abstract and non-abstract variants'
READ_ONLY_PROPERTY_OVERRIDES_READ_WRITE = \
    'Read-only property cannot override read-write property'
FORMAT_REQUIRES_MAPPING = 'Format requires a mapping'
RETURN_TYPE_CANNOT_BE_CONTRAVARIANT = "Cannot use a contravariant type variable as return type"
FUNCTION_PARAMETER_CANNOT_BE_COVARIANT = "Cannot use a covariant type variable as a parameter"
INCOMPATIBLE_IMPORT_OF = "Incompatible import of"
FUNCTION_TYPE_EXPECTED = "Function is missing a type annotation"
ONLY_CLASS_APPLICATION = "Type application is only supported for generic classes"
RETURN_TYPE_EXPECTED = "Function is missing a return type annotation"
ARGUMENT_TYPE_EXPECTED = "Function is missing a type annotation for one or more arguments"
KEYWORD_ARGUMENT_REQUIRES_STR_KEY_TYPE = \
    'Keyword argument only valid with "str" key type in call to "dict"'
ALL_MUST_BE_SEQ_STR = 'Type of __all__ must be {}, not {}'
INVALID_TYPEDDICT_ARGS = \
    'Expected keyword arguments, {...}, or dict(...) in TypedDict constructor'
TYPEDDICT_ITEM_NAME_MUST_BE_STRING_LITERAL = \
    'Expected TypedDict item name to be string literal'
MALFORMED_ASSERT = 'Assertion is always true, perhaps remove parentheses?'
NON_BOOLEAN_IN_CONDITIONAL = 'Condition must be a boolean'
DUPLICATE_TYPE_SIGNATURES = 'Function has duplicate type signatures'
GENERIC_INSTANCE_VAR_CLASS_ACCESS = 'Access to generic instance variables via class is ambiguous'

ARG_CONSTRUCTOR_NAMES = {
    ARG_POS: "Arg",
    ARG_OPT: "DefaultArg",
    ARG_NAMED: "NamedArg",
    ARG_NAMED_OPT: "DefaultNamedArg",
    ARG_STAR: "VarArg",
    ARG_STAR2: "KwArg",
}


class MessageBuilder:
    """Helper class for reporting type checker error messages with parameters.

    The methods of this class need to be provided with the context within a
    file; the errors member manages the wider context.

    IDEA: Support a 'verbose mode' that includes full information about types
          in error messages and that may otherwise produce more detailed error
          messages.
    """

    # Report errors using this instance. It knows about the current file and
    # import context.
    errors = None  # type: Errors

    modules = None  # type: Dict[str, MypyFile]

    # Number of times errors have been disabled.
    disable_count = 0

    # Hack to deduplicate error messages from union types
    disable_type_names = 0

    def __init__(self, errors: Errors, modules: Dict[str, MypyFile]) -> None:
        self.errors = errors
        self.modules = modules
        self.disable_count = 0
        self.disable_type_names = 0

    #
    # Helpers
    #

    def copy(self) -> 'MessageBuilder':
        new = MessageBuilder(self.errors.copy(), self.modules)
        new.disable_count = self.disable_count
        new.disable_type_names = self.disable_type_names
        return new

    def add_errors(self, messages: 'MessageBuilder') -> None:
        """Add errors in messages to this builder."""
        if self.disable_count <= 0:
            for info in messages.errors.error_info:
                self.errors.add_error_info(info)

    def disable_errors(self) -> None:
        self.disable_count += 1

    def enable_errors(self) -> None:
        self.disable_count -= 1

    def is_errors(self) -> bool:
        return self.errors.is_errors()

    def report(self, msg: str, context: Context, severity: str,
               file: str = None, origin: Context = None) -> None:
        """Report an error or note (unless disabled)."""
        if self.disable_count <= 0:
            self.errors.report(context.get_line() if context else -1,
                               context.get_column() if context else -1,
                               msg.strip(), severity=severity, file=file,
                               origin_line=origin.get_line() if origin else None)

    def fail(self, msg: str, context: Context, file: str = None,
             origin: Context = None) -> None:
        """Report an error message (unless disabled)."""
        self.report(msg, context, 'error', file=file, origin=origin)

    def note(self, msg: str, context: Context, file: str = None,
             origin: Context = None) -> None:
        """Report a note (unless disabled)."""
        self.report(msg, context, 'note', file=file, origin=origin)

    def warn(self, msg: str, context: Context, file: str = None,
             origin: Context = None) -> None:
        """Report a warning message (unless disabled)."""
        self.report(msg, context, 'warning', file=file, origin=origin)

    def format(self, typ: Type, verbosity: int = 0) -> str:
        """Convert a type to a relatively short string that is suitable for error messages.

        Mostly behave like format_simple below, but never return an empty string.
        """
        s = self.format_simple(typ, verbosity)
        if s != '':
            # If format_simple returns a non-trivial result, use that.
            return s
        elif isinstance(typ, FunctionLike):
            func = typ
            if func.is_type_obj():
                # The type of a type object type can be derived from the
                # return type (this always works).
                itype = cast(Instance, func.items()[0].ret_type)
                result = self.format(itype)
                if verbosity >= 1:
                    # In some contexts we want to be explicit about the distinction
                    # between type X and the type of type object X.
                    result += ' (type object)'
                return result
            elif isinstance(func, CallableType):
                return_type = strip_quotes(self.format(func.ret_type))
                if func.is_ellipsis_args:
                    return 'Callable[..., {}]'.format(return_type)
                arg_strings = []
                for arg_name, arg_type, arg_kind in zip(
                        func.arg_names, func.arg_types, func.arg_kinds):
                    if (arg_kind == ARG_POS and arg_name is None
                            or verbosity == 0 and arg_kind in (ARG_POS, ARG_OPT)):

                        arg_strings.append(
                            strip_quotes(
                                self.format(
                                    arg_type,
                                    verbosity = max(verbosity - 1, 0))))
                    else:
                        constructor = ARG_CONSTRUCTOR_NAMES[arg_kind]
                        if arg_kind in (ARG_STAR, ARG_STAR2) or arg_name is None:
                            arg_strings.append("{}({})".format(
                                constructor,
                                strip_quotes(self.format(arg_type))))
                        else:
                            arg_strings.append("{}({}, {})".format(
                                constructor,
                                strip_quotes(self.format(arg_type)),
                                repr(arg_name)))

                return 'Callable[[{}], {}]'.format(", ".join(arg_strings), return_type)
            else:
                # Use a simple representation for function types; proper
                # function types may result in long and difficult-to-read
                # error messages.
                return 'overloaded function'
        else:
            # Default case; we simply have to return something meaningful here.
            return 'object'

    def format_simple(self, typ: Type, verbosity: int = 0) -> str:
        """Convert simple types to string that is suitable for error messages.

        Return "" for complex types. Try to keep the length of the result
        relatively short to avoid overly long error messages.

        Examples:
          builtins.int -> 'int'
          Any type -> 'Any'
          None -> None
          callable type -> "" (empty string)
        """
        if isinstance(typ, Instance):
            itype = typ
            # Get the short name of the type.
            if itype.type.fullname() == 'types.ModuleType':
                return 'Module'
            if verbosity >= 2:
                base_str = itype.type.fullname()
            else:
                base_str = itype.type.name()
            if itype.args == []:
                # No type arguments. Place the type name in quotes to avoid
                # potential for confusion: otherwise, the type name could be
                # interpreted as a normal word.
                return '"{}"'.format(base_str)
            elif itype.type.fullname() == 'builtins.tuple':
                item_type_str = strip_quotes(self.format(itype.args[0]))
                return 'Tuple[{}, ...]'.format(item_type_str)
            elif itype.type.fullname() in reverse_type_aliases:
                alias = reverse_type_aliases[itype.type.fullname()]
                alias = alias.split('.')[-1]
                items = [strip_quotes(self.format(arg)) for arg in itype.args]
                return '{}[{}]'.format(alias, ', '.join(items))
            else:
                # There are type arguments. Convert the arguments to strings
                # (using format() instead of format_simple() to avoid empty
                # strings). If the result is too long, replace arguments
                # with [...].
                a = []  # type: List[str]
                for arg in itype.args:
                    a.append(strip_quotes(self.format(arg)))
                s = ', '.join(a)
                if len((base_str + s)) < 150:
                    return '{}[{}]'.format(base_str, s)
                else:
                    return '{}[...]'.format(base_str)
        elif isinstance(typ, TypeVarType):
            # This is similar to non-generic instance types.
            return '"{}"'.format(typ.name)
        elif isinstance(typ, TupleType):
            # Prefer the name of the fallback class (if not tuple), as it's more informative.
            if typ.fallback.type.fullname() != 'builtins.tuple':
                return self.format_simple(typ.fallback)
            items = []
            for t in typ.items:
                items.append(strip_quotes(self.format(t)))
            s = '"Tuple[{}]"'.format(', '.join(items))
            if len(s) < 400:
                return s
            else:
                return 'tuple(length {})'.format(len(items))
        elif isinstance(typ, TypedDictType):
            # If the TypedDictType is named, return the name
            if typ.fallback.type.fullname() != 'typing.Mapping':
                return self.format_simple(typ.fallback)
            items = []
            for (item_name, item_type) in typ.items.items():
                items.append('{}={}'.format(item_name, strip_quotes(self.format(item_type))))
            s = '"TypedDict({})"'.format(', '.join(items))
            return s
        elif isinstance(typ, UnionType):
            # Only print Unions as Optionals if the Optional wouldn't have to contain another Union
            print_as_optional = (len(typ.items) -
                                 sum(isinstance(t, NoneTyp) for t in typ.items) == 1)
            if print_as_optional:
                rest = [t for t in typ.items if not isinstance(t, NoneTyp)]
                return '"Optional[{}]"'.format(strip_quotes(self.format(rest[0])))
            else:
                items = []
                for t in typ.items:
                    items.append(strip_quotes(self.format(t)))
                s = '"Union[{}]"'.format(', '.join(items))
                if len(s) < 400:
                    return s
                else:
                    return 'union type ({} items)'.format(len(items))
        elif isinstance(typ, NoneTyp):
            return 'None'
        elif isinstance(typ, AnyType):
            return '"Any"'
        elif isinstance(typ, DeletedType):
            return '<deleted>'
        elif isinstance(typ, UninhabitedType):
            if typ.is_noreturn:
                return 'NoReturn'
            else:
                return '<uninhabited>'
        elif isinstance(typ, TypeType):
            return 'Type[{}]'.format(
                strip_quotes(self.format_simple(typ.item, verbosity)))
        elif typ is None:
            raise RuntimeError('Type is None')
        else:
            # No simple representation for this type that would convey very
            # useful information. No need to mention the type explicitly in a
            # message.
            return ''

    def format_distinctly(self, type1: Type, type2: Type) -> Tuple[str, str]:
        """Jointly format a pair of types to distinct strings.

        Increase the verbosity of the type strings until they become distinct.
        """
        verbosity = 0
        for verbosity in range(3):
            str1 = self.format(type1, verbosity=verbosity)
            str2 = self.format(type2, verbosity=verbosity)
            if str1 != str2:
                return (str1, str2)
        return (str1, str2)

    #
    # Specific operations
    #

    # The following operations are for generating specific error messages. They
    # get some information as arguments, and they build an error message based
    # on them.

    def has_no_attr(self, typ: Type, member: str, context: Context) -> Type:
        """Report a missing or non-accessible member.

        The type argument is the base type. If member corresponds to
        an operator, use the corresponding operator name in the
        messages. Return type Any.
        """
        if (isinstance(typ, Instance) and
                typ.type.has_readable_member(member)):
            self.fail('Member "{}" is not assignable'.format(member), context)
        elif member == '__contains__':
            self.fail('Unsupported right operand type for in ({})'.format(
                self.format(typ)), context)
        elif member in op_methods.values():
            # Access to a binary operator member (e.g. _add). This case does
            # not handle indexing operations.
            for op, method in op_methods.items():
                if method == member:
                    self.unsupported_left_operand(op, typ, context)
                    break
        elif member == '__neg__':
            self.fail('Unsupported operand type for unary - ({})'.format(
                self.format(typ)), context)
        elif member == '__pos__':
            self.fail('Unsupported operand type for unary + ({})'.format(
                self.format(typ)), context)
        elif member == '__invert__':
            self.fail('Unsupported operand type for ~ ({})'.format(
                self.format(typ)), context)
        elif member == '__getitem__':
            # Indexed get.
            # TODO: Fix this consistently in self.format
            if isinstance(typ, CallableType) and typ.is_type_obj():
                self.fail('The type {} is not generic and not indexable'.format(
                    self.format(typ)), context)
            else:
                self.fail('Value of type {} is not indexable'.format(
                    self.format(typ)), context)
        elif member == '__setitem__':
            # Indexed set.
            self.fail('Unsupported target for indexed assignment', context)
        elif member == '__call__':
            if isinstance(typ, Instance) and (typ.type.fullname() == 'builtins.function'):
                # "'function' not callable" is a confusing error message.
                # Explain that the problem is that the type of the function is not known.
                self.fail('Cannot call function of unknown type', context)
            else:
                self.fail('{} not callable'.format(self.format(typ)), context)
        else:
            # The non-special case: a missing ordinary attribute.
            if not self.disable_type_names:
                failed = False
                if isinstance(typ, Instance) and typ.type.names:
                    alternatives = set(typ.type.names.keys())
                    matches = [m for m in COMMON_MISTAKES.get(member, []) if m in alternatives]
                    matches.extend(best_matches(member, alternatives)[:3])
                    if matches:
                        self.fail('{} has no attribute "{}"; maybe {}?'.format(
                            self.format(typ), member, pretty_or(matches)), context)
                        failed = True
                if not failed:
                    self.fail('{} has no attribute "{}"'.format(self.format(typ),
                                                                member), context)
            else:
                self.fail('Some element of union has no attribute "{}"'.format(
                    member), context)
        return AnyType()

    def unsupported_operand_types(self, op: str, left_type: Any,
                                  right_type: Any, context: Context) -> None:
        """Report unsupported operand types for a binary operation.

        Types can be Type objects or strings.
        """
        left_str = ''
        if isinstance(left_type, str):
            left_str = left_type
        else:
            left_str = self.format(left_type)

        right_str = ''
        if isinstance(right_type, str):
            right_str = right_type
        else:
            right_str = self.format(right_type)

        if self.disable_type_names:
            msg = 'Unsupported operand types for {} (likely involving Union)'.format(op)
        else:
            msg = 'Unsupported operand types for {} ({} and {})'.format(
                op, left_str, right_str)
        self.fail(msg, context)

    def unsupported_left_operand(self, op: str, typ: Type,
                                 context: Context) -> None:
        if self.disable_type_names:
            msg = 'Unsupported left operand type for {} (some union)'.format(op)
        else:
            msg = 'Unsupported left operand type for {} ({})'.format(
                op, self.format(typ))
        self.fail(msg, context)

    def not_callable(self, typ: Type, context: Context) -> Type:
        self.fail('{} not callable'.format(self.format(typ)), context)
        return AnyType()

    def untyped_function_call(self, callee: CallableType, context: Context) -> Type:
        name = callee.name if callee.name is not None else '(unknown)'
        self.fail('Call to untyped function {} in typed context'.format(name), context)
        return AnyType()

    def incompatible_argument(self, n: int, m: int, callee: CallableType, arg_type: Type,
                              arg_kind: int, context: Context) -> None:
        """Report an error about an incompatible argument type.

        The argument type is arg_type, argument number is n and the
        callee type is 'callee'. If the callee represents a method
        that corresponds to an operator, use the corresponding
        operator name in the messages.
        """
        target = ''
        if callee.name:
            name = callee.name
            if callee.bound_args and callee.bound_args[0] is not None:
                base = self.format(callee.bound_args[0])
            else:
                base = extract_type(name)

            for op, method in op_methods.items():
                for variant in method, '__r' + method[2:]:
                    if name.startswith('"{}" of'.format(variant)):
                        if op == 'in' or variant != method:
                            # Reversed order of base/argument.
                            self.unsupported_operand_types(op, arg_type, base,
                                                           context)
                        else:
                            self.unsupported_operand_types(op, base, arg_type,
                                                           context)
                        return

            if name.startswith('"__getitem__" of'):
                self.invalid_index_type(arg_type, callee.arg_types[n - 1], base, context)
                return

            if name.startswith('"__setitem__" of'):
                if n == 1:
                    self.invalid_index_type(arg_type, callee.arg_types[n - 1], base, context)
                else:
                    msg = '{} (expression has type {}, target has type {})'
                    arg_type_str, callee_type_str = self.format_distinctly(arg_type,
                                                                           callee.arg_types[n - 1])
                    self.fail(msg.format(INCOMPATIBLE_TYPES_IN_ASSIGNMENT,
                                         arg_type_str, callee_type_str),
                              context)
                return

            target = 'to {} '.format(name)

        msg = ''
        if callee.name == '<list>':
            name = callee.name[1:-1]
            n -= 1
            msg = '{} item {} has incompatible type {}'.format(
                name.title(), n, self.format_simple(arg_type))
        elif callee.name == '<dict>':
            name = callee.name[1:-1]
            n -= 1
            key_type, value_type = cast(TupleType, arg_type).items
            msg = '{} entry {} has incompatible type {}: {}'.format(
                name.title(), n, self.format_simple(key_type), self.format_simple(value_type))
        elif callee.name == '<list-comprehension>':
            msg = 'List comprehension has incompatible type List[{}]'.format(
                strip_quotes(self.format(arg_type)))
        elif callee.name == '<set-comprehension>':
            msg = 'Set comprehension has incompatible type Set[{}]'.format(
                strip_quotes(self.format(arg_type)))
        elif callee.name == '<dictionary-comprehension>':
            msg = ('{} expression in dictionary comprehension has incompatible type {}; '
                   'expected type {}').format(
                'Key' if n == 1 else 'Value',
                self.format(arg_type),
                self.format(callee.arg_types[n - 1]))
        elif callee.name == '<generator>':
            msg = 'Generator has incompatible item type {}'.format(
                self.format_simple(arg_type))
        else:
            try:
                expected_type = callee.arg_types[m - 1]
            except IndexError:  # Varargs callees
                expected_type = callee.arg_types[-1]
            arg_type_str, expected_type_str = self.format_distinctly(arg_type, expected_type)
            if arg_kind == ARG_STAR:
                arg_type_str = '*' + arg_type_str
            elif arg_kind == ARG_STAR2:
                arg_type_str = '**' + arg_type_str
            msg = 'Argument {} {}has incompatible type {}; expected {}'.format(
                n, target, arg_type_str, expected_type_str)
        self.fail(msg, context)

    def invalid_index_type(self, index_type: Type, expected_type: Type, base_str: str,
                           context: Context) -> None:
        self.fail('Invalid index type {} for {}; expected type {}'.format(
            self.format(index_type), base_str, self.format(expected_type)), context)

    def too_few_arguments(self, callee: CallableType, context: Context,
                          argument_names: List[str]) -> None:
        if (argument_names is not None and not all(k is None for k in argument_names)
                and len(argument_names) >= 1):
            diff = [k for k in callee.arg_names if k not in argument_names]
            if len(diff) == 1:
                msg = 'Missing positional argument'
            else:
                msg = 'Missing positional arguments'
            if callee.name and diff and all(d is not None for d in diff):
                msg += ' "{}" in call to {}'.format('", "'.join(diff), callee.name)
        else:
            msg = 'Too few arguments'
            if callee.name:
                msg += ' for {}'.format(callee.name)
        self.fail(msg, context)

    def missing_named_argument(self, callee: CallableType, context: Context, name: str) -> None:
        msg = 'Missing named argument "{}"'.format(name)
        if callee.name:
            msg += ' for function {}'.format(callee.name)
        self.fail(msg, context)

    def too_many_arguments(self, callee: CallableType, context: Context) -> None:
        msg = 'Too many arguments'
        if callee.name:
            msg += ' for {}'.format(callee.name)
        self.fail(msg, context)

    def too_many_positional_arguments(self, callee: CallableType,
                                      context: Context) -> None:
        msg = 'Too many positional arguments'
        if callee.name:
            msg += ' for {}'.format(callee.name)
        self.fail(msg, context)

    def unexpected_keyword_argument(self, callee: CallableType, name: str,
                                    context: Context) -> None:
        msg = 'Unexpected keyword argument "{}"'.format(name)
        if callee.name:
            msg += ' for {}'.format(callee.name)
        self.fail(msg, context)
        if callee.definition:
            fullname = callee.definition.fullname()
            if fullname is not None and '.' in fullname:
                module_name = fullname.rsplit('.', 1)[0]
                path = self.modules[module_name].path
                self.note('{} defined here'.format(callee.name), callee.definition,
                          file=path, origin=context)

    def duplicate_argument_value(self, callee: CallableType, index: int,
                                 context: Context) -> None:
        self.fail('{} gets multiple values for keyword argument "{}"'.
                  format(capitalize(callable_name(callee)),
                         callee.arg_names[index]), context)

    def does_not_return_value(self, callee_type: Type, context: Context) -> None:
        """Report an error about use of an unusable type."""
        if isinstance(callee_type, FunctionLike) and callee_type.get_name() is not None:
            self.fail('{} does not return a value'.format(
                capitalize(callee_type.get_name())), context)
        else:
            self.fail('Function does not return a value', context)

    def deleted_as_rvalue(self, typ: DeletedType, context: Context) -> None:
        """Report an error about using an deleted type as an rvalue."""
        if typ.source is None:
            s = ""
        else:
            s = " '{}'".format(typ.source)
        self.fail('Trying to read deleted variable{}'.format(s), context)

    def deleted_as_lvalue(self, typ: DeletedType, context: Context) -> None:
        """Report an error about using an deleted type as an lvalue.

        Currently, this only occurs when trying to assign to an
        exception variable outside the local except: blocks.
        """
        if typ.source is None:
            s = ""
        else:
            s = " '{}'".format(typ.source)
        self.fail('Assignment to variable{} outside except: block'.format(s), context)

    def no_variant_matches_arguments(self, overload: Overloaded, arg_types: List[Type],
                                     context: Context) -> None:
        if overload.name():
            self.fail('No overload variant of {} matches argument types {}'
                      .format(overload.name(), arg_types), context)
        else:
            self.fail('No overload variant matches argument types {}'.format(arg_types), context)

    def wrong_number_values_to_unpack(self, provided: int, expected: int,
                                      context: Context) -> None:
        if provided < expected:
            if provided == 1:
                self.fail('Need more than 1 value to unpack ({} expected)'.format(expected),
                          context)
            else:
                self.fail('Need more than {} values to unpack ({} expected)'.format(
                    provided, expected), context)
        elif provided > expected:
            self.fail('Too many values to unpack ({} expected, {} provided)'.format(
                expected, provided), context)

    def type_not_iterable(self, type: Type, context: Context) -> None:
        self.fail('\'{}\' object is not iterable'.format(type), context)

    def incompatible_operator_assignment(self, op: str,
                                         context: Context) -> None:
        self.fail('Result type of {} incompatible in assignment'.format(op),
                  context)

    def signature_incompatible_with_supertype(
            self, name: str, name_in_super: str, supertype: str,
            context: Context) -> None:
        target = self.override_target(name, name_in_super, supertype)
        self.fail('Signature of "{}" incompatible with {}'.format(
            name, target), context)

    def argument_incompatible_with_supertype(
            self, arg_num: int, name: str, name_in_supertype: str,
            supertype: str, context: Context) -> None:
        target = self.override_target(name, name_in_supertype, supertype)
        self.fail('Argument {} of "{}" incompatible with {}'
                  .format(arg_num, name, target), context)

    def return_type_incompatible_with_supertype(
            self, name: str, name_in_supertype: str, supertype: str,
            context: Context) -> None:
        target = self.override_target(name, name_in_supertype, supertype)
        self.fail('Return type of "{}" incompatible with {}'
                  .format(name, target), context)

    def override_target(self, name: str, name_in_super: str,
                        supertype: str) -> str:
        target = 'supertype "{}"'.format(supertype)
        if name_in_super != name:
            target = '"{}" of {}'.format(name_in_super, target)
        return target

    def incompatible_type_application(self, expected_arg_count: int,
                                      actual_arg_count: int,
                                      context: Context) -> None:
        if expected_arg_count == 0:
            self.fail('Type application targets a non-generic function or class',
                      context)
        elif actual_arg_count > expected_arg_count:
            self.fail('Type application has too many types ({} expected)'
                      .format(expected_arg_count), context)
        else:
            self.fail('Type application has too few types ({} expected)'
                      .format(expected_arg_count), context)

    def could_not_infer_type_arguments(self, callee_type: CallableType, n: int,
                                       context: Context) -> None:
        if callee_type.name and n > 0:
            self.fail('Cannot infer type argument {} of {}'.format(
                n, callee_type.name), context)
        else:
            self.fail('Cannot infer function type argument', context)

    def invalid_var_arg(self, typ: Type, context: Context) -> None:
        self.fail('List or tuple expected as variable arguments', context)

    def invalid_keyword_var_arg(self, typ: Type, context: Context) -> None:
        if isinstance(typ, Instance) and (typ.type.fullname() == 'builtins.dict'):
            self.fail('Keywords must be strings', context)
        else:
            self.fail('Argument after ** must be a dictionary',
                      context)

    def undefined_in_superclass(self, member: str, context: Context) -> None:
        self.fail('"{}" undefined in superclass'.format(member), context)

    def too_few_string_formatting_arguments(self, context: Context) -> None:
        self.fail('Not enough arguments for format string', context)

    def too_many_string_formatting_arguments(self, context: Context) -> None:
        self.fail('Not all arguments converted during string formatting', context)

    def unsupported_placeholder(self, placeholder: str, context: Context) -> None:
        self.fail('Unsupported format character \'%s\'' % placeholder, context)

    def string_interpolation_with_star_and_key(self, context: Context) -> None:
        self.fail('String interpolation contains both stars and mapping keys', context)

    def requires_int_or_char(self, context: Context) -> None:
        self.fail('%c requires int or char', context)

    def key_not_in_mapping(self, key: str, context: Context) -> None:
        self.fail('Key \'%s\' not found in mapping' % key, context)

    def string_interpolation_mixing_key_and_non_keys(self, context: Context) -> None:
        self.fail('String interpolation mixes specifier with and without mapping keys', context)

    def cannot_determine_type(self, name: str, context: Context) -> None:
        self.fail("Cannot determine type of '%s'" % name, context)

    def cannot_determine_type_in_base(self, name: str, base: str, context: Context) -> None:
        self.fail("Cannot determine type of '%s' in base class '%s'" % (name, base), context)

    def invalid_method_type(self, sig: CallableType, context: Context) -> None:
        self.fail('Invalid method type', context)

    def invalid_class_method_type(self, sig: CallableType, context: Context) -> None:
        self.fail('Invalid class method type', context)

    def incompatible_conditional_function_def(self, defn: FuncDef) -> None:
        self.fail('All conditional function variants must have identical '
                  'signatures', defn)

    def cannot_instantiate_abstract_class(self, class_name: str,
                                          abstract_attributes: List[str],
                                          context: Context) -> None:
        attrs = format_string_list("'%s'" % a for a in abstract_attributes)
        self.fail("Cannot instantiate abstract class '%s' with abstract "
                  "attribute%s %s" % (class_name, plural_s(abstract_attributes),
                                   attrs),
                  context)

    def base_class_definitions_incompatible(self, name: str, base1: TypeInfo,
                                            base2: TypeInfo,
                                            context: Context) -> None:
        self.fail('Definition of "{}" in base class "{}" is incompatible '
                  'with definition in base class "{}"'.format(
                      name, base1.name(), base2.name()), context)

    def cant_assign_to_method(self, context: Context) -> None:
        self.fail(CANNOT_ASSIGN_TO_METHOD, context)

    def cant_assign_to_classvar(self, name: str, context: Context) -> None:
        self.fail('Cannot assign to class variable "%s" via instance' % name, context)

    def read_only_property(self, name: str, type: TypeInfo,
                           context: Context) -> None:
        self.fail('Property "{}" defined in "{}" is read-only'.format(
            name, type.name()), context)

    def incompatible_typevar_value(self, callee: CallableType, index: int,
                                   type: Type, context: Context) -> None:
        self.fail('Type argument {} of {} has incompatible value {}'.format(
            index, callable_name(callee), self.format(type)), context)

    def overloaded_signatures_overlap(self, index1: int, index2: int,
                                      context: Context) -> None:
        self.fail('Overloaded function signatures {} and {} overlap with '
                  'incompatible return types'.format(index1, index2), context)

    def overloaded_signatures_arg_specific(self, index1: int, context: Context) -> None:
        self.fail('Overloaded function implementation does not accept all possible arguments '
                  'of signature {}'.format(index1), context)

    def overloaded_signatures_ret_specific(self, index1: int, context: Context) -> None:
        self.fail('Overloaded function implementation cannot produce return type '
                  'of signature {}'.format(index1), context)

    def operator_method_signatures_overlap(
            self, reverse_class: str, reverse_method: str, forward_class: str,
            forward_method: str, context: Context) -> None:
        self.fail('Signatures of "{}" of "{}" and "{}" of "{}" '
                  'are unsafely overlapping'.format(
                      reverse_method, reverse_class,
                      forward_method, forward_class),
                  context)

    def forward_operator_not_callable(
            self, forward_method: str, context: Context) -> None:
        self.fail('Forward operator "{}" is not callable'.format(
            forward_method), context)

    def signatures_incompatible(self, method: str, other_method: str,
                                context: Context) -> None:
        self.fail('Signatures of "{}" and "{}" are incompatible'.format(
            method, other_method), context)

    def yield_from_invalid_operand_type(self, expr: Type, context: Context) -> Type:
        text = self.format(expr) if self.format(expr) != 'object' else expr
        self.fail('"yield from" can\'t be applied to {}'.format(text), context)
        return AnyType()

    def invalid_signature(self, func_type: Type, context: Context) -> None:
        self.fail('Invalid signature "{}"'.format(func_type), context)

    def reveal_type(self, typ: Type, context: Context) -> None:
        self.fail('Revealed type is \'{}\''.format(typ), context)

    def unsupported_type_type(self, item: Type, context: Context) -> None:
        self.fail('Unsupported type Type[{}]'.format(self.format(item)), context)

    def redundant_cast(self, typ: Type, context: Context) -> None:
        self.note('Redundant cast to {}'.format(self.format(typ)), context)

    def typeddict_instantiated_with_unexpected_items(self,
                                                     expected_item_names: List[str],
                                                     actual_item_names: List[str],
                                                     context: Context) -> None:
        self.fail('Expected items {} but found {}.'.format(
            expected_item_names, actual_item_names), context)

    def typeddict_item_name_must_be_string_literal(self,
                                                   typ: TypedDictType,
                                                   context: Context,
                                                   ) -> None:
        self.fail('Cannot prove expression is a valid item name; expected one of {}'.format(
            format_item_name_list(typ.items.keys())), context)

    def typeddict_item_name_not_found(self,
                                      typ: TypedDictType,
                                      item_name: str,
                                      context: Context,
                                      ) -> None:
        self.fail('\'{}\' is not a valid item name; expected one of {}'.format(
            item_name, format_item_name_list(typ.items.keys())), context)

    def type_arguments_not_allowed(self, context: Context) -> None:
        self.fail('Parameterized generics cannot be used with class or instance checks', context)


def capitalize(s: str) -> str:
    """Capitalize the first character of a string."""
    if s == '':
        return ''
    else:
        return s[0].upper() + s[1:]


def extract_type(name: str) -> str:
    """If the argument is the name of a method (of form C.m), return
    the type portion in quotes (e.g. "y"). Otherwise, return the string
    unmodified.
    """
    name = re.sub('^"[a-zA-Z0-9_]+" of ', '', name)
    return name


def strip_quotes(s: str) -> str:
    """Strip a double quote at the beginning and end of the string, if any."""
    s = re.sub('^"', '', s)
    s = re.sub('"$', '', s)
    return s


def plural_s(s: Sequence[Any]) -> str:
    if len(s) > 1:
        return 's'
    else:
        return ''


def format_string_list(s: Iterable[str]) -> str:
    l = list(s)
    assert len(l) > 0
    if len(l) == 1:
        return l[0]
    elif len(l) <= 5:
        return '%s and %s' % (', '.join(l[:-1]), l[-1])
    else:
        return '%s, ... and %s (%i methods suppressed)' % (', '.join(l[:2]), l[-1], len(l) - 3)


def format_item_name_list(s: Iterable[str]) -> str:
    l = list(s)
    if len(l) <= 5:
        return '[' + ', '.join(["'%s'" % name for name in l]) + ']'
    else:
        return '[' + ', '.join(["'%s'" % name for name in l[:5]]) + ', ...]'


def callable_name(type: CallableType) -> str:
    if type.name:
        return type.name
    else:
        return 'function'


def temp_message_builder() -> MessageBuilder:
    """Return a message builder usable for throwaway errors (which may not format properly)."""
    return MessageBuilder(Errors(), {})


# For hard-coding suggested missing member alternatives.
COMMON_MISTAKES = {
    'add': ('append', 'extend'),
}  # type: Dict[str, Sequence[str]]


def best_matches(current: str, options: Iterable[str]) -> List[str]:
    ratios = {v: difflib.SequenceMatcher(a=current, b=v).ratio() for v in options}
    return sorted((o for o in options if ratios[o] > 0.75),
                  reverse=True, key=lambda v: (ratios[v], v))


def pretty_or(args: List[str]) -> str:
    quoted = ['"' + a + '"' for a in args]
    if len(quoted) == 1:
        return quoted[0]
    if len(quoted) == 2:
        return "{} or {}".format(quoted[0], quoted[1])
    return ", ".join(quoted[:-1]) + ", or " + quoted[-1]
