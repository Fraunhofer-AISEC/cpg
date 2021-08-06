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
package de.fraunhofer.aisec.cpg.passes.scopes

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils.subnodesOfType
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ScopeManagerTest : BaseTest() {
    private lateinit var config: TranslationConfiguration

    @BeforeEach
    fun setUp() {
        config = TranslationConfiguration.builder().defaultPasses().build()
    }

    @Test
    @Throws(TranslationException::class)
    fun testSetScope() {
        val frontend: LanguageFrontend = JavaLanguageFrontend(config, ScopeManager())
        assertEquals(frontend, frontend.scopeManager.lang)

        frontend.scopeManager = ScopeManager()
        assertEquals(frontend, frontend.scopeManager.lang)
    }

    @Test
    @Throws(TranslationException::class)
    fun testReplaceNode() {
        val scopeManager = ScopeManager()
        val frontend = CXXLanguageFrontend(config, scopeManager)
        val tu = frontend.parse(File("src/test/resources/recordstmt.cpp"))
        val methods =
            subnodesOfType(tu.declarations, MethodDeclaration::class.java).filter {
                it !is ConstructorDeclaration
            }
        assertFalse(methods.isEmpty())

        methods.forEach {
            val scope = scopeManager.lookupScope(it)
            assertSame(it, scope!!.getAstNode())
        }

        val constructors = subnodesOfType(tu.declarations, ConstructorDeclaration::class.java)
        assertFalse(constructors.isEmpty())

        // make sure that the scope of the constructor actually has the constructor as an ast node.
        // this is necessary, since the constructor was probably created as a function declaration
        // which later gets 'upgraded' to a constructor declaration.
        constructors.forEach {
            val scope = scopeManager.lookupScope(it)
            Assertions.assertSame(it, scope!!.getAstNode())
        }
    }
}
