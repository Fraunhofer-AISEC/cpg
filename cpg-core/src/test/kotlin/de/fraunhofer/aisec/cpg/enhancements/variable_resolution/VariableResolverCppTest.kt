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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.CatchClause
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import java.io.File
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.stream.Collectors
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VariableResolverCppTest : BaseTest() {
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
    private val innerImpOuter: FieldDeclaration? = null
    private var main: FunctionDeclaration? = null
    private var outerFunction1: MethodDeclaration? = null
    private var forStatements: List<ForStatement>? = null
    private var outerFunction2: MethodDeclaration? = null
    private var outerFunction3: MethodDeclaration? = null
    private var outerFunction4: MethodDeclaration? = null
    private var outerFunction5: MethodDeclaration? = null
    private var innerFunction1: MethodDeclaration? = null
    private var innerFunction2: MethodDeclaration? = null
    private val callParamMap: MutableMap<String, Expression> = HashMap()
    @BeforeAll
    @Throws(ExecutionException::class, InterruptedException::class)
    fun initTests() {
        val topLevelPath = "src/test/resources/variables_extended/cpp/"
        val fileNames = Arrays.asList("scope_variables.cpp", "external_class.cpp")
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
                .debugParser(true)
                .defaultLanguages()
                .failOnError(true)
                .loadIncludes(true)
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

        val records = Util.filterCast(nodes, RecordDeclaration::class.java)

        // Extract all Variable declarations and field declarations for matching
        externalClass = getOfTypeWithName(nodes, RecordDeclaration::class.java, "ExternalClass")
        externVarName =
            getSubnodeOfTypeWithName(externalClass, FieldDeclaration::class.java, "varName")
        externStaticVarName =
            getSubnodeOfTypeWithName(externalClass, FieldDeclaration::class.java, "staticVarName")
        outerClass = getOfTypeWithName(nodes, RecordDeclaration::class.java, "ScopeVariables")
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
        val classes = Util.filterCast(nodes, RecordDeclaration::class.java)

        // Inner class and its fields
        innerClass =
            getOfTypeWithName(nodes, RecordDeclaration::class.java, "ScopeVariables::InnerClass")
        innerVarName =
            innerClass!!
                .fields
                .stream()
                .filter { n: FieldDeclaration -> n.name == "varName" }
                .findFirst()
                .get()
        innerStaticVarName =
            innerClass!!
                .fields
                .stream()
                .filter { n: FieldDeclaration -> n.name == "staticVarName" }
                .findFirst()
                .get()
        innerImpThis =
            innerClass!!
                .fields
                .stream()
                .filter { n: FieldDeclaration -> n.name == "this" }
                .findFirst()
                .get()
        main = getOfTypeWithName(nodes, FunctionDeclaration::class.java, "main")

        // Functions in the outer and inner object
        outerFunction1 =
            outerClass!!
                .methods
                .stream()
                .filter { method: MethodDeclaration -> method.name == "function1" }
                .collect(Collectors.toList())[0]
        forStatements =
            Util.filterCast(SubgraphWalker.flattenAST(outerFunction1), ForStatement::class.java)
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
        outerFunction5 =
            outerClass!!
                .methods
                .stream()
                .filter { method: MethodDeclaration -> method.name == "function5" }
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
        for (call in calls) {
            val first = call.arguments[0]
            val logId = (first as Literal<*>).value.toString()
            val second = call.arguments[1]
            callParamMap[logId] = second
        }
    }

    fun getCallWithReference(literal: String): DeclaredReferenceExpression? {
        val exp = callParamMap[literal]
        return if (exp is DeclaredReferenceExpression) exp else null
    }

    fun getCallWithMemberExpression(literal: String): MemberExpression? {
        val exp = callParamMap[literal]
        return if (exp is MemberExpression) exp else null
    }

    @Test
    fun testOuterVarNameAccessedImplicitThis() {
        VRUtil.assertUsageOf(callParamMap["func1_impl_this_varName"], outerVarName)
    }

    @Test
    fun testStaticFieldAccessedImplicitly() {
        VRUtil.assertUsageOf(callParamMap["func1_static_staticVarName"], outerStaticVarName)
    }

    @Test
    fun testVarNameOfFirstLoopAccessed() {
        val asReference = getCallWithReference("func1_first_loop_varName")
        Assertions.assertNotNull(asReference)
        val vDeclaration =
            getSubnodeOfTypeWithName(forStatements!![0], VariableDeclaration::class.java, "varName")
        VRUtil.assertUsageOf(callParamMap["func1_first_loop_varName"], vDeclaration)
    }

    @Test
    fun testAccessLocalVarNameInNestedBlock() {
        val innerBlock =
            getSubnodeOfTypeWithName(forStatements!![1], CompoundStatement::class.java, "")
        val nestedDeclaration =
            getSubnodeOfTypeWithName(innerBlock, VariableDeclaration::class.java, "varName")
        VRUtil.assertUsageOf(
            callParamMap["func1_nested_block_shadowed_local_varName"],
            nestedDeclaration
        )
    }

    @Test
    fun testVarNameOfSecondLoopAccessed() {
        val vDeclaration =
            getSubnodeOfTypeWithName(forStatements!![1], VariableDeclaration::class.java, "varName")
        VRUtil.assertUsageOf(callParamMap["func1_second_loop_varName"], vDeclaration)
    }

    @Test
    fun testParamVarNameAccessed() {
        val declaration =
            getSubnodeOfTypeWithName(
                outerFunction2,
                ParamVariableDeclaration::class.java,
                "varName"
            )
        VRUtil.assertUsageOf(callParamMap["func2_param_varName"], declaration)
    }

    @Test
    fun testMemberVarNameOverExplicitThis() {
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func2_this_varName"],
            outerImpThis,
            outerVarName
        )
    }

    @Test
    fun testVarNameDeclaredInIfClause() {
        val declaration =
            getSubnodeOfTypeWithName(
                getSubnodeOfTypeWithName(outerFunction2, IfStatement::class.java, Node.EMPTY_NAME),
                VariableDeclaration::class.java,
                "varName"
            )
        VRUtil.assertUsageOf(callParamMap["func2_if_varName"], declaration)
    }

    @Test
    fun testVarNameCoughtAsException() {
        val declaration =
            getSubnodeOfTypeWithName(
                getSubnodeOfTypeWithName(outerFunction2, CatchClause::class.java, Node.EMPTY_NAME),
                VariableDeclaration::class.java,
                "varName"
            )
        VRUtil.assertUsageOf(callParamMap["func2_catch_varName"], declaration)
    }

    @Test
    fun testMemberAccessedOverInstance() {
        val declaration =
            getSubnodeOfTypeWithName(
                outerFunction2,
                VariableDeclaration::class.java,
                "scopeVariables"
            )
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func2_instance_varName"],
            declaration,
            outerVarName
        )
    }

    @Test
    fun testMemberAccessedOverInstanceAfterParamDeclaration() {
        val declaration =
            getSubnodeOfTypeWithName(
                outerFunction3,
                VariableDeclaration::class.java,
                "scopeVariables"
            )
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func3_instance_varName"],
            declaration,
            outerVarName
        )
    }

    @Test
    fun testAccessExternalClassMemberVarnameOverInstance() {
        val declaration =
            getSubnodeOfTypeWithName(
                outerFunction3,
                VariableDeclaration::class.java,
                "externalClass"
            )
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func3_external_instance_varName"],
            declaration,
            externVarName
        )
    }

    @Test
    fun testExplicitlyReferenceStaticMemberInInternalClass() {
        VRUtil.assertUsageOf(
            callParamMap["func4_static_staticVarName"],
            outerStaticVarName!!.definition
        )
    }

    @Test
    fun testExplicitlyReferenceStaticMemberInExternalClass() {
        VRUtil.assertUsageOf(
            callParamMap["func4_external_staticVarName"],
            externStaticVarName!!.definition
        )
    }

    @Test
    fun testAccessExternalMemberOverInstance() {
        val externalInstance =
            getSubnodeOfTypeWithName(
                outerFunction4,
                VariableDeclaration::class.java,
                "externalClass"
            )
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func4_external_instance_varName"],
            externalInstance,
            externVarName
        )
    }

    @Test
    fun testAccessExternalStaticMemberAfterInstanceCreation() {
        VRUtil.assertUsageOf(
            callParamMap["func4_second_external_staticVarName"],
            externStaticVarName!!.definition
        )
    }

    @Test
    fun testAccessStaticMemberThroughInstanceFirst() {
        val declaration =
            getSubnodeOfTypeWithName(outerFunction5, VariableDeclaration::class.java, "first")
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func5_staticVarName_throughInstance_first"],
            declaration,
            outerStaticVarName!!.definition
        )
    }

    @Test
    fun testAccessStaticMemberThroughInstanceSecond() {
        val declaration =
            getSubnodeOfTypeWithName(outerFunction5, VariableDeclaration::class.java, "second")
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func5_staticVarName_throughInstance_second"],
            declaration,
            outerStaticVarName!!.definition
        )
    }

    @Test
    fun testImplicitThisAccessOfInnerClassMember() {
        VRUtil.assertUsageOf(callParamMap["func1_inner_imp_this_varName"], innerVarName)
    }

    @Test
    fun testAccessOfInnerClassMemberOverInstance() {
        val declaration =
            getSubnodeOfTypeWithName(innerFunction1, VariableDeclaration::class.java, "inner")
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func1_inner_instance_varName"],
            declaration,
            innerVarName
        )
    }

    @Test
    fun testAccessOfOuterMemberOverInstance() {
        val declaration =
            getSubnodeOfTypeWithName(
                innerFunction1,
                VariableDeclaration::class.java,
                "scopeVariables"
            )
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func1_outer_instance_varName"],
            declaration,
            outerVarName
        )
    }

    @Test
    fun testAccessOfOuterStaticMember() {
        VRUtil.assertUsageOf(
            callParamMap["func1_outer_static_staticVarName"],
            outerStaticVarName!!.definition
        )
    }

    @Test
    fun testAccessOfInnerStaticMember() {
        VRUtil.assertUsageOf(
            callParamMap["func1_inner_static_staticVarName"],
            innerStaticVarName!!.definition
        )
    }

    @Test
    fun testAccessOfInnerClassMemberOverInstanceWithSameNamedVariable() {
        val declaration =
            getSubnodeOfTypeWithName(innerFunction2, VariableDeclaration::class.java, "inner")
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func2_inner_instance_varName_with_shadows"],
            declaration,
            innerVarName
        )
    }

    @Test
    fun testAccessOfOuterMemberOverInstanceWithSameNamedVariable() {
        val declaration =
            getSubnodeOfTypeWithName(
                innerFunction2,
                VariableDeclaration::class.java,
                "scopeVariables"
            )
        VRUtil.assertUsageOfMemberAndBase(
            callParamMap["func2_outer_instance_varName_with_shadows"],
            declaration,
            outerVarName
        )
    }

    @Test
    fun testAccessOfOuterStaticMembertWithSameNamedVariable() {
        VRUtil.assertUsageOf(
            callParamMap["func2_outer_static_staticVarName_with_shadows"],
            outerStaticVarName!!.definition
        )
    }

    @Test
    fun testAccessOfInnerStaticMemberWithSameNamedVariable() {
        VRUtil.assertUsageOf(
            callParamMap["func2_inner_static_staticVarName_with_shadows"],
            innerStaticVarName!!.definition
        )
    }

    @Test
    fun testLocalVariableUsedAsParameter() {
        val declaration = getSubnodeOfTypeWithName(main, VariableDeclaration::class.java, "varName")
        VRUtil.assertUsageOf(callParamMap["main_local_varName"], declaration)
    }
}
