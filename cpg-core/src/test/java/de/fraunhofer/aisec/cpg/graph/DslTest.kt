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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TestUtils.assertRefersTo
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DslTest {
    @Test
    fun test() {
        val scopeManager = ScopeManager()
        val tu =
            TestLanguageFrontend(scopeManager).build {
                translationUnit("file.cpp") {
                    function("main") {
                        body {
                            declare { variable("a") { literal(1) } }
                            returnStmt { ref("a") + literal(2) }
                        }
                    }
                }
            }

        // Let's assert that we did this correctly
        val main = tu.functions["main"]
        assertNotNull(main)
        assertNotNull(main.scope)

        val body = main.body as? CompoundStatement
        assertNotNull(body)
        assertNotNull(body.scope)

        val declarationStatement = main.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(declarationStatement)
        assertNotNull(declarationStatement.scope)

        val variable = declarationStatement.singleDeclaration as? VariableDeclaration
        assertNotNull(variable)
        assertNotNull(variable.scope)
        assertLocalName("a", variable)

        val lit1 = variable.initializer as? Literal<*>
        assertNotNull(lit1)
        assertNotNull(lit1.scope)
        assertEquals(1, lit1.value)

        val returnStatement = main.bodyOrNull<ReturnStatement>(0)
        assertNotNull(returnStatement)
        assertNotNull(returnStatement.scope)

        val binOp = returnStatement.returnValue as? BinaryOperator
        assertNotNull(binOp)
        assertNotNull(binOp.scope)
        assertEquals("+", binOp.operatorCode)

        val ref = binOp.lhs as? DeclaredReferenceExpression
        assertNotNull(ref)
        assertNotNull(ref.scope)
        assertNull(ref.refersTo)
        assertLocalName("a", ref)

        val lit2 = binOp.rhs as? Literal<*>
        assertNotNull(lit2)
        assertNotNull(lit2.scope)
        assertEquals(2, lit2.value)

        val result = TranslationResult(TranslationManager.builder().build(), scopeManager)
        result.addTranslationUnit(tu)
        VariableUsageResolver().accept(result)

        // Now the reference should be resolved
        assertRefersTo(ref, variable)
    }
}
