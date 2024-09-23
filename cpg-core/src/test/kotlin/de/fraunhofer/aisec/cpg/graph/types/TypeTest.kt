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
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.objectType
import de.fraunhofer.aisec.cpg.graph.pointer
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.test.assertFullName
import de.fraunhofer.aisec.cpg.test.assertLocalName
import kotlin.test.*
import org.junit.jupiter.api.assertThrows

class TypeTest {
    @Test
    fun testType() {
        with(TestLanguageFrontend()) {
            val tu = newTranslationUnitDeclaration("file.extension")
            this.scopeManager.resetToGlobal(tu)

            val func = newFunctionDeclaration("main")
            assertLocalName("main", func)

            val simpleType = objectType("SomeObject")
            assertLocalName("SomeObject", simpleType)
        }
    }

    @Test
    fun testPrimitive() {
        with(TestLanguageFrontend()) {
            val boolean = primitiveType("boolean")
            assertLocalName("boolean", boolean)

            assertThrows<TranslationException> { primitiveType("BOOLEAN") }
        }
    }

    @Test
    fun testTypeOperations() {
        with(TestLanguageFrontend()) {
            var type: Type = objectType("myClass")
            type = type.pointer().pointer().pointer()
            assertIs<SecondOrderType>(type)
            assertEquals(3, type.referenceDepth)

            var operations = type.typeOperations
            assertEquals(3, operations.size)

            type = objectType("myNewClass")
            type = operations.apply(type)
            assertLocalName("myNewClass***", type)
        }
    }

    @Test
    fun testTypeReference() {
        val record = RecordDeclaration()
        record.name = Name("MyClass")

        val type = DeclaredType(record)
        assertFullName("MyClass", type)

        val typeReference = TypeReference("MyClass")
        assertEquals(Type.Origin.UNRESOLVED, typeReference.typeOrigin)

        // Simulate the type resolution
        typeReference.refersTo = type
        assertEquals(Type.Origin.RESOLVED, typeReference.typeOrigin)
    }

    @Test
    fun testTypeReferenceEquals() {
        val globalScope = GlobalScope()
        val scopeA = NameScope(null)
        scopeA.name = Name("A")

        val record = RecordDeclaration()
        record.scope = globalScope
        record.name = Name("MyClass")

        val type = DeclaredType(record)
        assertFullName("MyClass", type)

        // Construct two type references:
        // - refGlobal "lives" inside the global scope
        // - ref1 "lives" inside scopeA
        // - ref2 also "lives" inside scopeA
        //
        // If they are unresolved, refGlobal and ref1|ref2 should not be considered equal, since
        // even though they have the same name, their scope is different, and they could potentially
        // point to different types. ref1 and ref2 are within the same scope and have the same name
        // and should be equal.
        val refGlobal = TypeReference("MyClass")
        refGlobal.scope = globalScope
        assertEquals(Type.Origin.UNRESOLVED, refGlobal.typeOrigin)

        val ref1 = TypeReference("MyClass")
        ref1.scope = scopeA
        assertEquals(Type.Origin.UNRESOLVED, ref1.typeOrigin)

        val ref2 = TypeReference("MyClass")
        ref2.scope = scopeA
        assertEquals(Type.Origin.UNRESOLVED, ref2.typeOrigin)

        assertNotEquals(refGlobal, ref1)
        assertNotEquals(refGlobal, ref2)
        assertEquals(ref1, ref2)

        // simulate type resolution
        refGlobal.refersTo = type
        ref1.refersTo = type
        ref2.refersTo = type

        // all types should be equal now
        assertEquals(refGlobal, ref1)
        assertEquals(ref1, refGlobal)
        assertEquals(refGlobal, ref2)
        assertEquals(ref2, refGlobal)
        assertEquals(ref1, ref2)
        assertEquals(ref2, ref1)
    }
}
