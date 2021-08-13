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
from de.fraunhofer.aisec.cpg.graph.statements import CompoundStatement
from ._misc import NOT_IMPLEMENTED_MSG

import ast

DUMMY_CODE = ""  # TODO: Currently, I cannot access the source code...


def handle_statement(self, stmt):
    self.log_with_loc("Handling statement: %s" % (ast.dump(stmt)))

    if isinstance(stmt, ast.FunctionDef):
        return self.handle_function_or_method(stmt)
    elif isinstance(stmt, ast.AsyncFunctionDef):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.ClassDef):
        # TODO: NodeBuilder requires a "kind" parameters. Setting this to
        # "class" would automagically create a "this" receiver field.
        # However, the receiver can have any name in python (and even different
        # names per method).
        cls = NodeBuilder.newRecordDeclaration(stmt.name, "", DUMMY_CODE)
        self.scopemanager.enterScope(cls)
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
        self.scopemanager.leaveScope(cls)
        self.scopemanager.addDeclaration(cls)
        return cls
    elif isinstance(stmt, ast.Return):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.Delete):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return NodeBuilder.newStatement("")
    elif isinstance(stmt, ast.Assign):
        if len(stmt.targets) != 1:
            self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
            return NodeBuilder.newBinaryOperator("=", DUMMY_CODE)
        target = stmt.targets[0]

        # parse LHS and RHS as expressions
        lhs = self.handle_expression(target)
        rhs = self.handle_expression(stmt.value)

        if self.is_variable_declaration(lhs):
            # new var => set initializer
            lhs.setInitializer(rhs)
            # lhs.setType(rhs.getType())
            self.log_with_loc(
                "Parsed as new VariableDeclaration with initializer: %s" %
                (lhs))
            return lhs
        else:
            # found var => BinaryOperator "="
            binop = NodeBuilder.newBinaryOperator("=", DUMMY_CODE)
            binop.setLhs(lhs)
            binop.setRhs(rhs)
            return binop

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
        return self.handle_expression(stmt.value)
    elif isinstance(stmt, ast.Pass):
        p = NodeBuilder.newEmptyStatement("pass")
        return p
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
    # FunctionDef(identifier name, arguments args, stmt* body, expr*
    # decorator_list, expr? returns, string? type_comment)
    self.log_with_loc("Handling a function/method: %s" % (ast.dump(node)))

    if isinstance(node.name, str):
        name = node.name
    else:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        name = ""

    if record is not None:
        if name == "__init__":
            f = NodeBuilder.newConstructorDeclaration(name, DUMMY_CODE, record)
        else:
            # TODO handle static
            f = NodeBuilder.newMethodDeclaration(
                name, DUMMY_CODE, False, record)
    else:
        f = NodeBuilder.newFunctionDeclaration(name, DUMMY_CODE)

    self.scopemanager.enterScope(f)

    for arg in node.args.posonlyargs:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

    # First argument is the reciver in case of a method
    if record is not None:
        if len(node.args.args) > 0:
            tpe = TypeParser.createFrom(record.getName(), False)
            recv = NodeBuilder.newVariableDeclaration(
                node.args.args[0].arg, tpe, DUMMY_CODE, False)
            f.setReceiver(recv)
        else:
            self.log_with_loc(
                "Expected to find the receiver but got nothing...",
                loglevel="ERROR")
        for arg in node.args.args[1:]:
            self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    else:
        for arg in node.args.args:
            self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

    if node.args.vararg is not None:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    for arg in node.args.kwonlyargs:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    for arg in node.args.kw_defaults:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    if node.args.kwarg is not None:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    for arg in node.args.defaults:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

    if len(node.body) > 0:
        f.setBody(self.make_compound_statement(node.body))

    for decorator in node.decorator_list:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

    if node.returns is not None:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

    self.scopemanager.leaveScope(f)
    self.scopemanager.addDeclaration(f)

    return f


def make_compound_statement(self, stmts) -> CompoundStatement:
    self.log_with_loc("Making a CompoundStatement")

    if stmts is None or len(stmts) == 0:
        self.log_with_loc(
            "Expected at least one statement. Returning a dummy.",
            loglevel="WARN")
        return NodeBuilder.newCompoundStatement("")

    if len(stmts) == 1:
        s = self.handle_statement(stmts[0])
        if self.is_declaration(s):
            decl_stmt = NodeBuilder.newDeclarationStatement(DUMMY_CODE)
            decl_stmt.setSingleDeclaration(s)
            return decl_stmt
        else:
            return s
    else:
        compound_statement = NodeBuilder.newCompoundStatement(DUMMY_CODE)
        for s in stmts:
            s = self.handle_statement(s)
            if self.is_declaration(s):
                decl_stmt = NodeBuilder.newDeclarationStatement(DUMMY_CODE)
                decl_stmt.setSingleDeclaration(s)
                compound_statement.addStatement(decl_stmt)
            else:
                compound_statement.addStatement(s)

        return compound_statement
