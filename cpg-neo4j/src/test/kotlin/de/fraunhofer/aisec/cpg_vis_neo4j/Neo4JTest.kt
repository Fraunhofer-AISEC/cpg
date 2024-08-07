/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg_vis_neo4j

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.builder.translationResult
import de.fraunhofer.aisec.cpg.graph.declarations.ImportDeclaration
import de.fraunhofer.aisec.cpg.graph.functions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Tag

@Tag("integration")
class Neo4JTest {
    @Test
    @Throws(InterruptedException::class)
    fun testPush() {
        val (application, translationResult) = createTranslationResult()

        // 22 inferred functions, 1 inferred method, 2 inferred constructors, 11 regular functions
        assertEquals(36, translationResult.functions.size)

        application.pushToNeo4j(translationResult)
    }

    @Test
    fun testSimpleNameConverter() {
        val result =
            with(TestLanguageFrontend()) {
                translationResult {
                    val import = ImportDeclaration()
                    import.name = Name("myname")
                    import.alias = Name("myname", Name("myparent"), "::")
                    additionalNodes += import
                }
            }

        val app = Application()
        app.pushToNeo4j(result)

        val sessionAndSessionFactoryPair = app.connect()

        val session = sessionAndSessionFactoryPair.first
        session.beginTransaction().use { transaction ->
            val imports = session.loadAll(ImportDeclaration::class.java)
            assertNotNull(imports)

            var loadedImport = imports.singleOrNull()
            assertNotNull(loadedImport)
            assertEquals("myname", loadedImport.alias?.localName)

            transaction.commit()
        }
    }
}
