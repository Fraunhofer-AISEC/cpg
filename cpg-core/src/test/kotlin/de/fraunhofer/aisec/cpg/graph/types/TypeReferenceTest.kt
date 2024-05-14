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
package de.fraunhofer.aisec.cpg.graph.types

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.newRecordDeclaration
import de.fraunhofer.aisec.cpg.graph.ref
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.typeReference
import kotlin.test.*

class TypeReferenceTest {
    @Test
    fun testTypeReferences() {
        with(TestLanguageFrontend()) {
            scopeManager.resetToGlobal(null)

            // construct our type
            val myClass = newRecordDeclaration("MyClass", kind = "class")
            val type = myClass.declaringType

            // construct a type reference
            val ref = typeReference("MyClass")
            assertIs<TypeReference>(ref)
            // mock type resolution
            ref.refersTo = myClass

            // assert symmetric equals
            assertEquals(ref, type)
            assertEquals(type, ref)

            assertTrue(ref.scope is GlobalScope)

            val p = ref.reference(PointerType.PointerOrigin.POINTER)
            assertNotNull(p)

            val p2 = ref.reference(PointerType.PointerOrigin.POINTER)
            assertNotNull(p2)

            assertEquals(p, p2)

            val set = mutableSetOf<Type>()
            set.add(p)
            set.add(p2)
            println(set)
        }
    }
}
