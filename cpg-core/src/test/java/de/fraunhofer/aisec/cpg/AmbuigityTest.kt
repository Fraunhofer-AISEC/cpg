/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.graph
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AmbiguousExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import org.junit.jupiter.api.Test

class AmbuigityTest {
    @OptIn(ExperimentalGraph::class)
    @Test
    fun testAmbiguity() {
        // Our code:
        // int main() {
        //    a(b);
        //    b.size = 1;
        // }
        //
        // a(b) can either be a declaration statement with a variable b of type a
        // or it can be a call to the function a using the single argument b.
        //
        // The existence of a member expression in b.size gives us a strong hint that this is
        // actually a variable declaration of type a (probably a struct) and we could resolve that
        // ambiguity.

        val sm = ScopeManager()
        val lang = CXXLanguageFrontend(TranslationConfiguration.builder().build(), sm)
        TypeManager.getInstance().setLanguageFrontend(lang)

        val tu = NodeBuilder.newTranslationUnitDeclaration("")
        sm.resetToGlobal(tu)

        val func = NodeBuilder.newFunctionDeclaration("main")
        sm.enterScope(func)

        val block = NodeBuilder.newCompoundStatement()

        val one =
            fun(): DeclarationStatement {
                val stmt = NodeBuilder.newDeclarationStatement()
                stmt.addDeclaration(
                    NodeBuilder.newVariableDeclaration(
                        "b",
                        TypeParser.createFrom("a", false),
                        null,
                        false
                    )
                )
                return stmt
            }()
        val two =
            fun(): CallExpression {
                val call = NodeBuilder.newCallExpression("a", "a", null, false)
                call.addArgument(
                    NodeBuilder.newDeclaredReferenceExpression("b", UnknownType.getUnknownType())
                )
                return call
            }()

        val amb = AmbiguousExpression(one, two)
        block.addStatement(amb)

        val binOp = NodeBuilder.newBinaryOperator("=")
        binOp.lhs =
            NodeBuilder.newMemberExpression(
                NodeBuilder.newDeclaredReferenceExpression("b", UnknownType.getUnknownType()),
                UnknownType.getUnknownType(),
                "size",
                "."
            )
        binOp.rhs = NodeBuilder.newLiteral(1, TypeParser.createFrom("int", false))
        block.addStatement(binOp)

        func.body = block

        sm.leaveScope(func)
        sm.addDeclaration(func)

        // Pack it into a translation result
        val result = TranslationResult(TranslationManager.builder().build())
        result.addTranslationUnit(tu)

        // Call some passes
        val vur = VariableUsageResolver()
        vur.lang = lang
        vur.accept(result)

        val nodes = result.graph.nodes
        println(nodes)
    }
}
