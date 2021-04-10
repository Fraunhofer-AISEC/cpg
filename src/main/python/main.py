from de.fraunhofer.aisec.cpg.graph.declarations import Declaration
from de.fraunhofer.aisec.cpg.graph.declarations import FunctionDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import IncludeDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import MethodDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import NamespaceDeclaration
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
from de.fraunhofer.aisec.cpg.graph.statements import Statement
from de.fraunhofer.aisec.cpg.graph.statements import WhileStatement
from de.fraunhofer.aisec.cpg.graph.statements.expressions import ArrayCreationExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import ArrayRangeExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import ArraySubscriptionExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import BinaryOperator
from de.fraunhofer.aisec.cpg.graph.statements.expressions import ConstructExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import CallExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import DeclaredReferenceExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import Expression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import Literal
from de.fraunhofer.aisec.cpg.graph.statements.expressions import MemberCallExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import MemberExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import UnaryOperator
from de.fraunhofer.aisec.cpg.graph.types import Type
from de.fraunhofer.aisec.cpg.graph.types import TypeParser
from de.fraunhofer.aisec.cpg.sarif import PhysicalLocation
from de.fraunhofer.aisec.cpg.sarif import Region
from java.net import URI
from java.util import List as JavaList
import ast
import inspect
import re


#############################
# PROBLEMS / OPEN QUESTIONS #
#############################
# 1. Wie gehen wir mit Expression außerhalb von Funktionen um? Für Funktionen gibt es die FunctionDeclaration. Was gibt es für Expressions?
# 2. File Info kaputt
# 3. import == include???
# 4. Import from??? Import alias???
# 5. default args: wip für cpp
# 6. Exceptions -> unary op
# 7. listen / tupel?
# 8. 2 edges func -> pvd (scopemanager addDeclaration zu oft?)
# 9. self -> go receiver vom christian angucken
# 10. assign -> neue decl mit initializer (statt in visit_Name)
# 11. test Java isinstance aktuell mit java_name startswith -> :( -> chrisitan
# 12. function.py -> visit_return 2* ???

def debug_print(string, level=1):
    callerframerecord = inspect.stack()[level]
    frame = callerframerecord[0]
    info = inspect.getframeinfo(frame)
    print("%s\t%d:\t%s" % (info.function, info.lineno, string))


class CodeExtractor:
    # Simple/ugly class to extrace code snippets given a region
    def __init__(self, fname):
        with open(fname) as f:
            self.lines = f.read().splitlines()

    def get_snippet(self, lineno, col_offset, end_lineno, end_col_offset):
        # 1 vs 0-based indexing
        lineno -= 1
        # col_offset -= 1
        end_lineno -= 1
        # end_col_offset -= 1
        if lineno == end_lineno:
            return self.lines[lineno][col_offset:end_col_offset]
        else:
            res = []
            # first line is partially read
            res.append(self.lines[lineno][col_offset:])
            lineno += 1

            # fill with compelte lines
            while lineno + 1 < end_lineno:
                res.append(self.lines[lineno][:])
                lineno += 1

            # last line is partially read
            res.append(self.lines[end_lineno][:end_col_offset])

            return "\n".join(res)


