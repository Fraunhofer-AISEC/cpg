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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.graph.newRecordDeclaration
import de.fraunhofer.aisec.cpg.graph.objectType
import de.fraunhofer.aisec.cpg.graph.pointer
import de.fraunhofer.aisec.cpg.tryCast
import kotlin.test.*

class LanguageTest {

    @Test
    fun testTryCast() {
        with(TestLanguageFrontend()) {
            val baseType = objectType("baseType")

            val myTypeRecord = newRecordDeclaration("myType", "class")
            myTypeRecord.superClasses = mutableListOf(baseType)
            val myType = myTypeRecord.toType()

            val pointerBaseType = baseType.pointer()
            val pointerMyType = myType.pointer()

            // pointer-type and non-pointer types -> will not match in any case
            var matches = pointerMyType.tryCast(myType)
            assertEquals(CastNotPossible, matches)

            // the same type will always match
            matches = pointerMyType.tryCast(pointerMyType)
            assertEquals(DirectMatch, matches)

            // a pointer to the derived type will match a pointer to its base type (using an
            // implicit cast)
            matches = pointerMyType.tryCast(pointerBaseType)
            assertIs<ImplicitCast>(matches)

            // non-pointer types as well
            matches = myType.tryCast(baseType)
            assertIs<ImplicitCast>(matches)
        }
    }
}
