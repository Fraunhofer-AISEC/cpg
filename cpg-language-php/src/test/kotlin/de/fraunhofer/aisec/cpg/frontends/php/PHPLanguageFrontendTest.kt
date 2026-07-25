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
package de.fraunhofer.aisec.cpg.frontends.php

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PHPLanguageFrontendTest {

    // ── Functions ─────────────────────────────────────────────────────────────

    /** Verifies parsing of PHP function declarations and their parameters. */
    @Test
    fun testFunctionDeclarations() {
        val topLevel = Path.of("src", "test", "resources", "php")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("functions.php").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<PHPLanguage>()
            }
        assertNotNull(tu)

        val greet = tu.functions["greet"]
        assertNotNull(greet, "function 'greet' should exist")
        assertEquals(1, greet.parameters.size)
        assertLocalName("name", greet.parameters[0])

        val sum = tu.functions["sum"]
        assertNotNull(sum, "function 'sum' should exist")
        assertEquals(2, sum.parameters.size)
        assertLocalName("a", sum.parameters[0])
        assertLocalName("b", sum.parameters[1])
        assertNotNull(sum.parameters[1].default, "'b' should have a default value")

        val joinAll = tu.functions["joinAll"]
        assertNotNull(joinAll, "function 'joinAll' should exist")
        assertLocalName("parts", joinAll.parameters[0])
        assertTrue(joinAll.parameters[0].isVariadic, "'parts' should be variadic")

        val multiply = tu.functions["multiply"]
        assertNotNull(multiply, "function 'multiply' should exist")
        assertEquals(2, multiply.parameters.size)
        assertLocalName("left", multiply.parameters[0])
        assertLocalName("right", multiply.parameters[1])
        assertNotNull(multiply.parameters[1].default, "'right' should have a default value")
    }

    // ── Classes ───────────────────────────────────────────────────────────────

    /** Verifies parsing of namespaced PHP classes and their methods. */
    @Test
    fun testClassDeclaration() {
        val topLevel = Path.of("src", "test", "resources", "php")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("classes.php").toFile()), topLevel, true) {
                it.registerLanguage<PHPLanguage>()
            }
        assertNotNull(tu)

        // The class lives inside the 'App\Service' namespace
        val ns =
            tu.declarations
                .filterIsInstance<de.fraunhofer.aisec.cpg.graph.declarations.Namespace>()
                .firstOrNull()
        assertNotNull(ns, "namespace should exist")

        val counter =
            ns.declarations.filterIsInstance<Record>().firstOrNull {
                it.name.localName == "Counter"
            }
        assertNotNull(counter, "class 'Counter' should exist")

        // methods
        val ctor = counter.methods.firstOrNull { it.name.localName == "__construct" }
        assertNotNull(ctor, "constructor should exist")
        assertEquals(1, ctor.parameters.size)
        assertLocalName("initial", ctor.parameters[0])

        val increment = counter.methods.firstOrNull { it.name.localName == "increment" }
        assertNotNull(increment, "method 'increment' should exist")

        val get = counter.methods.firstOrNull { it.name.localName == "get" }
        assertNotNull(get, "method 'get' should exist")

        val gauge =
            ns.declarations.filterIsInstance<Record>().firstOrNull { it.name.localName == "Gauge" }
        assertNotNull(gauge, "class 'Gauge' should exist")

        val gaugeCtor = gauge.methods.firstOrNull { it.name.localName == "__construct" }
        assertNotNull(gaugeCtor, "Gauge constructor should exist")
        assertEquals(1, gaugeCtor.parameters.size)
        assertLocalName("initial", gaugeCtor.parameters[0])

        val read = gauge.methods.firstOrNull { it.name.localName == "read" }
        assertNotNull(read, "method 'read' should exist")
    }

    // ── Statements ────────────────────────────────────────────────────────────

    /** Verifies parsing of representative PHP control-flow statements. */
    @Test
    fun testStatements() {
        val topLevel = Path.of("src", "test", "resources", "php")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("statements.php").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<PHPLanguage>()
            }
        assertNotNull(tu)

        val classify = tu.functions["classify"]
        assertNotNull(classify, "function 'classify' should exist")
        // Body should be a block
        val classifyBody = classify.body
        assertNotNull(classifyBody, "classify body should not be null")

        val sumUntil = tu.functions["sumUntil"]
        assertNotNull(sumUntil, "function 'sumUntil' should exist")

        val sumArray = tu.functions["sumArray"]
        assertNotNull(sumArray, "function 'sumArray' should exist")
        val sumArrayLoop = sumArray.forEachLoops.firstOrNull()
        assertNotNull(sumArrayLoop, "sumArray should contain a foreach loop")
        val sumArrayVariable = sumArrayLoop.variable as? DeclarationStatement
        assertNotNull(sumArrayVariable, "simple foreach variable should be a declaration")
        assertLocalName("v", sumArrayVariable.singleDeclaration)

        val sumByKey = tu.functions["sumByKey"]
        assertNotNull(sumByKey, "function 'sumByKey' should exist")
        val sumByKeyLoop = sumByKey.forEachLoops.firstOrNull()
        assertNotNull(sumByKeyLoop, "sumByKey should contain a foreach loop")
        val keyValueVariables = sumByKeyLoop.variable as? ExpressionList
        assertNotNull(keyValueVariables, "key/value foreach should expose two loop variables")
        assertEquals(2, keyValueVariables.expressions.size)
        val keyDeclaration = keyValueVariables.expressions[0] as? DeclarationStatement
        assertNotNull(keyDeclaration, "key variable should be declared")
        assertLocalName("k", keyDeclaration.singleDeclaration)
        val valueDeclaration = keyValueVariables.expressions[1] as? DeclarationStatement
        assertNotNull(valueDeclaration, "value variable should be declared")
        assertLocalName("v", valueDeclaration.singleDeclaration)

        val overwriteWithLoop = tu.functions["overwriteWithLoop"]
        assertNotNull(overwriteWithLoop, "function 'overwriteWithLoop' should exist")
        val overwriteLoop = overwriteWithLoop.forEachLoops.firstOrNull()
        assertNotNull(overwriteLoop, "overwriteWithLoop should contain a foreach loop")
        assertTrue(
            overwriteLoop.variable !is DeclarationStatement,
            "complex foreach assignable should not create a declaration",
        )

        val risky = tu.functions["riskyOperation"]
        assertNotNull(risky, "function 'riskyOperation' should exist")
    }
}
