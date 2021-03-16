from de.fraunhofer.aisec.cpg.graph.declarations import Declaration
from de.fraunhofer.aisec.cpg.graph.declarations import FunctionDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import ParamVariableDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import TranslationUnitDeclaration
from de.fraunhofer.aisec.cpg.graph.statements import CompoundStatement
from de.fraunhofer.aisec.cpg.graph.statements import Statement
from de.fraunhofer.aisec.cpg.graph.statements.expressions import BinaryOperator
from de.fraunhofer.aisec.cpg.graph.statements.expressions import CallExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import DeclaredReferenceExpression
from de.fraunhofer.aisec.cpg.graph.statements.expressions import ExpressionList
from de.fraunhofer.aisec.cpg.graph.statements.expressions import Literal
from java.util import List as JavaList
import ast
import inspect

def run():
    global res
    res = parseCode(codeToParse)

def debug_print(string):
    callerframerecord = inspect.stack()[1]
    frame = callerframerecord[0]
    info = inspect.getframeinfo(frame)
    print("%s\t%d:\t%s" % (info.function, info.lineno, string))

class MyWalker(ast.NodeVisitor):
    def __init__(self):
        self.tud = TranslationUnitDeclaration()

    ### LITERALS ###
    def visit_Constant(self, node):
        debug_print(ast.dump(node))
        if isinstance(node.value, str):
            lit = Literal()
            lit.setValue(node.value)
            return lit
        elif isinstance(node.value, int):
            lit = Literal()
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
        raise NotImplementedError
    def visit_Tuple(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Set(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Dict(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    
    ### VARIABLES ###
    def visit_Name(self, node):
        debug_print(ast.dump(node))
        ref = DeclaredReferenceExpression()
        ref.setName(node.id)
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
        raise NotImplementedError
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
        debug_print(ast.dump(node))
        callStmt = CallExpression()
        call_args = []
        for a in node.args:
            call_args.append(self.visit(a))
        callStmt.setArguments(call_args)
        callStmt.setName(node.func.id)
        return callStmt

    def visit_keyword(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_IfExpr(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Attribute(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_NamedExpr(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    ### SUBSCRIPTING ###
    def visit_Subscript(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
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
        binop.setOperatorCode("=")
        lhs = ExpressionList()
        for n in node.targets:
            lhs.addExpression(self.visit(n))
        binop.setLhs(lhs)
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
        raise NotImplementedError
    def visit_Assert(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Delete(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Pass(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

    ### IMPORTS ###
    def visit_Import(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_ImportFrom(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_alias(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError


    ### CONTROL FLOW ###
    def visit_If(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_For(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
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
    def visit_FunctionDef(self, node):
        debug_print(ast.dump(node))
        fd = FunctionDeclaration()
        
        # handle name
        fd.setName(node.name)

        # handle args
        fd.addParameterList(self.visit(node.args))

        # handle body
        body = CompoundStatement()
        fd.setBody(body)
        for stmt in node.body:
            body.addStatement(self.visit(stmt))
            debug_print(fd.getBody())

        # handle decorator_list

        # handle returns

        # handle type_comment

        # store result
        # TODO
        self.tud.addDeclaration(fd)

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
            raise NotImplementedError
        return plist

    def visit_arg(self, node):
        debug_print(ast.dump(node))
        pvd = ParamVariableDeclaration()
        pvd.setName(node.arg)
        # TODO handle annotation
        return pvd

    def visit_Return(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Yield(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_YieldFrom(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Global(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_Nonlocal(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError
    def visit_ClassDef(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError

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
            self.visit(n)

    ### CATCH ALL ###
    def generic_visit(self, node):
        debug_print(ast.dump(node))
        raise NotImplementedError


def parseCode(code):
    a = ast.parse(code)
    print(ast.dump(a, indent = 2))

    walker = MyWalker()
    walker.visit(a)

    return walker.tud
