/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.llvm

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

class LLVMIRLanguageFrontendTest {
    @Test
    fun test1() {
        val topLevel = Path.of("src", "test", "resources", "llvm")

        val frontend =
            LLVMIRLanguageFrontend(TranslationConfiguration.builder().build(), ScopeManager())
        frontend.parse(topLevel.resolve("main.ll").toFile())
    }

    @Test
    fun test2() {
        val topLevel = Path.of("src", "test", "resources", "llvm")

        val frontend =
            LLVMIRLanguageFrontend(TranslationConfiguration.builder().build(), ScopeManager())
        val tu = frontend.parse(topLevel.resolve("integer_ops.ll").toFile())

        assertEquals(2, tu.declarations.size)

        val main =
            tu.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()

        assertNotNull(main)
        assertNotNull(main.body)

        assertEquals("i32*", main.type.name) // not sure why. it should just be i32
    }
}
