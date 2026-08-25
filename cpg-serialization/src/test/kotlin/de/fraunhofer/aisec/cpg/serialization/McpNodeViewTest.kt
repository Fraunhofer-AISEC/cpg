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
package de.fraunhofer.aisec.cpg.serialization

import de.fraunhofer.aisec.cpg.assumptions.Assumption
import de.fraunhofer.aisec.cpg.assumptions.AssumptionType
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.persistence.McpDetailLevel
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class McpNodeViewTest {
    private fun testFunction(): Function {
        val function = Function()
        function.name = Name("hello")
        function.code = "void hello() { printf(\"hi\"); }"
        function.comment = "greets the world"
        function.location = PhysicalLocation(URI.create("file:///tmp/hello.c"), Region(1, 1, 1, 30))
        return function
    }

    @Test
    fun summaryOmitsCodeAndComment() {
        val view = testFunction().toMcpView(McpDetailLevel.SUMMARY)

        assertEquals(
            "hello",
            view["name"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content },
        )
        assertNull(view["code"])
        assertNull(view["comment"])

        // McpLocationConverter is a composite converter, so its keys are merged directly into the
        // top-level view (matching the flattening behavior of the Neo4j `properties()` export)
        // instead of being nested under a "location" key.
        assertEquals(
            "hello.c",
            view["fileName"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content },
        )
        assertEquals(
            "1",
            view["startLine"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content },
        )
    }

    @Test
    fun fullIncludesCodeAndComment() {
        val view = testFunction().toMcpView(McpDetailLevel.FULL)

        assertEquals(
            "void hello() { printf(\"hi\"); }",
            (view["code"] as kotlinx.serialization.json.JsonPrimitive).content,
        )
        assertEquals(
            "greets the world",
            (view["comment"] as kotlinx.serialization.json.JsonPrimitive).content,
        )
    }

    @Test
    fun overriddenLocationPropertyIsFlattenedToo() {
        // Assumption re-declares `location` (via its own `assumptionLocation`) rather than
        // inheriting Node's, so it needs its own @McpConvert - this verifies that annotation is
        // actually picked up, since Kotlin reflection does not inherit annotations across
        // overridden properties.
        val assumption =
            Assumption(
                assumptionType = AssumptionType.InferenceAssumption,
                message = "this is an assumption",
                assumptionLocation =
                    PhysicalLocation(URI.create("file:///tmp/assumed.c"), Region(2, 1, 2, 10)),
            )

        val view = assumption.toMcpView(McpDetailLevel.SUMMARY)

        assertEquals(
            "assumed.c",
            view["fileName"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content },
        )
        assertEquals(
            "2",
            view["startLine"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content },
        )
    }
}
