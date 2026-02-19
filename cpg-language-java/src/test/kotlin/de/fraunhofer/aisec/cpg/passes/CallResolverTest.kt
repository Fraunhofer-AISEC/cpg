/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.*

class CallResolverTest : BaseTest() {
    private fun testMethods(records: List<Record>, intType: Type, stringType: Type) {
        val callsRecord = findByUniqueName(records, "Calls")
        val externalRecord = findByUniqueName(records, "External")
        val superClassRecord = findByUniqueName(records, "SuperClass")
        val innerMethods = findByName(callsRecord.methods, "innerTarget")
        val innerCalls = findByName(callsRecord.calls, "innerTarget")
        checkCalls(intType, stringType, innerMethods, innerCalls)
        val superMethods = findByName(superClassRecord.methods, "superTarget").toMutableList()
        // We can't infer that a call to superTarget(int, int, int) is intended to be part of the
        // superclass. It looks like a call to a member of Calls.java, thus we need to add these
        // methods to the lookup
        superMethods.addAll(findByName(callsRecord.methods, "superTarget"))
        val superCalls = findByName(callsRecord.calls, "superTarget")
        checkCalls(intType, stringType, superMethods, superCalls)
        val externalMethods = findByName(externalRecord.methods, "externalTarget")
        val externalCalls = findByName(callsRecord.calls, "externalTarget")
        checkCalls(intType, stringType, externalMethods, externalCalls)
    }

    private fun ensureNoUnknownClassDummies(records: List<Record>) {
        val callsRecord = findByUniqueName(records, "Calls")
        assertTrue(records.stream().noneMatch { it.name.localName == "Unknown" })

        val unknownCall = findByUniqueName(callsRecord.calls, "unknownTarget")
        assertEquals(listOf<Any>(), unknownCall.invokes)
    }

    private fun checkCalls(
        intType: Type,
        stringType: Type,
        methods: Collection<Function>,
        calls: Collection<CallExpression>,
    ) {
        val signatures = listOf(listOf(), listOf(intType, intType), listOf(intType, stringType))
        for (signature in signatures) {
            for (call in calls.filter { it.signature == signature }) {
                val target =
                    findByUniquePredicate(methods) { m: Function ->
                        m.matchesSignature(signature) != IncompatibleSignature
                    }
                assertEquals(listOf(target), call.invokes)
            }
        }

        // Check for inferred nodes
        val inferenceSignature = listOf(intType, intType, intType)
        for (inferredCall in
            calls.filter { c: CallExpression -> c.signature == inferenceSignature }) {
            val inferredTarget =
                findByUniquePredicate(methods) { m: Function ->
                    m.matchesSignature(inferenceSignature) != IncompatibleSignature
                }
            assertEquals(listOf(inferredTarget), inferredCall.invokes)
            assertTrue(inferredTarget.isInferred)
        }
    }

    private fun testOverriding(records: List<Record>) {
        val callsRecord = findByUniqueName(records, "Calls")
        val externalRecord = findByUniqueName(records, "External")
        val superClassRecord = findByUniqueName(records, "SuperClass")
        val originalMethod = findByUniqueName(superClassRecord.methods, "overridingTarget")
        val overridingMethod = findByUniqueName(externalRecord.methods, "overridingTarget")
        val call = findByUniqueName(callsRecord.calls, "overridingTarget")

        // TODO related to #204: Currently we have both the original and the overriding method in
        //  the invokes list. This check needs to be adjusted to the choice we make on solving #204
        assertTrue(call.invokes.contains(overridingMethod))
        assertEquals<List<Function>>(listOf(originalMethod), overridingMethod.overrides)
        assertEquals<List<Function>>(
            listOf(overridingMethod),
            originalMethod.overriddenBy,
        )
    }

    @Test
    @Throws(Exception::class)
    fun testJava() {
        val result =
            analyze("java", topLevel, true) {
                it.registerLanguage<JavaLanguage>()
                it.inferenceConfiguration(
                    InferenceConfiguration.builder().inferRecords(false).build()
                )
            }
        val tu = result.components.flatMap { it.translationUnits }.firstOrNull()
        assertNotNull(tu)

        val records = result.records
        val intType = tu.primitiveType("int")
        val stringType = tu.primitiveType(("java.lang.String"))
        testMethods(records, intType, stringType)
        testOverriding(records)
        ensureNoUnknownClassDummies(records)
    }

    companion object {
        private val topLevel = Path.of("src", "test", "resources", "calls")
    }
}
