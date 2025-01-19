/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.GraphExamples.Companion.testFrontend
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.builder.record
import de.fraunhofer.aisec.cpg.graph.builder.t
import de.fraunhofer.aisec.cpg.graph.builder.translationResult
import de.fraunhofer.aisec.cpg.graph.builder.translationUnit
import de.fraunhofer.aisec.cpg.graph.newTypedefDeclaration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TypeResolverTest {
    @Test
    fun testTypeResolver() {
        val config =
            TranslationConfiguration.builder()
                .defaultPasses()
                .registerLanguage(TestLanguage("."))
                .build()

        val result =
            testFrontend(config).build {
                translationResult {
                    translationUnit("typedef.cpp") {
                        // class A {};
                        record("A") {}

                        // typedef A myA;
                        scopeManager.addDeclaration(newTypedefDeclaration(t("A"), t("myA")))
                    }
                }
            }

        val myA = result.typedefs["myA"]
        assertNotNull(myA)

        // After aliasing both the types should be the same
        assertEquals(myA.type, myA.alias)
    }
}
