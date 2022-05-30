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
import de.fraunhofer.aisec.cpg.TestUtils.findByName
import de.fraunhofer.aisec.cpg.TestUtils.getOfTypeWithName
import de.fraunhofer.aisec.cpg.TestUtils.getSubnodeOfTypeWithName
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager.Companion.builder
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.NodeComparator
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import java.io.File
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.stream.Collectors
import kotlin.test.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VariableResolverJavaTest : BaseTest() {

    fun getCallWithReference(literal: String): DeclaredReferenceExpression? {
        val exp = callParamMap[literal]
        return if (exp is DeclaredReferenceExpression) exp else null
    }

    fun getCallWithMemberExpression(literal: String): MemberExpression? {
        val exp = callParamMap[literal]
        return if (exp is MemberExpression) exp else null
    }

    @Test
    fun testVarNameDeclaredInLoop() {
        val firstLoopLocal =
            getSubnodeOfTypeWithName(forStatements!![0], VariableDeclaration::class.java, "varName")
        VRUtil.assertUsageOf(callParamMap["func1_first_loop_varName"], firstLoopLocal)
    }

    @Test
    fun testVarNameInSecondLoop() {
        val secondLoopLocal =
            getSubnodeOfTypeWithName(forStatements!![1], VariableDeclaration::class.java, "varName")
        VRUtil.assertUsageOf(callParamMap["func1_second_loop_varName"], secondLoopLocal)
    }

    @Test
    fun testImplicitThisVarNameAfterLoops() {
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func1_imp_this_varName"],
            outerImpThis,
            outerVarName
        )
    }

    @Test
    fun testReferenceToParameter() {
        val param: ValueDeclaration? =
            getSubnodeOfTypeWithName(
                outerFunction2,
                ParamVariableDeclaration::class.java,
                "varName"
            )
        VRUtil.assertUsageOf(callParamMap["func2_param_varName"], param)
    }

    @Test
    fun testVarNameInInstanceOfExternalClass() {
        val externalClassInstance =
            getSubnodeOfTypeWithName(
                outerFunction3,
                VariableDeclaration::class.java,
                "externalClass"
            )
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func3_external_instance_varName"],
            externalClassInstance,
            externVarName
        )
    }

    @Test
    fun testStaticVarNameInExternalClass() {
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func3_external_static_staticVarName"],
            externalClass,
            externStaticVarName
        )
    }

    @Test
    fun testStaticVarnameWithoutPreviousInstance() {
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func4_external_static_staticVarName"],
            externalClass,
            externStaticVarName
        )
    }

    @Test
    fun testVarNameOverImpThisInnerClass() {
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func1_inner_imp_this_varName"],
            innerImpThis,
            innerVarName
        )
    }

    @Test
    fun testVarNameInOuterFromInnerClass() {
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func1_outer_this_varName"],
            outerImpThis,
            outerVarName
        )
    }

    @Test
    fun testStaticOuterFromInner() {
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func1_outer_static_staticVarName"],
            outerClass,
            outerStaticVarName
        )
    }

    @Test
    fun testParamVarNameInInnerClass() {
        VRUtil.assertUsageOf(
            callParamMap["func2_inner_param_varName"],
            getSubnodeOfTypeWithName(
                innerFunction2,
                ParamVariableDeclaration::class.java,
                "varName"
            )
        )
    }

    @Test
    fun testInnerVarnameOverExplicitThis() {
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func2_inner_this_varName"],
            innerImpThis,
            innerVarName
        )
    }

    @Test
    fun testStaticVarNameAsCoughtExcpetionInInner() {
        val staticVarNameException =
            getSubnodeOfTypeWithName(
                innerFunction3,
                VariableDeclaration::class.java,
                "staticVarName"
            )
        VRUtil.assertUsageOf(
            callParamMap["func3_inner_exception_staticVarName"],
            staticVarNameException
        )
    }

    @Test
    fun testVarNameAsCoughtExcpetionInInner() {
        val varNameExcepetion =
            getSubnodeOfTypeWithName(innerFunction3, VariableDeclaration::class.java, "varName")
        VRUtil.assertUsageOf(callParamMap["func3_inner_exception_varName"], varNameExcepetion)
    }

    companion object {
        // Externally defined static global
        private var externalClass: RecordDeclaration? = null
        private var externVarName: FieldDeclaration? = null
        private var externStaticVarName: FieldDeclaration? = null
        private var outerClass: RecordDeclaration? = null
        private var outerVarName: FieldDeclaration? = null
        private var outerStaticVarName: FieldDeclaration? = null
        private var outerImpThis: FieldDeclaration? = null
        private var innerClass: RecordDeclaration? = null
        private var innerVarName: FieldDeclaration? = null
        private var innerStaticVarName: FieldDeclaration? = null
        private var innerImpThis: FieldDeclaration? = null
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
            val topLevelPath = "src/test/resources/variables_extended/java/"
            val fileNames = listOf("ScopeVariables.java", "ExternalClass.java")
            val fileLocations =
                fileNames
                    .stream()
                    .map { fileName: String -> File(topLevelPath + fileName) }
                    .collect(Collectors.toList())
            val config =
                TranslationConfiguration.builder()
                    .sourceLocations(*fileLocations.toTypedArray())
                    .topLevel(File(topLevelPath))
                    .defaultPasses()
                    .defaultLanguages()
                    .debugParser(true)
                    .failOnError(true)
                    .build()
            val analyzer = builder().config(config).build()
            val tu = analyzer.analyze().get().translationUnits
            val nodes =
                tu.stream()
                    .flatMap { tUnit: TranslationUnitDeclaration? ->
                        SubgraphWalker.flattenAST(tUnit).stream()
                    }
                    .collect(Collectors.toList())
            val calls = findByName(Util.filterCast(nodes, CallExpression::class.java), "printLog")
            calls.sortedWith(NodeComparator())
            val records = Util.filterCast(nodes, RecordDeclaration::class.java)

            // Extract all Variable declarations and field declarations for matching
            externalClass =
                getOfTypeWithName(
                    nodes,
                    RecordDeclaration::class.java,
                    "variables_extended.ExternalClass"
                )
            externVarName =
                getSubnodeOfTypeWithName(externalClass, FieldDeclaration::class.java, "varName")
            externStaticVarName =
                getSubnodeOfTypeWithName(
                    externalClass,
                    FieldDeclaration::class.java,
                    "staticVarName"
                )
            outerClass =
                getOfTypeWithName(
                    nodes,
                    RecordDeclaration::class.java,
                    "variables_extended.ScopeVariables"
                )
            outerVarName =
                outerClass!!
                    .fields
                    .stream()
                    .filter { n: FieldDeclaration -> n.name == "varName" }
                    .findFirst()
                    .get()
            outerStaticVarName =
                outerClass!!
                    .fields
                    .stream()
                    .filter { n: FieldDeclaration -> n.name == "staticVarName" }
                    .findFirst()
                    .get()
            outerImpThis =
                outerClass!!
                    .fields
                    .stream()
                    .filter { n: FieldDeclaration -> n.name == "this" }
                    .findFirst()
                    .get()

            // Inner class and its fields
            innerClass =
                getOfTypeWithName(
                    nodes,
                    RecordDeclaration::class.java,
                    "variables_extended.ScopeVariables.InnerClass"
                )
            innerVarName =
                innerClass!!
                    .fields
                    .stream()
                    .filter { n: FieldDeclaration -> n.name == "varName" }
                    .findFirst()
                    .get()
            innerStaticVarName =
                getSubnodeOfTypeWithName(innerClass, FieldDeclaration::class.java, "staticVarName")
            innerImpThis =
                getSubnodeOfTypeWithName(innerClass, FieldDeclaration::class.java, "this")
            innerImpOuter =
                getSubnodeOfTypeWithName(
                    innerClass,
                    FieldDeclaration::class.java,
                    "ScopeVariables.this"
                )
            main = getSubnodeOfTypeWithName(outerClass, MethodDeclaration::class.java, "main")
            outerFunction1 =
                outerClass!!
                    .methods
                    .stream()
                    .filter { method: MethodDeclaration -> method.name == "function1" }
                    .collect(Collectors.toList())[0]
            forStatements =
                Util.filterCast(SubgraphWalker.flattenAST(outerFunction1), ForStatement::class.java)

            // Functions i nthe outer and inner object
            outerFunction2 =
                outerClass!!
                    .methods
                    .stream()
                    .filter { method: MethodDeclaration -> method.name == "function2" }
                    .collect(Collectors.toList())[0]
            outerFunction3 =
                outerClass!!
                    .methods
                    .stream()
                    .filter { method: MethodDeclaration -> method.name == "function3" }
                    .collect(Collectors.toList())[0]
            outerFunction4 =
                outerClass!!
                    .methods
                    .stream()
                    .filter { method: MethodDeclaration -> method.name == "function4" }
                    .collect(Collectors.toList())[0]
            innerFunction1 =
                innerClass!!
                    .methods
                    .stream()
                    .filter { method: MethodDeclaration -> method.name == "function1" }
                    .collect(Collectors.toList())[0]
            innerFunction2 =
                innerClass!!
                    .methods
                    .stream()
                    .filter { method: MethodDeclaration -> method.name == "function2" }
                    .collect(Collectors.toList())[0]
            innerFunction3 =
                innerClass!!
                    .methods
                    .stream()
                    .filter { method: MethodDeclaration -> method.name == "function3" }
                    .collect(Collectors.toList())[0]
            for (call in calls) {
                val first = call.arguments[0]
                val logId = (first as Literal<*>).value.toString()
                val second = call.arguments[1]
                callParamMap[logId] = second
            }
        }
    }
}
