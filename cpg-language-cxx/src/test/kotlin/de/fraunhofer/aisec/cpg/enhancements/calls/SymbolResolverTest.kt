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
package de.fraunhofer.aisec.cpg.enhancements.calls

import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.TestUtils.assertInvokes
import de.fraunhofer.aisec.cpg.TestUtils.assertRefersTo
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class SymbolResolverTest {
    @Test
    fun testCCallWithImplicitCast() {
        val file = File("src/test/resources/c/implicit_casts.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }

        val f = tu.functions["f"]
        assertNotNull(f)
        assertFalse(f.isImplicit)

        val fCall = tu.calls["f"]
        assertNotNull(fCall)
        assertInvokes(fCall, f)
    }

    @Test
    fun testCPPCallWithImplicitCast() {
        val file = File("src/test/resources/cxx/implicit_casts.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }

        val f = tu.functions["f"]
        assertNotNull(f)
        assertFalse(f.isImplicit)

        val fCall = tu.calls["f"]
        assertNotNull(fCall)
        assertInvokes(fCall, f)

        val gChar = tu.functions[{ it.signature == "g(char)void" }]
        assertNotNull(gChar)
        val gInt = tu.functions[{ it.signature == "g(int)void" }]
        assertNotNull(gInt)
        val gFloat = tu.functions[{ it.signature == "g(float)void" }]
        assertNotNull(gFloat)

        val gCall = tu.calls["g"]
        assertNotNull(gCall)
        assertInvokes(gCall, gChar)

        val hBase = tu.functions[{ it.signature == "h(Base*)void" }]
        assertNotNull(hBase)
        val hOne = tu.functions[{ it.signature == "h(One*)void" }]
        assertNotNull(hOne)

        val hCall = tu.calls["h"]
        assertNotNull(hCall)
        assertInvokes(hCall, hOne)
    }

    @Test
    fun testCPPMethodDefinitionOutsideOfClass() {
        val file = File("src/test/resources/cxx/outside_class_definition.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        val field = tu.fields["field"]
        assertNotNull(field)
        assertFalse(field.isInferred)

        val fieldRef = tu.refs["field"]
        assertNotNull(fieldRef)
        assertRefersTo(fieldRef, field)
    }
}
