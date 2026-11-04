/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.Cast
import de.fraunhofer.aisec.cpg.graph.expressions.PointerDereference
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.*

class StdlibProfileTest : BaseTest() {
    private fun parse(name: String): TranslationUnit {
        val file = File("src/test/resources/c/$name")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)
        return tu
    }

    /**
     * Checks the two `va_arg` uses of the test files: they are casts of the `va_list` `ap` to the
     * requested type. The `va_list` stays the operand, so that data flows from it into the result.
     */
    private fun assertVaArgCasts(tu: TranslationUnit) {
        assertEquals(emptyList(), tu.problems)

        val next = tu.functions["next"]
        assertNotNull(next)
        val ap = next.parameters["ap"]
        assertNotNull(ap)

        for ((variable, type) in listOf("v" to "int", "s" to "char*")) {
            val init = next.variables[variable]?.initializer
            assertIs<Cast>(init)
            assertEquals(type, init.castType.name.toString())
            assertEquals(type, init.type.name.toString())

            val operand = init.expression
            assertIs<Reference>(operand)
            assertEquals(ap, operand.refersTo)
            assertContains(init.prevDFG, operand)
        }
    }

    @Test
    fun testVaArg() {
        // Without <stdarg.h>, va_arg(ap, int) is not an expression the parser understands, because
        // a type is not a valid argument. The profile defines it like the header would.
        assertVaArgCasts(parse("va_arg.c"))
    }

    @Test
    fun testBuiltinVaArg() {
        // CDT knows __builtin_va_arg itself, but expands it to a dereference of a pointer cast to
        // a `typeof` of the type. We want the type itself.
        assertVaArgCasts(parse("va_arg_builtin.c"))
    }

    @Test
    fun testVaArgDefinedBySource() {
        // If <stdarg.h> is parsed (or the source defines the macro itself), it defines the same
        // macro as the profile, which is harmless.
        assertVaArgCasts(parse("va_arg_redefined.c"))
    }

    @Test
    fun testVaArgForms() {
        // The forms that occur in real code, e.g., in the sqlite3 amalgamation: the result of a
        // va_arg is assigned to, and the type is a typedef or consists of several words.
        val tu = parse("va_arg_forms.c")
        assertEquals(emptyList(), tu.problems)

        val forms = tu.functions["forms"]
        assertNotNull(forms)
        val ap = forms.parameters["ap"]

        val store = forms.assigns.firstOrNull()
        assertNotNull(store)
        val target = store.lhs.single()
        assertIs<PointerDereference>(target)
        val storeCast = target.input
        assertIs<Cast>(storeCast)
        assertEquals("int*", storeCast.castType.name.toString())
        assertEquals(ap, (storeCast.expression as? Reference)?.refersTo)

        // the type of a typedef is resolved
        for ((variable, type) in
            listOf("big" to "long long int", "n" to "unsigned long int", "d" to "double")) {
            val init = forms.variables[variable]?.initializer
            assertIs<Cast>(init)
            assertEquals(type, init.castType.name.toString(), "type of $variable")
            assertEquals(ap, (init.expression as? Reference)?.refersTo)
        }
    }

    @Test
    fun testTypeofDereferenceIsNoVaArg() {
        val tu = parse("va_arg_typeof.c")
        assertEquals(emptyList(), tu.problems)

        val deref = tu.functions["deref"]
        assertNotNull(deref)
        val init = deref.variables["v"]?.initializer
        assertIs<PointerDereference>(init)
        assertIs<Cast>(init.input)
    }
}
