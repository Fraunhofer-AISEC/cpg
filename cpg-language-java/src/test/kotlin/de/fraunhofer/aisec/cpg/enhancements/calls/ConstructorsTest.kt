/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.enhancements.calls

import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.graph.allDescendants
import de.fraunhofer.aisec.cpg.graph.allVariables
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.*

internal class ConstructorsTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "constructors")

    @Test
    @Throws(Exception::class)
    fun testJava() {
        val result = analyze("java", topLevel, true) { it.registerLanguage<JavaLanguage>() }
        val constructors = result.allDescendants<ConstructorDeclaration>()
        val noArg = findByUniquePredicate(constructors) { it.parameters.isEmpty() }
        val singleArg = findByUniquePredicate(constructors) { it.parameters.size == 1 }
        val twoArgs = findByUniquePredicate(constructors) { it.parameters.size == 2 }
        val variables = result.allVariables
        val a1 = findByUniqueName(variables, "a1")
        assertNotNull(a1)
        assertTrue(a1.initializer is NewExpression)
        assertTrue((a1.initializer as? NewExpression)?.initializer is ConstructExpression)

        val a1Initializer = (a1.initializer as NewExpression).initializer as ConstructExpression
        assertEquals(noArg, a1Initializer.constructor)

        val a2 = findByUniqueName(variables, "a2")
        assertNotNull(a2)
        assertTrue(a2.initializer is NewExpression)
        assertTrue((a2.initializer as? NewExpression)?.initializer is ConstructExpression)

        val a2Initializer = (a2.initializer as NewExpression).initializer as ConstructExpression
        assertEquals(singleArg, a2Initializer.constructor)

        val a3 = findByUniqueName(variables, "a3")
        assertNotNull(a3)
        assertTrue(a3.initializer is NewExpression)
        assertTrue((a3.initializer as NewExpression).initializer is ConstructExpression)

        val a3Initializer = (a3.initializer as NewExpression).initializer as ConstructExpression
        assertEquals(twoArgs, a3Initializer.constructor)

        val a4 = findByUniqueName(variables, "a4")
        assertNotNull(a4)
        assertNull(a4.initializer)
    }
}
