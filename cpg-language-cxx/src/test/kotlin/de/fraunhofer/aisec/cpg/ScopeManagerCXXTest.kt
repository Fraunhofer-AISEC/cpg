/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.frontends.cxx.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame

internal class ScopeManagerTest : BaseTest() {
    private lateinit var config: TranslationConfiguration

    @BeforeTest
    fun setUp() {
        config = TranslationConfiguration.builder().defaultPasses().build()
    }

    @Test
    @Throws(TranslationException::class)
    fun testReplaceNode() {
        val ctx = TranslationContext(config)
        val frontend = CXXLanguageFrontend(ctx, CPPLanguage())
        val tu = frontend.parse(File("src/test/resources/cxx/recordstmt.cpp"))
        val methods = tu.descendants<MethodDeclaration>().filter { it !is ConstructorDeclaration }
        assertFalse(methods.isEmpty())

        methods.forEach {
            val scope = ctx.scopeManager.lookupScope(it)
            assertSame(it, scope!!.astNode)
        }

        val constructors = tu.descendants<ConstructorDeclaration>()
        assertFalse(constructors.isEmpty())

        // make sure that the scope of the constructor actually has the constructor as an ast node.
        // this is necessary, since the constructor was probably created as a function declaration
        // which later gets 'upgraded' to a constructor declaration.
        constructors.forEach {
            val scope = ctx.scopeManager.lookupScope(it)
            assertSame(it, scope!!.astNode)
        }
    }
}
