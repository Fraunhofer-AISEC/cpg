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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.ForEach
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.ListType
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnnotationTypeTest {
    private val topLevel = java.nio.file.Path.of("src/test/resources/annotation_type")

    @Test
    fun testContainerAnnotationPropagatesElementType() {
        java.nio.file.Files.createDirectories(topLevel)
        val srcFile = topLevel.resolve("test.py").toFile()
        srcFile.writeText(
            """
            class Bar:
                def is_closed(self):
                    return True

            class Foo:
                def __init__(self):
                    self.items: list[Bar] = []

                def do_thing(self):
                    for item in list(self.items):
                        item.is_closed()
            """
                .trimIndent()
        )

        val tu =
            analyzeAndGetFirstTU(listOf(srcFile), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        val foo = tu.records["Foo"]
        assertNotNull(foo, "Foo record should exist")
        val field = foo.fields.find { it.name.localName == "items" }
        assertNotNull(field, "field 'items' should exist")
        println("field type: ${field.type}")
        println("field type class: ${field.type::class.simpleName}")
        println("field assignedTypes: ${field.assignedTypes}")
        for (at in field.assignedTypes) {
            println("  assignedType: $at class=${at::class.simpleName} name=${at.name}")
            if (at is ListType) {
                println("    elementType: ${at.elementType} name=${at.elementType.name}")
            }
        }
        assertTrue(
            field.assignedTypes.any { it is ListType },
            "field should have a ListType in assignedTypes, got: ${field.assignedTypes}",
        )

        val listType =
            field.assignedTypes.filterIsInstance<ListType>().find {
                it.elementType.name.localName == "Bar"
            }
        assertNotNull(
            listType,
            "field should have a ListType with elementType Bar, got: ${field.assignedTypes}",
        )
        println("listType elementType: ${listType.elementType}")

        // Check the loop variable
        val doThing = foo.methods.find { it.name.localName == "do_thing" }
        assertNotNull(doThing)
        val forEach = doThing?.body?.allChildren<ForEach>()?.firstOrNull()
        assertNotNull(forEach)
        val loopVar = forEach?.variable as? Reference
        assertNotNull(loopVar)
        println("loopVar type: ${loopVar.type}")
        println("loopVar assignedTypes: ${loopVar.assignedTypes}")
        assertTrue(
            loopVar.assignedTypes.any { it.name.localName == "Bar" },
            "loop variable should have Bar in assignedTypes, got: ${loopVar.assignedTypes}",
        )
    }
}
