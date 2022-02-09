#
# Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#                    $$$$$$\  $$$$$$$\   $$$$$$\
#                   $$  __$$\ $$  __$$\ $$  __$$\
#                   $$ /  \__|$$ |  $$ |$$ /  \__|
#                   $$ |      $$$$$$$  |$$ |$$$$\
#                   $$ |      $$  ____/ $$ |\_$$ |
#                   $$ |  $$\ $$ |      $$ |  $$ |
#                   \$$$$$   |$$ |      \$$$$$   |
#                    \______/ \__|       \______/
#
from ._misc import NOT_IMPLEMENTED_MSG
from ._misc import handle_operator_code
from ._spotless_dummy import *
from de.fraunhofer.aisec.cpg.graph import NodeBuilder
from de.fraunhofer.aisec.cpg.graph.types import TypeParser
from de.fraunhofer.aisec.cpg.graph.types import UnknownType
import ast


def handle_expression(self, expr):
    self.log_with_loc("Start \"handle_expression\" for:\n%s\n" %
                      (self.get_src_code(expr)))
    r = self.handle_expression_impl(expr)
    self.add_loc_info(expr, r)
    self.log_with_loc("End \"handle_expr\" for:\n%s\nResult is: %s" %
                      (self.get_src_code(expr),
                       r))
    return r


