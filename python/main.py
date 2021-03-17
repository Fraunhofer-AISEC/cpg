#from de.fraunhofer.aisec.cpg.graph.types import Type
from de.fraunhofer.aisec.cpg.graph.declarations import Declaration
from de.fraunhofer.aisec.cpg.graph.declarations import VariableDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import FunctionDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import IncludeDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import MethodDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import ParamVariableDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import RecordDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import TranslationUnitDeclaration
from de.fraunhofer.aisec.cpg.graph.statements import CompoundStatement
from de.fraunhofer.aisec.cpg.graph.statements import EmptyStatement
from de.fraunhofer.aisec.cpg.graph.statements import ForEachStatement
from de.fraunhofer.aisec.cpg.graph.statements import IfStatement
from de.fraunhofer.aisec.cpg.graph.statements import ReturnStatement
from de.fraunhofer.aisec.cpg.graph.statements import Statement
from de.fraunhofer.aisec.cpg.graph.statements.expressions import ArrayCreationExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import ArraySubscriptionExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import UnaryOperator
from de.fraunhofer.aisec.cpg.graph.statements.expressions import BinaryOperator
from de.fraunhofer.aisec.cpg.graph.statements.expressions import CallExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import DeclaredReferenceExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import ExpressionList
from de.fraunhofer.aisec.cpg.graph.statements.expressions import Literal
from de.fraunhofer.aisec.cpg.graph.statements.expressions import MemberCallExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import MemberExpression
from de.fraunhofer.aisec.cpg.graph.types import Type
from de.fraunhofer.aisec.cpg.graph.types import TypeParser
from java.net import URI
from java.util import List as JavaList
import ast
import inspect

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

def run():
    global global_res
    global_res = parseCode(global_codeToParse, global_fname, global_scopemanager)

def debug_print(string):
    callerframerecord = inspect.stack()[1]
    frame = callerframerecord[0]
    info = inspect.getframeinfo(frame)
    print("%s\t%d:\t%s" % (info.function, info.lineno, string))

