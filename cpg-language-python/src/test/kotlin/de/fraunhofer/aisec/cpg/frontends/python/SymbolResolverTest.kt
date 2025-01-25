/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertRefersTo
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SymbolResolverTest {
    @Test
    fun testFields() {
        val topLevel = File("src/test/resources/python/fields.py")
        val result =
            analyze(listOf(topLevel), topLevel.toPath(), true) {
                it.registerLanguage<PythonLanguage>()
            }

        val a =
            result.namespaces["fields"]
                .variables[{ it.name.localName == "a" && it !is FieldDeclaration }]
        assertNotNull(a)

        val fieldA = result.records["MyClass"]?.fields["a"]
        assertNotNull(fieldA)

        val aRefs = result.refs("a")
        aRefs.filterIsInstance<MemberExpression>().forEach { assertRefersTo(it, fieldA) }
        aRefs.filter { it !is MemberExpression }.forEach { assertRefersTo(it, a) }

        val osRefs = result.refs("os")
        assertEquals(1, osRefs.size)

        val osNameRefs = result.refs("os.name")
        assertEquals(1, osNameRefs.size)
    }
}
