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
package de.fraunhofer.aisec.cpg.passes.concepts

import de.fraunhofer.aisec.cpg.graph.concepts.auth.Authorization
import de.fraunhofer.aisec.cpg.graph.concepts.auth.RequestContext
import de.fraunhofer.aisec.cpg.graph.concepts.policy.Context
import de.fraunhofer.aisec.cpg.graph.concepts.simple.ConfigConcept
import de.fraunhofer.aisec.cpg.graph.concepts.simple.SimpleDataConcept
import de.fraunhofer.aisec.cpg.graph.concepts.simple.SimpleDataOperation
import de.fraunhofer.aisec.cpg.graph.concepts.simple.UserConcept
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Demonstrates and tests the enhanced data class support for concepts and operations. This test
 * shows that data classes can now properly inherit from Concept while maintaining all data class
 * benefits.
 */
class EnhancedDataClassConceptDemo {

    @Test
    fun testDataClassConceptInheritance() {
        // Create data class concepts
        val concept1 = SimpleDataConcept("test-1", "First concept")
        val concept2 = SimpleDataConcept("test-1", "First concept")
        val concept3 = SimpleDataConcept("test-2", "Second concept")

        // Test data class functionality
        assertEquals(concept1, concept2) // Data class equality works
        assertNotEquals(concept1, concept3)
        assertEquals(concept1.hashCode(), concept2.hashCode()) // Consistent hashCode
        assertNotEquals(concept1.hashCode(), concept3.hashCode())

        // Test toString works
        val expectedString = "SimpleDataConcept(conceptId=test-1, description=First concept)"
        assertEquals(expectedString, concept1.toString())

        // Test copy functionality
        val copied = concept1.copy(description = "Modified description")
        assertEquals("test-1", copied.conceptId)
        assertEquals("Modified description", copied.description)
        assertNotEquals(concept1, copied)
    }

    @Test
    fun testDataClassOperationInheritance() {
        val concept = SimpleDataConcept("op-concept", "Operation concept")
        val operation1 = SimpleDataOperation("op-1", concept)
        val operation2 = SimpleDataOperation("op-1", concept)
        val operation3 = SimpleDataOperation("op-2", concept)

        // Test data class functionality for operations
        assertEquals(operation1, operation2)
        assertNotEquals(operation1, operation3)
        assertEquals(operation1.hashCode(), operation2.hashCode())

        // Test copy functionality
        val copied = operation1.copy(operationId = "op-modified")
        assertEquals("op-modified", copied.operationId)
        assertEquals(concept, copied.concept)
        assertNotEquals(operation1, copied)
    }

    @Test
    fun testConceptInheritance() {
        val concept = SimpleDataConcept("inheritance-test", "Test concept")

        // Verify it's still a Concept
        assertTrue(concept is de.fraunhofer.aisec.cpg.graph.concepts.Concept)

        // Verify ops property is available
        assertTrue(concept.ops.isEmpty())

        // Verify name is set correctly (from Concept's init block)
        assertEquals("SimpleDataConcept", concept.name?.localName)
    }

    @Test
    fun testExistingConceptsAsDataClasses() {
        // Test Authorization as data class
        val auth1 = Authorization("auth-1", setOf("read", "write"))
        val auth2 = Authorization("auth-1", setOf("read", "write"))
        val auth3 = Authorization("auth-2", setOf("admin"))

        assertEquals(auth1, auth2)
        assertNotEquals(auth1, auth3)

        val authCopy = auth1.copy(permissions = setOf("read"))
        assertEquals("auth-1", authCopy.authorizationId)
        assertEquals(setOf("read"), authCopy.permissions)

        // Test RequestContext as data class
        val context1 = RequestContext("req-1", "GET", "/api/test")
        val context2 = RequestContext("req-1", "GET", "/api/test")
        assertEquals(context1, context2)

        val contextCopy = context1.copy(method = "POST")
        assertEquals("POST", contextCopy.method)
        assertEquals("/api/test", contextCopy.path)

        // Test Context as data class
        val ctx1 = Context("default")
        val ctx2 = Context() // Uses default parameter
        assertEquals(ctx1, ctx2)

        val ctxCopy = ctx1.copy(contextId = "custom")
        assertEquals("custom", ctxCopy.contextId)
    }

    @Test
    fun testMultipleConceptTypes() {
        // Test various data class concepts
        val user = UserConcept("user-123", "testuser", setOf("admin", "user"))
        val config = ConfigConcept("app-config", mapOf("timeout" to 30, "debug" to true))
        val simple = SimpleDataConcept("simple-1")

        // All should be proper Concepts
        assertTrue(user is de.fraunhofer.aisec.cpg.graph.concepts.Concept)
        assertTrue(config is de.fraunhofer.aisec.cpg.graph.concepts.Concept)
        assertTrue(simple is de.fraunhofer.aisec.cpg.graph.concepts.Concept)

        // Test data class functionality
        val userCopy = user.copy(roles = setOf("user"))
        assertEquals("user-123", userCopy.userId)
        assertEquals(setOf("user"), userCopy.roles)

        val configCopy = config.copy(configName = "test-config")
        assertEquals("test-config", configCopy.configName)
        assertEquals(mapOf("timeout" to 30, "debug" to true), configCopy.settings)
    }
}
