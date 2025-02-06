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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.invoke
import de.fraunhofer.aisec.cpg.graph.refs
import de.fraunhofer.aisec.cpg.graph.scopes.LocalScope
import de.fraunhofer.aisec.cpg.graph.statements
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CollectionComprehension
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.KeyValueExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.variables
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertLocalName
import de.fraunhofer.aisec.cpg.test.assertNotRefersTo
import de.fraunhofer.aisec.cpg.test.assertRefersTo
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollectionComprehensionPython3Test {
    private lateinit var result: TranslationResult

    @BeforeAll
    fun setup() {
        val topLevel = Path.of("src", "test", "resources", "python")
        result =
            analyze(listOf(topLevel.resolve("comprehension.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
                it.symbols(
                    mapOf(
                        "PYTHON_VERSION_MAJOR" to "3",
                        "PYTHON_VERSION_MINOR" to "0",
                        "PYTHON_VERSION_MICRO" to "0",
                    )
                )
            }
        assertNotNull(result)
    }

    @Test
    fun testComprehensionExpressionTuple() {
        // Get the function tuple_comp
        val tupleComp = result.functions["tuple_comp"]
        assertIs<FunctionDeclaration>(
            tupleComp,
            "There must be a function called \"tuple_comp\" in the file. It must be neither null nor any other class than a FunctionDeclaration.",
        )

        // Get the body
        val body = tupleComp.body
        assertIs<Block>(
            body,
            "The body of each function is modeled as a Block in the CPG. This must also apply to the function \"tuple_comp\".",
        )
        // The first statement is an assigment of a list comprehension with an if to a variable "a"
        val tupleAsVariableAssignment = body.statements[0]
        assertIs<AssignExpression>(
            tupleAsVariableAssignment,
            "The statement is an AssignExpression",
        )
        val tupleAsVariable = tupleAsVariableAssignment.rhs[0]
        assertIs<CollectionComprehension>(
            tupleAsVariable,
            "The right hand side must be a CollectionComprehension representing python's list comprehension \"[bar(k, v) for (k, v) in x]\".",
        )
        val barCall = tupleAsVariable.statement
        assertIs<CallExpression>(
            barCall,
            "The statement inside the list comprehension is a call to bar with arguments k and v",
        )
        assertLocalName("bar", barCall, "The CallExpression calls bar()")
        val argK = barCall.arguments[0]
        assertIs<Reference>(argK, "The first argument of bar() is a reference k")
        assertLocalName("k", argK, "The first argument of bar() is a reference k")
        val argV = barCall.arguments[1]
        assertIs<Reference>(argV, "The second argument of bar() is a reference v")
        assertLocalName("v", argV, "The second argument of bar() is a reference v")
        assertEquals(
            1,
            tupleAsVariable.comprehensionExpressions.size,
            "There is a single comprehension expression (\"for (k, v) in x\")",
        )
        val initializerListExpression = tupleAsVariable.comprehensionExpressions[0].variable
        assertIs<InitializerListExpression>(
            initializerListExpression,
            "The variable is actually tuple which is represented as an InitializerListExpression in the CPG",
        )
        val variableK = initializerListExpression.initializers[0]
        assertIs<Reference>(
            variableK,
            "The first element in the tuple is a variable reference \"k\"",
        )
        assertLocalName(
            "k",
            variableK,
            "The first element in the tuple is a variable reference \"k\"",
        )
        val variableV = initializerListExpression.initializers[1]
        assertIs<Reference>(
            variableV,
            "The second element in the tuple is a variable reference \"v\"",
        )
        assertLocalName(
            "v",
            variableV,
            "The second element in the tuple is a variable reference \"V\"",
        )

        // Check that the declarations exist for the variables k and v
        val declK = variableK.refersTo
        assertIs<VariableDeclaration>(declK, "The refersTo should be a VariableDeclaration")
        assertIs<LocalScope>(
            declK.scope,
            "The scope of the variable is the local scope belonging to the list comprehension. In particular, it is not the FunctionScope.",
        )
        assertEquals(
            tupleAsVariable,
            declK.scope?.astNode,
            "The scope of the variable is the local scope belonging to the list comprehension. In particular, it is not the FunctionScope.",
        )
        assertRefersTo(
            argK,
            declK,
            "The argument k of the call also refers to the variable k declared in the comprehension expression.",
        )
        val declV = variableV.refersTo
        assertIs<VariableDeclaration>(declV, "The refersTo should be a VariableDeclaration")
        assertIs<LocalScope>(
            declV.scope,
            "The scope of the variable is the local scope belonging to the list comprehension. In particular, it is not the FunctionScope.",
        )
        assertEquals(
            tupleAsVariable,
            declV.scope?.astNode,
            "The scope of the variable is the local scope belonging to the list comprehension. In particular, it is not the FunctionScope.",
        )
        assertRefersTo(
            argV,
            declV,
            "The argument v of the call also refers to the variable v declared in the comprehension expression.",
        )
    }

    @Test
    fun testListComprehensions() {
        val listCompFunctionDeclaration = result.functions["list_comp"]
        assertIs<FunctionDeclaration>(
            listCompFunctionDeclaration,
            "There must be a function called \"list_comp\" in the file. It must be neither null nor any other class than a FunctionDeclaration.",
        )

        val paramX = listCompFunctionDeclaration.parameters[0]
        assertIs<ParameterDeclaration>(
            paramX,
            "The function \"list_comp\" has a parameter called \"x^\".",
        )
        assertLocalName("x", paramX, "The function \"list_comp\" has a parameter called \"x\".")

        val body = listCompFunctionDeclaration.body
        assertIs<Block>(
            body,
            "The body of each function is modeled as a Block in the CPG. This must also apply to the function \"list_comp\".",
        )
        val singleWithIfAssignment = body.statements[0]
        assertIs<AssignExpression>(singleWithIfAssignment)
        val singleWithIf = singleWithIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithIf)
        val fooCall = singleWithIf.statement
        assertIs<CallExpression>(fooCall)
        val usageI = fooCall.arguments[0]
        assertIs<Reference>(usageI)
        assertEquals(1, singleWithIf.comprehensionExpressions.size)
        val variableI = singleWithIf.comprehensionExpressions[0].variable
        assertIs<Reference>(variableI)
        assertLocalName("i", variableI)
        val declI = variableI.refersTo
        assertIs<VariableDeclaration>(declI)
        assertEquals(singleWithIf, declI.scope?.astNode)
        val iterableX = singleWithIf.comprehensionExpressions[0].iterable
        assertIs<Reference>(iterableX)
        assertLocalName("x", iterableX)
        assertRefersTo(iterableX, paramX)
        val ifPredicate = singleWithIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(ifPredicate)
        assertEquals("==", ifPredicate.operatorCode)
        assertRefersTo(usageI, declI)

        val fooIOutside = body.statements[4]
        assertIs<CallExpression>(fooIOutside)
        val outsideI = fooIOutside.arguments[0]
        assertIs<Reference>(outsideI)
        assertLocalName("i", outsideI)
        assertNotRefersTo(outsideI, declI)

        val singleWithoutIfAssignment = body.statements[1]
        assertIs<AssignExpression>(singleWithoutIfAssignment)
        val singleWithoutIf = singleWithoutIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithoutIf)
        assertIs<CallExpression>(singleWithoutIf.statement)
        assertEquals(1, singleWithoutIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithoutIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithoutIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithoutIf.comprehensionExpressions[0].iterable)
        assertNull(singleWithoutIf.comprehensionExpressions[0].predicate)

        val singleWithDoubleIfAssignment = body.statements[2]
        assertIs<AssignExpression>(singleWithDoubleIfAssignment)
        val singleWithDoubleIf = singleWithDoubleIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithDoubleIf)
        assertIs<CallExpression>(singleWithDoubleIf.statement)
        assertEquals(1, singleWithDoubleIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithDoubleIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithDoubleIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithDoubleIf.comprehensionExpressions[0].iterable)
        val doubleIfPredicate = singleWithDoubleIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(doubleIfPredicate)
        assertEquals("and", doubleIfPredicate.operatorCode)

        val doubleAssignment = body.statements[3] as? AssignExpression
        assertIs<AssignExpression>(doubleAssignment)
        val double = doubleAssignment.rhs[0] as? CollectionComprehension
        assertNotNull(double)
        assertIs<CallExpression>(double.statement)
        assertEquals(2, double.comprehensionExpressions.size)
    }

    @Test
    fun testSetComprehensions() {
        val setCompFunctionDeclaration = result.functions["set_comp"]
        assertIs<FunctionDeclaration>(
            setCompFunctionDeclaration,
            "There must be a function called \"set_comp\" in the file. It must be neither null nor any other class than a FunctionDeclaration.",
        )

        val body = setCompFunctionDeclaration.body
        assertIs<Block>(
            body,
            "The body of each function is modeled as a Block in the CPG. This must also apply to the function \"set_comp\".",
        )
        val singleWithIfAssignment = body.statements[0]
        assertIs<AssignExpression>(singleWithIfAssignment)
        val singleWithIf = singleWithIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithIf)
        assertIs<CallExpression>(singleWithIf.statement)
        assertEquals(1, singleWithIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithIf.comprehensionExpressions[0].iterable)
        val ifPredicate = singleWithIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(ifPredicate)
        assertEquals("==", ifPredicate.operatorCode)

        val singleWithoutIfAssignment = body.statements[1]
        assertIs<AssignExpression>(singleWithoutIfAssignment)
        val singleWithoutIf = singleWithoutIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithoutIf)
        assertIs<CallExpression>(singleWithoutIf.statement)
        assertEquals(1, singleWithoutIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithoutIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithoutIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithoutIf.comprehensionExpressions[0].iterable)
        assertNull(singleWithoutIf.comprehensionExpressions[0].predicate)

        val singleWithDoubleIfAssignment = body.statements[2]
        assertIs<AssignExpression>(singleWithDoubleIfAssignment)
        val singleWithDoubleIf = singleWithDoubleIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithDoubleIf)
        assertIs<CallExpression>(singleWithDoubleIf.statement)
        assertEquals(1, singleWithDoubleIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithDoubleIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithDoubleIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithDoubleIf.comprehensionExpressions[0].iterable)
        val doubleIfPredicate = singleWithDoubleIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(doubleIfPredicate)
        assertEquals("and", doubleIfPredicate.operatorCode)

        val doubleAssignment = body.statements[3]
        assertIs<AssignExpression>(doubleAssignment)
        val double = doubleAssignment.rhs[0]
        assertIs<CollectionComprehension>(double)
        assertIs<CallExpression>(double.statement)
        assertEquals(2, double.comprehensionExpressions.size)
    }

    @Test
    fun testDictComprehensions() {
        val dictCompFunctionDeclaration = result.functions["dict_comp"]
        assertIs<FunctionDeclaration>(
            dictCompFunctionDeclaration,
            "There must be a function called \"dict_comp\" in the file. It must be neither null nor any other class than a FunctionDeclaration.",
        )

        val body = dictCompFunctionDeclaration.body
        assertIs<Block>(
            body,
            "The body of each function is modeled as a Block in the CPG. This must also apply to the function \"dict_comp\".",
        )
        val singleWithIfAssignment = body.statements[0]
        assertIs<AssignExpression>(singleWithIfAssignment)
        val singleWithIf = singleWithIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithIf)
        var statement = singleWithIf.statement
        assertIs<KeyValueExpression>(statement)
        assertIs<Reference>(statement.key)
        assertLocalName("i", statement.key)
        assertIs<CallExpression>(statement.value)
        assertEquals(1, singleWithIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithIf.comprehensionExpressions[0].iterable)
        val ifPredicate = singleWithIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(ifPredicate)
        assertEquals("==", ifPredicate.operatorCode)

        val singleWithoutIfAssignment = body.statements[1]
        assertIs<AssignExpression>(singleWithoutIfAssignment)
        val singleWithoutIf = singleWithoutIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithoutIf)
        statement = singleWithIf.statement
        assertIs<KeyValueExpression>(statement)
        assertIs<Reference>(statement.key)
        assertLocalName("i", statement.key)
        assertIs<CallExpression>(statement.value)
        assertEquals(1, singleWithoutIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithoutIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithoutIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithoutIf.comprehensionExpressions[0].iterable)
        assertNull(singleWithoutIf.comprehensionExpressions[0].predicate)

        val singleWithDoubleIfAssignment = body.statements[2]
        assertIs<AssignExpression>(singleWithDoubleIfAssignment)
        val singleWithDoubleIf = singleWithDoubleIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithDoubleIf)
        statement = singleWithIf.statement
        assertIs<KeyValueExpression>(statement)
        assertIs<Reference>(statement.key)
        assertLocalName("i", statement.key)
        assertIs<CallExpression>(statement.value)
        assertEquals(1, singleWithDoubleIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithDoubleIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithDoubleIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithDoubleIf.comprehensionExpressions[0].iterable)
        val doubleIfPredicate = singleWithDoubleIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(doubleIfPredicate)
        assertEquals("and", doubleIfPredicate.operatorCode)

        val doubleAssignment = body.statements[3] as? AssignExpression
        assertIs<AssignExpression>(doubleAssignment)
        val double = doubleAssignment.rhs[0] as? CollectionComprehension
        assertNotNull(double)
        statement = singleWithIf.statement
        assertIs<KeyValueExpression>(statement)
        assertIs<Reference>(statement.key)
        assertLocalName("i", statement.key)
        assertIs<CallExpression>(statement.value)
        assertEquals(2, double.comprehensionExpressions.size)
    }

    @Test
    fun testGeneratorExpr() {
        val generatorFunctionDeclaration = result.functions["generator"]
        assertIs<FunctionDeclaration>(
            generatorFunctionDeclaration,
            "There must be a function called \"generator\" in the file. It must be neither null nor any other class than a FunctionDeclaration.",
        )

        val body = generatorFunctionDeclaration.body
        assertIs<Block>(
            body,
            "The body of each function is modeled as a Block in the CPG. This must also apply to the function \"generator\".",
        )
        val singleWithIfAssignment = body.statements[0]
        assertIs<AssignExpression>(singleWithIfAssignment)
        val singleWithIf = singleWithIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithIf)
        assertIs<BinaryOperator>(singleWithIf.statement)
        assertEquals(1, singleWithIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithIf.comprehensionExpressions[0].variable)
        assertIs<CallExpression>(singleWithIf.comprehensionExpressions[0].iterable)
        assertLocalName("range", singleWithIf.comprehensionExpressions[0].iterable)
        val ifPredicate = singleWithIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(ifPredicate)
        assertEquals("==", ifPredicate.operatorCode)

        val singleWithoutIfAssignment = body.statements[1]
        assertIs<AssignExpression>(singleWithoutIfAssignment)
        val singleWithoutIf = singleWithoutIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithoutIf)
        assertIs<BinaryOperator>(singleWithoutIf.statement)
        assertEquals(1, singleWithoutIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithoutIf.comprehensionExpressions[0].variable)
        assertIs<CallExpression>(singleWithIf.comprehensionExpressions[0].iterable)
        assertLocalName("range", singleWithIf.comprehensionExpressions[0].iterable)
        assertNull(singleWithoutIf.comprehensionExpressions[0].predicate)
    }

    @Test
    /**
     * This test ensures that variables in a comprehension do not bind to the outer scope. See
     * [testCompBindingAssignExpr] for exceptions.
     */
    fun testCompBinding() {
        val compBindingFunctionDeclaration = result.functions["comp_binding"]
        assertIs<FunctionDeclaration>(
            compBindingFunctionDeclaration,
            "There must be a function called \"comp_binding\" in the file. It must be neither null nor any other class than a FunctionDeclaration.",
        )

        val xDecl = compBindingFunctionDeclaration.variables.firstOrNull()
        assertIs<VariableDeclaration>(xDecl)

        assertEquals(
            5,
            compBindingFunctionDeclaration.variables.size,
            "Expected five variables. One for the \"outside\" x and one for each of the four comprehensions.",
        )

        assertEquals(
            2,
            xDecl.usages.size,
            "Expected two usages: one for the initial assignment and one for the usage in \"print(x)\".",
        )

        val comprehensions =
            compBindingFunctionDeclaration.body.statements.filterIsInstance<
                CollectionComprehension
            >()
        assertEquals(4, comprehensions.size, "Expected to find four comprehensions.")

        comprehensions.forEach { it.refs("x").forEach { ref -> assertNotRefersTo(ref, xDecl) } }
    }

    @Test
    /**
     * This test ensures that variables in a comprehension do not bind to the outer scope if they
     * are used in an `AssignExpr`. See https://peps.python.org/pep-0572/#scope-of-the-target
     */
    fun testCompBindingAssignExpr() {
        val compBindingAssignExprFunctionDeclaration = result.functions["comp_binding_assign_expr"]
        assertIs<FunctionDeclaration>(
            compBindingAssignExprFunctionDeclaration,
            "There must be a function called \"comp_binding_assign_expr\" in the file. It must be neither null nor any other class than a FunctionDeclaration.",
        )

        val xDecl = compBindingAssignExprFunctionDeclaration.variables["x"]
        assertIs<VariableDeclaration>(
            xDecl,
            "There must be a VariableDeclaration with the local name \"x\" inside the function.",
        )

        assertEquals(
            2,
            compBindingAssignExprFunctionDeclaration.variables.size,
            "Expected two variables. One for the \"outside\" x and one for the \"temp\" inside the comprehension.",
        )

        assertEquals(
            3,
            xDecl.usages.size,
            "Expected three usages: one for the initial assignment, one for the comprehension and one for the usage in \"print(x)\".",
        )

        val comprehension =
            compBindingAssignExprFunctionDeclaration.body.statements.singleOrNull {
                it is CollectionComprehension
            }
        assertNotNull(comprehension)
        val xRef = comprehension.refs("x").singleOrNull()
        assertNotNull(xRef)
        assertRefersTo(xRef, xDecl)
    }

    @Test
    /**
     * This test ensures that variables in a comprehension do not bind to the outer scope if they
     * are used in an `AssignExpr`. See https://peps.python.org/pep-0572/#scope-of-the-target
     */
    fun testCompBindingAssignExprNested() {
        val compBindingAssignExprNestedFunctionDeclaration =
            result.functions["comp_binding_assign_expr_nested"]
        assertIs<FunctionDeclaration>(
            compBindingAssignExprNestedFunctionDeclaration,
            "There must be a function called \"comp_binding_assign_expr_nested\" in the file. It must be neither null nor any other class than a FunctionDeclaration.",
        )

        val xDecl = compBindingAssignExprNestedFunctionDeclaration.variables["x"]
        assertIs<VariableDeclaration>(
            xDecl,
            "There must be a VariableDeclaration with the local name \"x\" inside the function.",
        )

        assertEquals(
            3,
            compBindingAssignExprNestedFunctionDeclaration.variables.size,
            "Expected two variables. One for the \"outside\" x, one for the \"temp\" inside the comprehension and one for the \"a\" inside the comprehension.",
        )

        assertEquals(
            3,
            xDecl.usages.size,
            "Expected three usages: one for the initial assignment, one for the comprehension and one for the usage in \"print(x)\".",
        )
        val body = compBindingAssignExprNestedFunctionDeclaration.body
        assertIs<Block>(body, "The body of a function must be a Block.")
        val outerComprehension = body.statements.singleOrNull { it is CollectionComprehension }
        assertIs<CollectionComprehension>(
            outerComprehension,
            "There must be exactly one CollectionComprehension (the list comprehension) in the statement of the body. Note: The inner collection comprehension would be reached by the extension function Node::statements which does not apply here.",
        )
        val innerComprehension = outerComprehension.statement
        assertIs<CollectionComprehension>(
            innerComprehension,
            "The inner comprehension is the statement of the outer list comprehension",
        )
        val xRef = innerComprehension.refs("x").singleOrNull()
        assertNotNull(
            xRef,
            "There is only one usage of \"x\" which is inside the inner comprehension's statement.",
        )
        assertRefersTo(
            xRef,
            xDecl,
            "The reference of \"x\" inside the inner comprehension's statement refers to the variable declared outside the comprehensions.",
        )
    }
}
