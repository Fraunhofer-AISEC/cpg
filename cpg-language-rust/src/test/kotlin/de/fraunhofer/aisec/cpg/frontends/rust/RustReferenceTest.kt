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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustReferenceTest : BaseTest() {
    @Test
    fun testReferenceAndDereference() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("references.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_references"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val statements = body.statements

        // Statement 0: let x = 5;
        assertNotNull(statements.getOrNull(0), "Should have statement for let x = 5")

        // Statement 1: let r = &x; -> should have UnaryOperator with operatorCode "&"
        val refOps = body.allChildren<UnaryOperator>().filter { it.operatorCode == "&" }
        assertTrue(refOps.isNotEmpty(), "Should have & reference operator")

        // Statement 2: let mr = &mut x; -> should have UnaryOperator with operatorCode "&mut"
        val mutRefOps = body.allChildren<UnaryOperator>().filter { it.operatorCode == "&mut" }
        assertTrue(mutRefOps.isNotEmpty(), "Should have &mut reference operator")

        // Statement 3: let v = *r; -> should have UnaryOperator with operatorCode "*"
        val derefOps = body.allChildren<UnaryOperator>().filter { it.operatorCode == "*" }
        assertTrue(derefOps.isNotEmpty(), "Should have * dereference operator")
    }
}
