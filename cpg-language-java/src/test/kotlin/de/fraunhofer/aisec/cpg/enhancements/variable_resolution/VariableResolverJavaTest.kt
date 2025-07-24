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

import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import java.util.concurrent.ExecutionException
import kotlin.test.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VariableResolverJavaTest : BaseTest() {

    @Test
    fun testVarNameDeclaredInLoop() {
        val firstLoopLocal = forStatements?.get(0).allVariables["varName"]
        assertUsageOf(callParamMap["func1_first_loop_varName"], firstLoopLocal)
    }

    @Test
    fun testVarNameInSecondLoop() {
        val secondLoopLocal = forStatements?.get(1).allVariables["varName"]
        assertUsageOf(callParamMap["func1_second_loop_varName"], secondLoopLocal)
    }

    @Test
    fun testImplicitThisVarNameAfterLoops() {
        assertUsageOfMemberAndBase(callParamMap["func1_imp_this_varName"], outerClass, outerVarName)
    }

    @Test
    fun testReferenceToParameter() {
        val param: ValueDeclaration? = outerFunction2.allParameters["varName"]
        assertUsageOf(callParamMap["func2_param_varName"], param)
    }

    @Test
    fun testVarNameInInstanceOfExternalClass() {
        val externalClassInstance = outerFunction3.allVariables["externalClass"]
        assertUsageOfMemberAndBase(
            callParamMap["func3_external_instance_varName"],
            externalClassInstance,
            externVarName,
        )
    }

    @Test
    fun testStaticVarNameInExternalClass() {
        assertUsageOfMemberAndBase(
            callParamMap["func3_external_static_staticVarName"],
            externalClass,
            externStaticVarName,
        )
    }

    @Test
    fun testStaticVarnameWithoutPreviousInstance() {
        assertUsageOfMemberAndBase(
            callParamMap["func4_external_static_staticVarName"],
            externalClass,
            externStaticVarName,
        )
    }

    @Test
    fun testVarNameOverImpThisInnerClass() {
        assertUsageOfMemberAndBase(
            callParamMap["func1_inner_imp_this_varName"],
            function1Receiver,
            innerVarName,
        )
    }

    @Test
    fun testVarNameInOuterFromInnerClass() {
        assertUsageOfMemberAndBase(
            callParamMap["func1_outer_this_varName"],
            implicitOuterThis,
            outerVarName,
        )
    }

    @Test
    fun testStaticOuterFromInner() {
        assertUsageOfMemberAndBase(
            callParamMap["func1_outer_static_staticVarName"],
            outerClass,
            outerStaticVarName,
        )
    }

    @Test
    fun testParamVarNameInInnerClass() {
        assertUsageOf(
            callParamMap["func2_inner_param_varName"],
            innerFunction2.allParameters["varName"],
        )
    }

    @Test
    fun testInnerVarnameOverExplicitThis() {
        assertUsageOfMemberAndBase(
            callParamMap["func2_inner_this_varName"],
            function2Receiver,
            innerVarName,
        )
    }

    @Test
    fun testStaticVarNameAsCoughtExcpetionInInner() {
        val staticVarNameException = innerFunction3.allVariables["staticVarName"]
        assertUsageOf(callParamMap["func3_inner_exception_staticVarName"], staticVarNameException)
    }

    @Test
    fun testVarNameAsCaughtExceptionInInner() {
        val varNameException = innerFunction3.allVariables["varName"]
        assertUsageOf(callParamMap["func3_inner_exception_varName"], varNameException)
    }

    companion object {
        // Externally defined static global
        private var externalClass: RecordDeclaration? = null
        private var externVarName: FieldDeclaration? = null
        private var externStaticVarName: FieldDeclaration? = null
        private var outerClass: RecordDeclaration? = null
        private var outerVarName: FieldDeclaration? = null
        private var outerStaticVarName: FieldDeclaration? = null
        private var innerClass: RecordDeclaration? = null
        private var innerVarName: FieldDeclaration? = null
        private var innerStaticVarName: FieldDeclaration? = null
        private var implicitOuterThis: FieldDeclaration? = null
        private var function1Receiver: VariableDeclaration? = null
        private var function2Receiver: VariableDeclaration? = null
        private var innerImpOuter: FieldDeclaration? = null
        private var main: MethodDeclaration? = null
        private var outerFunction1: MethodDeclaration? = null
        private var forStatements: List<ForStatement>? = null
        private var outerFunction2: MethodDeclaration? = null
        private var outerFunction3: MethodDeclaration? = null
        private var outerFunction4: MethodDeclaration? = null
        private var innerFunction1: MethodDeclaration? = null
        private var innerFunction2: MethodDeclaration? = null
        private var innerFunction3: MethodDeclaration? = null
        private val callParamMap: MutableMap<String, Expression> = HashMap()

        @BeforeAll
        @JvmStatic
        @Throws(ExecutionException::class, InterruptedException::class)
        fun initTests() {
            val topLevel = Path.of("src/test/resources/variables_extended/java/")
            val fileNames =
                listOf(
                        topLevel.resolve("ScopeVariables.java"),
                        topLevel.resolve("ExternalClass.java"),
                    )
                    .map(Path::toFile)
            val result = analyze(fileNames, topLevel, true) { it.registerLanguage<JavaLanguage>() }

            val calls = result.allCalls { it.name.localName == "printLog" }
            val records = result.allRecords

            // Extract all Variable declarations and field declarations for matching
            externalClass = records["variables_extended.ExternalClass"]
            externVarName = externalClass.allFields["varName"]
            externStaticVarName = externalClass.allFields["staticVarName"]
            outerClass = records["variables_extended.ScopeVariables"]
            outerVarName = outerClass.allFields["varName"]
            outerStaticVarName = outerClass.allFields["staticVarName"]

            // Inner class and its fields
            innerClass = records["variables_extended.ScopeVariables.InnerClass"]
            implicitOuterThis = innerClass.allFields["this\$ScopeVariables"]
            innerVarName = innerClass.allFields["varName"]
            innerStaticVarName = innerClass.allFields["staticVarName"]
            function1Receiver = innerClass.allMethods["function1"]?.receiver
            function2Receiver = innerClass.allMethods["function2"]?.receiver
            innerImpOuter = innerClass.allFields["this\$ScopeVariables"]
            main = outerClass.allMethods["main"]
            outerFunction1 = outerClass.allMethods["function1"]
            forStatements = outerFunction1.allDescendants()

            // Functions in the outer and inner object
            outerFunction2 = outerClass.allMethods["function2"]
            outerFunction3 = outerClass.allMethods["function3"]
            outerFunction4 = outerClass.allMethods["function4"]
            innerFunction1 = innerClass.allMethods["function1"]
            innerFunction2 = innerClass.allMethods["function2"]
            innerFunction3 = innerClass.allMethods["function3"]

            for (call in calls) {
                val first = call.arguments[0]
                val logId = (first as Literal<*>).value.toString()
                val second = call.arguments[1]
                callParamMap[logId] = second
            }
        }
    }
}
