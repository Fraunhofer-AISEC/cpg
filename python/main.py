from java.lang import System
from de.fraunhofer.aisec.cpg.graph.declarations import TranslationUnitDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import Declaration
import ast

def run():
    global res
    res = parseCode(codeToParse)

class MyWalker(ast.NodeVisitor):
    ### LITERALS ###
    def visit_Constant(self, node):
        raise NotImplementedError
    def visit_FormattedValue(self, node):
        raise NotImplementedError
    def visit_JoinedStr(self, node):
        raise NotImplementedError
    def visit_List(self, node):
        raise NotImplementedError
    def visit_Tuple(self, node):
        raise NotImplementedError
    def visit_Set(self, node):
        raise NotImplementedError
    def visit_Dict(self, node):
        raise NotImplementedError
    
    ### VARIABLES ###
    def visit_Name(self, node):
        raise NotImplementedError
    def visit_Load(self, node):
        raise NotImplementedError
    def visit_Store(self, node):
        raise NotImplementedError
    def visit_Del(self, node):
        raise NotImplementedError
    def visit_Starred(self, node):
        raise NotImplementedError

    ### EXORESSIONS ###
    def visit_Expr(self, node):
        raise NotImplementedError
    def visit_UnaryOp(self, node):
        raise NotImplementedError
    def visit_UAdd(self, node):
        raise NotImplementedError
    def visit_USub(self, node):
        raise NotImplementedError
    def visit_Not(self, node):
        raise NotImplementedError
    def visit_Invert(self, node):
        raise NotImplementedError
    def visit_BinOp(self, node):
        raise NotImplementedError
    def visit_Add(self, node):
        raise NotImplementedError
    def visit_Sub(self, node):
        raise NotImplementedError
    def visit_Mult(self, node):
        raise NotImplementedError
    def visit_Div(self, node):
        raise NotImplementedError
    def visit_FloorDiv(self, node):
        raise NotImplementedError
    def visit_Mod(self, node):
        raise NotImplementedError
    def visit_Pow(self, node):
        raise NotImplementedError
    def visit_LShift(self, node):
        raise NotImplementedError
    def visit_RShift(self, node):
        raise NotImplementedError
    def visit_BitOr(self, node):
        raise NotImplementedError
    def visit_BitXor(self, node):
        raise NotImplementedError
    def visit_BitAnd(self, node):
        raise NotImplementedError
    def visit_MatMult(self, node):
        raise NotImplementedError
    def visit_BoolOp(self, node):
        raise NotImplementedError
    def visit_And(self, node):
        raise NotImplementedError
    def visit_Or(self, node):
        raise NotImplementedError
    def visit_Compare(self, node):
        raise NotImplementedError
    def visit_Eq(self, node):
        raise NotImplementedError
    def visit_NotEq(self, node):
        raise NotImplementedError
    def visit_Lt(self, node):
        raise NotImplementedError
    def visit_LtE(self, node):
        raise NotImplementedError
    def visit_Gt(self, node):
        raise NotImplementedError
    def visit_GtE(self, node):
        raise NotImplementedError
    def visit_Is(self, node):
        raise NotImplementedError
    def visit_IsNot(self, node):
        raise NotImplementedError
    def visit_In(self, node):
        raise NotImplementedError
    def visit_NotIn(self, node):
        raise NotImplementedError
    def visit_Call(self, node):
        raise NotImplementedError
    def visit_keyword(self, node):
        raise NotImplementedError
    def visit_IfExpr(self, node):
        raise NotImplementedError
    def visit_Attribute(self, node):
        raise NotImplementedError
    def visit_NamedExpr(self, node):
        raise NotImplementedError

    ### SUBSCRIPTING ###
    def visit_Subscript(self, node):
        raise NotImplementedError
    def visit_Slice(self, node):
        raise NotImplementedError

    ### COMPREHENSIONS ###
    def visit_ListComp(self, node):
        raise NotImplementedError
    def visit_SetComp(self, node):
        raise NotImplementedError
    def visit_GeneratorExp(self, node):
        raise NotImplementedError
    def visit_DictComp(self, node):
        raise NotImplementedError
    def visit_comprehension(self, node):
        raise NotImplementedError

    ### STATEMENTS ###
    def visit_Assign(self, node):
        raise NotImplementedError
    def visit_AnnAssign(self, node):
        raise NotImplementedError
    def visit_AugAssign(self, node):
        raise NotImplementedError
    def visit_Raise(self, node):
        raise NotImplementedError
    def visit_Assert(self, node):
        raise NotImplementedError
    def visit_Delete(self, node):
        raise NotImplementedError
    def visit_Pass(self, node):
        raise NotImplementedError

    ### IMPORTS ###
    def visit_Import(self, node):
        raise NotImplementedError
    def visit_ImportFrom(self, node):
        raise NotImplementedError
    def visit_alias(self, node):
        raise NotImplementedError


    ### CONTROL FLOW ###
    def visit_If(self, node):
        raise NotImplementedError
    def visit_For(self, node):
        raise NotImplementedError
    def visit_While(self, node):
        raise NotImplementedError
    def visit_Break(self, node):
        raise NotImplementedError
    def visit_Continue(self, node):
        raise NotImplementedError
    def visit_Try(self, node):
        raise NotImplementedError
    def visit_ExceptHandler(self, node):
        raise NotImplementedError
    def visit_With(self, node):
        raise NotImplementedError
    def visit_withitem(self, node):
        raise NotImplementedError

    ### FUNCTION AND CLASS DEFINITIONS ###
    def visit_FunctionDef(self, node):
        raise NotImplementedError
    def visit_Lambda(self, node):
        raise NotImplementedError
    def visit_arguments(self, node):
        raise NotImplementedError
    def visit_arg(self, node):
        raise NotImplementedError
    def visit_Return(self, node):
        raise NotImplementedError
    def visit_Yield(self, node):
        raise NotImplementedError
    def visit_YieldFrom(self, node):
        raise NotImplementedError
    def visit_Global(self, node):
        raise NotImplementedError
    def visit_Nonlocal(self, node):
        raise NotImplementedError
    def visit_ClassDef(self, node):
        raise NotImplementedError

    ### ASYNC AND AWAIT ###
    def visit_AsyncFunctionDef(self, node):
        raise NotImplementedError
    def visit_Await(self, node):
        raise NotImplementedError
    def visit_AsyncFor(self, node):
        raise NotImplementedError
    def visit_AsyncWith(self, node):
        raise NotImplementedError

    ### CATCH ALL ###
    def generic_visit(self, node):
        raise NotImplementedError


def parseCode(code):
    tud_root = TranslationUnitDeclaration()
    a = ast.parse(code)
    System.out.println(ast.dump(a, indent = 2))

    MyWalker().visit(a)

    return tud_root
