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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.CatchClause
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import java.nio.file.Path
import java.util.concurrent.ExecutionException
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VariableResolverCppTest : BaseTest() {
    private var externalClass: RecordDeclaration? = null
    private var externVarName: FieldDeclaration? = null
    private var externStaticVarName: FieldDeclaration? = null
    private var outerClass: RecordDeclaration? = null
    private var outerVarName: FieldDeclaration? = null
    private var outerStaticVarName: FieldDeclaration? = null
    private var function2Receiver: VariableDeclaration? = null
    private var innerClass: RecordDeclaration? = null
    private var innerVarName: FieldDeclaration? = null
    private var innerStaticVarName: FieldDeclaration? = null
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
        val topLevel = Path.of("src/test/resources/variables_extended/cpp/")
        val files =
            listOf("scope_variables.cpp", "external_class.cpp").map {
                topLevel.resolve(it).toFile()
            }
        val result = analyze(files, topLevel, true)
        val calls = result.calls { it.name == "printLog" }
        val records = result.records
        val functions = result.functions

        // Extract all Variable declarations and field declarations for matching
        externalClass = records["ExternalClass"]
        externVarName = externalClass.fields["varName"]
        externStaticVarName = externalClass.fields["staticVarName"]
        outerClass = records["ScopeVariables"]
        outerVarName = outerClass?.byNameOrNull("varName")
        outerStaticVarName = outerClass?.byNameOrNull("staticVarName")
        function2Receiver = outerClass?.byNameOrNull<MethodDeclaration>("function2")?.receiver

        // Inner class and its fields
        innerClass = records["ScopeVariables::InnerClass"]
        innerVarName = innerClass.fields["varName"]
        innerStaticVarName = innerClass.fields["staticVarName"]
        main = functions["main"]

        // Functions in the outer and inner object
        outerFunction1 =
            outerClass?.methods?.first { method: MethodDeclaration -> method.name == "function1" }
        forStatements = outerFunction1.allChildren()
        outerFunction2 =
            outerClass?.methods?.first { method: MethodDeclaration -> method.name == "function2" }
        outerFunction3 =
            outerClass?.methods?.first { method: MethodDeclaration -> method.name == "function3" }
        outerFunction4 =
            outerClass?.methods?.first { method: MethodDeclaration -> method.name == "function4" }
        outerFunction5 =
            outerClass?.methods?.first { method: MethodDeclaration -> method.name == "function5" }
        innerFunction1 =
            innerClass?.methods?.first { method: MethodDeclaration -> method.name == "function1" }
        innerFunction2 =
            innerClass?.methods?.first { method: MethodDeclaration -> method.name == "function2" }
        for (call in calls) {
            val first = call.arguments[0]
            val logId = (first as Literal<*>).value.toString()
            val second = call.arguments[1]
            callParamMap[logId] = second
        }
    }

    @Test
    fun testOuterVarNameAccessedImplicitThis() {
        assertUsageOf(callParamMap["func1_impl_this_varName"], outerVarName)
    }

    @Test
    fun testStaticFieldAccessedImplicitly() {
        assertUsageOf(callParamMap["func1_static_staticVarName"], outerStaticVarName)
    }

    @Test
    fun testVarNameOfFirstLoopAccessed() {
        val asReference = callParamMap["func1_first_loop_varName"] as? DeclaredReferenceExpression
        assertNotNull(asReference)
        val vDeclaration = forStatements?.first().variables["varName"]
        assertUsageOf(callParamMap["func1_first_loop_varName"], vDeclaration)
    }

    @Test
    fun testAccessLocalVarNameInNestedBlock() {
        val innerBlock = forStatements?.get(1).allChildren<CompoundStatement>()[""]
        val nestedDeclaration = innerBlock.variables["varName"]
        assertUsageOf(callParamMap["func1_nested_block_shadowed_local_varName"], nestedDeclaration)
    }

    @Test
    fun testVarNameOfSecondLoopAccessed() {
        val vDeclaration = forStatements?.get(1).variables["varName"]
        assertUsageOf(callParamMap["func1_second_loop_varName"], vDeclaration)
    }

    @Test
    fun testParamVarNameAccessed() {
        val declaration = outerFunction2?.parameters["varName"]
        assertUsageOf(callParamMap["func2_param_varName"], declaration)
    }

    @Test
    fun testMemberVarNameOverExplicitThis() {
        assertUsageOfMemberAndBase(
            callParamMap["func2_this_varName"],
            function2Receiver,
            outerVarName
        )
    }

    @Test
    fun testVarNameDeclaredInIfClause() {
        val declaration =
            outerFunction2.allChildren<IfStatement>()[Node.EMPTY_NAME].variables["varName"]
        assertUsageOf(callParamMap["func2_if_varName"], declaration)
    }

    @Test
    fun testVarNameCoughtAsException() {
        val declaration = outerFunction2.allChildren<CatchClause>()[""].variables["varName"]
        assertUsageOf(callParamMap["func2_catch_varName"], declaration)
    }

    @Test
    fun testMemberAccessedOverInstance() {
        val declaration = outerFunction2.variables["scopeVariables"]
        assertUsageOfMemberAndBase(
            callParamMap["func2_instance_varName"],
            declaration,
            outerVarName
        )
    }

    @Test
    fun testMemberAccessedOverInstanceAfterParamDeclaration() {
        val declaration = outerFunction3.variables["scopeVariables"]
        assertUsageOfMemberAndBase(
            callParamMap["func3_instance_varName"],
            declaration,
            outerVarName
        )
    }

    @Test
    fun testAccessExternalClassMemberVarnameOverInstance() {
        val declaration = outerFunction3.variables["externalClass"]
        assertUsageOfMemberAndBase(
            callParamMap["func3_external_instance_varName"],
            declaration,
            externVarName
        )
    }

    @Test
    fun testExplicitlyReferenceStaticMemberInInternalClass() {
        assertUsageOf(callParamMap["func4_static_staticVarName"], outerStaticVarName?.definition)
    }

    @Test
    fun testExplicitlyReferenceStaticMemberInExternalClass() {
        assertUsageOf(callParamMap["func4_external_staticVarName"], externStaticVarName?.definition)
    }

    @Test
    fun testAccessExternalMemberOverInstance() {
        val externalInstance = outerFunction4.variables["externalClass"]
        assertUsageOfMemberAndBase(
            callParamMap["func4_external_instance_varName"],
            externalInstance,
            externVarName
        )
    }

    @Test
    fun testAccessExternalStaticMemberAfterInstanceCreation() {
        assertUsageOf(
            callParamMap["func4_second_external_staticVarName"],
            externStaticVarName?.definition
        )
    }

    @Test
    fun testAccessStaticMemberThroughInstanceFirst() {
        val declaration = outerFunction5.variables["first"]
        assertUsageOfMemberAndBase(
            callParamMap["func5_staticVarName_throughInstance_first"],
            declaration,
            outerStaticVarName?.definition
        )
    }

    @Test
    fun testAccessStaticMemberThroughInstanceSecond() {
        val declaration = outerFunction5.variables["second"]
        assertUsageOfMemberAndBase(
            callParamMap["func5_staticVarName_throughInstance_second"],
            declaration,
            outerStaticVarName?.definition
        )
    }

    @Test
    fun testImplicitThisAccessOfInnerClassMember() {
        assertUsageOf(callParamMap["func1_inner_imp_this_varName"], innerVarName)
    }

    @Test
    fun testAccessOfInnerClassMemberOverInstance() {
        val declaration = innerFunction1.variables["inner"]
        assertUsageOfMemberAndBase(
            callParamMap["func1_inner_instance_varName"],
            declaration,
            innerVarName
        )
    }

    @Test
    fun testAccessOfOuterMemberOverInstance() {
        val declaration = innerFunction1.variables["scopeVariables"]
        assertUsageOfMemberAndBase(
            callParamMap["func1_outer_instance_varName"],
            declaration,
            outerVarName
        )
    }

    @Test
    fun testAccessOfOuterStaticMember() {
        assertUsageOf(
            callParamMap["func1_outer_static_staticVarName"],
            outerStaticVarName?.definition
        )
    }

    @Test
    fun testAccessOfInnerStaticMember() {
        assertUsageOf(
            callParamMap["func1_inner_static_staticVarName"],
            innerStaticVarName?.definition
        )
    }

    @Test
    fun testAccessOfInnerClassMemberOverInstanceWithSameNamedVariable() {
        val declaration = innerFunction2.variables["inner"]
        assertUsageOfMemberAndBase(
            callParamMap["func2_inner_instance_varName_with_shadows"],
            declaration,
            innerVarName
        )
    }

    @Test
    fun testAccessOfOuterMemberOverInstanceWithSameNamedVariable() {
        val declaration = innerFunction2.variables["scopeVariables"]
        assertUsageOfMemberAndBase(
            callParamMap["func2_outer_instance_varName_with_shadows"],
            declaration,
            outerVarName
        )
    }

    @Test
    fun testAccessOfOuterStaticMemberWithSameNamedVariable() {
        assertUsageOf(
            callParamMap["func2_outer_static_staticVarName_with_shadows"],
            outerStaticVarName?.definition
        )
    }

    @Test
    fun testAccessOfInnerStaticMemberWithSameNamedVariable() {
        assertUsageOf(
            callParamMap["func2_inner_static_staticVarName_with_shadows"],
            innerStaticVarName?.definition
        )
    }

    @Test
    fun testLocalVariableUsedAsParameter() {
        val declaration = main.variables["varName"]
        assertUsageOf(callParamMap["main_local_varName"], declaration)
    }
}
