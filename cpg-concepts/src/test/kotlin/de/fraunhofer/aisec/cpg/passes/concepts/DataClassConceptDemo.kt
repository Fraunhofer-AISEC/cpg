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
package de.fraunhofer.aisec.cpg.passes.concepts

import de.fraunhofer.aisec.cpg.graph.concepts.auth.Authorization
import de.fraunhofer.aisec.cpg.graph.concepts.auth.RequestContext
import de.fraunhofer.aisec.cpg.graph.concepts.policy.Context
import de.fraunhofer.aisec.cpg.graph.concepts.simple.ConfigConcept
import de.fraunhofer.aisec.cpg.graph.concepts.simple.DatabaseOperation
import de.fraunhofer.aisec.cpg.graph.concepts.simple.ReadOperation
import de.fraunhofer.aisec.cpg.graph.concepts.simple.SimpleDataConcept
import de.fraunhofer.aisec.cpg.graph.concepts.simple.UserConcept
import de.fraunhofer.aisec.cpg.graph.concepts.simple.WriteOperation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * This test demonstrates true data class concepts and operations without the underlyingNode
 * parameter, providing automatic equals/hashCode/toString/copy implementations.
 */
class DataClassConceptDemo {

    @Test
    fun testSimpleDataConcepts() {
        val concept1 = SimpleDataConcept("test", 42)
        val concept2 = SimpleDataConcept("test", 42)
        val concept3 = SimpleDataConcept("test", 43)

        // Data classes automatically provide equals/hashCode
        assertEquals(concept1, concept2)
        assertNotEquals(concept1, concept3)
        assertEquals(concept1.hashCode(), concept2.hashCode())
    }

    @Test
    fun testUserConceptDataClass() {
        val user1 = UserConcept("user123", "john_doe", "john@example.com")
        val user2 = UserConcept("user123", "john_doe", "john@example.com")
        val user3 = user1.copy(email = "john.doe@example.com")

        assertEquals(user1, user2)
        assertNotEquals(user1, user3)
        assertEquals("user123", user3.userId)
        assertEquals("john.doe@example.com", user3.email)
    }

    @Test
    fun testConfigConceptDataClass() {
        val config = ConfigConcept("database.url", "localhost:5432", "production")
        val devConfig = config.copy(environment = "development")

        assertEquals("database.url", config.key)
        assertEquals("development", devConfig.environment)
        assertNotEquals(config, devConfig)
    }

    @Test
    fun testOperationDataClasses() {
        val concept = SimpleDataConcept("test", 100)
        val readOp1 = ReadOperation(concept, 1000L)
        val readOp2 = ReadOperation(concept, 2000L)

        // Operations can have different timestamps but same concept
        assertEquals(concept, readOp1.concept)
        assertEquals(concept, readOp2.concept)
        // Different timestamps make them unequal
        assertNotEquals(readOp1, readOp2)
    }

    @Test
    fun testWriteOperationDataClass() {
        val user = UserConcept("user456", "jane_doe", "jane@example.com")
        val writeOp1 = WriteOperation(user, "user_data", "CREATE")
        val writeOp2 = WriteOperation(user, "user_data", "UPDATE")

        assertEquals(user, writeOp1.concept)
        assertEquals("user_data", writeOp1.data)
        assertNotEquals(writeOp1, writeOp2) // Different operation types
    }

    @Test
    fun testDatabaseOperationDataClass() {
        val config = ConfigConcept("db.host", "localhost", "test")
        val dbOp = DatabaseOperation(config, "SELECT * FROM users WHERE id = ?", mapOf("id" to 123))

        assertEquals(config, dbOp.concept)
        assertEquals("SELECT * FROM users WHERE id = ?", dbOp.query)
        assertTrue(dbOp.parameters.containsKey("id"))
        assertEquals(123, dbOp.parameters["id"])
    }

    @Test
    fun testConvertedConceptDataClasses() {
        // Test the converted existing concept classes
        val auth1 = Authorization("auth123", setOf("read", "write"))
        val auth2 = Authorization("auth123", setOf("read", "write"))
        val auth3 = auth1.copy(permissions = setOf("read"))

        assertEquals(auth1, auth2)
        assertNotEquals(auth1, auth3)
        assertEquals(setOf("read"), auth3.permissions)
    }

    @Test
    fun testRequestContextDataClass() {
        val context1 =
            RequestContext("req123", "GET", "/api/users", mapOf("Authorization" to "Bearer token"))
        val context2 = context1.copy(method = "POST")

        assertEquals("req123", context1.requestId)
        assertEquals("GET", context1.method)
        assertEquals("POST", context2.method)
        assertNotEquals(context1, context2)
    }

    @Test
    fun testPolicyContextDataClass() {
        val context1 = Context("default")
        val context2 = Context("admin")
        val context3 = context1.copy()

        assertEquals(context1, context3)
        assertNotEquals(context1, context2)
        assertEquals("admin", context2.contextId)
    }

    @Test
    fun testDataClassToString() {
        val user = UserConcept("user789", "test_user", "test@example.com")
        val expectedString =
            "UserConcept(userId=user789, username=test_user, email=test@example.com)"
        assertEquals(expectedString, user.toString())
    }
}
