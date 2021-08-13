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
from ._spotless_dummy import *
from de.fraunhofer.aisec.cpg.graph import NodeBuilder
from de.fraunhofer.aisec.cpg.graph.types import TypeParser
from de.fraunhofer.aisec.cpg.graph.types import UnknownType
from ._misc import NOT_IMPLEMENTED_MSG

import ast

DUMMY_CODE = ""  # TODO: Currently, I cannot access the source code...


def handle_expression(self, expr):
    self.log_with_loc("Handling expression: %s" % (ast.dump(expr)))

    if isinstance(expr, ast.BoolOp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.NamedExpr):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.BinOp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.UnaryOp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.Lambda):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.IfExp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.Dict):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.Set):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.ListComp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.SetComp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.DictComp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.GeneratorExp):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.Await):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.Yield):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.YieldFrom):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.Compare):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
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

        name = ref.getName()

        if self.is_member_expression(ref):
            base_name = ref.getBase().getName()

            fqn = "%s.%s" % (base_name, name)

            member = NodeBuilder.newDeclaredReferenceExpression(
                name, UnknownType.getUnknownType(), DUMMY_CODE)
            call = NodeBuilder.newMemberCallExpression(
                name, fqn, ref.getBase(), member, ".", DUMMY_CODE)
        else:
            # this can be a simple function call or a ctor
            record = self.scopemanager.getRecordForName(
                self.scopemanager.getCurrentScope(), name)
            if record is not None:
                self.log_with_loc("Received a record: %s" % (record))
                call = NodeBuilder.newConstructExpression(DUMMY_CODE)
                call.setName(expr.func.id)
                tpe = TypeParser.createFrom(record.getName(), False)
                call.setType(tpe)
            else:
                # TODO int, float, ...
                if name == "str" and len(expr.args) == 1:
                    cast = newCastExpression(DUMMY_CODE)
                    cast.setCastType(TypeParser.createFrom("str", False))
                    cast.setExpression(self.handle_expression(expr.args[0])
                                       )
                    return cast
                else:
                    call = NodeBuilder.newCallExpression(
                        name, name, DUMMY_CODE, False)
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
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.JoinedStr):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.Constant):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.Attribute):
        value = self.handle_expression(expr.value)
        self.log_with_loc("Parsed base as: %s" % (value))
        mem = NodeBuilder.newMemberExpression(
            value, UnknownType.getUnknownType(), expr.attr, ".", DUMMY_CODE)
        return mem
    elif isinstance(expr, ast.Subscript):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.Starred):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.Name):
        ref = NodeBuilder.newDeclaredReferenceExpression(
            expr.id, UnknownType.getUnknownType(), DUMMY_CODE)
        resolved = self.scopemanager.resolve(ref)
        if resolved is None:
            v = NodeBuilder.newVariableDeclaration(
                expr.id, UnknownType.getUnknownType(), DUMMY_CODE, False)
            self.scopemanager.addDeclaration(v)
            return v
        else:
            return ref
    elif isinstance(expr, ast.List):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.Tuple):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.Slice):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    else:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
