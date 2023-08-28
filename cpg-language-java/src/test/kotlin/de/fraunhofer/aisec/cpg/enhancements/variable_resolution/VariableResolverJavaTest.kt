/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.enhancements.variable_resolution

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.TestUtils.assertUsageOf
import de.fraunhofer.aisec.cpg.TestUtils.assertUsageOfMemberAndBase
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.ForStmt
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import java.nio.file.Path
import java.util.concurrent.ExecutionException
import kotlin.test.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VariableResolverJavaTest : BaseTest() {

    @Test
    fun testVarNameDeclaredInLoop() {
        val firstLoopLocal = forStmts?.get(0).variables["varName"]
        assertUsageOf(callParamMap["func1_first_loop_varName"], firstLoopLocal)
    }

    @Test
    fun testVarNameInSecondLoop() {
        val secondLoopLocal = forStmts?.get(1).variables["varName"]
        assertUsageOf(callParamMap["func1_second_loop_varName"], secondLoopLocal)
    }

    @Test
    fun testImplicitThisVarNameAfterLoops() {
        assertUsageOfMemberAndBase(callParamMap["func1_imp_this_varName"], outerClass, outerVarName)
    }

    @Test
    fun testReferenceToParameter() {
        val param: ValueDecl? = outerFunction2.parameters["varName"]
        assertUsageOf(callParamMap["func2_param_varName"], param)
    }

    @Test
    fun testVarNameInInstanceOfExternalClass() {
        val externalClassInstance = outerFunction3.variables["externalClass"]
        assertUsageOfMemberAndBase(
            callParamMap["func3_external_instance_varName"],
            externalClassInstance,
            externVarName
        )
    }

    @Test
    fun testStaticVarNameInExternalClass() {
        assertUsageOfMemberAndBase(
            callParamMap["func3_external_static_staticVarName"],
            externalClass,
            externStaticVarName
        )
    }

    @Test
    fun testStaticVarnameWithoutPreviousInstance() {
        assertUsageOfMemberAndBase(
            callParamMap["func4_external_static_staticVarName"],
            externalClass,
            externStaticVarName
        )
    }

    @Test
    fun testVarNameOverImpThisInnerClass() {
        assertUsageOfMemberAndBase(
            callParamMap["func1_inner_imp_this_varName"],
            function1Receiver,
            innerVarName
        )
    }

    @Test
    fun testVarNameInOuterFromInnerClass() {
        assertUsageOfMemberAndBase(
            callParamMap["func1_outer_this_varName"],
            implicitOuterThis,
            outerVarName
        )
    }

    @Test
    fun testStaticOuterFromInner() {
        assertUsageOfMemberAndBase(
            callParamMap["func1_outer_static_staticVarName"],
            outerClass,
            outerStaticVarName
        )
    }

    @Test
    fun testParamVarNameInInnerClass() {
        assertUsageOf(
            callParamMap["func2_inner_param_varName"],
            innerFunction2.parameters["varName"]
        )
    }

    @Test
    fun testInnerVarnameOverExplicitThis() {
        assertUsageOfMemberAndBase(
            callParamMap["func2_inner_this_varName"],
            function2Receiver,
            innerVarName
        )
    }

    @Test
    fun testStaticVarNameAsCoughtExcpetionInInner() {
        val staticVarNameException = innerFunction3.variables["staticVarName"]
        assertUsageOf(callParamMap["func3_inner_exception_staticVarName"], staticVarNameException)
    }

    @Test
    fun testVarNameAsCaughtExceptionInInner() {
        val varNameException = innerFunction3.variables["varName"]
        assertUsageOf(callParamMap["func3_inner_exception_varName"], varNameException)
    }

    companion object {
        // Externally defined static global
        private var externalClass: RecordDecl? = null
        private var externVarName: FieldDecl? = null
        private var externStaticVarName: FieldDecl? = null
        private var outerClass: RecordDecl? = null
        private var outerVarName: FieldDecl? = null
        private var outerStaticVarName: FieldDecl? = null
        private var innerClass: RecordDecl? = null
        private var innerVarName: FieldDecl? = null
        private var innerStaticVarName: FieldDecl? = null
        private var implicitOuterThis: FieldDecl? = null
        private var function1Receiver: VariableDecl? = null
        private var function2Receiver: VariableDecl? = null
        private var innerImpOuter: FieldDecl? = null
        private var main: MethodDecl? = null
        private var outerFunction1: MethodDecl? = null
        private var forStmts: List<ForStmt>? = null
        private var outerFunction2: MethodDecl? = null
        private var outerFunction3: MethodDecl? = null
        private var outerFunction4: MethodDecl? = null
        private var innerFunction1: MethodDecl? = null
        private var innerFunction2: MethodDecl? = null
        private var innerFunction3: MethodDecl? = null
        private val callParamMap: MutableMap<String, Expression> = HashMap()

        @BeforeAll
        @JvmStatic
        @Throws(ExecutionException::class, InterruptedException::class)
        fun initTests() {
            val topLevel = Path.of("src/test/resources/variables_extended/java/")
            val fileNames =
                listOf(
                        topLevel.resolve("ScopeVariables.java"),
                        topLevel.resolve("ExternalClass.java")
                    )
                    .map(Path::toFile)
            val result = analyze(fileNames, topLevel, true) { it.registerLanguage(JavaLanguage()) }

            val calls = result.calls { it.name.localName == "printLog" }
            val records = result.records

            // Extract all Variable declarations and field declarations for matching
            externalClass = records["variables_extended.ExternalClass"]
            externVarName = externalClass.fields["varName"]
            externStaticVarName = externalClass.fields["staticVarName"]
            outerClass = records["variables_extended.ScopeVariables"]
            outerVarName = outerClass.fields["varName"]
            outerStaticVarName = outerClass.fields["staticVarName"]

            // Inner class and its fields
            innerClass = records["variables_extended.ScopeVariables.InnerClass"]
            implicitOuterThis = innerClass.fields["this\$ScopeVariables"]
            innerVarName = innerClass.fields["varName"]
            innerStaticVarName = innerClass.fields["staticVarName"]
            function1Receiver = innerClass.methods["function1"]?.receiver
            function2Receiver = innerClass.methods["function2"]?.receiver
            innerImpOuter = innerClass.fields["this\$ScopeVariables"]
            main = outerClass.methods["main"]
            outerFunction1 = outerClass.methods["function1"]
            forStmts = outerFunction1.allChildren()

            // Functions in the outer and inner object
            outerFunction2 = outerClass.methods["function2"]
            outerFunction3 = outerClass.methods["function3"]
            outerFunction4 = outerClass.methods["function4"]
            innerFunction1 = innerClass.methods["function1"]
            innerFunction2 = innerClass.methods["function2"]
            innerFunction3 = innerClass.methods["function3"]

            for (call in calls) {
                val first = call.arguments[0]
                val logId = (first as Literal<*>).value.toString()
                val second = call.arguments[1]
                callParamMap[logId] = second
            }
        }
    }
}
