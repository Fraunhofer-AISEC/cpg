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
from de.fraunhofer.aisec.cpg.graph import NodeBuilder
from de.fraunhofer.aisec.cpg.graph.statements import CompoundStatement
from de.fraunhofer.aisec.cpg.graph.types import TypeParser
from de.fraunhofer.aisec.cpg.graph.types import UnknownType
import ast


def handle_statement(self, stmt):
    self.log_with_loc("Handling statement: %s" % (ast.dump(stmt)))

    if isinstance(stmt, ast.FunctionDef):
        return self.handle_function_or_method(stmt)
    elif isinstance(stmt, ast.AsyncFunctionDef):
        return self.handle_function_or_method(stmt)
    elif isinstance(stmt, ast.ClassDef):
        # TODO: NodeBuilder requires a "kind" parameters. Setting this to
        # "class" would automagically create a "this" receiver field.
        # However, the receiver can have any name in python (and even different
        # names per method).
        cls = NodeBuilder.newRecordDeclaration(stmt.name, "",
                                               self.get_src_code(stmt))
        self.add_loc_info(stmt, cls)
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
        for s in stmt.body:
            if isinstance(s, ast.FunctionDef):
                cls.addMethod(self.handle_function_or_method(s, cls))
            elif isinstance(s, ast.stmt):
                handled_stmt = self.handle_statement(s)
                if self.is_declaration(handled_stmt):
                    # TODO wrap this in a function...
                    decl_stmt = NodeBuilder.newDeclarationStatement(
                        self.get_src_code(s))
                    self.add_loc_info(s, decl_stmt)
                    decl_stmt.setSingleDeclaration(handled_stmt)
                    cls.addStatement(decl_stmt)
                elif self.is_statement(handled_stmt):
                    cls.addStatement(handled_stmt)
                else:
                    self.log_with_loc(
                        "Expected a statement or a declaration. Received %s" %
                        (type(handled_stmt)), loglevel="ERROR")
            else:
                self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        for decorator in stmt.decorator_list:
            self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        self.scopemanager.leaveScope(cls)
        self.scopemanager.addDeclaration(cls)
        return cls
    elif isinstance(stmt, ast.Return):
        r = NodeBuilder.newReturnStatement(self.get_src_code(stmt))
        self.add_loc_info(stmt, r)
        if stmt.value is not None:
            r.setReturnValue(self.handle_expression(stmt.value)
                             )
        return r
    elif isinstance(stmt, ast.Delete):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newStatement("")
        self.add_loc_info(stmt, r)
        return r
    elif isinstance(stmt, ast.Assign):
        if len(stmt.targets) != 1:
            self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
            r = NodeBuilder.newBinaryOperator("=", self.get_src_code(stmt))
            self.add_loc_info(stmt, r)
            return r
        target = stmt.targets[0]

        # parse LHS and RHS as expressions
        lhs = self.handle_expression(target)
        rhs = self.handle_expression(stmt.value)

        if not self.is_declared_reference(
                lhs) and not self.is_member_expression(lhs):
            self.log_with_loc(
                "Expected a DeclaredReferenceExpression or MemberExpression "
                "but got \"%s\". Skipping." %
                (lhs.java_name), loglevel="ERROR")
            r = NodeBuilder.newBinaryOperator("=", self.get_src_code(stmt))
            self.add_loc_info(stmt, r)
            return r

        resolved_lhs = self.scopemanager.resolveReference(lhs)
        inRecord = self.scopemanager.isInRecord()
        inFunction = self.scopemanager.isInFunction()

        if resolved_lhs is not None:
            # found var => BinaryOperator "="
            binop = NodeBuilder.newBinaryOperator("=", self.get_src_code(stmt))
            self.add_loc_info(stmt, binop)
            binop.setLhs(lhs)
            binop.setRhs(rhs)
            return binop
        else:
            if inRecord and not inFunction:
                """
                class Foo:
                    class_var = 123
                """
                if self.is_declared_reference(lhs):
                    name = lhs.getName()
                else:
                    name = "DUMMY"
                    self.log_with_loc(
                        "Expected a DeclaredReferenceExpression but got a "
                        "MemberExpression. Using a dummy.",
                        loglevel="ERROR")

                self.log_with_loc(
                    "Could not resolve -> creating a new field for: %s" %
                    (name))
                v = NodeBuilder.newFieldDeclaration(
                    name,
                    rhs.getType(),
                    None,
                    self.get_src_code(stmt),
                    None,
                    rhs,
                    False)  # TODO None -> infos eintragen
                self.scopemanager.addDeclaration(v)
                return v
            elif inRecord and inFunction:
                """
                class Foo:
                    def bar(self):
                        baz = 123
                        self.new_field = 456
                """
                if self.is_declared_reference(lhs):
                    self.log_with_loc(
                        "Could not resolve -> creating a new variable for: %s"
                        % (lhs.getName()))
                    v = NodeBuilder.newVariableDeclaration(
                        lhs.getName(), rhs.getType(),
                        self.get_src_code(stmt), False)
                    v.setInitializer(rhs)
                    self.scopemanager.addDeclaration(v)
                    return v
                else:  # MemberExpression
                    self.log_with_loc(
                        "Probably a new field for: %s" %
                        (lhs.getName()))
                    current_function = self.scopemanager.getCurrentFunction()
                    recv_name = None
                    mem_base_is_receiver = False
                    if current_function is not None:
                        recv = current_function.getReceiver()
                        if recv is not None:
                            recv_name = recv.getName()
                    base = lhs.getBase()
                    if self.is_declared_reference(base):
                        mem_base_is_receiver = base.getName() == recv_name
                    if not mem_base_is_receiver:
                        self.log_with_loc("I'm confused.", loglevel="ERROR")
                        return NodeBuilder.newStatement("DUMMY")
                    v = NodeBuilder.newFieldDeclaration(
                        lhs.getName(), rhs.getType(), None,
                        self.get_src_code(stmt), None, rhs, False)
                    self.scopemanager.addDeclaration(v)
                    self.scopemanager.getCurrentRecord().addField(v)
                    return v
            elif not inRecord:
                """
                either in a function or at file top-level
                """
                self.log_with_loc(
                    "Could not resolve -> creating a new variable for: %s" %
                    (lhs.getName()))
                v = NodeBuilder.newVariableDeclaration(
                    lhs.getName(), rhs.getType(),
                    self.get_src_code(stmt), False)
                v.setInitializer(rhs)
                self.scopemanager.addDeclaration(v)
                return v

    elif isinstance(stmt, ast.AugAssign):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newStatement("")
        self.add_loc_info(stmt, r)
        return r
    elif isinstance(stmt, ast.AnnAssign):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newStatement("")
        self.add_loc_info(stmt, r)
        return r
    elif isinstance(stmt, ast.For):
        return self.handle_for(stmt)
    elif isinstance(stmt, ast.AsyncFor):
        return self.handle_for(stmt)
    elif isinstance(stmt, ast.While):
        # While(expr test, stmt* body, stmt* orelse)
        whl_stmt = NodeBuilder.newWhileStatement(self.get_src_code(stmt))
        self.add_loc_info(stmt, whl_stmt)
        expr = self.handle_expression(stmt.test)
        if self.is_declaration(expr):
            whl_stmt.setConditionDeclaration(expr)
        else:
            whl_stmt.setCondition(expr)
        body = self.make_compound_statement(stmt.body)
        whl_stmt.setStatement(body)
        if stmt.orelse is not None and len(stmt.orelse) != 0:
            self.log_with_loc(
                "\"orelse\" is currently not suppoorted for "
                "\"while\" statments -> skipping",
                loglevel="ERROR")
        return whl_stmt
    elif isinstance(stmt, ast.If):
        if_stmt = NodeBuilder.newIfStatement(self.get_src_code(stmt))
        self.add_loc_info(stmt, if_stmt)
        # Condition
        if_stmt.setCondition(self.handle_expression(stmt.test))
        # Then
        body = self.make_compound_statement(stmt.body)
        if_stmt.setThenStatement(body)
        # Else
        if stmt.orelse is not None and len(stmt.orelse) != 0:
            orelse = self.make_compound_statement(stmt.orelse)
            if_stmt.setElseStatement(orelse)
        return if_stmt

    elif isinstance(stmt, ast.With):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newStatement("")
        self.add_loc_info(stmt, r)
        return r
    elif isinstance(stmt, ast.AsyncWith):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newStatement("")
        self.add_loc_info(stmt, r)
        return r
    elif isinstance(stmt, ast.Raise):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newStatement("")
        self.add_loc_info(stmt, r)
        return r
    elif isinstance(stmt, ast.Assert):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newStatement("")
        self.add_loc_info(stmt, r)
        return r
    elif isinstance(stmt, ast.Import):
        """
        ast.Import = class Import(stmt)
         |  Import(alias* names)

         Example: import Foo, Bar as Baz, Blub
        """

        decl_stmt = NodeBuilder.newDeclarationStatement(
            self.get_src_code(stmt))
        self.add_loc_info(stmt, decl_stmt)
        for s in stmt.names:
            if s.asname is not None:
                name = s.asname
                src = name + " as " + s.asname
            else:
                name = s.name
                src = name
            tpe = UnknownType.getUnknownType()
            v = NodeBuilder.newVariableDeclaration(
                name, tpe, src, False)
            # inacurate but ast.alias does not hold location information
            self.add_loc_info(stmt, v)
            self.scopemanager.addDeclaration(v)
            decl_stmt.addDeclaration(v)
        return decl_stmt
    elif isinstance(stmt, ast.ImportFrom):
        """
        ast.ImportFrom = class ImportFrom(stmt)
         |  ImportFrom(identifier? module, alias* names, int? level)

         Example: from foo import bar, baz as blub
        """

        # general warning
        self.log_with_loc(
            "Cannot correctly handle \"import from\". Using an approximation.",
            loglevel="ERROR")

        decl_stmt = NodeBuilder.newDeclarationStatement(
            self.get_src_code(stmt))
        self.add_loc_info(stmt, decl_stmt)
        for s in stmt.names:
            if s.asname is not None:
                name = s.asname
                src = name + " as " + s.asname
            else:
                name = s.name
                src = name
            tpe = UnknownType.getUnknownType()
            v = NodeBuilder.newVariableDeclaration(
                name, tpe, src, False)
            # inacurate but ast.alias does not hold location information
            self.add_loc_info(stmt, v)
            self.scopemanager.addDeclaration(v)
            decl_stmt.addDeclaration(v)
        return decl_stmt
    elif isinstance(stmt, ast.Global):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newStatement("")
        self.add_loc_info(stmt, r)
        return r
    elif isinstance(stmt, ast.Nonlocal):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newStatement("")
        self.add_loc_info(stmt, r)
        return r
    elif isinstance(stmt, ast.Expr):
        return self.handle_expression(stmt.value)
    elif isinstance(stmt, ast.Pass):
        p = NodeBuilder.newEmptyStatement("pass")
        self.add_loc_info(stmt, p)
        return p
    elif isinstance(stmt, ast.Break):
        brk = NodeBuilder.newBreakStatement(self.get_src_code(stmt))
        self.add_loc_info(stmt, brk)
        return brk
    elif isinstance(stmt, ast.Continue):
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        r = NodeBuilder.newStatement("")
        self.add_loc_info(stmt, r)
        return r
    elif isinstance(stmt, ast.Try):
        s = NodeBuilder.newTryStatement(self.get_src_code(stmt))
        self.add_loc_info(stmt, s)
        try_block = self.make_compound_statement(stmt.body)
        finally_block = self.make_compound_statement(stmt.finalbody)
        if stmt.orelse is not None and len(stmt.orelse) != 0:
            self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        if len(stmt.handlers) != 0:
            self.log_with_loc(
                "Try handlers. " +
                NOT_IMPLEMENTED_MSG,
                loglevel="ERROR")
        s.setTryBlock(try_block)
        s.setFinallyBlock(finally_block)
        return s
    else:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        self.log_with_loc(
            "Received unepxected stmt: %s with type %s" %
            (stmt, type(stmt)))
        r = NodeBuilder.newStatement("")
        self.add_loc_info(stmt, r)
        return r


