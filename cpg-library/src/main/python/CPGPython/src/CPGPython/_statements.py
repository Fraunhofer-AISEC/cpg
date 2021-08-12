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
from ._misc import NOT_IMPLEMENTED_MSG

import ast

DUMMY_CODE = ""  # Currently, I cannot access the source code...


def handle_statement(self, stmt):
    self.log_with_loc("Handling statement: %s" % (ast.dump(stmt)))

    if isinstance(stmt, ast.FunctionDef):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.AsyncFunctionDef):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.ClassDef):
        # TODO: NodeBuilder requires a "kind" parameters. Setting this to
        # "class" would automagically create a "this" receiver field.
        # However, the receiver can have any name in python (and even different
        # names per method).
        cls = NodeBuilder.newRecordDeclaration(stmt.name, "", DUMMY_CODE)
        bases = []
        for base in stmt.bases:
            if not isinstance(base, ast.Name):
                self.log_with_loc(
                    "Expected a name, but got: %s" %
                    (type(base)), loglevel="ERROR")
            else:
                tname = "%s" % (base.id)
                t = TypeParser.createFrom(tname, True)
                bases.append(t)
        cls.setSuperClasses(bases)
        for keyword in stmt.keywords:
            self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        for s in stmt. body:
            if isinstance(s, ast.FunctionDef):
                cls.addMethod(self.handle_function_or_method(s, cls))
            elif isinstance(s, ast.stmt):
                # TODO
                self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
            else:
                self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        for decorator in stmt.decorator_list:
            self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return cls
    elif isinstance(stmt, ast.Return):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.Delete):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.Assign):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.AugAssign):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.AnnAssign):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.For):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.AsyncFor):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.While):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.If):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.With):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.AsyncWith):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.Raise):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.Assert):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.Import):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.ImportFrom):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.Global):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.Nonlocal):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.Expr):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.Pass):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.Break):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.Continue):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    else:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")


def handle_function_or_method(self, node, record=None):
    self.log_with_loc("Handling a function/method: %s" % (ast.dump(node)))

    if isinstance(node.name, ast.Name):
        name = node.name
    else:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        name = ""

    if record is not None:
        f = NodeBuilder.newMethodDeclaration(name, DUMMY_CODE, False, record)
    else:
        f = NodeBuilder.newFunctionDeclaration(name, DUMMY_CODE)
    return f
