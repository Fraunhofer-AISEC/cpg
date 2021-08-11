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
from de.fraunhofer.aisec.cpg.graph import Annotation
from de.fraunhofer.aisec.cpg.graph import AnnotationMember
import ast
from de.fraunhofer.aisec.cpg.graph.declarations import Declaration
from de.fraunhofer.aisec.cpg.graph.declarations import FunctionDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import IncludeDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import MethodDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import ConstructorDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import NamespaceDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import FieldDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import ParamVariableDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import RecordDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import TranslationUnitDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import VariableDeclaration
from de.fraunhofer.aisec.cpg.graph.statements import CompoundStatement, TryStatement
from de.fraunhofer.aisec.cpg.graph.statements import DeclarationStatement
from de.fraunhofer.aisec.cpg.graph.statements import EmptyStatement
from de.fraunhofer.aisec.cpg.graph.statements import ForEachStatement
from de.fraunhofer.aisec.cpg.graph.statements import IfStatement
from de.fraunhofer.aisec.cpg.graph.statements import ReturnStatement
from de.fraunhofer.aisec.cpg.graph.statements import WhileStatement
from de.fraunhofer.aisec.cpg.graph.statements.expressions import ArrayRangeExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import ArraySubscriptionExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import BinaryOperator
from de.fraunhofer.aisec.cpg.graph.statements.expressions import CallExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import ConditionalExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import CastExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import ConstructExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import DeclaredReferenceExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import Expression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import InitializerListExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import KeyValueExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import Literal
from de.fraunhofer.aisec.cpg.graph.statements.expressions import MemberCallExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import MemberExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import UnaryOperator
from de.fraunhofer.aisec.cpg.graph.types import TypeParser
from de.fraunhofer.aisec.cpg.sarif import PhysicalLocation
from de.fraunhofer.aisec.cpg.sarif import Region
from java.net import URI
from java.util import ArrayList
import inspect


def log_with_loc(self, string, level=1, loglevel="DEBUG"):
    callerframerecord = inspect.stack()[level]
    frame = callerframerecord[0]
    info = inspect.getframeinfo(frame)
    msg = "%s\t%d:\t%s" % (info.function, info.lineno, string)

    if loglevel == "DEBUG":
        self.logger.debug(msg)
    elif loglevel == "INFO":
        self.logger.info(msg)
    elif loglevel == "WARN":
        self.logger.warn(msg)
    elif loglevel == "ERROR":
        self.logger.error(msg)
    else:
        # catch all
        self.logger.error(msg)


def add_loc_info(self, node, obj):
    # Adds location information of node to obj
    if not isinstance(node, ast.AST):
        self.log_with_loc(type(node))
        self.log_with_loc(node[0].lineno)
        self.log_with_loc("<--- CALLER", level=2)
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return
    obj.setFile(self.fname)
    uri = URI("file://" + self.fname)
    obj.setLocation(PhysicalLocation(uri,
                                     Region(node.lineno,
                                            node.col_offset,
                                            node.end_lineno,
                                            node.end_col_offset)
                                     )
                    )
    obj.setCode(self.sourcecode.get_snippet(node.lineno,
                                            node.col_offset,
                                            node.end_lineno,
                                            node.end_col_offset)
                )
    # obj.setCode(ast.unparse(node)) # alternative to CodeExtractor class


def is_variable_declaration(self, target):
    return target.java_name == "de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration"


def is_declared_reference(self, target):
    return target.java_name == "de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression"


def is_field_declaration(self, target):
    return target.java_name == "de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration"


def is_member_expression(self, target):
    return target.java_name == "de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression"


def is_declaration(self, target):
    return target.java_name.startswith('de.fraunhofer.aisec.cpg.graph.declarations.')


def is_method_declaration(self, target):
    return target.java_name == "de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration"


def is_ctor_declaration(self, target):
    return target.java_name == "de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration"

### LITERALS ###