def handle_function_or_method(self, node, record=None):
    if not isinstance(
            node,
            ast.FunctionDef) and not isinstance(
            node,
            ast.AsyncFunctionDef):
        self.log_with_loc(
            "Expected either ast.FunctionDef or ast.AsyncFunctionDef",
            loglevel="ERROR")
        r = NodeBuilder.newFunctionDeclaration("DUMMY", "DUMMY")
        self.add_loc_info(node, r)
        return r

    if isinstance(node, ast.AsyncFunctionDef):
        self.log_with_loc(
            "\"async\" is currently not supported and the information is lost "
            "in the graph.", loglevel="ERROR")

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
            f = NodeBuilder.newConstructorDeclaration(
                name, self.get_src_code(node), record)
        else:
            # TODO handle static
            f = NodeBuilder.newMethodDeclaration(
                name, self.get_src_code(node), False, record)
    else:
        f = NodeBuilder.newFunctionDeclaration(name, self.get_src_code(node))
    self.add_loc_info(node, f)

    self.scopemanager.enterScope(f)

    for arg in node.args.posonlyargs:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

    # First argument is the reciver in case of a method
    if record is not None:
        if len(node.args.args) > 0:
            recv_node = node.args.args[0]
            tpe = TypeParser.createFrom(record.getName(), False)
            recv = NodeBuilder.newVariableDeclaration(
                recv_node.arg, tpe, self.get_src_code(recv_node), False)
            self.add_loc_info(recv_node, recv)
            f.setReceiver(recv)
            self.scopemanager.addDeclaration(recv)
        else:
            self.log_with_loc(
                "Expected to find the receiver but got nothing...",
                loglevel="ERROR")
        for arg in node.args.args[1:]:
            self.handle_argument(arg)
    else:
        for arg in node.args.args:
            self.handle_argument(arg)

    if node.args.vararg is not None:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    for arg in node.args.kwonlyargs:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    for arg in node.args.kw_defaults:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    if node.args.kwarg is not None:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    for arg in node.args.defaults:
        self.log_with_loc(
            "Default args. " +
            NOT_IMPLEMENTED_MSG,
            loglevel="ERROR")

    if len(node.body) > 0:
        f.setBody(self.make_compound_statement(node.body))

    annotations = []
    for decorator in node.decorator_list:
        # cannot do this because kw arguments are not properly handled yet in
        # functions
        # expr = self.visit(decorator)

        members = []

        if isinstance(decorator.func, ast.Attribute):
            ref = self.handle_expression(decorator.func)
            annotation = NodeBuilder.newAnnotation(
                ref.getName(), self.get_src_code(decorator.func))
            self.add_loc_info(decorator.func, annotation)

            # add the base as a receiver annotation
            member = NodeBuilder.newAnnotationMember(
                "receiver", ref.getBase(), self.get_src_code(decorator.func))
            self.add_loc_info(decorator.func, member)

            members.append(member)
        elif isinstance(decorator.func, ast.Name):
            ref = self.handle_expression(decorator.func)
            annotation = NodeBuilder.newAnnotation(
                ref.getName(), self.get_src_code(decorator.func))
            self.add_loc_info(decorator.func, member)

        else:
            self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

        # add first arg as value
        if len(decorator.args) > 0:
            arg0 = decorator.args[0]
            value = self.handle_expression(arg0)

            member = NodeBuilder.newAnnotationMember(
                "value", value, self.get_src_code(arg0))
            self.add_loc_info(arg0, member)

            members.append(member)

        # loop through keywords args
        for kw in decorator.keywords:
            member = NodeBuilder.newAnnotationMember(
                kw.arg, self.handle_expression(
                    kw.value), self.get_src_code(kw))
            self.add_loc_info(kw, member)

            members.append(member)

        annotation.setMembers(members)
        annotations.append(annotation)

    f.addAnnotations(annotations)

    if node.returns is not None:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

    self.scopemanager.leaveScope(f)
    self.scopemanager.addDeclaration(f)

    return f


