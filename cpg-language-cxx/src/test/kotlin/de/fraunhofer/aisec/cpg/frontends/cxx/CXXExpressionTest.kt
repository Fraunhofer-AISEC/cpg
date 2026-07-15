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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.expressions.Construction
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.New
import de.fraunhofer.aisec.cpg.graph.expressions.PointerDereference
import de.fraunhofer.aisec.cpg.graph.expressions.PointerReference
import de.fraunhofer.aisec.cpg.graph.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CXXExpressionTest {
    @Test
    fun testExplicitTypeConversion() {
        val file = File("src/test/resources/cxx/explicit_type_conversion.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        // We should have two calls (int and myint64)
        val casts = tu.casts
        assertEquals(2, casts.size)
        assertEquals(listOf("int", "long long int"), casts.map { it.name.localName })

        val cast = tu.casts.firstOrNull()
        assertNotNull(cast)
        assertEquals(cast, cast.expression.astParent)
    }

    @Test
    fun testNewWithTemplateAndInitializerList() {
        val file = File("src/test/resources/cxx/new_initializer_list.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        // Template brace-init: `new Foo<int>{42}` – initializer must be a Construction
        val f = tu.variables["f"]
        assertNotNull(f)

        val fNew = f.initializer
        assertIs<New>(fNew)

        val fConstruct = fNew.initializer
        assertIs<Construction>(fConstruct)
        assertEquals(1, fConstruct.arguments.size)
        val fArg = fConstruct.arguments[0]
        assertIs<Literal<*>>(fArg)
        assertEquals(42, (fArg.value as Number).toInt())

        // Non-template brace-init: `new Bar{7}` – initializer must also be a Construction
        val g = tu.variables["g"]
        assertNotNull(g)

        val gNew = g.initializer
        assertIs<New>(gNew)

        val gConstruct = gNew.initializer
        assertIs<Construction>(gConstruct)
        assertEquals(1, gConstruct.arguments.size)
        val gArg = gConstruct.arguments[0]
        assertIs<Literal<*>>(gArg)
        assertEquals(7, (gArg.value as Number).toInt())
    }

    /**
     * Regression: `*_p++ = _c` (classic C idiom, e.g. Apple's `__sputc` in `_stdio.h`) used to make
     * the outer [PointerDereference] inherit the name of the inner postfix `++` [UnaryOperator], so
     * its name ended up as literally `"++"`. The [SymbolResolver] then treated that as a global
     * reference, couldn't find a declaration and inferred a phantom [Variable] named `++`.
     *
     * The fix in `ExpressionHandler.handleUnaryExpression` derives the deref/addr-of name only when
     * the operand is itself a [Reference] and passes null otherwise, and reuses the already handled
     * sub-expression instead of running the handler a second time.
     */
    @Test
    fun testUnaryPointerDoesNotLeakOperatorName() {
        val file = File("src/test/resources/c/deref_postincrement.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        // The bug produced a global Variable declaration named "++". The fix must eliminate it.
        val bogus = tu.variables { it.name.localName == "++" }
        assertTrue(
            bogus.isEmpty(),
            "no Variable should be named '++', but found ${bogus.size}: " +
                bogus.joinToString { "${it.name} @ ${it.location} (inferred=${it.isInferred})" },
        )

        // The RHS of the assignment inside `deref_postinc` is `*_p++`. Its shape must be a
        // PointerDereference wrapping the postfix ++ UnaryOperator, and the deref must not carry
        // the operator's name.
        val derefFn = tu.functions["deref_postinc"]
        assertNotNull(derefFn)
        val assign = derefFn.allChildren<Assign>().singleOrNull()
        assertNotNull(assign, "expected exactly one assignment in deref_postinc")
        val deref = assign.lhs.singleOrNull()
        assertIs<PointerDereference>(deref)
        assertFalse(
            deref.name.localName == "++",
            "PointerDereference should not have inherited the '++' name from its operand",
        )
        val inc = deref.input
        assertIs<UnaryOperator>(inc)
        assertEquals("++", inc.operatorCode)
        assertTrue(inc.isPostfix)

        // Sanity: plain `&x` on a simple Reference still gets a meaningful name (there is no
        // reason to lose precision when the operand *is* a Reference).
        val addrFn = tu.functions["addr_of_local"]
        assertNotNull(addrFn)
        val addr = addrFn.allChildren<PointerReference>().singleOrNull()
        assertNotNull(addr, "expected a PointerReference in addr_of_local")
        assertEquals("x", addr.name.localName)
    }
}
