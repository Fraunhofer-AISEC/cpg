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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.FunctionScope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollectionComprehensionPython2Test {

    private lateinit var result: TranslationResult

    @BeforeAll
    fun setup() {
        val topLevel = Path.of("src", "test", "resources", "python")
        result =
            analyze(listOf(topLevel.resolve("comprehension.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
                it.symbols(
                    mapOf(
                        "PYTHON_VERSION_MAJOR" to "2",
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
        // The first statement is expected to be an assigment of a list comprehension with an if to
        // a variable "a"
        val tupleAsVariableAssignment = body.statements[0]
        assertIs<AssignExpression>(
            tupleAsVariableAssignment,
            "The statement is expected to be an AssignExpression",
        )
        val tupleAsVariable = tupleAsVariableAssignment.rhs[0]
        assertIs<CollectionComprehension>(
            tupleAsVariable,
            "The right hand side must be a CollectionComprehension representing python's list comprehension \"[bar(k, v) for (k, v) in x]\".",
        )
        val barCall = tupleAsVariable.statement
        assertIs<CallExpression>(
            barCall,
            "The statement inside the list comprehension is expected to be a call to bar with arguments k and v",
        )
        assertLocalName("bar", barCall, "The CallExpression calls bar()")
        val argK = barCall.arguments[0]
        assertIs<Reference>(argK, "The first argument of bar() is expected to be a reference k")
        assertLocalName("k", argK, "The first argument of bar() is expected to be a reference k")
        val argV = barCall.arguments[1]
        assertIs<Reference>(argV, "The second argument of bar() is expected to be a reference v")
        assertLocalName("v", argV, "The second argument of bar() is expected to be a reference v")
        assertEquals(
            1,
            tupleAsVariable.comprehensionExpressions.size,
            "There is expected to be a single comprehension expression (\"for (k, v) in x\")",
        )
        val initializerListExpression = tupleAsVariable.comprehensionExpressions[0].variable
        assertIs<InitializerListExpression>(
            initializerListExpression,
            "The variable is expected to be actually tuple which is represented as an InitializerListExpression in the CPG",
        )
        val variableK = initializerListExpression.initializers[0]
        assertIs<Reference>(
            variableK,
            "The first element in the tuple is expected to be a variable reference \"k\"",
        )
        assertLocalName(
            "k",
            variableK,
            "The first element in the tuple is expected to be a variable reference \"k\"",
        )
        val variableV = initializerListExpression.initializers[1]
        assertIs<Reference>(
            variableV,
            "The second element in the tuple is expected to be a variable reference \"v\"",
        )
        assertLocalName(
            "v",
            variableV,
            "The second element in the tuple is expected to be a variable reference \"V\"",
        )

        // Check that the declarations exist for the variables k and v
        val declK = variableK.refersTo
        assertIs<VariableDeclaration>(declK, "The refersTo should be a VariableDeclaration")
        assertIs<FunctionScope>(
            declK.scope,
            "The scope of the variable is expected to be the function scope belonging to the function declaration. In particular, it is not the LocalScope.",
        )
        assertEquals(
            tupleComp,
            declK.scope?.astNode,
            "The scope of the variable  is expected to be the function scope belonging to the function declaration. In particular, it is not the LocalScope.",
        )
        assertRefersTo(
            argK,
            declK,
            "The argument k of the call also refers to the variable k declared in the comprehension expression.",
        )
        val declV = variableV.refersTo
        assertIs<VariableDeclaration>(declV, "The refersTo should be a VariableDeclaration")
        assertIs<FunctionScope>(
            declV.scope,
            "The scope of the variable is expected to be the function scope belonging to the function declaration. In particular, it is not the LocalScope.",
        )
        assertEquals(
            tupleComp,
            declV.scope?.astNode,
            "The scope of the variable is expected to be the function scope belonging to the function declaration. In particular, it is not the LocalScope.",
        )
        assertRefersTo(
            argV,
            declV,
            "The argument v of the call also refers to the variable v declared in the comprehension expression.",
        )
    }

    @Test
    fun testListComprehensions() {
        val listComp = result.functions["list_comp"]
        assertNotNull(listComp)
        val paramX = listComp.parameters[0]
        assertIs<ParameterDeclaration>(paramX)
        assertLocalName("x", paramX)

        val body = listComp.body
        assertIs<Block>(body)
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
        assertEquals(listComp, declI.scope?.astNode)
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
        assertRefersTo(outsideI, declI)

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
        // TODO: Add tests on the comprehension expressions
    }

    @Test
    fun testSetComprehensions() {
        val listComp = result.functions["set_comp"]
        assertNotNull(listComp)

        val body = listComp.body as? Block
        assertNotNull(body)
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
        val listComp = result.functions["dict_comp"]
        assertNotNull(listComp)

        val body = listComp.body as? Block
        assertNotNull(body)
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
        val listComp = result.functions["generator"]
        assertNotNull(listComp)

        val body = listComp.body as? Block
        assertNotNull(body)
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
        val compBindingFunc = result.functions["comp_binding"]
        assertIs<FunctionDeclaration>(compBindingFunc)

        val xDecl = compBindingFunc.variables.firstOrNull()
        assertIs<VariableDeclaration>(xDecl)

        assertEquals(
            1,
            compBindingFunc.variables.size,
            "Expected one variable where all other \"x\" refer to",
        )

        assertEquals(
            11,
            xDecl.usages.size,
            "Expected eleven usages: one for the initial assignment and one for the usage in \"print(x)\" and nine inside the list/set/dict comprehensions.",
        )

        val comprehensions =
            compBindingFunc.body.statements.filterIsInstance<CollectionComprehension>()
        assertEquals(4, comprehensions.size, "Expected to find four comprehensions.")

        comprehensions.forEach { it.refs("x").forEach { ref -> assertRefersTo(ref, xDecl) } }
    }

    @Test
    /**
     * This test ensures that variables in a comprehension do not bind to the outer scope if they
     * are used in an `AssignExpr`. See https://peps.python.org/pep-0572/#scope-of-the-target
     */
    fun testCompBindingAssignExpr() {
        val compBindingAssignFunc = result.functions["comp_binding_assign_expr"]
        assertIs<FunctionDeclaration>(compBindingAssignFunc)

        val xDecl = compBindingAssignFunc.variables.firstOrNull()
        assertIs<VariableDeclaration>(xDecl)

        assertEquals(
            1,
            compBindingAssignFunc.variables.size,
            "Expected two variables. One for the \"outside\" x.",
        )

        assertEquals(
            3,
            xDecl.usages.size,
            "Expected three usages: one for the initial assignment, one for the comprehension and one for the usage in \"print(x)\".",
        )

        val comprehension =
            compBindingAssignFunc.body.statements.singleOrNull { it is CollectionComprehension }
        assertNotNull(comprehension)
        val xRef = comprehension.refs("x").singleOrNull()
        assertNotNull(xRef)
        assertRefersTo(xRef, xDecl)

        // TODO: test for other types of comprehensions
        // TODO: test nested comprehensions -> AssignExpression binds to
        // containing scope
    }
}