def handle_argument(self, arg: ast.arg):
    self.log_with_loc("Handling an argument: %s" % (ast.dump(arg)))
    if arg.annotation is not None:
        tpe = TypeParser.createFrom(arg.annotation.id, False)
    else:
        tpe = UnknownType.getUnknownType()
    # TODO variadic
    pvd = NodeBuilder.newMethodParameterIn(arg.arg,
                                           tpe, False, self.get_src_code(arg))
    self.add_loc_info(arg, pvd)
    self.scopemanager.addDeclaration(pvd)
    return pvd


def handle_for(self, stmt):
    if not isinstance(stmt, ast.AsyncFor) and not isinstance(stmt, ast.For):
        self.log_with_loc(("Expected ast.AsyncFor or ast.For. Skipping"
                          " evaluation."), loglevel="ERROR")
        r = NodeBuilder.newStatement("")
        self.add_loc_info(stmt, r)
        return r
    if isinstance(stmt, ast.AsyncFor):
        self.log_with_loc((
            "\"async\" is currently not supported. The statement"
            " is parsed but the \"async\" information is not available in the"
            " graph."), loglevel="ERROR")

    # We can handle the AsyncFor / For statement now:
    for_stmt = NodeBuilder.newForEachStatement(self.get_src_code(stmt))
    self.add_loc_info(stmt, for_stmt)
    target = self.handle_expression(stmt.target)
    if self.is_variable_declaration(target):
        decl_stmt = NodeBuilder.newDeclarationStatement(
            self.get_src_code(stmt.target))
        self.add_loc_info(stmt.target, decl_stmt)
        decl_stmt.setSingleDeclaration(target)
        target = decl_stmt
    for_stmt.setVariable(target)
    it = self.handle_expression(stmt.iter)
    for_stmt.setIterable(it)
    body = self.make_compound_statement(stmt.body)
    for_stmt.setStatement(body)

    if stmt.orelse is not None and len(stmt.orelse) != 0:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

    return for_stmt


def make_compound_statement(self, stmts) -> CompoundStatement:
    self.log_with_loc("Making a CompoundStatement")

    if stmts is None or len(stmts) == 0:
        self.log_with_loc(
            "Expected at least one statement. Returning a dummy.",
            loglevel="WARN")
        return NodeBuilder.newCompoundStatement("")

    if False and len(stmts) == 1:
        """ TODO decide how to handle this... """
        s = self.handle_statement(stmts[0])
        if self.is_declaration(s):
            decl_stmt = NodeBuilder.newDeclarationStatement(
                self.get_src_code(stmts[0]))
            self.add_loc_info(stmts[0], decl_stmt)
            decl_stmt.setSingleDeclaration(s)
            return decl_stmt
        else:
            return s
    else:
        compound_statement = NodeBuilder.newCompoundStatement("")
        # TODO location
        for s in stmts:
            s = self.handle_statement(s)
            if self.is_declaration(s):
                decl_stmt = NodeBuilder.newDeclarationStatement(s.getCode())
                decl_stmt.setLocation(s.getLocation())
                decl_stmt.setSingleDeclaration(s)
                compound_statement.addStatement(decl_stmt)
            else:
                compound_statement.addStatement(s)

        return compound_statement
