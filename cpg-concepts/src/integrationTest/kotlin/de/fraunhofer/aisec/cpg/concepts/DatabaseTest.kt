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
package de.fraunhofer.aisec.cpg.concepts

import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.database.Database
import de.fraunhofer.aisec.cpg.graph.concepts.database.DatabaseOpAdd
import de.fraunhofer.aisec.cpg.graph.concepts.database.DatabasePass
import de.fraunhofer.aisec.cpg.graph.followNextDFGEdgesUntilHit
import de.fraunhofer.aisec.cpg.graph.operationNodes
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class DatabaseTest {
    @Test
    fun testDBSimple() {
        val topLevel = File("src/integrationTest/resources/python")
        val result =
            analyze(listOf(topLevel.resolve("sqlalchemy.py")), topLevel.toPath(), true) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<DatabasePass>()
            }

        assertNotNull(result)

        val db = result.conceptNodes.filterIsInstance<Database>().singleOrNull()
        assertNotNull(db)

        val add = result.operationNodes.filterIsInstance<DatabaseOpAdd>().singleOrNull()
        assertNotNull(add)

        val cpgCall = db.underlyingNode
        assertIs<CallExpression>(cpgCall)

        val df = cpgCall.followNextDFGEdgesUntilHit { it is DatabaseOpAdd }.fulfilled.singleOrNull()
        assertNotNull(df)
    }
}
