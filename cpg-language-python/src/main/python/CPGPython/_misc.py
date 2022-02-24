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
import inspect
import ast
from de.fraunhofer.aisec.cpg.graph import NodeBuilder
from de.fraunhofer.aisec.cpg.sarif import PhysicalLocation
from de.fraunhofer.aisec.cpg.sarif import Region
from java.net import URI

NOT_IMPLEMENTED_MSG = "This has not been implemented, yet. Using a dummy."
CPG_JAVA = "de.fraunhofer.aisec.cpg"


def get_src_code(self, node: ast.AST):
    return self.sourcecode.get_snippet(node.lineno, node.col_offset,
                                       node.end_lineno, node.end_col_offset)


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
    """
    Add file location meta information to CPG objects.
    """

    obj.setFile(self.fname)

    if not isinstance(node, ast.AST):
        self.log_with_loc(
            "Expected an AST object but received %s. Not adding location." %
            (type(node)), loglevel="ERROR")
        return

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
    n = CPG_JAVA + ".graph.declarations.VariableDeclaration"
    return target is not None and target.java_name == n


def is_declared_reference(self, target):
    n = CPG_JAVA + ".graph.statements.expressions.DeclaredReferenceExpression"
    return target is not None and target.java_name == n


def is_field_declaration(self, target):
    n = CPG_JAVA + ".graph.declarations.FieldDeclaration"
    return target is not None and target.java_name == n


def is_member_expression(self, target):
    n = CPG_JAVA + ".graph.statements.expressions.MemberExpression"
    return target is not None and target.java_name == n


def is_declaration(self, target):
    n = CPG_JAVA + ".graph.declarations."
    return target is not None and target.java_name.startswith(
        n)


def is_method_declaration(self, target):
    n = CPG_JAVA + ".graph.declarations.MethodDeclaration"
    return target is not None and target.java_name == n


def is_ctor_declaration(self, target):
    n = CPG_JAVA + ".graph.declarations.ConstructorDeclaration"
    return target is not None and target.java_name == n


def is_statement(self, target):
    n = CPG_JAVA + ".graph.statements."
    return target is not None and target.java_name.startswith(
        n)


def wrap_declaration_to_stmt(self, stmt):
    """
    Wrap a single declaration in a DeclarationStatement
    """
    if not self.is_declaration(stmt):
        self.log_with_loc(
            "Expected a declaration but got \"%s\". Using a dummy." %
            (stmt.java_name), loglevel="ERROR")
        return NodeBuilder.newDeclarationStatement("DUMMY")
    decl_stmt = NodeBuilder.newDeclarationStatement(
        stmt.getCode())
    decl_stmt.setLocation(stmt.getLocation())
    decl_stmt.setSingleDeclaration(stmt)
    return decl_stmt


def handle_operator_code(self, opcode):
    """
    Parses an operator code and returns its string representation.
    Returns an empty string on error.
    """
    if isinstance(opcode, ast.Add):
        op = "+"
    elif isinstance(opcode, ast.Sub):
        op = "-"
    elif isinstance(opcode, ast.Mult):
        op = "*"
    elif isinstance(opcode, ast.MatMult):
        op = "*"
    elif isinstance(opcode, ast.Div):
        op = "/"
    elif isinstance(opcode, ast.Mod):
        op = "%"
    elif isinstance(opcode, ast.Pow):
        op = "^"
    elif isinstance(opcode, ast.LShift):
        op = "<<"
    elif isinstance(opcode, ast.RShift):
        op = ">>"
    elif isinstance(opcode, ast.BitOr):
        op = "|"
    elif isinstance(opcode, ast.BitXor):
        op = "^"
    elif isinstance(opcode, ast.BitAnd):
        op = "&"
    elif isinstance(opcode, ast.FloorDiv):
        op = "//"
    else:
        self.log_with_loc(
            "Failed to identify the operator. Using an empty dummy.",
            loglevel="ERROR")
        op = ""
    return op