class MyWalker(ast.NodeVisitor):
    def __init__(self, fname, scopemanager):
        self.tud = TranslationUnitDeclaration()
        self.fname = fname
        self.scopemanager = scopemanager
    
    def add_loc_info(self, node, obj):
        ''' Adds location information of node to obj '''
        # TODO cannot import sarif PhysicalLocation and Region -> ???
        obj.setFile(self.fname)
        if False:
            uri = URI("file://" + self.fname)
            obj.setLocation(PhysicalLocation(uri, Region(node.lineno,
                node.col_offset, node.end_lineno, node.end_col_offset)))
            


    ### LITERALS ###
    def visit_Constant(self, node):
        debug_print(ast.dump(node))
        if isinstance(node.value, str):
            lit = Literal()
            self.add_loc_info(node, lit)
            lit.setValue(node.value)
            #lit.setType(Type("str"))
            return lit
        elif isinstance(node.value, int):
            lit = Literal()
            self.add_loc_info(node, lit)
            lit.setValue(node.value)
            return lit
        else:
            raise NotImplementedError

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
        lit.setType(TypeParser.createFrom("List", True))
        elts = []
        for e in node.elts:
            elts.append(self.visit(e))
        lit.setValue(elts)
        return lit

    def visit_Tuple(self, node):
        debug_print(ast.dump(node))
        lit = Literal()
        self.add_loc_info(node, lit)
        lit.setType(TypeParser.createFrom("Tuple", True))
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
        raise NotImplementedError
    
    ### VARIABLES ###
    def visit_Name(self, node, get_declaration = False):
        debug_print(ast.dump(node))
        record = self.scopemanager.getRecordForName(self.scopemanager.getCurrentScope(),
                node.id)
        if record is None:
            ref = VariableDeclaration()
            self.add_loc_info(node, ref)
            ref.setName(node.id)
            self.scopemanager.addDeclaration(ref)
            if get_declaration == True:
                return ref
        # otherwise -> return reference (default case)
        ref = DeclaredReferenceExpression()
        self.add_loc_info(node, ref)
        ref.setName(node.id)
        ref.setRefersTo(record)
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
        # TODO? self.add_loc_info(node, ref)
        return self.visit(node.value)

    def visit_UnaryOp(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
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
        binop.setOperatorCode(node.op)
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
    def visit_BoolOp(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_And(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Or(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Compare(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
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
        # TODO what???
        # TODO keywords starargs kwargs
        debug_print(ast.dump(node))
        if isinstance(node.func, ast.Name):
            call = CallExpression()
            call.setName(node.func.id)
        elif isinstance(node.func, ast.Attribute):
            call = MemberCallExpression()
            call.setName(node.func.value)
        else:
            raise NotImplementedError
        self.add_loc_info(node, call)
        call_args = []
        for a in node.args:
            call_args.append(self.visit(a))
        call.setArguments(call_args)
        return call


    def visit_keyword(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_IfExpr(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_Attribute(self, node):
        debug_print(ast.dump(node))
        mem = MemberExpression()
        self.add_loc_info(node, mem)
        if isinstance(node.value, ast.Name):
            exp = self.visit(node.value)
        elif isinstance(node.value, ast.Attribute):
            exp = self.visit(node.value)
        else:
            debug_print(type(node.value))
            raise NotImplementedError
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
        debug_print(s)
        return s

    def visit_Slice(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

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
    def visit_Assign(self, node):
        debug_print(ast.dump(node))
        # binary operator
        binop = BinaryOperator()
        self.add_loc_info(node, binop)
        binop.setOperatorCode("=")
        if len(node.targets) != 1:
            raise NotImplementedError
        target = node.targets[0]
        binop.setLhs(self.visit(target))
        binop.setRhs(self.visit(node.value))
        return binop

    def visit_AnnAssign(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_AugAssign(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Raise(self, node):
        debug_print(ast.dump(node))
        op = UnaryOperator()
        self.add_loc_info(node, op)
        op.setOperatorCode("raise")
        op.setInput(self.visit(node.exc)) # TODO or setCode???
        debug_print(op)
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
        return stmt

    ### IMPORTS ###
    def visit_Import(self, node):
        debug_print(ast.dump(node))
        imp = IncludeDeclaration()
        self.add_loc_info(node, imp)
        if len(node.names) != 1:
            raise NotImplementedError
        imp.setFilename(self.fname + node.names[0].name)
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
        if isinstance(node.body, list):
            body = CompoundStatement()
            self.add_loc_info(node.body, body)
            for n in node.body:
                body.addStatement(self.visit(n))
            stmt.setThenStatement(body)
        else:
            raise NotImplementedError
        if isinstance(node.orelse, list):
            orelse = CompoundStatement()
            self.add_loc_info(node.orelse, orelse)
            for n in node.orelse:
                orelse.addStatement(self.visit(n))
            stmt.setElseStatement(orelse)
        else:
            raise NotImplementedError
        return stmt

    def visit_For(self, node):
        debug_print(ast.dump(node))
        stmt = ForEachStatement()
        self.add_loc_info(node, stmt)
        if isinstance(node.target, ast.Name):
            stmt.setVariable(self.visit_Name(node.target, get_declaration = True))
        else:
            raise NotImplementedError # what???
        stmt.setIterable(self.visit(node.iter))
        stmt.setStatement(self.visit(node.body))
        return stmt

    def visit_While(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Break(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Continue(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Try(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_ExceptHandler(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_With(self, node):
        debug_print(ast.dump(node))
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
        self.scopemanager.enterScope(fd)
        self.add_loc_info(node, fd)
        
        # handle name
        fd.setName(node.name)

        # handle args
        fd.addParameterList(self.visit(node.args))

        # handle body
        body = CompoundStatement()
        self.add_loc_info(node, body)
        fd.setBody(body)
        for stmt in node.body:
            body.addStatement(self.visit(stmt))

        # handle decorator_list

        # handle returns

        # handle type_comment

        self.scopemanager.leaveScope(fd)

        return fd

    def visit_Lambda(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    def visit_arguments(self, node):
        debug_print(ast.dump(node))
        plist = []
        for p in node.posonlyargs:
            raise NotImplementedError
        for p in node.args:
            plist.append(self.visit(p))
        if node.vararg is not None:
            raise NotImplementedError
        for p in node.kwonlyargs:
            raise NotImplementedError
        for p in node.kw_defaults:
            raise NotImplementedError
        if node.kwarg is not None:
            raise NotImplementedError
        for p in node.defaults:
            pass
            # raise NotImplementedError
            # no cpg support???
        return plist

    def visit_arg(self, node):
        debug_print(ast.dump(node))
        pvd = ParamVariableDeclaration()
        self.scopemanager.addDeclaration(pvd)
        self.add_loc_info(node, pvd)
        pvd.setName(node.arg)
        # TODO handle annotation
        return pvd

    def visit_Return(self, node):
        debug_print(ast.dump(node))
        r = ReturnStatement()
        self.add_loc_info(node, r)
        r.setReturnValue(self.visit(node.value))
        return r

    def visit_Yield(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_YieldFrom(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Global(self, node):
        debug_print(ast.dump(node))
        if len(node.names) != 1:
            raise NotImplementedError
        ref = DeclaredReferenceExpression()
        self.add_loc_info(node, ref)
        ref.setName(node.names[0])
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
        # bases
        if len(node.bases) == 0:
            pass
        elif len(node.bases) == 1:
            b = node.bases[0]
            if isinstance(b, ast.Attribute):
                tname = "%s.%s" % (b.value.id, b.attr)
                t = TypeParser.createFrom(tname, True)
            else:
                raise NotImplementedError
        else:
            raise NotImplementedError
        rec.setSuperClasses([t])
        if len(node.keywords) != 0:
            raise NotImplementedError
        #if node.starargs is not None:
        #    raise NotImplementedError
        #if node.kwargs is not None:
        #    raise NotImplementedError
        # body
        for b in node.body:
            if isinstance(b, ast.FunctionDef):
                fd = self.visit_FunctionDef(b, returnMethod=True)
                rec.addMethod(fd)
            else:
                raise NotImplementedError

        if len(node.decorator_list) != 0:
            raise NotImplementedError


        debug_print(rec)

        return rec

    ### ASYNC AND AWAIT ###
    def visit_AsyncFunctionDef(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Await(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
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
        for n in node.body:
            self.tud.addDeclaration(self.visit(n))

    ### CATCH ALL ###
    def generic_visit(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError


def parseCode(code, fname, scopemanager):
    root = ast.parse(code, filename = fname, type_comments = True)
    #debug_print(ast.dump(root, indent = 2))

    walker = MyWalker(fname, scopemanager)
    walker.visit(root)

    return walker.tud
