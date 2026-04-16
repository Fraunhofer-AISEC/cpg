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
package de.fraunhofer.aisec.cpg.graph.edges

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.newMethod
import de.fraunhofer.aisec.cpg.graph.newRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class EdgesTest {
    @Test
    fun testUnwrap() {
        with(TestLanguageFrontend()) {
            val record = newRecord("myRecord", kind = "class")
            val method = newMethod("myFunc")
            record.methods += method

            assertEquals(1, record.methods.size)
            assertEquals(method, record.methods.firstOrNull())

            assertEquals(
                "Record[name=myRecord,location=<null>,name=myRecord,kind=class,superTypeDeclarations=[],fields=[],methods=[Method[name=myFunc,location=<null>,parameters=[]]],constructors=[],records=[]]",
                record.toString(),
            )
        }
    }
}