def visit_Constant(self, node):
    self.log_with_loc(ast.dump(node))
    lit = Literal()
    self.add_loc_info(node, lit)
    lit.setValue(node.value)
    lit.setName(str(node.value))
    if type(node.value) is str:
        lit.setType(TypeParser.createFrom("str", False))
    elif type(node.value) is int:
        lit.setType(TypeParser.createFrom("int", False))
    elif type(node.value) is float:
        lit.setType(TypeParser.createFrom("float", False))
    elif type(node.value) is complex:
        lit.setType(TypeParser.createFrom("complex", False))
    elif type(node.value) is bool:
        lit.setType(TypeParser.createFrom("bool", False))
    elif type(node.value) is type(None):
        lit.setType(TypeParser.createFrom("None", False))
    elif type(node.value) is bytes:
        lit.setType(TypeParser.createFrom("byte[]", False))
    else:
        self.log_with_loc(type(node.value))
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return lit


def visit_FormattedValue(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_JoinedStr(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_List(self, node):
    self.log_with_loc(ast.dump(node))

    ile = InitializerListExpression()
    self.add_loc_info(node, ile)

    list = ArrayList()

    for el in node.elts:
        expr = self.visit(el)
        list.add(expr)

    ile.setInitializers(list)

    return ile


def visit_Tuple(self, node):
    # self.log_with_loc(ast.dump(node))
    # lit = Literal()
    # self.add_loc_info(node, lit)
    # lit.setType(TypeParser.createFrom("Tuple", False))
    # elts = []
    # for e in node.elts:
    #    elts.append(self.visit(e))
    # lit.setValue(elts)
    # TODO
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return Expression()


def visit_Set(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Dict(self, node: ast.Dict):
    self.log_with_loc(ast.dump(node))

    ile = InitializerListExpression()
    self.add_loc_info(node, ile)

    list = ArrayList()

    # loop through keys and get values
    for i in range(0, len(node.keys)):
        key = node.keys[i]
        value = node.values[i]

        # construct a key value expression
        key_value = KeyValueExpression()

        key_expr = self.visit(key)
        value_expr = self.visit(value)

        # _should_ always be a literal
        key_value.setKey(key_expr)
        key_value.setValue(value_expr)

        list.add(key_value)

    ile.setInitializers(list)

    return ile

### VARIABLES ###


def visit_Name(self, node):
    self.log_with_loc(ast.dump(node))
    ref = DeclaredReferenceExpression()
    self.add_loc_info(node, ref)
    ref.setName(node.id)

    resolved_ref = self.scopemanager.resolve(ref)
    self.log_with_loc("Resolving node yields: %s" % (resolved_ref))

    if resolved_ref is None:
        self.log_with_loc("Creating a new VariableDeclaration")
        v = VariableDeclaration()
        self.add_loc_info(node, v)
        v.setName(node.id)
        self.scopemanager.addDeclaration(v)
        self.log_with_loc("Returning a declaration: %s" % (v))
        return v
    else:
        self.log_with_loc("Returning a DeclaredReferenceExpression: %s" %
                          (ref))
        # ref.setRefersTo(resolved_ref)
        return ref


def visit_Load(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Store(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Del(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Starred(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return

### EXORESSIONS ###


def visit_Expr(self, node):
    self.log_with_loc(ast.dump(node))
    return self.visit(node.value)


def visit_UnaryOp(self, node):
    self.log_with_loc(ast.dump(node))
    unop = UnaryOperator()
    self.add_loc_info(node, unop)
    unop.setOperatorCode(node.op)
    unop.setInput(self.visit(node.operand))
    if isinstance(node.op, ast.UAdd):
        unop.setOperatorCode("+")
        unop.setName("+")
    elif isinstance(node.op, ast.USub):
        unop.setOperatorCode("-")
        unop.setName("-")
    elif isinstance(node.op, ast.Not):
        unop.setOperatorCode("!")
        unop.setName("!")
    elif isinstance(node.op, ast.Invert):
        unop.setOperatorCode("~")
        unop.setName("~")
    else:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return unop


def visit_UAdd(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_USub(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Not(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Invert(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_BinOp(self, node):
    self.log_with_loc(ast.dump(node))
    binop = BinaryOperator()
    self.add_loc_info(node, binop)
    if isinstance(node.op, ast.Add):
        binop.setOperatorCode("+")
        binop.setName("+")
    elif isinstance(node.op, ast.Sub):
        binop.setOperatorCode("-")
        binop.setName("-")
    elif isinstance(node.op, ast.Mult):
        binop.setOperatorCode("*")
        binop.setName("*")
    elif isinstance(node.op, ast.Div):
        binop.setOperatorCode("/")
        binop.setName("/")
    elif isinstance(node.op, ast.FloorDiv):
        binop.setOperatorCode("//")
        binop.setName("//")
    elif isinstance(node.op, ast.Mod):
        binop.setOperatorCode("%")
        binop.setName("%")
    elif isinstance(node.op, ast.Pow):
        binop.setOperatorCode("**")
        binop.setName("**")
    elif isinstance(node.op, ast.LShift):
        binop.setOperatorCode("<<")
        binop.setName("<<")
    elif isinstance(node.op, ast.RShift):
        binop.setOperatorCode(">>")
        binop.setName(">>")
    elif isinstance(node.op, ast.BitOr):
        binop.setOperatorCode("|")
        binop.setName("|")
    elif isinstance(node.op, ast.BitXor):
        binop.setOperatorCode("^")
        binop.setName("^")
    elif isinstance(node.op, ast.BitAnd):
        binop.setOperatorCode("&")
        binop.setName("&")
    elif isinstance(node.op, ast.MatMult):
        binop.setOperatorCode("*")
        binop.setName("*")
    else:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    binop.setLhs(self.visit(node.left))
    binop.setRhs(self.visit(node.right))
    return binop


def visit_Add(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Sub(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Mult(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Div(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_FloorDiv(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Mod(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Pow(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_LShift(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_RShift(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_BitOr(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_BitXor(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_BitAnd(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_MatMult(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_BoolOp(self, node: ast.BoolOp):
    self.log_with_loc(ast.dump(node))
    binOp = BinaryOperator()

    if isinstance(node.op, ast.And):
        binOp.setOperatorCode("&&")
    elif isinstance(node.op, ast.Or):
        binOp.setOperatorCode("||")
    else:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return

    # TODO: split into multiple binary operators, python supports many values

    if len(node.values) == 2:
        lhs = self.visit(node.values[0])
        rhs = self.visit(node.values[1])
    else:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

    return binOp


def visit_And(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Or(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Compare(self, node):
    self.log_with_loc(ast.dump(node))
    comp = BinaryOperator()
    self.add_loc_info(node, comp)
    if len(node.ops) != 1:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    op = node.ops[0]
    if isinstance(op, ast.Eq):
        comp.setOperatorCode("==")
        comp.setName("==")
    elif isinstance(op, ast.NotEq):
        comp.setOperatorCode("!=")
        comp.setName("!=")
    elif isinstance(op, ast.Lt):
        comp.setOperatorCode("<")
        comp.setName("<")
    elif isinstance(op, ast.LtE):
        comp.setOperatorCode("<=")
        comp.setName("<=")
    elif isinstance(op, ast.Gt):
        comp.setOperatorCode(">")
        comp.setName(">")
    elif isinstance(op, ast.GtE):
        comp.setOperatorCode(">=")
        comp.setName(">=")
    elif isinstance(op, ast.Is):
        comp.setOperatorCode("is")
        comp.setName("is")
    elif isinstance(op, ast.IsNot):
        comp.setOperatorCode("is not")
        comp.setName("is not")
    elif isinstance(op, ast.In):
        comp.setOperatorCode("in")
        comp.setName("in")
    elif isinstance(op, ast.NotIn):
        comp.setOperatorCode("not in")
        comp.setName("not in")
    else:
        self.log_with_loc(op)
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    comp.setLhs(self.visit(node.left))
    if len(node.comparators) != 1:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    comp.setRhs(self.visit(node.comparators[0]))
    return comp


def visit_Eq(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_NotEq(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Lt(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_LtE(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Gt(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_GtE(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Is(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_IsNot(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_In(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_NotIn(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Call(self, node: ast.Call):
    # TODO keywords starargs
    self.log_with_loc(ast.dump(node))

    # a call can be one of
    # - simple function call
    # - member call
    # - constructor call
    #
    # We parse node.func regularly using a visitor and decide what it is
    ref = self.visit(node.func)
    self.log_with_loc("Evaluated ref as: %s" % (ref))

    name = ref.getName()

    if self.is_member_expression(ref):
        self.log_with_loc("Visiting a MemberExpression")
        base_name = ref.getBase().getName()

        fqn = "%s.%s" % (base_name, name)

        call = MemberCallExpression()
        call.setName(name)
        call.setFqn(fqn)
        call.setOperatorCode(".")

        member = DeclaredReferenceExpression()
        member.setName(name)

        call.setBase(ref.getBase())
        call.setMember(member)
    else:
        # this can be a simple function call or a ctor
        record = self.scopemanager.getRecordForName(self.scopemanager.getCurrentScope(),
                                                    name)

        if record is not None:
            call = ConstructExpression()
            call.setName(node.func.id)
            call.setType(TypeParser.createFrom(record.getName(), False))
        else:
            if name == "str" and len(node.args) == 1:
                cast = CastExpression()
                self.add_loc_info(node, cast)
                cast.setCastType(TypeParser.createFrom("str", False))
                cast.setExpression(self.visit(node.args[0]))

                return cast

            else:
                call = CallExpression()
                call.setName(name)
                call.setFqn(name)

    self.add_loc_info(node, call)

    # add arguments
    for a in node.args:
        call.addArgument(self.visit(a))

    for keyword in node.keywords:
        if keyword.arg is not None:
            call.addArgument(self.visit(keyword.value), keyword.arg)
        else:
            # TODO: keywords without args, aka **arg
            self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

    self.log_with_loc("Returning call as: %s" % (call))
    return call


def visit_keyword(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_IfExp(self, node):
    self.log_with_loc(ast.dump(node))

    expr = ConditionalExpression()
    self.add_loc_info(node, expr)
    expr.setCondition(self.visit(node.test))
    expr.setThenExpr(self.visit(node.body))
    expr.setElseExpr(self.visit(node.orelse))

    return expr


def visit_Attribute(self, node):
    self.log_with_loc(ast.dump(node))
    return self.handle_attribute_simple_case(node)

    if self.scopemanager.isInRecord() and self.scopemanager.isInFunction():
        # We first distinguish between class level and function level code.
        self.log_with_loc("Visiting an attribute inside a class function.")

        methodreceiver = self.scopemanager.getCurrentFunction().getReceiver()
        base = self.visit(node.value)
        if self.is_declared_reference(base) and base.getName() == methodreceiver.getName():
            # this might be a new class field
            d = DeclaredReferenceExpression()
            d.setName(node.attr)
            self.add_loc_info(node, d)
            return d
    else:
        return self.handle_attribute_simple_case(node)


def handle_attribute_simple_case(self, node):
    self.log_with_loc(ast.dump(node))
    # Simply create a MemberExpression with resolved base and node.attr. No
    # FiledDeclaration etc...

    base = self.visit(node.value)
    mem = MemberExpression()
    self.add_loc_info(node, mem)

    mem.setName(node.attr)
    mem.setBase(base)
    mem.setOperatorCode(node.attr)

    self.log_with_loc("Returning a MemberExpression: %s" % (mem))
    return mem


def visit_NamedExpr(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return

### SUBSCRIPTING ###


def visit_Subscript(self, node):
    self.log_with_loc(ast.dump(node))
    s = ArraySubscriptionExpression()
    self.add_loc_info(node, s)
    s.setArrayExpression(self.visit(node.value))
    s.setSubscriptExpression(self.visit(node.slice))
    return s


def visit_Slice(self, node):
    self.log_with_loc(ast.dump(node))
    slc = ArrayRangeExpression()
    self.add_loc_info(node, slc)
    if node.lower != None:
        slc.setFloor(self.visit(node.lower))
    if node.upper != None:
        slc.setCeiling(self.visit(node.upper))
    if node.step != None:
        self.log_with_loc("Step not yet supported.", loglevel="ERROR")
    return slc

### COMPREHENSIONS ###


def visit_ListComp(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_SetComp(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_GeneratorExp(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_DictComp(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_comprehension(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return

### STATEMENTS ###


def visit_Assign(self, node: ast.Assign):
    self.log_with_loc(ast.dump(node))

    if len(node.targets) != 1:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        return

    target = node.targets[0]

    # parse LHS and RHS as expressions
    lhs = self.visit(target)
    self.log_with_loc("Parsed lhs as: %s" % (lhs))
    rhs = self.visit(node.value)
    self.log_with_loc("Parsed rhs as: %s" % (rhs))

    if self.is_variable_declaration(lhs):
        lhs.setInitializer(rhs)
        return lhs
    elif self.is_field_declaration(lhs):
        lhs.setInitializer(rhs)
        return lhs
    elif self.is_declared_reference(lhs) or self.is_member_expression(lhs):
        binop = BinaryOperator()
        self.add_loc_info(node, binop)
        binop.setOperatorCode("=")
        binop.setLhs(lhs)
        binop.setRhs(rhs)
        binop.setName("=")
        return binop
    else:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")


def visit_AnnAssign(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_AugAssign(self, node):
    self.log_with_loc(ast.dump(node))
    binop = BinaryOperator()
    self.add_loc_info(node, binop)
    binop.setOperatorCode(node.op)
    binop.setLhs(self.visit(node.target))
    binop.setRhs(self.visit(node.value))
    binop.setName(node.op)
    return binop


def visit_Raise(self, node):
    self.log_with_loc(ast.dump(node))
    op = UnaryOperator()
    self.add_loc_info(node, op)
    op.setOperatorCode("raise")
    if node.exc != None:
        op.setInput(self.visit(node.exc))
    return op


def visit_Assert(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Delete(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Pass(self, node):
    self.log_with_loc(ast.dump(node))
    stmt = EmptyStatement()
    self.add_loc_info(node, stmt)
    stmt.setName("pass")
    return stmt

### IMPORTS ###


def visit_Import(self, node):
    self.log_with_loc(ast.dump(node))
    imp = IncludeDeclaration()
    self.add_loc_info(node, imp)
    if len(node.names) != 1:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    imp.setFilename(self.fname + node.names[0].name)

    # make scopmanager aware of import
    vd = VariableDeclaration()
    self.add_loc_info(node, vd)
    vd.setName(node.names[0].name)
    self.scopemanager.addGlobal(vd)
    return imp


def visit_ImportFrom(self, node):
    self.log_with_loc(ast.dump(node))
    imp = IncludeDeclaration()
    self.add_loc_info(node, imp)
    if node.level != 0:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    imp.setFilename(self.fname + node.module)

    # make scopmanager aware of import
    vd = VariableDeclaration()
    self.add_loc_info(node, vd)
    vd.setName(node.module)
    self.scopemanager.addGlobal(vd)
    return imp


def visit_alias(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return

### CONTROL FLOW ###


def visit_If(self, node):
    self.log_with_loc(ast.dump(node))
    stmt = IfStatement()
    self.add_loc_info(node, stmt)
    stmt.setName("If")
    # Condition
    stmt.setCondition(self.visit(node.test))
    # Then
    body = self.make_compound_statement(node, node.body)
    stmt.setThenStatement(body)
    # Else
    if node.orelse != None and len(node.orelse) != 0:
        orelse = self.make_compound_statement(node, node.orelse)
        stmt.setElseStatement(orelse)

    return stmt


def make_compound_statement(self, node, node_list) -> CompoundStatement:
    # TODO: generalize this, because this code repeats

    if node_list is None or len(node_list) == 0:
        self.log_with_loc("Expected at least one node", loglevel="ERROR")
        dummy = CompoundStatement()
        self.add_loc_info(node, dummy)
        return dummy

    # do not wrap a single statement as CompoundStatement
    if len(node_list) == 1:
        s = self.visit(node_list[0])
        # TODO move to a new function. it repeats
        if s is not None and self.is_declaration(s):
            d = DeclarationStatement()
            self.add_loc_info(node, d)
            d.setSingleDeclaration(s)
            return d
        else:
            return s

    body = CompoundStatement()
    self.add_loc_info(node, body)

    for b in node_list:
        s = self.visit(b)

        if s is not None and self.is_declaration(s):
            # wrap the statement
            d = DeclarationStatement()
            self.add_loc_info(node, d)
            d.setSingleDeclaration(s)
            body.addStatement(d)
        elif s is not None:
            body.addStatement(s)

    return body


def visit_For(self, node: ast.For):
    self.log_with_loc(ast.dump(node))

    stmt = ForEachStatement()
    self.add_loc_info(node, stmt)

    target = self.visit(node.target)
    if self.is_declared_reference(target):
        stmt.setTarget(target)
    elif self.is_variable_declaration(target):
        stmt.setVariable(target)
    else:
        self.log_with_loc("Parsed target as: %s" % (target))
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

    it = self.visit(node.iter)
    self.log_with_loc("Parsed iterabele as: %s" % (it))
    stmt.setIterable(it)
    body = self.make_compound_statement(node, node.body)
    stmt.setStatement(body)

    return stmt


def visit_While(self, node):
    self.log_with_loc(ast.dump(node))
    w = WhileStatement()
    self.add_loc_info(node, w)
    w.setCondition(self.visit(node.test))
    w.setStatement(self.make_compound_statement(node, node.body))
    if node.orelse != None and len(node.orelse) != 0:
        self.log_with_loc("while -> orelse not implemented, yet",
                          loglevel="ERROR")
    return w


def visit_Break(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Continue(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Try(self, node: ast.Try):
    self.log_with_loc(ast.dump(node))

    t = TryStatement()

    t.setTryBlock(self.make_compound_statement(node, node.body))

    # TODO: parse catch handlers

    t.setFinallyBlock(self.make_compound_statement(node, node.finalbody))

    return t


def visit_ExceptHandler(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_With(self, node):
    self.log_with_loc(ast.dump(node))
    # TODO LATER -> new cpg node or foreach + try/catch?
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_withitem(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return

### FUNCTION AND CLASS DEFINITIONS ###


def visit_FunctionDef(self, node: ast.FunctionDef, recordDec=None):
    self.log_with_loc(ast.dump(node))
    if recordDec is not None:
        if node.name == "__init__":
            fd = ConstructorDeclaration()
        else:
            fd = MethodDeclaration()
        fd.setRecordDeclaration(recordDec)
    else:
        fd = FunctionDeclaration()
    self.add_loc_info(node, fd)

    # handle name
    fd.setName(node.name)
    self.scopemanager.enterScope(fd)

    annotations = ArrayList()

    for decorator in node.decorator_list:
        # cannot do this because kw arguments are not properly handled yet in functions
        # expr = self.visit(decorator)

        members = ArrayList()
        annotation = Annotation()
        self.add_loc_info(decorator, annotation)

        if isinstance(decorator.func, ast.Attribute):
            ref = self.visit(decorator.func)

            annotation.setName(ref.getName())

            # add the base as a receiver annotation
            member = AnnotationMember()
            self.add_loc_info(decorator.func, member)
            member.setName("receiver")
            member.setValue(ref.getBase())

            members.add(member)
        elif isinstance(decorator, ast.Name):
            ref = self.visit(decorator.func)

            annotation.setName(ref.getName())

        # add first arg as value
        if len(decorator.args) > 0:
            arg0 = decorator.args[0]
            value = self.visit(arg0)

            member = AnnotationMember()
            self.add_loc_info(arg0, value)
            member.setName("value")
            member.setValue(value)

            members.add(member)

        # loop through keywords args
        for kw in decorator.keywords:
            member = AnnotationMember()
            member.setName(kw.arg)
            member.setValue(self.visit(kw.value))

            members.add(member)

        annotation.setMembers(members)
        annotations.add(annotation)

    fd.addAnnotations(annotations)

    # self.log_with_loc(node.de)

    # handle args
    # scopeManager adds them to fd
    self.visit_arguments(node.args, fd)

    # handle body
    fd.setBody(self.make_compound_statement(node, node.body))

    # handle decorator_list

    # handle return annotation

    # handle type_comment

    self.scopemanager.leaveScope(fd)
    self.scopemanager.addDeclaration(fd)

    return fd


def visit_Lambda(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_arguments(self, node, fd=None):
    self.log_with_loc(ast.dump(node))
    for p in node.posonlyargs:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

    if self.isMethodOrCtor(fd):
        # first argument is "self" (or any other name for "self")
        slf = VariableDeclaration()
        self.add_loc_info(node.args[0], slf)
        slf.setName(node.args[0].arg)
        slf.setType(TypeParser.createFrom(fd.getRecordDeclaration().getName(),
                                          False))
        fd.setReceiver(slf)
        self.scopemanager.addDeclaration(slf)
        for p in node.args[1:]:
            x = self.visit(p)
    else:
        for p in node.args:
            x = self.visit(p)
    # if node.vararg is not None:
    #    raise NotImplementedError
    for p in node.kwonlyargs:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    for p in node.kw_defaults:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    # if node.kwarg is not None:
    #    raise NotImplementedError
    for p in node.defaults:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
        pass
        # raise NotImplementedError
        # no cpg support???
    return


def visit_arg(self, node):
    self.log_with_loc(ast.dump(node))
    pvd = ParamVariableDeclaration()
    self.add_loc_info(node, pvd)
    pvd.setName(node.arg)
    if node.annotation != None:
        pvd.setType(TypeParser.createFrom(node.annotation.id, False))
    self.scopemanager.addDeclaration(pvd)
    return pvd


def visit_Return(self, node):
    self.log_with_loc(ast.dump(node))
    r = ReturnStatement()
    self.add_loc_info(node, r)
    if node.value != None:
        r.setReturnValue(self.visit(node.value))
    r.setName("return")
    return r


def visit_Yield(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_YieldFrom(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_Global(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return  # TODO: no support for global vars, yet. how to model them as CPG
    # nodes?
    if len(node.names) != 1:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    ref = DeclaredReferenceExpression()
    self.add_loc_info(node, ref)
    ref.setName(node.names[0])
    # self.scopemanager.addDeclaration(ref)

    return ref


def visit_Nonlocal(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_ClassDef(self, node):
    self.log_with_loc(ast.dump(node))
    rec = RecordDeclaration()
    self.add_loc_info(node, rec)
    # name
    rec.setName(node.name)
    self.scopemanager.enterScope(rec)
    t = None

    # bases
    if len(node.bases) == 0:
        pass
    elif len(node.bases) == 1:
        b = node.bases[0]

        if isinstance(b, ast.Attribute):
            tname = "%s.%s" % (b.value.id, b.attr)
            t = TypeParser.createFrom(tname, True)
        elif isinstance(b, ast.Name):
            tname = "%s" % (b.id)
            t = TypeParser.createFrom(tname, True)
        else:
            self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    else:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

    if t != None:
        rec.setSuperClasses([t])
    if len(node.keywords) != 0:
        self.log_with_loc(node.keywords)
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    # if node.starargs is not None:
    #    raise NotImplementedError
    # if node.kwargs is not None:
    #    raise NotImplementedError
    # body
    for b in node.body:
        if isinstance(b, ast.FunctionDef):
            # Method or Constructor
            fd = self.visit_FunctionDef(b, recordDec=rec)
            rec.addMethod(fd)
        elif isinstance(b, ast.Expr):
            stmt = self.visit(b)
            if self.is_field_declaration(stmt):
                d_stmt = DeclarationStatement()
                self.add_loc_info(b, d_stmt)
                d_stmt.setSingleDeclaration(stmt)
                stmt = d_stmt
            rec.addStatement(stmt)
        elif isinstance(b, ast.stmt):
            stmt = self.visit(b)
            if self.is_field_declaration(stmt):
                d_stmt = DeclarationStatement()
                self.add_loc_info(b, d_stmt)
                d_stmt.setSingleDeclaration(stmt)
                stmt = d_stmt
            rec.addStatement(stmt)
        else:
            self.log_with_loc(b)
            self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

    if len(node.decorator_list) != 0:
        self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")

    self.scopemanager.leaveScope(rec)
    self.scopemanager.addDeclaration(rec)
    return rec

### ASYNC AND AWAIT ###


def visit_AsyncFunctionDef(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    # TODO: async modifier
    return self.visit_FunctionDef(node)
    # raise NotImplementedError


def visit_Await(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    # TODO: implement
    return Expression()
    # raise NotImplementedError


def visit_AsyncFor(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return


def visit_AsyncWith(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    return

### MISC ###


def visit_Module(self, node):
    self.log_with_loc(ast.dump(node))
    '''The AST is wrapped in a 'Module' with 'body' list. Visit the body.'''
    nsd = NamespaceDeclaration()

    # TODO how to name the namespace?
    # TODO improve readability
    nsd_name = ".".join(self.fname.split("/")[-1].split(".")[:-1])
    nsd.setName(nsd_name)

    self.scopemanager.enterScope(nsd)

    for n in node.body:
        d = self.visit(n)

        # python also allows non-declarations on top-level, we DO not (yet)
        # so for now we only add declarations

        if d is Declaration:
            self.scopemanager.addDeclaration(d)

    self.scopemanager.leaveScope(nsd)

    self.scopemanager.addDeclaration(nsd)

### CATCH ALL ###


def generic_visit(self, node):
    self.log_with_loc(ast.dump(node))
    self.log_with_loc(NOT_IMPLEMENTED_MSG, loglevel="ERROR")
    print("GENERIC VISIT")
    return


def isMethodOrCtor(self, f):
    if self.is_method_declaration(f):
        return True
    elif self.is_ctor_declaration(f):
        return True
    else:
        return False