def handle_expression_impl(self, expr):
    if isinstance(expr, ast.BoolOp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
    elif isinstance(expr, ast.NamedExpr):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
    elif isinstance(expr, ast.BinOp):
        opcode = self.handle_operator_code(expr.op)
        binop = NodeBuilder.newBinaryOperator(opcode,
                                              self.get_src_code(expr))
        binop.setLhs(self.handle_expression(expr.left))
        binop.setRhs(self.handle_expression(expr.right))
        return binop
    elif isinstance(expr, ast.UnaryOp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
    elif isinstance(expr, ast.Lambda):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
    elif isinstance(expr, ast.IfExp):
        test = self.handle_expression(expr.test)
        body = self.handle_expression(expr.body)
        orelse = self.handle_expression(expr.orelse)
        r = NodeBuilder.newConditionalExpression(
            test, body, orelse, UnknownType.getUnknownType())
        return r
    elif isinstance(expr, ast.Dict):
        self.log_with_loc("Handling a \"dict\": %s" % (ast.dump(expr)))
        ile = NodeBuilder.newInitializerListExpression(self.get_src_code(expr))

        lst = []

        # loop through keys and get values
        for i in range(0, len(expr.keys)):
            key = expr.keys[i]
            value = expr.values[i]

            if key is not None:
                key_expr = self.handle_expression(key)
            else:
                key_expr = None
            if value is not None:
                value_expr = self.handle_expression(value)
            else:
                value_expr = None

            # construct a key value expression
            key_value = NodeBuilder.newKeyValueExpression(
                key_expr, value_expr, self.get_src_code(expr))
            # TODO location info

            lst.append(key_value)

        ile.setInitializers(lst)

        return ile
    elif isinstance(expr, ast.Set):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
    elif isinstance(expr, ast.ListComp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
    elif isinstance(expr, ast.SetComp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
    elif isinstance(expr, ast.DictComp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
    elif isinstance(expr, ast.GeneratorExp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
    elif isinstance(expr, ast.Await):
        self.log_with_loc((
            "\"await\" is currently not supported. The expression"
            " is parsed but the \"await\" information is not available in the"
            " graph."), loglevel="ERROR")
        return self.handle_expression(expr.value)
    elif isinstance(expr, ast.Yield):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
    elif isinstance(expr, ast.YieldFrom):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
    elif isinstance(expr, ast.Compare):
        # Compare(expr left, cmpop* ops, expr* comparators)
        if len(expr.ops) != 1 or len(expr.comparators) != 1:
            self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
            r = NodeBuilder.newBinaryOperator("DUMMY", self.get_src_code(expr))
            return r
        op = expr.ops[0]
        if isinstance(op, ast.Eq):
            op_code = "=="
        elif isinstance(op, ast.NotEq):
            op_code = "!="
        elif isinstance(op, ast.Lt):
            op_code = "<"
        elif isinstance(op, ast.LtE):
            op_code = "<="
        elif isinstance(op, ast.Gt):
            op_code = ">"
        elif isinstance(op, ast.GtE):
            op_code = ">="
        elif isinstance(op, ast.Is):
            op_code = "is"
        elif isinstance(op, ast.IsNot):
            op_code = "is not"
        elif isinstance(op, ast.In):
            op_code = "in"
        elif isinstance(op, ast.NotIn):
            op_code = "not in"
        else:
            self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
            r = NodeBuilder.newBinaryOperator("DUMMY", self.get_src_code(expr))
            return r
        comp = NodeBuilder.newBinaryOperator(op_code,
                                             self.get_src_code(expr))
        comp.setLhs(self.handle_expression(expr.left))
        comp.setRhs(self.handle_expression(expr.comparators[0]))
        return comp
    elif isinstance(expr, ast.Call):
        # Call(expr func, expr* args, keyword* keywords)
        # TODO copy & paste -> improve

        # a call can be one of
        # - simple function call
        # - member call
        # - constructor call
        #
        # We parse node.func regularly using a visitor and decide what it is
        ref = self.handle_expression(expr.func)
        self.log_with_loc("Parsed ref as %s" % (ref))

        name = ref.getName()

        if self.is_member_expression(ref):
            base_name = ref.getBase().getName()

            fqn = "%s.%s" % (base_name, name)

            member = NodeBuilder.newDeclaredReferenceExpression(
                name, UnknownType.getUnknownType(), self.get_src_code(expr))
            call = NodeBuilder.newMemberCallExpression(
                name, fqn, ref.getBase(), member, ".", self.get_src_code(expr))
        else:
            # this can be a simple function call or a ctor
            record = self.scopemanager.getRecordForName(
                self.scopemanager.getCurrentScope(), name)
            if record is not None:
                self.log_with_loc("Received a record: %s" % (record))
                call = NodeBuilder.newConstructExpression(
                    self.get_src_code(expr))
                call.setName(expr.func.id)
                tpe = TypeParser.createFrom(record.getName(), False)
                call.setType(tpe)
            else:
                # TODO int, float, ...
                if name == "str" and len(expr.args) == 1:
                    cast = NodeBuilder.newCastExpression(
                        self.get_src_code(expr))
                    cast.setCastType(TypeParser.createFrom("str", False))
                    cast.setExpression(self.handle_expression(expr.args[0]))
                    return cast
                else:
                    call = NodeBuilder.newCallExpression(
                        name, name, self.get_src_code(expr), False)
        for a in expr.args:
            call.addArgument(self.handle_expression(a))
        for keyword in expr.keywords:
            if keyword.arg is not None:
                call.addArgument(self.handle_expression(keyword.value),
                                 keyword.arg)
            else:
                # TODO: keywords without args, aka **arg
                self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        self.log_with_loc("Parsed call: %s" % (call))
        return call

    elif isinstance(expr, ast.FormattedValue):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
    elif isinstance(expr, ast.JoinedStr):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
    elif isinstance(expr, ast.Constant):
        if isinstance(expr.value, type(None)):
            tpe = TypeParser.createFrom("None", False)
        elif isinstance(expr.value, bool):
            tpe = TypeParser.createFrom("bool", False)
        elif isinstance(expr.value, int):
            tpe = TypeParser.createFrom("int", False)
        elif isinstance(expr.value, float):
            tpe = TypeParser.createFrom("float", False)
        elif isinstance(expr.value, complex):
            tpe = TypeParser.createFrom("complex", False)
        elif isinstance(expr.value, str):
            tpe = TypeParser.createFrom("str", False)
        elif isinstance(expr.value, bytes):
            tpe = TypeParser.createFrom("byte[]", False)
        else:
            self.log_with_loc("Found unexpected type - using a dummy: %s" %
                              (type(expr.value)), loglevel="ERROR")
            tpe = UnknownType.getUnknownType()
        lit = NodeBuilder.newLiteral(expr.value, tpe, self.get_src_code(expr))
        lit.setName(str(expr.value))
        return lit

    elif isinstance(expr, ast.Attribute):
        value = self.handle_expression(expr.value)
        self.log_with_loc("Parsed base/value as: %s" % (value))
        if self.is_declaration(value):
            self.log_with_loc(
                ("Found a new declaration. "
                 "Wrapping it in a DeclaredReferenceExpression."),
                loglevel="DEBUG")
            value = NodeBuilder.newDeclaredReferenceExpression(value.getName(),
                                                               value.getType(),
                                                               value.getCode())
        mem = NodeBuilder.newMemberExpression(
            value, UnknownType.getUnknownType(), expr.attr, ".",
            self.get_src_code(expr))
        return mem

    elif isinstance(expr, ast.Subscript):
        value = self.handle_expression(expr.value)
        slc = self.handle_expression(expr.slice)
        exp = NodeBuilder.newArraySubscriptionExpression(
            self.get_src_code(expr))
        exp.setArrayExpression(value)
        exp.setSubscriptExpression(slc)
        return exp
    elif isinstance(expr, ast.Starred):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
    elif isinstance(expr, ast.Name):
        r = NodeBuilder.newDeclaredReferenceExpression(
            expr.id, UnknownType.getUnknownType(), self.get_src_code(expr))
        return r
    elif isinstance(expr, ast.List):
        ile = NodeBuilder.newInitializerListExpression(self.get_src_code(expr))

        lst = []

        for el in expr.elts:
            expr = self.handle_expression(el)
            lst.append(expr)

        ile.setInitializers(lst)

        return ile
    elif isinstance(expr, ast.Tuple):
        ile = NodeBuilder.newInitializerListExpression(self.get_src_code(expr))

        lst = []

        for el in expr.elts:
            expr = self.handle_expression(el)
            lst.append(expr)

        ile.setInitializers(lst)

        return ile
    elif isinstance(expr, ast.Slice):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
    else:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newExpression("")
        return r
