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
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
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
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.Subscript):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.Starred):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
    elif isinstance(expr, ast.Name):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newExpression("")
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
