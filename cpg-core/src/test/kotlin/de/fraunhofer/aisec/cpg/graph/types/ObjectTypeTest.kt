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
package de.fraunhofer.aisec.cpg.graph.types

import de.fraunhofer.aisec.cpg.declare
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.newMethodDeclaration
import de.fraunhofer.aisec.cpg.graph.newRecordDeclaration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ObjectTypeTest {
    @Test
    fun testMethods() {
        with(TestLanguageFrontend()) {
            val parent = newRecordDeclaration("parent", kind = "class")
            scopeManager.enterScope(parent)
            val foo = declare(newMethodDeclaration("foo"))
            parent.innerMethods += foo
            scopeManager.leaveScope(parent)

            val child = newRecordDeclaration("child", kind = "class")
            scopeManager.enterScope(child)
            val bar = declare(newMethodDeclaration("bar"))
            child.innerMethods += bar
            child.superClasses += parent.toType()
            scopeManager.leaveScope(child)

            val childType = child.toType()

            assertIs<ObjectType>(childType)

            val methods = childType.methods
            assertNotNull(methods)
            assertEquals(
                setOf(foo, bar),
                methods,
                "Child type should have methods from itself and parent",
            )

            val fields = childType.fields
            assertNotNull(fields)
            assertEquals(0, fields.size, "Child type should not have any fields defined")

            val constructors = childType.constructors
            assertNotNull(constructors)
            assertEquals(
                0,
                constructors.size,
                "Child type should not have any constructors defined",
            )
        }
    }
}
