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
package de.fraunhofer.aisec.cpg.frontends.cpp

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.TestUtils.analyzeWithBuilder
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import kotlin.test.*
import kotlin.test.Test

internal class CXXIncludeTest : BaseTest() {
    @Test
    @Throws(Exception::class)
    fun testDefinitionsAndDeclaration() {
        val file = File("src/test/resources/include.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        for (d in tu.declarations) {
            println(d.fullName.localName + " " + d.location)
        }
        assertEquals(6, tu.declarations.size)

        val someClass =
            tu.getDeclarationsByName("SomeClass", RecordDeclaration::class.java).iterator().next()
        assertNotNull(someClass)

        val main = tu.getDeclarationsByName("main", FunctionDeclaration::class.java)
        assertFalse(main.isEmpty())

        val someClassConstructor =
            tu.getDeclarationsByName("SomeClass", ConstructorDeclaration::class.java)
                .iterator()
                .next()
        assertNotNull(someClassConstructor)
        assertEquals(someClass, someClassConstructor.recordDeclaration)

        val doSomething =
            tu.getDeclarationsByName("DoSomething", MethodDeclaration::class.java).iterator().next()
        assertNotNull(doSomething)
        assertEquals(someClass, doSomething.recordDeclaration)

        val returnStatement = doSomething.getBodyStatementAs(0, ReturnStatement::class.java)
        assertNotNull(returnStatement)

        val ref = returnStatement.returnValue as DeclaredReferenceExpression
        assertNotNull(ref)

        val someField = someClass.fields["someField"]
        assertNotNull(someField)
        assertEquals(someField, ref.refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testCodeAndRegionInInclude() {
        // checks, whether code and region for nodes in includes are properly set
        val file = File("src/test/resources/include.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val someClass = tu.getDeclarationsByName("SomeClass", RecordDeclaration::class.java)
        assertFalse(someClass.isEmpty())

        val decl = someClass.iterator().next().constructors[0]
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
                    .defaultLanguages()
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
                    .defaultLanguages()
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
                    .defaultLanguages()
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
                    .defaultLanguages()
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
                    .defaultLanguages()
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
    fun testLoadIncludes() {
        val file = File("src/test/resources/include.cpp")
        val translationUnitDeclarations =
            analyzeWithBuilder(
                TranslationConfiguration.builder()
                    .sourceLocations(listOf(file))
                    .topLevel(file.parentFile)
                    .loadIncludes(false)
                    .debugParser(true)
                    .defaultLanguages()
                    .failOnError(true)
            )
        assertNotNull(translationUnitDeclarations)

        // first one should NOT be a class (since it is defined in the header)
        val recordDeclaration =
            translationUnitDeclarations[0].getDeclarationAs(0, RecordDeclaration::class.java)
        assertNull(recordDeclaration)
    }
}
