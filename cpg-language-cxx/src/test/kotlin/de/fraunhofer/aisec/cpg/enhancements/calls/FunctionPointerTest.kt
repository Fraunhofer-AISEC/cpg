/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Construct
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import java.util.regex.Pattern
import kotlin.test.*

internal class FunctionPointerTest : BaseTest() {
    @Throws(Exception::class)
    private fun analyze(
        language: String,
        configModifier: Consumer<TranslationConfiguration.Builder>? = null,
    ): TranslationResult {
        val topLevel = Path.of("src", "test", "resources", "functionPointers")

        return analyze(language, topLevel, true, configModifier)
    }

    @Throws(Exception::class)
    fun test(language: String, configModifier: Consumer<TranslationConfiguration.Builder>? = null) {
        val result = analyze(language, configModifier)
        val functions = result.functions
        val main = functions["main", SearchModifier.UNIQUE]
        val calls = main.calls
        val noParam =
            functions[
                { it.name.localName == "target" && it.parameters.isEmpty() }, SearchModifier.UNIQUE]
        findByUniquePredicate(functions) {
            it.name.localName == "target" && it.parameters.isEmpty()
        }
        val singleParam =
            findByUniquePredicate(functions) {
                it.name.localName == "target" && it.parameters.size == 1
            }
        val noParamUnknown =
            findByUniquePredicate(functions) {
                it.name.localName == "fun" && it.parameters.isEmpty()
            }
        val singleParamUnknown =
            findByUniquePredicate(functions) {
                it.name.localName == "fun" && it.parameters.size == 1
            }
        val pattern = Pattern.compile("\\((?<member>.+)?\\*(?<obj>.+(\\.|::))?(?<func>.+)\\)")
        for (call in calls) {
            if (call is Construct) {
                continue
            }

            val callee = call.callee

            // check for class function pointers
            val callName =
                if (callee is BinaryOperator) {
                    callee.rhs.name.localName
                } else {
                    call.name.localName
                }

            var func: String
            if (!callName.contains("(")) {
                func = callName
                assertNotEquals("", func, "Unexpected call $func")
            } else {
                val matcher = pattern.matcher(callName)
                assertTrue(matcher.matches(), "Unexpected call $callName")
                func = matcher.group("func")
            }
            when (func) {
                "no_param",
                "no_param_uninitialized",
                "no_param_field",
                "no_param_field_uninitialized" -> {
                    assertEquals(listOf(noParam), call.invokes)
                }
                "single_param",
                "single_param_uninitialized",
                "single_param_field",
                "single_param_field_uninitialized" ->
                    assertEquals(listOf(singleParam), call.invokes)
                "no_param_unknown",
                "no_param_unknown_uninitialized",
                "no_param_unknown_field",
                "no_param_unknown_field_uninitialized" -> {
                    assertEquals(listOf(noParamUnknown), call.invokes)
                    assertTrue(noParamUnknown.isInferred)
                }
                "single_param_unknown",
                "single_param_unknown_uninitialized",
                "single_param_unknown_field",
                "single_param_unknown_field_uninitialized" -> {
                    assertEquals(listOf(singleParamUnknown), call.invokes)
                    assertTrue(singleParamUnknown.isInferred)
                }
                else -> fail("Unexpected call $callName")
            }
            val variables = result.variables

            for (variable in variables) {
                when (variable.name.localName) {
                    "no_param_unused",
                    "no_param_unused_field",
                    "no_param_unused_uninitialized" ->
                        assertEquals(noParam, getSourceFunction(variable))
                    "single_param_unused",
                    "single_param_unused_field",
                    "single_param_unused_field_uninitialized" ->
                        assertEquals(singleParam, getSourceFunction(variable))
                }
            }
        }
    }

    private fun getSourceFunction(variable: Variable): Function {
        val functions: MutableList<Function> = ArrayList()
        val worklist: Deque<Node> = ArrayDeque()
        val seen = Collections.newSetFromMap(IdentityHashMap<Node, Boolean>())
        worklist.push(variable)
        while (!worklist.isEmpty()) {
            val curr = worklist.pop()
            if (!seen.add(curr)) {
                continue
            }
            if (curr is Function) {
                functions.add(curr)
            } else {
                curr.prevDFG.forEach(Consumer { e: Node -> worklist.push(e) })
            }
        }
        if (functions.size == 0) {
            variable.usageEdges
                .filter { it.access == AccessValues.WRITE }
                .forEach { worklist.push(it.end) }
            while (!worklist.isEmpty()) {
                val curr = worklist.pop()
                if (!seen.add(curr)) {
                    continue
                }
                if (curr is Function) {
                    functions.add(curr)
                } else {
                    curr.prevDFG.forEach(Consumer { e: Node -> worklist.push(e) })
                }
            }
        }

        assertEquals(1, functions.size)
        return functions[0]
    }

    @Test
    @Throws(Exception::class)
    fun testC() {
        test("c") { it.registerLanguage<CLanguage>() }
    }

    @Test
    @Throws(Exception::class)
    fun testCPP() {
        test("cpp") { it.registerLanguage<CPPLanguage>() }
    }
}
