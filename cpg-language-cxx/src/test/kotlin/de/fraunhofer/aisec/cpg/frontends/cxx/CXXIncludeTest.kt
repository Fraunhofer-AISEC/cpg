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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.sarif.Region
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.*
import kotlin.test.Test

internal class CXXIncludeTest : BaseTest() {
    @Test
    @Throws(Exception::class)
    fun testDefinitionsAndDeclaration() {
        val file = File("src/test/resources/include.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        for (d in tu.declarations) {
            println(d.name.localName + " " + d.location)
        }
        assertEquals(6, tu.declarations.size)

        val someClass = tu.records["SomeClass"]
        assertNotNull(someClass)

        val main = tu.functions["main"]
        assertNotNull(main)

        val someClassConstructor = someClass.innerConstructors["SomeClass::SomeClass"]
        assertNotNull(someClassConstructor)
        assertEquals(someClass, someClassConstructor.recordDeclaration)

        val doSomething = tu.methods["SomeClass::DoSomething"]?.definition as? MethodDeclaration
        assertNotNull(doSomething)
        assertEquals(someClass, doSomething.recordDeclaration)

        val returnStatement = doSomething.returns.firstOrNull()
        assertNotNull(returnStatement)

        val ref = returnStatement.returnValue as Reference
        assertNotNull(ref)

        val someField = someClass.innerFields["someField"]
        assertNotNull(someField)
        assertEquals(someField, ref.refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testCodeAndRegionInInclude() {
        // checks, whether code and region for nodes in includes are properly set
        val file = File("src/test/resources/include.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val someClass = tu.records["SomeClass"]
        assertNotNull(someClass)

        val decl = someClass.innerConstructors[0]
        assertEquals("SomeClass();", decl.code)

        val location = decl.location
        assertNotNull(location)
        assertEquals(Region(16, 3, 16, 15), location.region)
    }

    @Test
    @Throws(Exception::class)
    fun testIncludeBlacklist() {
        val file = File("src/test/resources/include.cpp")
        val translationUnitDeclarations =
            analyzeWithBuilder(
                TranslationConfiguration.builder()
                    .sourceLocations(listOf(file))
                    .topLevel(file.parentFile)
                    .loadIncludes(true)
                    .debugParser(true)
                    .registerLanguage<CPPLanguage>()
                    .includeBlocklist(File("src/test/resources/include.h").absolutePath)
                    .failOnError(true)
            )
        val next = translationUnitDeclarations.iterator().next()
        assertNotNull(next)

        // another-include.h should be there - include.h should not be there
        assertEquals(1, next.includes.size)
        assertTrue(
            next.includes.stream().anyMatch { d: IncludeDeclaration ->
                (d.filename == File("src/test/resources/another-include.h").absolutePath)
            }
        )
    }

    @Test
    @Throws(Exception::class)
    fun testIncludeBlacklistRelative() {
        val file = File("src/test/resources/include.cpp")
        val translationUnitDeclarations =
            analyzeWithBuilder(
                TranslationConfiguration.builder()
                    .sourceLocations(listOf(file))
                    .topLevel(file.parentFile)
                    .loadIncludes(true)
                    .debugParser(true)
                    .registerLanguage<CPPLanguage>()
                    .includeBlocklist("include.h")
                    .failOnError(true)
            )
        val next = translationUnitDeclarations.iterator().next()
        assertNotNull(next)

        // another-include.h should be there - include.h should not be there
        assertEquals(1, next.includes.size)
        assertTrue(
            next.includes.stream().anyMatch { d: IncludeDeclaration ->
                (d.filename == File("src/test/resources/another-include.h").absolutePath)
            }
        )
    }

    @Test
    @Throws(Exception::class)
    fun testIncludeWhitelist() {
        val file = File("src/test/resources/include.cpp")
        val translationUnitDeclarations =
            analyzeWithBuilder(
                TranslationConfiguration.builder()
                    .sourceLocations(listOf(file))
                    .topLevel(file.parentFile)
                    .loadIncludes(true)
                    .debugParser(true)
                    .registerLanguage<CPPLanguage>()
                    .includeWhitelist(File("src/test/resources/include.h").absolutePath)
                    .failOnError(true)
            )
        val next = translationUnitDeclarations.iterator().next()
        assertNotNull(next)

        // include.h should be there - another-include.h should not be there
        assertEquals(1, next.includes.size)
        assertTrue(
            next.includes.stream().anyMatch { d: IncludeDeclaration ->
                (d.filename == File("src/test/resources/include.h").absolutePath)
            }
        )
    }

    @Test
    @Throws(Exception::class)
    fun testIncludeWhitelistRelative() {
        val file = File("src/test/resources/include.cpp")
        val translationUnitDeclarations =
            analyzeWithBuilder(
                TranslationConfiguration.builder()
                    .sourceLocations(listOf(file))
                    .topLevel(file.parentFile)
                    .loadIncludes(true)
                    .debugParser(true)
                    .registerLanguage<CPPLanguage>()
                    .includeWhitelist("include.h")
                    .failOnError(true)
            )
        val next = translationUnitDeclarations.iterator().next()
        assertNotNull(next)

        // include.h should be there - another-include.h should not be there
        assertEquals(1, next.includes.size)
        assertTrue(
            next.includes.stream().anyMatch { d: IncludeDeclaration ->
                (d.filename == File("src/test/resources/include.h").absolutePath)
            }
        )
    }

    @Test
    @Throws(Exception::class)
    fun testIncludeBothLists() {
        val file = File("src/test/resources/include.cpp")
        val translationUnitDeclarations =
            analyzeWithBuilder(
                TranslationConfiguration.builder()
                    .sourceLocations(listOf(file))
                    .topLevel(file.parentFile)
                    .loadIncludes(true)
                    .debugParser(true)
                    .registerLanguage<CPPLanguage>()
                    .includeBlocklist("include.h") // blacklist entries take priority
                    .includeWhitelist("include.h")
                    .includeWhitelist("another-include.h")
                    .failOnError(true)
            )
        val next = translationUnitDeclarations.iterator().next()
        assertNotNull(next)

        // while the whitelist has two entries, one is also part of the blacklist and thus will be
        // overridden, so only 1 entry should be left
        assertEquals(1, next.includes.size)
        // another-include.h will stay in the include list
        assertTrue(
            next.includes.stream().anyMatch { d: IncludeDeclaration ->
                (d.filename == File("src/test/resources/another-include.h").absolutePath)
            }
        )
    }

    @Test
    @Throws(Exception::class)
    fun testLoadIncludesDisabled() {
        val file = File("src/test/resources/include.cpp")
        val tus =
            analyzeWithBuilder(
                TranslationConfiguration.builder()
                    .sourceLocations(listOf(file))
                    .topLevel(file.parentFile)
                    .loadIncludes(false)
                    .debugParser(true)
                    .registerLanguage<CPPLanguage>()
                    .failOnError(true)
            )
        assertNotNull(tus)

        val tu = tus.firstOrNull()
        assertNotNull(tu)

        // the tu should not contain any classes, since they are defined in the header (which are
        // not loaded) and inference is off.
        assertTrue(tu.records.isEmpty())

        // however, we should still have two methods (one of which is a constructor declaration)
        assertEquals(2, tu.methods.size)
        assertEquals(1, tu.methods.filterIsInstance<ConstructorDeclaration>().size)
    }

    @Test
    @Throws(Exception::class)
    fun testUnityBuild() {
        val file = File("src/test/resources/include.cpp")
        val tus =
            analyzeWithBuilder(
                TranslationConfiguration.builder()
                    .sourceLocations(listOf(file))
                    .loadIncludes(true)
                    .useUnityBuild(true)
                    .debugParser(true)
                    .registerLanguage<CPPLanguage>()
                    .failOnError(true)
            )
        assertNotNull(tus)

        val tu = tus.firstOrNull()
        assertNotNull(tu)
        assertFalse(tu.records.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testUnityBuildWithComponent() {
        val file = File("src/test/resources/include.cpp")
        val tus =
            analyzeWithBuilder(
                TranslationConfiguration.builder()
                    .sourceLocations(listOf(file))
                    .topLevel(file.parentFile)
                    .loadIncludes(true)
                    .useUnityBuild(true)
                    .debugParser(true)
                    .registerLanguage<CPPLanguage>()
                    .failOnError(true)
            )
        assertNotNull(tus)

        val tu = tus.firstOrNull()
        assertNotNull(tu)
        assertFalse(tu.records.isEmpty())
    }
}
