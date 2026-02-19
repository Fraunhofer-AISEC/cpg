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
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustTraitsTest : BaseTest() {
    @Test
    fun testTraits() {
        val topLevel = Path.of("src", "test", "resources", "rust", "traits")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("traits.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val myTrait = tu.records["MyTrait"]
        assertNotNull(myTrait)
        assertEquals("trait", myTrait.kind)

        val myTraitType = myTrait.toType()
        val requiredMethod = myTraitType.methods["required_method"]
        assertNotNull(requiredMethod)
        assertFalse(requiredMethod.hasBody())

        val defaultMethod = myTraitType.methods["default_method"]
        assertNotNull(defaultMethod)
        assertTrue(defaultMethod.hasBody())

        val myStruct = tu.records["MyStruct"]
        assertNotNull(myStruct)

        // Check implementation
        val myStructType = myStruct.toType()
        val implMethod = myStructType.methods["required_method"]
        assertNotNull(implMethod)
        assertEquals(myStruct, implMethod.recordDeclaration)
        assertTrue(implMethod.hasBody())

        val genericFooTemplate =
            tu.declarations.filterIsInstance<FunctionTemplateDeclaration>().first {
                it.name.localName == "generic_foo"
            }
        val tParam = genericFooTemplate.parameters.getOrNull(0) as? TypeParameterDeclaration
        assertNotNull(tParam)
        assertEquals("T", tParam.name.localName)

        // Check bounds on the ParameterizedType
        val tType = tParam.type
        assertTrue(tType.superTypes.any { it.name.localName == "Clone" })
        assertTrue(tType.superTypes.any { it.name.localName == "MyTrait" })
    }

    @Test
    fun testAssociatedTypes() {
        val topLevel = Path.of("src", "test", "resources", "rust", "traits")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("traits.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // The Iterator trait should parse successfully with an associated type
        val iteratorTrait = tu.records["Iterator"]
        assertNotNull(iteratorTrait)
        assertEquals("trait", iteratorTrait.kind)

        // Iterator should have a "next" method signature
        val iteratorType = iteratorTrait.toType()
        val nextMethod = iteratorType.methods["next"]
        assertNotNull(nextMethod)

        // The Counter struct should implement Iterator
        val counter = tu.records["Counter"]
        assertNotNull(counter)
        assertTrue(counter.implementedInterfaces.any { it.name.localName == "Iterator" })

        // Counter should have a "next" method implementation
        val counterType = counter.toType()
        val counterNext = counterType.methods["next"]
        assertNotNull(counterNext)
        assertTrue(counterNext.hasBody())
    }

    @Test
    fun testBranchTraitWithTypeParams() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("declarations/declarations.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val templates = tu.allChildren<TemplateDeclaration>()
        val converterTemplate = templates.find { it.name.localName == "Converter" }
        assertNotNull(converterTemplate, "Should have Converter template")
    }

    @Test
    fun testBranchAnnotatedTrait() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("declarations/declarations.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val record = tu.records["Annotated"]
        assertNotNull(record, "Should have Annotated trait")
        val recordType = record.toType()
        assertTrue(
            recordType.methods.any { it.name.localName == "compute" },
            "Trait should have compute method",
        )
    }

    @Test
    fun testBranchTraitImpl() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val counter = tu.records["Counter"]
        assertNotNull(counter, "Should have Counter")
        val counterType = counter.toType()
        assertTrue(
            counterType.methods.any { it.name.localName == "summarize" },
            "Counter should have summarize from Summary trait impl",
        )
    }

    @Test
    fun testDeepTraitWithMethods() {
        val topLevel = Path.of("src", "test", "resources", "rust", "traits")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("traits_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val drawable = tu.records["Drawable"]
        assertNotNull(drawable, "Should have Drawable trait")
        val drawableType = drawable.toType()
        assertTrue(drawableType.methods.size >= 2, "Drawable should have at least 2 methods")
    }

    @Test
    fun testDeepTraitWithSuperTrait() {
        val topLevel = Path.of("src", "test", "resources", "rust", "traits")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("traits_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val resizable = tu.records["Resizable"]
        assertNotNull(resizable, "Should have Resizable trait")
    }

    @Test
    fun testDeepImplTraitForStruct() {
        val topLevel = Path.of("src", "test", "resources", "rust", "traits")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("traits_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val circle = tu.records["Circle"]
        assertNotNull(circle, "Should have Circle struct")
        val circleType = circle.toType()
        assertTrue(circleType.methods.isNotEmpty(), "Circle should have methods from impl blocks")
    }
}
