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
package de.fraunhofer.aisec.cpg.frontends.experimental.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ReferenceType
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustIntegrationTest : BaseTest() {
    @Test
    fun testKitchenSink() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("kitchen_sink.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // === Module ===
        val utilsMod =
            tu.declarations.filterIsInstance<NamespaceDeclaration>().firstOrNull {
                it.name.localName == "utils"
            }
        assertNotNull(utilsMod, "Module 'utils' should exist")

        // === Structs ===
        val point = tu.records["Point"]
        assertNotNull(point)
        assertEquals("struct", point.kind)
        assertTrue(point.fields.any { it.name.localName == "x" })
        assertTrue(point.fields.any { it.name.localName == "y" })

        // === Enums ===
        val shape = tu.records["Shape"]
        assertNotNull(shape)
        assertEquals("enum", shape.kind)

        // === Traits ===
        val drawable = tu.records["Drawable"]
        assertNotNull(drawable)
        assertEquals("trait", drawable.kind)
        assertNotNull(drawable.methods["draw"])
        assertNotNull(drawable.methods["description"])

        // === Impl ===
        assertTrue(
            point.implementedInterfaces.any { it.name.localName == "Drawable" },
            "Point should implement Drawable",
        )
        assertNotNull(point.methods["draw"])

        // === Generic function with lifetime and trait bounds ===
        val processTemplate =
            tu.declarations.filterIsInstance<FunctionTemplateDeclaration>().firstOrNull {
                it.name.localName == "process"
            }
        assertNotNull(processTemplate, "process should be a template function")
        // Should have lifetime 'a and type parameter T
        assertTrue(processTemplate.parameters.size >= 2)

        // T should have Clone and Drawable bounds
        val tParam =
            processTemplate.parameters.filterIsInstance<TypeParameterDeclaration>().firstOrNull {
                it.name.localName == "T"
            }
        assertNotNull(tParam)
        assertTrue(tParam.type.superTypes.any { it.name.localName == "Clone" })
        assertTrue(tParam.type.superTypes.any { it.name.localName == "Drawable" })

        // === Async ===
        val fetchData = tu.functions["fetch_data"]
        assertNotNull(fetchData)
        assertTrue(fetchData.annotations.any { it.name.localName == "Async" })

        // === Control flow ===
        val controlFlow = tu.functions["control_flow"]
        assertNotNull(controlFlow)
        val body = controlFlow.body as? Block
        assertNotNull(body)

        // Should contain if statement, loop, and match
        val ifStmt = body.allChildren<IfStatement>()
        assertTrue(ifStmt.isNotEmpty(), "Should have if statements")

        val matchStmt = body.allChildren<SwitchStatement>()
        assertTrue(matchStmt.isNotEmpty(), "Should have match/switch statements")

        val labelStmt = body.allChildren<LabelStatement>()
        assertTrue(labelStmt.isNotEmpty(), "Should have labeled loops")
        assertTrue(labelStmt.any { it.label == "outer" })

        // mut counter should be mutable
        val counterVar =
            body.allChildren<VariableDeclaration>().firstOrNull { it.name.localName == "counter" }
        assertNotNull(counterVar)
        assertTrue(counterVar.annotations.any { it.name.localName == "mut" })

        // === Ownership ===
        val ownership = tu.functions["ownership"]
        assertNotNull(ownership)
        val dataParam = ownership.parameters.firstOrNull { it.name.localName == "data" }
        assertNotNull(dataParam)
        assertTrue(dataParam.type is ReferenceType)

        // === Macros ===
        val useMacros = tu.functions["use_macros"]
        assertNotNull(useMacros)
        val macroCalls = useMacros.allChildren<CallExpression>()
        assertTrue(macroCalls.any { it.name.localName == "println" })

        // === Type alias ===
        // Coordinate type alias should exist (added to scope)

        // === Derive attribute ===
        val config = tu.records["Config"]
        assertNotNull(config)
        assertTrue(
            config.annotations.any { it.name.localName == "derive(Clone)" },
            "Config should have derive(Clone) annotation",
        )

        // === For-in loops ===
        val forLoopDemo = tu.functions["for_loop_demo"]
        assertNotNull(forLoopDemo)
        val forBody = forLoopDemo.body as? Block
        assertNotNull(forBody)
        val forEachStmts = forBody.allChildren<ForEachStatement>()
        assertTrue(forEachStmts.isNotEmpty(), "Should have for-each statements")

        // === References and closures ===
        val closuresAndRefs = tu.functions["closures_and_refs"]
        assertNotNull(closuresAndRefs)
        val closureBody = closuresAndRefs.body as? Block
        assertNotNull(closureBody)
        val refOps = closureBody.allChildren<UnaryOperator>().filter { it.operatorCode == "&" }
        assertTrue(refOps.isNotEmpty(), "Should have & reference operators")
        val lambdas = closureBody.allChildren<LambdaExpression>()
        assertTrue(lambdas.isNotEmpty(), "Should have closures")

        // === Struct expressions ===
        val structDemo = tu.functions["struct_demo"]
        assertNotNull(structDemo)
        val structBody = structDemo.body as? Block
        assertNotNull(structBody)
        val constructs = structBody.allChildren<ConstructExpression>()
        assertTrue(constructs.isNotEmpty(), "Should have struct construction")

        // === Constants and statics ===
        val piConst =
            tu.allChildren<VariableDeclaration>().firstOrNull { it.name.localName == "PI" }
        assertNotNull(piConst, "Should have const PI")
        assertTrue(
            piConst.annotations.any { it.name.localName == "const" },
            "PI should have @const",
        )

        val countStatic =
            tu.allChildren<VariableDeclaration>().firstOrNull { it.name.localName == "COUNT" }
        assertNotNull(countStatic, "Should have static COUNT")
        assertTrue(
            countStatic.annotations.any { it.name.localName == "static" },
            "COUNT should have @static",
        )

        // === Indexing and casting ===
        val indexAndCast = tu.functions["index_and_cast"]
        assertNotNull(indexAndCast)
        val indexBody = indexAndCast.body as? Block
        assertNotNull(indexBody)
        val subscripts = indexBody.allChildren<SubscriptExpression>()
        assertTrue(subscripts.isNotEmpty(), "Should have subscript expressions")
        val casts = indexBody.allChildren<CastExpression>()
        assertTrue(casts.isNotEmpty(), "Should have cast expressions")
    }
}
