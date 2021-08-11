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
from ._code_extractor import CodeExtractor
from de.fraunhofer.aisec.cpg.graph import NodeBuilder
import ast


class PythonASTToCPG(ast.NodeVisitor):
    def __init__(self, fname, frontend, code):
        self.sourcecode = CodeExtractor(fname)
        self.tud = NodeBuilder.newTranslationUnitDeclaration(fname, code)
        self.tud.setName(fname)
        self.fname = fname
        self.frontend = frontend
        self.scopemanager = frontend.getScopeManager()
        self.scopemanager.resetToGlobal(self.tud)
        self.logger = self.frontend.log
        self.rootNode = ast.parse(code, filename=fname, type_comments=True)

    from ._internal import add_loc_info, generic_visit, handle_attribute_simple_case, is_ctor_declaration, is_declaration, is_declared_reference, is_field_declaration, is_member_expression, is_method_declaration, isMethodOrCtor, is_variable_declaration, log_with_loc, make_compound_statement, visit_Add, visit_alias, visit_And, visit_AnnAssign, visit_arg, visit_arguments, visit_Assert, visit_Assign, visit_AsyncFor, visit_AsyncFunctionDef, visit_AsyncWith, visit_Attribute, visit_AugAssign, visit_Await, visit_BinOp, visit_BitAnd, visit_BitOr, visit_BitXor, visit_BoolOp, visit_Break, visit_Call, visit_ClassDef, visit_Compare, visit_comprehension, visit_Constant, visit_Continue, visit_Del, visit_Delete, visit_Dict, visit_DictComp, visit_Div, visit_Eq, visit_ExceptHandler, visit_Expr, visit_FloorDiv, visit_For, visit_FormattedValue, visit_FunctionDef, visit_GeneratorExp, visit_Global, visit_Gt, visit_GtE, visit_If, visit_IfExp, visit_Import, visit_ImportFrom, visit_In, visit_Invert, visit_Is, visit_IsNot, visit_JoinedStr, visit_keyword, visit_Lambda, visit_List, visit_ListComp, visit_Load, visit_LShift, visit_Lt, visit_LtE, visit_MatMult, visit_Mod, visit_Module, visit_Mult, visit_Name, visit_NamedExpr, visit_Nonlocal, visit_Not, visit_NotEq, visit_NotIn, visit_Or, visit_Pass, visit_Pow, visit_Raise, visit_Return, visit_RShift, visit_Set, visit_SetComp, visit_Slice, visit_Starred, visit_Store, visit_Sub, visit_Subscript, visit_Try, visit_Tuple, visit_UAdd, visit_UnaryOp, visit_USub, visit_While, visit_With, visit_withitem, visit_Yield, visit_YieldFrom
    from ._statements import handle_statement

    def execute(self):
        if isinstance(self.rootNode, ast.Module):
            self.log_with_loc("Handling tree root: %s" %
                              (ast.dump(self.rootNode)))
            # Module(stmt* body, type_ignore* type_ignores)
            # TODO how to name the namespace?
            # TODO improve readability
            nsd_name = ".".join(self.fname.split("/")[-1].split(".")[:-1])
            nsd = NodeBuilder.newNamespaceDeclaration(nsd_name, "")
            self.tud.addDeclaration(nsd)
            self.scopemanager.enterScope(nsd)

            for stmt in self.rootNode.body:
                self.log_with_loc("Handling statement %s" % (ast.dump(stmt)))
                r = self.handle_statement(stmt)
                self.log_with_loc("Handling statement result is: %s" % (r))
                if self.is_declaration(r):
                    # TODO do we need both???
                    self.scopemanager.addDeclaration(r)
                    nsd.addDeclaration(r)
                else:
                    self.log_with_loc("Don't know what to do with this: %s" %
                                      (r))

            self.scopemanager.leaveScope(nsd)
            self.scopemanager.addDeclaration(nsd)
        else:
            self.logger.error("Expected an ast.Module node but recieved %s." %
                              (type(self.rootNode)))
            raise RuntimeError
