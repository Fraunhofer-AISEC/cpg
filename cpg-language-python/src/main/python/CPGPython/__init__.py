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

    # import methods from other files
    from ._expressions import handle_expression
    from ._expressions import handle_expression_impl
    from ._misc import add_loc_info
    from ._misc import get_src_code
    from ._misc import handle_operator_code
    from ._misc import is_declaration
    from ._misc import is_declared_reference
    from ._misc import is_field_declaration
    from ._misc import is_member_expression
    from ._misc import is_statement
    from ._misc import is_variable_declaration
    from ._misc import log_with_loc
    from ._misc import wrap_declaration_to_stmt
    from ._statements import handle_argument
    from ._statements import handle_assign
    from ._statements import handle_assign_impl
    from ._statements import handle_for
    from ._statements import handle_function_or_method
    from ._statements import handle_statement
    from ._statements import handle_statement_impl
    from ._statements import make_compound_statement

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
                r = self.handle_statement(stmt)
                if self.is_declaration(r):
                    r = self.wrap_declaration_to_stmt(r)
                nsd.addStatement(r)

            self.scopemanager.leaveScope(nsd)
            self.scopemanager.addDeclaration(nsd)
        else:
            self.log_with_loc("Expected an ast.Module node but recieved %s." %
                              (type(self.rootNode)), level="ERROR")
            raise RuntimeError
