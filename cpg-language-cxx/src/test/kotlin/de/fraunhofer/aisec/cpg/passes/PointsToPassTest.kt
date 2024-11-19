/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PointsToPassTest {

    @Test
    fun testPointsToPass() {
        val result =
            /*analyze(listOf(Path.of(topLevel.toString(), "calls.cpp").toFile()), topLevel, true) {
                    it.registerLanguage<CPPLanguage>()
                    it.inferenceConfiguration(
                        InferenceConfiguration.builder().inferRecords(false).build()
                    )
                }
            val tu = result.components.flatMap { it.translationUnits }.firstOrNull()
                analyze(File("cpg-language-cxx/src/test/resources/c/pointsto.cpp"))*/
            translate(File("cpg-language-cxx/src/test/resources/c/pointsto.cpp"))
                .components
                .firstOrNull()
                ?.translationUnits
                ?.firstOrNull()
        assertNotNull(result)

        val iDecl =
            result.allChildren<VariableDeclaration> { it.location?.region?.startLine == 4 }.first()
        val literal0 =
            result.allChildren<Literal<*>> { it.location?.region?.startLine == 4 }.first()
        val jDecl =
            result.allChildren<VariableDeclaration> { it.location?.region?.startLine == 5 }.first()
        val literal1 =
            result.allChildren<Literal<*>> { it.location?.region?.startLine == 5 }.first()
        val aDecl =
            result.allChildren<VariableDeclaration> { it.location?.region?.startLine == 6 }.first()

        val iPointerRef =
            result.allChildren<PointerReference> { it.location?.region?.startLine == 6 }.first()

        val iRefLine8 =
            result.allChildren<Reference> { it.location?.region?.startLine == 8 }.first()
        val iRefLine9 =
            result.allChildren<Reference> { it.location?.region?.startLine == 9 }.first()
        val literal2 =
            result.allChildren<Literal<*>> { it.location?.region?.startLine == 9 }.first()
        val iRefLine10 =
            result.allChildren<Reference> { it.location?.region?.startLine == 10 }.first()
        val iRefLine11 =
            result.allChildren<Reference> { it.location?.region?.startLine == 11 }.first()

        val aPointerDerefLine12 =
            result.allChildren<PointerDereference> { it.location?.region?.startLine == 12 }.first()
        val aPointerDerefLine15 =
            result.allChildren<PointerDereference> { it.location?.region?.startLine == 15 }.first()

        assertTrue(iDecl.memoryAddress.name.localName == "i")
        assertTrue(iDecl.memoryValue.size == 1)
        assertTrue(iDecl.memoryValue.first() == literal0)

        assertTrue(jDecl.memoryAddress.name.localName == "j")
        assertTrue(jDecl.memoryValue.size == 1)
        assertTrue(jDecl.memoryValue.first() == literal1)

        assertTrue(aDecl.memoryAddress.name.localName == "a")
        assertTrue(aDecl.memoryValue.size == 1)
        assertTrue(aDecl.memoryValue.first() == iDecl.memoryAddress)

        assertTrue(iPointerRef.memoryAddress.isEmpty())
        assertTrue(iPointerRef.memoryValue.size == 1)
        assertTrue(iPointerRef.memoryValue.first() == iDecl.memoryAddress)

        assertTrue(iRefLine8.memoryAddress.size == 1)
        assertTrue(iRefLine8.memoryAddress.first() == iDecl.memoryAddress)
        assertTrue(iRefLine8.memoryValue.size == 1)
        assertTrue(iRefLine8.memoryValue.first() == literal0)

        assertTrue(iRefLine9.memoryAddress.size == 1)
        assertTrue(iRefLine9.memoryAddress.first() == iDecl.memoryAddress)
        // TODO: fix this assertTrue(iRefLine9.memoryValue.size == 1)
        assertTrue(iRefLine9.memoryValue.filterIsInstance<Literal<*>>().first() == literal2)

        assertTrue(iRefLine10.memoryAddress.size == 1)
        assertTrue(iRefLine10.memoryAddress.first() == iDecl.memoryAddress)
        assertTrue(iRefLine10.memoryValue.size == 1)
        assertTrue(iRefLine10.memoryValue.first() == literal2)

        assertTrue(iRefLine11.memoryAddress.size == 1)
        assertTrue(iRefLine11.memoryAddress.first() == iDecl.memoryAddress)
        // TODO: fix this assertTrue(iRefLine11.memoryValue.size == 1)
        assertTrue(iRefLine11.memoryValue.filterIsInstance<BinaryOperator>().isNotEmpty())

        assertTrue(aPointerDerefLine12.memoryAddress.size == 1)
        assertTrue(aPointerDerefLine12.memoryAddress.first() == iDecl.memoryAddress)
        assertTrue(aPointerDerefLine12.memoryValue.size == 1)
        assertTrue(aPointerDerefLine12.memoryValue.filterIsInstance<BinaryOperator>().isNotEmpty())
    }
}