class MyWalker(ast.NodeVisitor):
    def __init__(self, fname, frontend):
        self.sourcecode = CodeExtractor(fname)
        self.tud = TranslationUnitDeclaration()
        self.tud.setName(fname)
        self.fname = fname
        self.frontend = frontend
        self.scopemanager = frontend.getScopeManager()
        self.scopemanager.resetToGlobal(self.tud)

    def add_loc_info(self, node, obj):
        # Adds location information of node to obj
        if not isinstance(node, ast.AST):
            debug_print(type(node))
            debug_print(node[0].lineno)
            debug_print("<--- CALLER", level=2)
            raise NotImplementedError
        obj.setFile(self.fname)
        uri = URI("file://" + self.fname)
        obj.setLocation(PhysicalLocation(uri, Region(node.lineno,
                                                     node.col_offset, node.end_lineno, node.end_col_offset)))
        obj.setCode(self.sourcecode.get_snippet(node.lineno,
                                                node.col_offset, node.end_lineno, node.end_col_offset))
        # obj.setCode(ast.unparse(node)) # alternative to CodeExtractor class

    ### LITERALS ###
    def visit_Constant(self, node):
        debug_print(ast.dump(node))
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
            debug_print(type(node.value))
            raise NotImplementedError
        return lit

    def visit_FormattedValue(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_JoinedStr(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_List(self, node):
        debug_print(ast.dump(node))
        lit = Literal()
        self.add_loc_info(node, lit)
        lit.setType(TypeParser.createFrom("List", False))
        elts = []
        for e in node.elts:
            elts.append(self.visit(e))
        lit.setValue(elts)
        return lit

    def visit_Tuple(self, node):
        debug_print(ast.dump(node))
        lit = Literal()
        self.add_loc_info(node, lit)
        lit.setType(TypeParser.createFrom("Tuple", False))
        elts = []
        for e in node.elts:
            elts.append(self.visit(e))
        lit.setValue(elts)
        return lit

    def visit_Set(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Dict(self, node):
        debug_print(ast.dump(node))
        # TODO implement it
        #raise NotImplementedError
        return Expression()

    ### VARIABLES ###
    def visit_Name(self, node):
        debug_print(ast.dump(node))
        ref = DeclaredReferenceExpression()
        self.add_loc_info(node, ref)
        ref.setName(node.id)

        # resolved_ref = self.scopemanager.resolve(ref)
        # if resolved_ref != None:
        #    ref.setRefersTo(resolved_ref)
        # else:
        #    debug_print("Failed to resolve name.")
        #    raise RuntimeError
        return ref

    def visit_Load(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Store(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Del(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Starred(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    ### EXORESSIONS ###
    def visit_Expr(self, node):
        debug_print(ast.dump(node))
        return self.visit(node.value)

    def visit_UnaryOp(self, node):
        debug_print(ast.dump(node))
        unop = UnaryOperator()
        self.add_loc_info(node, unop)
        unop.setOperatorCode(node.op)
        unop.setInput(self.visit(node.operand))
        if isinstance(node.op, ast.UAdd):
            unop.setName("UAdd")
        elif isinstance(node.op, ast.USub):
            unop.setName("USub")
        elif isinstance(node.op, ast.Not):
            unop.setName("Not")
        elif isinstance(node.op, ast.Invert):
            unop.setName("Invert")
        else:
            raise NotImplementedError
        return unop

    def visit_UAdd(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_USub(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Not(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Invert(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_BinOp(self, node):
        debug_print(ast.dump(node))
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
            binop.setOperatorCode("Pow")
            binop.setName("Pow")
        elif isinstance(node.op, ast.LShift):
            binop.setOperatorCode("LShift")
            binop.setName("LShift")
        elif isinstance(node.op, ast.RShift):
            binop.setOperatorCode("RShift")
            binop.setName("RShift")
        elif isinstance(node.op, ast.BitOr):
            binop.setOperatorCode("BitOr")
            binop.setName("BitOr")
        elif isinstance(node.op, ast.BitXor):
            binop.setOperatorCode("BitXor")
            binop.setName("BitXor")
        elif isinstance(node.op, ast.BitAnd):
            binop.setOperatorCode("BitAnd")
            binop.setName("BitAnd")
        elif isinstance(node.op, ast.MatMult):
            binop.setOperatorCode("MatMult")
            binop.setName("MatMult")
        else:
            raise NotImplementedError
        binop.setLhs(self.visit(node.left))
        binop.setRhs(self.visit(node.right))
        return binop

    def visit_Add(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Sub(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Mult(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Div(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_FloorDiv(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Mod(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Pow(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_LShift(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_RShift(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_BitOr(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_BitXor(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_BitAnd(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_MatMult(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_BoolOp(self, node: ast.BoolOp):
        debug_print(ast.dump(node))
        binOp = BinaryOperator()

        if isinstance(node.op, ast.And):
            binOp.setOperatorCode("&&")
        elif isinstance(node.op, ast.Or):
            binOp.setOperatorCode("||")
        else:
            raise NotImplementedError

        # TODO: split into multiple binary operators, python supports many values

        if len(node.values) == 2:
            lhs = self.visit(node.values[0])
            rhs = self.visit(node.values[1])
        else:
            raise NotImplementedError

        return binOp

    def visit_And(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Or(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Compare(self, node):
        debug_print(ast.dump(node))
        comp = BinaryOperator()
        self.add_loc_info(node, comp)
        if len(node.ops) != 1:
            raise NotImplementedError
        op = node.ops[0]
        if isinstance(op, ast.Eq):
            comp.setOperatorCode("Eq")
            comp.setName("Eq")
        elif isinstance(op, ast.NotEq):
            comp.setOperatorCode("NotEq")
            comp.setName("NotEq")
        elif isinstance(op, ast.Lt):
            comp.setOperatorCode("Lt")
            comp.setName("Lt")
        elif isinstance(op, ast.LtE):
            comp.setOperatorCode("LtE")
            comp.setName("LtE")
        elif isinstance(op, ast.Gt):
            comp.setOperatorCode("Gt")
            comp.setName("Gt")
        elif isinstance(op, ast.GtE):
            comp.setOperatorCode("GtE")
            comp.setName("GtE")
        elif isinstance(op, ast.Is):
            comp.setOperatorCode("Is")
            comp.setName("Is")
        elif isinstance(op, ast.IsNot):
            comp.setOperatorCode("IsNot")
            comp.setName("IsNot")
        elif isinstance(op, ast.In):
            comp.setOperatorCode("In")
            comp.setName("In")
        elif isinstance(op, ast.NotIn):
            comp.setOperatorCode("NotIn")
            comp.setName("NotIn")
        else:
            debug_print(op)
            raise NotImplementedError
        comp.setLhs(self.visit(node.left))
        if len(node.comparators) != 1:
            raise NotImplementedError
        comp.setRhs(self.visit(node.comparators[0]))
        return comp

    def visit_Eq(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_NotEq(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Lt(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_LtE(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Gt(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_GtE(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Is(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_IsNot(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_In(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_NotIn(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Call(self, node):
        # TODO keywords starargs kwargs
        debug_print(ast.dump(node))

        # a call can be one of
        # - simple function call
        # - member call
        # - constructor call
        #
        # We parse node.func regularly using a visitor and decide what it is
        ref = self.visit(node.func)

        name = ref.getName()

        if ref.java_name == "de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression":
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
                call = CallExpression()
                call.setName(name)
                call.setFqn(name)

        self.add_loc_info(node, call)

        # add arguments
        for a in node.args:
            call.addArgument(self.visit(a))

        return call

    def visit_keyword(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_IfExp(self, node):
        debug_print(ast.dump(node))
        # TODO: implement this - how?
        #raise NotImplementedError
        return Expression()

    def visit_Attribute(self, node):
        debug_print(ast.dump(node))

        mem = MemberExpression()
        # self.add_loc_info(node, mem)

        if isinstance(node.value, ast.Name):
            exp = self.visit(node.value)
        elif isinstance(node.value, ast.Attribute):
            exp = self.visit(node.value)
        elif isinstance(node.value, ast.Subscript):
            exp = self.visit(node.value)
        else:
            debug_print(type(node.value))
            raise NotImplementedError

        mem.setName(node.attr)
        mem.setBase(exp)
        mem.setOperatorCode(node.attr)

        return mem

    def visit_NamedExpr(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    ### SUBSCRIPTING ###
    def visit_Subscript(self, node):
        debug_print(ast.dump(node))
        s = ArraySubscriptionExpression()
        self.add_loc_info(node, s)
        s.setArrayExpression(self.visit(node.value))
        s.setSubscriptExpression(self.visit(node.slice))
        return s

    def visit_Slice(self, node):
        debug_print(ast.dump(node))
        slc = ArrayRangeExpression()
        self.add_loc_info(node, slc)
        if node.lower != None:
            slc.setFloor(self.visit(node.lower))
        if node.upper != None:
            slc.setCeiling(self.visit(node.upper))
        if node.step != None:
            debug_print("Step not yet supported.")
            raise NotImplementedError
        return slc

    ### COMPREHENSIONS ###
    def visit_ListComp(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_SetComp(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_GeneratorExp(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_DictComp(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_comprehension(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    ### STATEMENTS ###
    def visit_Assign(self, node: ast.Assign):
        debug_print(ast.dump(node))

        if len(node.targets) != 1:
            raise NotImplementedError

        target = node.targets[0]

        # if isinstance(target, ast.Attribute):
        #    debug_print("HERE")

        # parse LHS and RHS as expressions
        lhs = self.visit(target)
        rhs = self.visit(node.value)

        if lhs.java_name == "de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression":
            # Check whether this assigns to a declared var or to a new var
            resolved_ref = self.scopemanager.resolve(lhs)

            if resolved_ref is None:
                # new var -> variable declaration + initializer list
                v = VariableDeclaration()
                self.add_loc_info(node, v)
                v.setName(lhs.getName())
                v.setInitializer(rhs)

                self.scopemanager.addDeclaration(v)
                return v

        # found var => BinaryOperator "="
        binop = BinaryOperator()
        self.add_loc_info(node, binop)
        binop.setOperatorCode("=")
        binop.setLhs(lhs)
        binop.setRhs(rhs)
        binop.setName("=")
        return binop

    def visit_AnnAssign(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_AugAssign(self, node):
        debug_print(ast.dump(node))
        binop = BinaryOperator()
        self.add_loc_info(node, binop)
        binop.setOperatorCode(node.op)
        binop.setLhs(self.visit(node.target))
        binop.setRhs(self.visit(node.value))
        binop.setName(node.op)
        return binop

    def visit_Raise(self, node):
        debug_print(ast.dump(node))
        op = UnaryOperator()
        self.add_loc_info(node, op)
        op.setOperatorCode("raise")
        if node.exc != None:
            op.setInput(self.visit(node.exc))
        return op

    def visit_Assert(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Delete(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Pass(self, node):
        debug_print(ast.dump(node))
        stmt = EmptyStatement()
        self.add_loc_info(node, stmt)
        stmt.setName("pass")
        return stmt

    ### IMPORTS ###
    def visit_Import(self, node):
        debug_print(ast.dump(node))
        imp = IncludeDeclaration()
        self.add_loc_info(node, imp)
        if len(node.names) != 1:
            raise NotImplementedError
        imp.setFilename(self.fname + node.names[0].name)

        # make scopmanager aware of import
        vd = VariableDeclaration()
        self.add_loc_info(node, vd)
        vd.setName(node.names[0].name)
        self.scopemanager.addGlobal(vd)
        return imp

    def visit_ImportFrom(self, node):
        debug_print(ast.dump(node))
        imp = IncludeDeclaration()
        self.add_loc_info(node, imp)
        if node.level != 0:
            raise NotImplementedError
        imp.setFilename(self.fname + node.module)

        # make scopmanager aware of import
        vd = VariableDeclaration()
        self.add_loc_info(node, vd)
        vd.setName(node.module)
        self.scopemanager.addGlobal(vd)
        return imp

    def visit_alias(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    ### CONTROL FLOW ###
    def visit_If(self, node):
        debug_print(ast.dump(node))
        stmt = IfStatement()
        self.add_loc_info(node, stmt)
        stmt.setCondition(self.visit(node.test))
        stmt.setName("If")
        if len(node.body) == 1:
            stmt.setThenStatement(self.visit(node.body[0]))
        else:
            body = self.make_compound_statement(node, node.body)

            stmt.setThenStatement(body)
        if len(node.orelse) == 1:
            stmt.setElseStatement(self.visit(node.orelse[0]))
        else:
            orelse = self.make_compound_statement(node, node.orelse)

            stmt.setElseStatement(orelse)
        return stmt

    def make_compound_statement(self, node, node_list) -> CompoundStatement:
        # TODO: generalize this, because this code repeats
        body = CompoundStatement()
        self.add_loc_info(node, body)

        for b in node_list:
            s = self.visit(b)

            if s.java_name.startswith('de.fraunhofer.aisec.cpg.graph.declarations.'):
                # wrap the statement
                d = DeclarationStatement()
                self.add_loc_info(node, d)
                d.setSingleDeclaration(s)
                body.addStatement(d)
            else:
                body.addStatement(s)

        return body


    def visit_For(self, node: ast.For):
        debug_print(ast.dump(node))

        stmt = ForEachStatement()
        self.add_loc_info(node, stmt)

        ref = self.visit(node.target)

        if ref.java_name == "de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression":
            # create a new variable
            var = VariableDeclaration()
            self.add_loc_info(node.target, var)
            var.setName(ref.getName())

            stmt.setVariable(var)
        else:
            debug_print(ref.java_name)
            # tuple or list
            raise NotImplementedError  # what???

        stmt.setIterable(self.visit(node.iter))
        if len(node.body) == 1:
            stmt.setStatement(self.visit(node.body))
        else:
            body = self.make_compound_statement(node, node.body)

            stmt.setStatement(body)
        return stmt

    def visit_While(self, node):
        debug_print(ast.dump(node))
        w = WhileStatement()
        self.add_loc_info(node, w)
        w.setCondition(self.visit(node.test))
        body = CompoundStatement()
        self.add_loc_info(node, body)
        for n in node.body:
            body.addStatement(self.visit(n))
        w.setStatement(body)
        if node.orelse != None and len(node.orelse) != 0:
            debug_print("while -> orelse not implemented, yet")
            raise NotImplementedError
        return w

    def visit_Break(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Continue(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Try(self, node: ast.Try):
        debug_print(ast.dump(node))
        # TODO parse body of try statement
        t = TryStatement()

        # node.body

        return t

    def visit_ExceptHandler(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_With(self, node):
        debug_print(ast.dump(node))
        return  # TODO LATER -> new cpg node or foreach + try/catch?
        raise NotImplementedError

    def visit_withitem(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    ### FUNCTION AND CLASS DEFINITIONS ###
    def visit_FunctionDef(self, node, returnMethod=False):
        debug_print(ast.dump(node))
        if returnMethod:
            fd = MethodDeclaration()
        else:
            fd = FunctionDeclaration()
        self.add_loc_info(node, fd)

        # handle name
        fd.setName(node.name)
        self.scopemanager.enterScope(fd)

        # handle args
        # scopeManager adds them to fd
        self.visit_arguments(node.args, fd)

        # handle body
        body = CompoundStatement()
        body.setName("body")
        self.add_loc_info(node, body)
        fd.setBody(body)
        for stmt in node.body:
            s = self.visit(stmt)

            if s is None:
                continue

            # TODO
            if s.java_name.startswith('de.fraunhofer.aisec.cpg.graph.declarations.'):
                # wrap the statement
                d = DeclarationStatement()
                self.add_loc_info(node, d)
                d.setSingleDeclaration(s)
                body.addStatement(d)
            else:
                body.addStatement(s)

        # handle decorator_list

        # handle return annotation

        # handle type_comment

        self.scopemanager.leaveScope(fd)
        self.scopemanager.addDeclaration(fd)

        return fd

    def visit_Lambda(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_arguments(self, node, fd=None):
        debug_print(ast.dump(node))
        if fd != None:
            debug_print("visit_arguments with FunctionDeclaration")
        for p in node.posonlyargs:
            raise NotImplementedError
        for p in node.args:
            x = self.visit(p)
            if fd != None:
                # fd.addParameter(x)
                pass  # PVD -> automagically handled by cpg
        #if node.vararg is not None:
        #    raise NotImplementedError
        for p in node.kwonlyargs:
            raise NotImplementedError
        for p in node.kw_defaults:
            raise NotImplementedError
        #if node.kwarg is not None:
        #    raise NotImplementedError
        for p in node.defaults:
            pass
            # raise NotImplementedError
            # no cpg support???
        return

    def visit_arg(self, node):
        debug_print(ast.dump(node))
        pvd = ParamVariableDeclaration()
        self.add_loc_info(node, pvd)
        pvd.setName(node.arg)
        if node.annotation != None:
            pvd.setType(TypeParser.createFrom(node.annotation.id, False))
        self.scopemanager.addDeclaration(pvd)
        return pvd

    def visit_Return(self, node):
        debug_print(ast.dump(node))
        r = ReturnStatement()
        self.add_loc_info(node, r)
        if node.value != None:
            r.setReturnValue(self.visit(node.value))
        r.setName("return")
        return r

    def visit_Yield(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_YieldFrom(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Global(self, node):
        return  # TODO: no support for global vars, yet. how to model them as CPG
        # nodes?
        debug_print(ast.dump(node))
        if len(node.names) != 1:
            raise NotImplementedError
        ref = DeclaredReferenceExpression()
        self.add_loc_info(node, ref)
        ref.setName(node.names[0])
        self.scopemanager.addDeclaration(ref)
        return ref

    def visit_Nonlocal(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_ClassDef(self, node):
        debug_print(ast.dump(node))
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
                raise NotImplementedError
        else:
            raise NotImplementedError

        if t != None:
            rec.setSuperClasses([t])
        if len(node.keywords) != 0:
            debug_print(node.keywords)
            raise NotImplementedError
        # if node.starargs is not None:
        #    raise NotImplementedError
        # if node.kwargs is not None:
        #    raise NotImplementedError
        # body
        for b in node.body:
            if isinstance(b, ast.FunctionDef):
                # TODO __init__ -> ctor
                fd = self.visit_FunctionDef(b, returnMethod=True)
                rec.addMethod(fd)
            elif isinstance(b, ast.Expr):
                # TODO what to do about expressions inside a class?
                self.visit(b)
            else:
                debug_print(type(b))
                debug_print(b)
                raise NotImplementedError

        if len(node.decorator_list) != 0:
            raise NotImplementedError

        self.scopemanager.leaveScope(rec)
        self.scopemanager.addDeclaration(rec)
        return rec

    ### ASYNC AND AWAIT ###
    def visit_AsyncFunctionDef(self, node):
        debug_print(ast.dump(node))
        # TODO: async modifier
        return self.visit_FunctionDef(node)
        #raise NotImplementedError

    def visit_Await(self, node):
        debug_print(ast.dump(node))
        # TODO: implement
        return Expression()
        #raise NotImplementedError

    def visit_AsyncFor(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_AsyncWith(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    ### MISC ###
    def visit_Module(self, node):
        debug_print(ast.dump(node))
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
                self.scopemanager.addDeclaration(self.visit(n))

        self.scopemanager.leaveScope(nsd)
        self.scopemanager.addDeclaration(nsd)

    ### CATCH ALL ###
    def generic_visit(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError


def parseCode(code, fname, frontend):
    root = ast.parse(code, filename=fname, type_comments=True)
    # debug_print(ast.dump(root, indent = 2))

    walker = MyWalker(fname, frontend)
    walker.visit(root)

    return walker.tud
