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
from ._spotless_dummy import *
from de.fraunhofer.aisec.cpg.graph import ExpressionBuilderKt
from de.fraunhofer.aisec.cpg.graph import NodeBuilderKt
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
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
    elif isinstance(expr, ast.NamedExpr):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
    elif isinstance(expr, ast.BinOp):
        # This could be a simple + or a complex number like 3+5j
        lhs = self.handle_expression(expr.left)
        rhs = self.handle_expression(expr.right)
        if (isinstance(expr.right, ast.Constant)
                and isinstance(expr.right.value, complex)):
            if not (self.is_literal(lhs) and self.is_literal(rhs)):
                self.log_with_loc(
                    "Expected two literals. Returning a \"None\" Literal.",
                    loglevel="ERROR")
                return ExpressionBuilderKt(
                    self.frontend,
                    None,
                    UnknownType.getUnknownType(),
                    self.get_src_code(expr),
                    expr)
            # we got a complex number
            complextype = NodeBuilderKt.parseType(self.frontend, "complex")

            # TODO: fix this once the CPG supports complex numbers
            realpart = complex(lhs.getValue())
            imagpart = complex(rhs.getValue())
            ret = ExpressionBuilderKt.newLiteral(
                self.frontend,
                # currently no support for complex numbers in the java part
                str(realpart + imagpart),
                complextype,
                self.get_src_code(expr),
                expr)
            return ret
        else:
            opcode = self.handle_operator_code(expr.op)
            binop = ExpressionBuilderKt.newBinaryOperator(
                self.frontend, opcode, self.get_src_code(expr))
            binop.setLhs(lhs)
            binop.setRhs(rhs)
            return binop
    elif isinstance(expr, ast.UnaryOp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
    elif isinstance(expr, ast.Lambda):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
    elif isinstance(expr, ast.IfExp):
        test = self.handle_expression(expr.test)
        body = self.handle_expression(expr.body)
        orelse = self.handle_expression(expr.orelse)
        r = ExpressionBuilderKt.newConditionalExpression(
            self.frontend, test, body, orelse, UnknownType.getUnknownType())
        return r
    elif isinstance(expr, ast.Dict):
        ile = ExpressionBuilderKt.newInitializerListExpression(
            self.frontend, self.get_src_code(expr))

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
            key_value = ExpressionBuilderKt.newKeyValueExpression(
                self.frontend, key_expr, value_expr, self.get_src_code(expr))
            if key is not None and value is not None:
                self.add_mul_loc_infos(key, value, key_value)

            lst.append(key_value)

        ile.setInitializers(lst)

        return ile
    elif isinstance(expr, ast.Set):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
    elif isinstance(expr, ast.ListComp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
    elif isinstance(expr, ast.SetComp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
    elif isinstance(expr, ast.DictComp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
    elif isinstance(expr, ast.GeneratorExp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
    elif isinstance(expr, ast.Await):
        self.log_with_loc((
            "\"await\" is currently not supported. The expression"
            " is parsed but the \"await\" information is not available in the"
            " graph."), loglevel="ERROR")
        return self.handle_expression(expr.value)
    elif isinstance(expr, ast.Yield):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
    elif isinstance(expr, ast.YieldFrom):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
    elif isinstance(expr, ast.Compare):
        # Compare(expr left, cmpop* ops, expr* comparators)
        if len(expr.ops) != 1 or len(expr.comparators) != 1:
            self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
            r = ExpressionBuilderKt.newBinaryOperator(
                self.frontend, "DUMMY", self.get_src_code(expr))
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
            r = ExpressionBuilderKt.newBinaryOperator(
                self.frontend, "DUMMY", self.get_src_code(expr))
            return r
        comp = ExpressionBuilderKt.newBinaryOperator(
            self.frontend, op_code, self.get_src_code(expr))
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
        self.log_with_loc("Parsed ref as %s" % ref)

        refname = ref.getName()

        if self.is_member_expression(ref):
            call = ExpressionBuilderKt.newMemberCallExpression(
                self.frontend,
                ref, False, self.get_src_code(expr))
        else:
            # try to see, whether this refers to a known class and thus is a
            # constructor.
            record = self.scopemanager.getRecordForName(
                self.scopemanager.getCurrentScope(), refname)
            if record is not None:
                self.log_with_loc("Received a record: %s" % record)
                call = ExpressionBuilderKt.newConstructExpression(
                    self.frontend, expr.func.id, self.get_src_code(expr))
                tpe = record.toType()
                call.setType(tpe)
            else:
                # TODO int, float, ...
                if refname == "str" and len(expr.args) == 1:
                    cast = ExpressionBuilderKt.newCastExpression(
                        self.frontend, self.get_src_code(expr))
                    cast.setCastType(
                        NodeBuilderKt.parseType(self.frontend, "str"))
                    cast.setExpression(
                        self.handle_expression(expr.args[0]))
                    return cast
                else:
                    call = ExpressionBuilderKt.newCallExpression(
                        self.frontend,
                        ref, refname, self.get_src_code(expr))
        for a in expr.args:
            call.addArgument(self.handle_expression(a))
        for keyword in expr.keywords:
            if keyword.arg is not None:
                call.addArgument(
                    self.handle_expression(keyword.value),
                    keyword.arg)
            else:
                # TODO: keywords without args, aka **arg
                self.log_with_loc(
                    NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        self.log_with_loc("Parsed call: %s" % call)
        return call

    elif isinstance(expr, ast.FormattedValue):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
    elif isinstance(expr, ast.JoinedStr):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
    elif isinstance(expr, ast.Constant):
        resultvalue = expr.value
        if isinstance(expr.value, type(None)):
            tpe = NodeBuilderKt.parseType(self.frontend, "None")
        elif isinstance(expr.value, bool):
            tpe = NodeBuilderKt.parseType(self.frontend, "bool")
        elif isinstance(expr.value, int):
            tpe = NodeBuilderKt.parseType(self.frontend, "int")
        elif isinstance(expr.value, float):
            tpe = NodeBuilderKt.parseType(self.frontend, "float")
        elif isinstance(expr.value, complex):
            tpe = NodeBuilderKt.parseType(self.frontend, "complex")
            # TODO: fix this once the CPG supports complex numbers
            resultvalue = str(resultvalue)
        elif isinstance(expr.value, str):
            tpe = NodeBuilderKt.parseType(self.frontend, "str")
        elif isinstance(expr.value, bytes):
            tpe = NodeBuilderKt.parseType(self.frontend, "byte[]")
        else:
            self.log_with_loc(
                "Found unexpected type - using a dummy: %s" %
                (type(expr.value)),
                loglevel="ERROR")
            tpe = UnknownType.getUnknownType()
        lit = ExpressionBuilderKt.newLiteral(
            self.frontend,
            resultvalue, tpe, self.get_src_code(expr))
        return lit

    elif isinstance(expr, ast.Attribute):
        value = self.handle_expression(expr.value)
        self.log_with_loc("Parsed base/value as: %s" % value)
        if self.is_declaration(value):
            self.log_with_loc(
                ("Found a new declaration. "
                 "Wrapping it in a DeclaredReferenceExpression."),
                loglevel="DEBUG")
            value = ExpressionBuilderKt.newDeclaredReferenceExpression(
                self.frontend,
                value.getName(), value.getType(), value.getCode())
        mem = ExpressionBuilderKt.newMemberExpression(
            self.frontend, expr.attr, value, UnknownType.getUnknownType(),
            ".", self.get_src_code(expr))
        return mem

    elif isinstance(expr, ast.Subscript):
        value = self.handle_expression(expr.value)
        slc = self.handle_expression(expr.slice)
        exp = ExpressionBuilderKt.newArraySubscriptionExpression(
            self.frontend, self.get_src_code(expr))
        exp.setArrayExpression(value)
        exp.setSubscriptExpression(slc)
        return exp
    elif isinstance(expr, ast.Starred):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
    elif isinstance(expr, ast.Name):
        r = ExpressionBuilderKt.newDeclaredReferenceExpression(
            self.frontend, expr.id, UnknownType.getUnknownType(),
            self.get_src_code(expr))

        # Take a little shortcut and set refersTo, in case this is a method
        # receiver. This allows us to play more nicely with member (call)
        # expressions on the current class, since then their base type is
        # known.
        current_function = self.scopemanager.getCurrentFunction()
        if self.is_method_declaration(current_function):
            recv = current_function.getReceiver()
            if recv is not None:
                if expr.id == recv.getName().getLocalName():
                    r.setRefersTo(recv)
                    r.setType(recv.getType())

        return r
    elif isinstance(expr, ast.List):
        ile = ExpressionBuilderKt.newInitializerListExpression(
            self.frontend, self.get_src_code(expr))

        lst = []

        for el in expr.elts:
            expr = self.handle_expression(el)
            lst.append(expr)

        ile.setInitializers(lst)

        return ile
    elif isinstance(expr, ast.Tuple):
        ile = ExpressionBuilderKt.newInitializerListExpression(
            self.frontend, self.get_src_code(expr))

        lst = []

        for el in expr.elts:
            expr = self.handle_expression(el)
            lst.append(expr)

        ile.setInitializers(lst)

        return ile
    elif isinstance(expr, ast.Slice):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
    else:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = ExpressionBuilderKt.newExpression(self.frontend, "")
        return r
