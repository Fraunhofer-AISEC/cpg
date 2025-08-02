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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ast.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.CollectionComprehension
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.ComprehensionExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.InitializerListExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.KeyValueExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.SubscriptExpression
import de.fraunhofer.aisec.cpg.graph.edges.flows.IndexedDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.invoke
import de.fraunhofer.aisec.cpg.graph.records
import de.fraunhofer.aisec.cpg.graph.refs
import de.fraunhofer.aisec.cpg.graph.scopes.LocalScope
import de.fraunhofer.aisec.cpg.graph.statements
import de.fraunhofer.aisec.cpg.graph.variables
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertLiteralValue
import de.fraunhofer.aisec.cpg.test.assertLocalName
import de.fraunhofer.aisec.cpg.test.assertNotRefersTo
import de.fraunhofer.aisec.cpg.test.assertRefersTo
import java.nio.file.Path
import kotlin.test.Ignore
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollectionComprehensionTest {
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
        // The first statement is expected to be an assignment of a list comprehension with an if to
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
        val declarationK = variableK.refersTo
        assertIs<VariableDeclaration>(declarationK, "The refersTo should be a VariableDeclaration")
        assertIs<LocalScope>(
            declarationK.scope,
            "The scope of the variable is the local scope belonging to the list comprehension. In particular, it is not the FunctionScope.",
        )
        assertEquals(
            tupleAsVariable,
            declarationK.scope?.astNode,
            "The scope of the variable is the local scope belonging to the list comprehension. In particular, it is not the FunctionScope.",
        )
        assertRefersTo(
            argK,
            declarationK,
            "The argument k of the call also refers to the variable k declared in the comprehension expression.",
        )
        val declarationV = variableV.refersTo
        assertIs<VariableDeclaration>(declarationV, "The refersTo should be a VariableDeclaration")
        assertIs<LocalScope>(
            declarationV.scope,
            "The scope of the variable is the local scope belonging to the list comprehension. In particular, it is not the FunctionScope.",
        )
        assertEquals(
            tupleAsVariable,
            declarationV.scope?.astNode,
            "The scope of the variable is the local scope belonging to the list comprehension. In particular, it is not the FunctionScope.",
        )
        assertRefersTo(
            argV,
            declarationV,
            "The argument v of the call also refers to the variable v declared in the comprehension expression.",
        )

        // Check that the ILE flows to the variables with the indexed granularity
        assertContains(
            initializerListExpression.nextDFG,
            variableK,
            "We expect that the data of the ILE flows to the reference \"k\" with index granularity and index 1",
        )
        val granularityTupleToK =
            initializerListExpression.nextDFGEdges.single { it.end == variableK }.granularity
        assertIs<IndexedDataflowGranularity>(
            granularityTupleToK,
            "We expect that the data of the ILE flows to the reference \"k\" with index granularity and index 1",
        )
        assertEquals(
            0,
            granularityTupleToK.partialTarget,
            "We expect that the data of the ILE flows to the reference \"k\" with index granularity and index 1",
        )
        assertContains(
            initializerListExpression.nextDFG,
            variableV,
            "We expect that the data of the ILE flows to the reference \"v\" with index granularity and index 1",
        )
        val granularityTupleToV =
            initializerListExpression.nextDFGEdges.single { it.end == variableV }.granularity
        assertIs<IndexedDataflowGranularity>(
            granularityTupleToV,
            "We expect that the data of the ILE flows to the reference \"v\" with index granularity and index 1",
        )
        assertEquals(
            1,
            granularityTupleToV.partialTarget,
            "We expect that the data of the ILE flows to the reference \"v\" with index granularity and index 1",
        )

        // Check that the variables flow to their usages
        assertEquals(
            setOf<Node>(argK),
            variableK.nextDFG.toSet(),
            "We expect that the \"k\" in the tuple flows to its usage",
        )
        assertEquals(
            setOf<Node>(argV),
            variableV.nextDFG.toSet(),
            "We expect that the \"v\" in the tuple flows to its usage",
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
        assertIs<AssignExpression>(
            singleWithIfAssignment,
            "The first statement in the body is \"a = [foo(i) for i in x if i == 10}^\" which should be represented by an AssignExpression in the CPG.",
        )
        val singleWithIf = singleWithIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(
            singleWithIf,
            "The right hand side of the assignment \"a = [foo(i) for i in x if i == 10]\" is expected to be modeled as a CollectionComprehension \"[foo(i) for i in x if i == 10]\" in the CPG.",
        )
        var statement = singleWithIf.statement
        var variable = singleWithIf.comprehensionExpressions[0].variable
        assertIs<CallExpression>(
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertLocalName(
            "foo",
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertEquals(
            1,
            singleWithIf.comprehensionExpressions.size,
            "The CollectionComprehension \"[foo(i) for i in x if i == 10]\" has exactly one comprehensionExpressions which is \"for i in x if i == 10\"",
        )
        assertIs<LocalScope>(
            variable.scope,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertSame(
            singleWithIf,
            variable.scope?.astNode,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertLocalName(
            "i",
            variable,
            "The variable of the comprehension expression \"for i in x if i == 10\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            variable,
            "The variable of the comprehension expression \"for i in x if i == 10\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            singleWithIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x if i == 10\" is expected to be a Reference with localName \"x\"",
        )
        assertLocalName(
            "x",
            singleWithIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x if i == 10\" is expected to be a Reference with localName \"x\"",
        )
        val ifPredicate = singleWithIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(
            ifPredicate,
            "The two predicates \"if i == 10\" is expected to be represented by a binary operator \"==\" in the CPG.",
        )
        assertEquals(
            "==",
            ifPredicate.operatorCode,
            "The two predicates \"if i == 10\" is expected to be represented by a binary operator \"==\" in the CPG.",
        )

        val singleWithoutIfAssignment = body.statements[1]
        assertIs<AssignExpression>(
            singleWithoutIfAssignment,
            "The second statement in the body is \"b = [foo(i) for i in x]\" which should be represented by an AssignExpression in the CPG.",
        )
        val singleWithoutIf = singleWithoutIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(
            singleWithoutIf,
            "The right hand side of the assignment \"b = [foo(i) for i in x]\" is expected to be modeled as a CollectionComprehension \"[foo(i) for i in x]\" in the CPG.",
        )
        statement = singleWithoutIf.statement
        variable = singleWithoutIf.comprehensionExpressions[0].variable
        assertIs<CallExpression>(
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertLocalName(
            "foo",
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertEquals(
            1,
            singleWithoutIf.comprehensionExpressions.size,
            "The CollectionComprehension \"[foo(i) for i in x]\" has exactly one comprehensionExpressions which is \"for i in x\"",
        )
        assertIs<LocalScope>(
            variable.scope,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertSame(
            singleWithoutIf,
            variable.scope?.astNode,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertLocalName(
            "i",
            variable,
            "The variable of the comprehension expression \"for i in x\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            variable,
            "The variable of the comprehension expression \"for i in x\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            singleWithoutIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x\" is expected to be a Reference with localName \"x\"",
        )
        assertLocalName(
            "x",
            singleWithoutIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x\" is expected to be a Reference with localName \"x\"",
        )
        assertNull(
            singleWithoutIf.comprehensionExpressions[0].predicate,
            "The comprehension expression \"for i in x\" should not have any predicate.",
        )

        val singleWithDoubleIfAssignment = body.statements[2]
        assertIs<AssignExpression>(
            singleWithDoubleIfAssignment,
            "The third statement in the body is \"c = [foo(i) for i in x if i == 10 if i < 20]\" which should be represented by an AssignExpression in the CPG.",
        )
        val singleWithDoubleIf = singleWithDoubleIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(
            singleWithDoubleIf,
            "The right hand side of the assignment \"c = [foo(i) for i in x if i == 10 if i < 20]\" is expected to be modeled as a CollectionComprehension \"[foo(i) for i in x if i == 10 if i < 20]\" in the CPG.",
        )
        statement = singleWithDoubleIf.statement
        variable = singleWithDoubleIf.comprehensionExpressions[0].variable
        assertIs<CallExpression>(
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertLocalName(
            "foo",
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertEquals(
            1,
            singleWithDoubleIf.comprehensionExpressions.size,
            "The CollectionComprehension \"[foo(i) for i in x if i == 10 if i < 20]\" has exactly one comprehensionExpressions which is \"for i in x if i == 10 if i < 20\"",
        )
        assertIs<LocalScope>(
            variable.scope,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertSame(
            singleWithDoubleIf,
            variable.scope?.astNode,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertLocalName(
            "i",
            variable,
            "The variable of the comprehension expression \"for i in x if i == 10 if i < 20\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            variable,
            "The variable of the comprehension expression \"for i in x if i == 10 if i < 20\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            singleWithDoubleIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x if i == 10 if i < 20\" is expected to be a Reference with localName \"x\"",
        )
        assertLocalName(
            "x",
            singleWithDoubleIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x if i == 10 if i < 20\" is expected to be a Reference with localName \"x\"",
        )
        val doubleIfPredicate = singleWithDoubleIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(
            doubleIfPredicate,
            "The two predicates \"if i == 10 if i < 20\" are expected to be connected with the binary operator \"and\" in the CPG.",
        )
        assertEquals(
            "and",
            doubleIfPredicate.operatorCode,
            "The two predicates \"if i == 10 if i < 20\" are expected to be connected with the binary operator \"and\" in the CPG.",
        )

        val doubleAssignment = body.statements[3]
        assertIs<AssignExpression>(
            doubleAssignment,
            "The third statement in the body is \"d = [foo(i) for z in y if z in x for i in z if i == 10 ]\" which should be represented by an AssignExpression in the CPG.",
        )
        val double = doubleAssignment.rhs[0]
        assertIs<CollectionComprehension>(
            double,
            "The right hand side of the assignment \"d = [foo(i) for z in y if z in x for i in z if i == 10 ]\" is expected to be modeled as a CollectionComprehension \"[foo(i) for z in y if z in x for i in z if i == 10 ]\" in the CPG.",
        )
        statement = double.statement
        assertIs<CallExpression>(
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertLocalName(
            "foo",
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertEquals(
            2,
            double.comprehensionExpressions.size,
            "The CollectionComprehension \"[foo(i) for z in y if z in x for i in z if i == 10 ]\" has two comprehension expressions which are \"for z in y if z in x\" and \"for i in z if i == 10\"",
        )
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
        assertIs<AssignExpression>(
            singleWithIfAssignment,
            "The first statement in the body is \"a = {foo(i) for i in x if i == 10}^\" which should be represented by an AssignExpression in the CPG.",
        )
        val singleWithIf = singleWithIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(
            singleWithIf,
            "The right hand side of the assignment \"a = {foo(i) for i in x if i == 10}\" is expected to be modeled as a CollectionComprehension \"{foo(i) for i in x if i == 10}\" in the CPG.",
        )
        var statement = singleWithIf.statement
        var variable = singleWithIf.comprehensionExpressions[0].variable
        assertIs<CallExpression>(
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertLocalName(
            "foo",
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertEquals(
            1,
            singleWithIf.comprehensionExpressions.size,
            "The CollectionComprehension \"{foo(i) for i in x if i == 10}\" has exactly one comprehensionExpressions which is \"for i in x if i == 10\"",
        )
        assertIs<LocalScope>(
            variable.scope,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertSame(
            singleWithIf,
            variable.scope?.astNode,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertLocalName(
            "i",
            variable,
            "The variable of the comprehension expression \"for i in x if i == 10\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            variable,
            "The variable of the comprehension expression \"for i in x if i == 10\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            singleWithIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x if i == 10\" is expected to be a Reference with localName \"x\"",
        )
        assertLocalName(
            "x",
            singleWithIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x if i == 10\" is expected to be a Reference with localName \"x\"",
        )
        val ifPredicate = singleWithIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(
            ifPredicate,
            "The two predicates \"if i == 10\" is expected to be represented by a binary operator \"==\" in the CPG.",
        )
        assertEquals(
            "==",
            ifPredicate.operatorCode,
            "The two predicates \"if i == 10\" is expected to be represented by a binary operator \"==\" in the CPG.",
        )

        val singleWithoutIfAssignment = body.statements[1]
        assertIs<AssignExpression>(
            singleWithoutIfAssignment,
            "The second statement in the body is \"b = {foo(i) for i in x}\" which should be represented by an AssignExpression in the CPG.",
        )
        val singleWithoutIf = singleWithoutIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(
            singleWithoutIf,
            "The right hand side of the assignment \"b = {foo(i) for i in x}\" is expected to be modeled as a CollectionComprehension \"{foo(i) for i in x}\" in the CPG.",
        )
        statement = singleWithoutIf.statement
        variable = singleWithoutIf.comprehensionExpressions[0].variable
        assertIs<CallExpression>(
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertLocalName(
            "foo",
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertEquals(
            1,
            singleWithoutIf.comprehensionExpressions.size,
            "The CollectionComprehension \"{foo(i) for i in x}\" has exactly one comprehensionExpressions which is \"for i in x\"",
        )
        assertIs<LocalScope>(
            variable.scope,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertSame(
            singleWithoutIf,
            variable.scope?.astNode,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertLocalName(
            "i",
            variable,
            "The variable of the comprehension expression \"for i in x\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            variable,
            "The variable of the comprehension expression \"for i in x\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            singleWithoutIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x\" is expected to be a Reference with localName \"x\"",
        )
        assertLocalName(
            "x",
            singleWithoutIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x\" is expected to be a Reference with localName \"x\"",
        )
        assertNull(
            singleWithoutIf.comprehensionExpressions[0].predicate,
            "The comprehension expression \"for i in x\" should not have any predicate.",
        )

        val singleWithDoubleIfAssignment = body.statements[2]
        assertIs<AssignExpression>(
            singleWithDoubleIfAssignment,
            "The third statement in the body is \"c = {foo(i) for i in x if i == 10 if i < 20}\" which should be represented by an AssignExpression in the CPG.",
        )
        val singleWithDoubleIf = singleWithDoubleIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(
            singleWithDoubleIf,
            "The right hand side of the assignment \"c = {foo(i) for i in x if i == 10 if i < 20}\" is expected to be modeled as a CollectionComprehension \"{foo(i) for i in x if i == 10 if i < 20}\" in the CPG.",
        )
        statement = singleWithDoubleIf.statement
        variable = singleWithDoubleIf.comprehensionExpressions[0].variable
        assertIs<CallExpression>(
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertLocalName(
            "foo",
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertEquals(
            1,
            singleWithDoubleIf.comprehensionExpressions.size,
            "The CollectionComprehension \"{foo(i) for i in x if i == 10 if i < 20}\" has exactly one comprehensionExpressions which is \"for i in x if i == 10 if i < 20\"",
        )
        assertIs<LocalScope>(
            variable.scope,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertSame(
            singleWithDoubleIf,
            variable.scope?.astNode,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertLocalName(
            "i",
            variable,
            "The variable of the comprehension expression \"for i in x if i == 10 if i < 20\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            variable,
            "The variable of the comprehension expression \"for i in x if i == 10 if i < 20\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            singleWithDoubleIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x if i == 10 if i < 20\" is expected to be a Reference with localName \"x\"",
        )
        assertLocalName(
            "x",
            singleWithDoubleIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x if i == 10 if i < 20\" is expected to be a Reference with localName \"x\"",
        )
        val doubleIfPredicate = singleWithDoubleIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(
            doubleIfPredicate,
            "The two predicates \"if i == 10 if i < 20\" are expected to be connected with the binary operator \"and\" in the CPG.",
        )
        assertEquals(
            "and",
            doubleIfPredicate.operatorCode,
            "The two predicates \"if i == 10 if i < 20\" are expected to be connected with the binary operator \"and\" in the CPG.",
        )

        val doubleAssignment = body.statements[3]
        assertIs<AssignExpression>(
            doubleAssignment,
            "The third statement in the body is \"d = {foo(i) for z in y if z in x for i in z if i == 10 }\" which should be represented by an AssignExpression in the CPG.",
        )
        val double = doubleAssignment.rhs[0]
        assertIs<CollectionComprehension>(
            double,
            "The right hand side of the assignment \"d = {foo(i) for z in y if z in x for i in z if i == 10 }\" is expected to be modeled as a CollectionComprehension \"{foo(i) for z in y if z in x for i in z if i == 10 }\" in the CPG.",
        )
        statement = double.statement
        assertIs<CallExpression>(
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertLocalName(
            "foo",
            statement,
            "The CollectionComprehension has the statement \"foo(i)\" which is expected to be modeled as a CallExpression with localName \"foo\".",
        )
        assertEquals(
            2,
            double.comprehensionExpressions.size,
            "The CollectionComprehension \"{foo(i) for z in y if z in x for i in z if i == 10 }\" has two comprehension expressions which are \"for z in y if z in x\" and \"for i in z if i == 10\"",
        )
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
        assertIs<AssignExpression>(
            singleWithIfAssignment,
            "The first statement in the body is \"a = {i: foo(i) for i in x if i == 10}^\" which should be represented by an AssignExpression in the CPG.",
        )
        val singleWithIf = singleWithIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(
            singleWithIf,
            "The right hand side of the assignment \"a = {i: foo(i) for i in x if i == 10}\" is expected to be modeled as a CollectionComprehension \"{i: foo(i) for i in x if i == 10}\" in the CPG.",
        )
        var statement = singleWithIf.statement
        var variable = singleWithIf.comprehensionExpressions[0].variable
        assertIs<KeyValueExpression>(
            statement,
            "The CollectionComprehension has the statement \"i: foo(i)\" which is expected to be modeled as a KeyValueExpression.",
        )
        assertIs<Reference>(
            statement.key,
            "The key of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a Reference with localName \"i\"",
        )
        assertLocalName(
            "i",
            statement.key,
            "The key of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<CallExpression>(
            statement.value,
            "The value of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a CallExpression with localName \"foo\"",
        )
        assertLocalName(
            "foo",
            statement.value,
            "The value of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a CallExpression with localName \"foo\"",
        )
        assertEquals(
            1,
            singleWithIf.comprehensionExpressions.size,
            "The CollectionComprehension \"{i: foo(i) for i in x if i == 10}\" has exactly one comprehensionExpressions which is \"for i in x if i == 10\"",
        )
        assertIs<LocalScope>(
            variable.scope,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertSame(
            singleWithIf,
            variable.scope?.astNode,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertLocalName(
            "i",
            variable,
            "The variable of the comprehension expression \"for i in x if i == 10\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            variable,
            "The variable of the comprehension expression \"for i in x if i == 10\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            singleWithIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x if i == 10\" is expected to be a Reference with localName \"x\"",
        )
        assertLocalName(
            "x",
            singleWithIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x if i == 10\" is expected to be a Reference with localName \"x\"",
        )
        val ifPredicate = singleWithIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(
            ifPredicate,
            "The two predicates \"if i == 10\" is expected to be represented by a binary operator \"==\" in the CPG.",
        )
        assertEquals(
            "==",
            ifPredicate.operatorCode,
            "The two predicates \"if i == 10\" is expected to be represented by a binary operator \"==\" in the CPG.",
        )

        val singleWithoutIfAssignment = body.statements[1]
        assertIs<AssignExpression>(
            singleWithoutIfAssignment,
            "The second statement in the body is \"b = {i: foo(i) for i in x}\" which should be represented by an AssignExpression in the CPG.",
        )
        val singleWithoutIf = singleWithoutIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(
            singleWithoutIf,
            "The right hand side of the assignment \"b = {i: foo(i) for i in x}\" is expected to be modeled as a CollectionComprehension \"{i: foo(i) for i in x}\" in the CPG.",
        )
        statement = singleWithoutIf.statement
        variable = singleWithoutIf.comprehensionExpressions[0].variable
        assertIs<KeyValueExpression>(
            statement,
            "The CollectionComprehension has the statement \"i: foo(i)\" which is expected to be modeled as a KeyValueExpression.",
        )
        assertIs<Reference>(
            statement.key,
            "The key of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a Reference with localName \"i\"",
        )
        assertLocalName(
            "i",
            statement.key,
            "The key of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<CallExpression>(
            statement.value,
            "The value of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a CallExpression with localName \"foo\"",
        )
        assertLocalName(
            "foo",
            statement.value,
            "The value of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a CallExpression with localName \"foo\"",
        )
        assertEquals(
            1,
            singleWithoutIf.comprehensionExpressions.size,
            "The CollectionComprehension \"{i: foo(i) for i in x}\" has exactly one comprehensionExpressions which is \"for i in x\"",
        )
        assertIs<LocalScope>(
            variable.scope,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertSame(
            singleWithoutIf,
            variable.scope?.astNode,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertLocalName(
            "i",
            variable,
            "The variable of the comprehension expression \"for i in x\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            variable,
            "The variable of the comprehension expression \"for i in x\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            singleWithoutIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x\" is expected to be a Reference with localName \"x\"",
        )
        assertLocalName(
            "x",
            singleWithoutIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x\" is expected to be a Reference with localName \"x\"",
        )
        assertNull(
            singleWithoutIf.comprehensionExpressions[0].predicate,
            "The comprehension expression \"for i in x\" should not have any predicate.",
        )

        val singleWithDoubleIfAssignment = body.statements[2]
        assertIs<AssignExpression>(
            singleWithDoubleIfAssignment,
            "The third statement in the body is \"c = {i: foo(i) for i in x if i == 10 if i < 20}\" which should be represented by an AssignExpression in the CPG.",
        )
        val singleWithDoubleIf = singleWithDoubleIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(
            singleWithDoubleIf,
            "The right hand side of the assignment \"c = {i: foo(i) for i in x if i == 10 if i < 20}\" is expected to be modeled as a CollectionComprehension \"{i: foo(i) for i in x if i == 10 if i < 20}\" in the CPG.",
        )
        statement = singleWithDoubleIf.statement
        variable = singleWithDoubleIf.comprehensionExpressions[0].variable
        assertIs<KeyValueExpression>(
            statement,
            "The CollectionComprehension has the statement \"i: foo(i)\" which is expected to be modeled as a KeyValueExpression.",
        )
        assertIs<Reference>(
            statement.key,
            "The key of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a Reference with localName \"i\"",
        )
        assertLocalName(
            "i",
            statement.key,
            "The key of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<CallExpression>(
            statement.value,
            "The value of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a CallExpression with localName \"foo\"",
        )
        assertLocalName(
            "foo",
            statement.value,
            "The value of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a CallExpression with localName \"foo\"",
        )
        assertEquals(
            1,
            singleWithDoubleIf.comprehensionExpressions.size,
            "The CollectionComprehension \"{i: foo(i) for i in x if i == 10 if i < 20}\" has exactly one comprehensionExpressions which is \"for i in x if i == 10 if i < 20\"",
        )
        assertIs<LocalScope>(
            variable.scope,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertSame(
            singleWithDoubleIf,
            variable.scope?.astNode,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertLocalName(
            "i",
            variable,
            "The variable of the comprehension expression \"for i in x if i == 10 if i < 20\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            variable,
            "The variable of the comprehension expression \"for i in x if i == 10 if i < 20\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<Reference>(
            singleWithDoubleIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x if i == 10 if i < 20\" is expected to be a Reference with localName \"x\"",
        )
        assertLocalName(
            "x",
            singleWithDoubleIf.comprehensionExpressions[0].iterable,
            "The iterable of the comprehension expression \"for i in x if i == 10 if i < 20\" is expected to be a Reference with localName \"x\"",
        )
        val doubleIfPredicate = singleWithDoubleIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(
            doubleIfPredicate,
            "The two predicates \"if i == 10 if i < 20\" are expected to be connected with the binary operator \"and\" in the CPG.",
        )
        assertEquals(
            "and",
            doubleIfPredicate.operatorCode,
            "The two predicates \"if i == 10 if i < 20\" are expected to be connected with the binary operator \"and\" in the CPG.",
        )

        val doubleAssignment = body.statements[3]
        assertIs<AssignExpression>(
            doubleAssignment,
            "The third statement in the body is \"d = {i: foo(i) for z in y if z in x for i in z if i == 10 }\" which should be represented by an AssignExpression in the CPG.",
        )
        val double = doubleAssignment.rhs[0]
        assertIs<CollectionComprehension>(
            double,
            "The right hand side of the assignment \"d = {i: foo(i) for z in y if z in x for i in z if i == 10 }\" is expected to be modeled as a CollectionComprehension \"{i: foo(i) for z in y if z in x for i in z if i == 10 }\" in the CPG.",
        )
        statement = double.statement
        assertIs<KeyValueExpression>(
            statement,
            "The CollectionComprehension has the statement \"i: foo(i)\" which is expected to be modeled as a KeyValueExpression.",
        )
        assertIs<Reference>(
            statement.key,
            "The key of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a Reference with localName \"i\"",
        )
        assertLocalName(
            "i",
            statement.key,
            "The key of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a Reference with localName \"i\"",
        )
        assertIs<CallExpression>(
            statement.value,
            "The value of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a CallExpression with localName \"foo\"",
        )
        assertLocalName(
            "foo",
            statement.value,
            "The value of the CollectionComprehension of the KeyValueExpression \"i: foo(i)\" is expected to be a CallExpression with localName \"foo\"",
        )
        assertEquals(
            2,
            double.comprehensionExpressions.size,
            "The CollectionComprehension \"{i: foo(i) for z in y if z in x for i in z if i == 10 }\" has two comprehension expressions which are \"for z in y if z in x\" and \"for i in z if i == 10\"",
        )
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
        var variable = singleWithIf.comprehensionExpressions[0].variable
        assertIs<LocalScope>(
            variable.scope,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertSame(
            singleWithIf,
            variable.scope?.astNode,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertLocalName("i", variable)
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
        variable = singleWithoutIf.comprehensionExpressions[0].variable
        assertIs<LocalScope>(
            variable.scope,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertSame(
            singleWithoutIf,
            variable.scope?.astNode,
            "The scope of the variable is expected to be a local scope belonging to the list comprehension.",
        )
        assertLocalName("i", variable)
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

        val xDeclaration = compBindingFunctionDeclaration.variables.firstOrNull()
        assertIs<VariableDeclaration>(xDeclaration)

        assertEquals(
            5,
            compBindingFunctionDeclaration.variables.size,
            "Expected five variables. One for the \"outside\" x and one for each of the four comprehensions.",
        )

        assertEquals(
            2,
            xDeclaration.usages.size,
            "Expected two usages: one for the initial assignment and one for the usage in \"print(x)\".",
        )

        val comprehensions =
            compBindingFunctionDeclaration.body.statements.filterIsInstance<
                CollectionComprehension
            >()
        assertEquals(4, comprehensions.size, "Expected to find four comprehensions.")

        comprehensions.forEach {
            it.refs("x").forEach { ref -> assertNotRefersTo(ref, xDeclaration) }
        }
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

        val xDeclaration = compBindingAssignExprFunctionDeclaration.variables["x"]
        assertIs<VariableDeclaration>(
            xDeclaration,
            "There must be a VariableDeclaration with the local name \"x\" inside the function.",
        )

        assertEquals(
            2,
            compBindingAssignExprFunctionDeclaration.variables.size,
            "Expected two variables. One for the \"outside\" x and one for the \"temp\" inside the comprehension.",
        )

        assertEquals(
            3,
            xDeclaration.usages.size,
            "Expected three usages: one for the initial assignment, one for the comprehension and one for the usage in \"print(x)\".",
        )

        val comprehension =
            compBindingAssignExprFunctionDeclaration.body.statements.singleOrNull {
                it is CollectionComprehension
            }
        assertNotNull(comprehension)
        val xRef = comprehension.refs("x").singleOrNull()
        assertNotNull(xRef)
        assertRefersTo(xRef, xDeclaration)
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

        val xDeclaration = compBindingAssignExprNestedFunctionDeclaration.variables["x"]
        assertIs<VariableDeclaration>(
            xDeclaration,
            "There must be a VariableDeclaration with the local name \"x\" inside the function.",
        )

        assertEquals(
            3,
            compBindingAssignExprNestedFunctionDeclaration.variables.size,
            "Expected two variables. One for the \"outside\" x, one for the \"temp\" inside the comprehension and one for the \"a\" inside the comprehension.",
        )

        assertEquals(
            3,
            xDeclaration.usages.size,
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
            xDeclaration,
            "The reference of \"x\" inside the inner comprehension's statement refers to the variable declared outside the comprehensions.",
        )
    }

    @Test
    fun testCompBindingListAssignment() {
        val comprehensionWithListAssignmentFunctionDeclaration =
            result.functions["comprehension_with_list_assignment"]
        assertIs<FunctionDeclaration>(
            comprehensionWithListAssignmentFunctionDeclaration,
            "There must be a function called \"comprehension_with_list_assignment\" in the file. It must be neither null nor any other class than a FunctionDeclaration.",
        )

        // Get the body
        val body = comprehensionWithListAssignmentFunctionDeclaration.body
        assertIs<Block>(
            body,
            "The body of each function is modeled as a Block in the CPG. This must also apply to the function \"comprehension_with_list_assignment\".",
        )

        val listBInitialization = body.statements[0]
        assertIs<AssignExpression>(
            listBInitialization,
            "The first statement of the function \"comprehension_with_list_assignment\" is expected to be the initialization of list \"b\" by the statement \"b = [0, 1, 2]\" which is expected to be represented by an AssignExpression in the CPG.",
        )
        val refBFirstStatement = listBInitialization.lhs[0]
        assertIs<Reference>(
            refBFirstStatement,
            "The left hand side of the assignment \"b = [0, 1, 2]\" is expected to be represented by a Reference with localName \"b\" in the CPG.",
        )
        assertLocalName(
            "b",
            refBFirstStatement,
            "The left hand side of the assignment \"b = [0, 1, 2]\" is expected to be represented by a Reference with localName \"b\" in the CPG.",
        )
        val bDeclaration = listBInitialization.variables["b"]
        assertIs<VariableDeclaration>(
            bDeclaration,
            "There must be a VariableDeclaration with the local name \"b\" inside the first statement of the function \"comprehension_with_list_assignment\".",
        )
        assertRefersTo(
            refBFirstStatement,
            bDeclaration,
            "The reference \"b\" is expected to refer to the variable declaration \"b\" in the same statement.",
        )

        // Check if the AST of the list comprehension fits our expectations.
        val listComprehensionWithTupleAndAssignmentToListElement = body.statements[1]
        assertIs<CollectionComprehension>(
            listComprehensionWithTupleAndAssignmentToListElement,
            "The second statement of the function \"comprehension_with_list_assignment\" is expected to be python's list comprehension \"[a for (a, b[0]) in [(1, 2), (2, 4), (3, 6)]]\" which is represented by a CollectionComprehension in the CPG",
        )

        val comprehensionExpression =
            listComprehensionWithTupleAndAssignmentToListElement.comprehensionExpressions
                .singleOrNull()
        assertEquals(
            1,
            listComprehensionWithTupleAndAssignmentToListElement.comprehensionExpressions.size,
            "There is expected to be exactly one CollectionComprehension in the list comprehension \"[a for (a, b[0]) in [(1, 2), (2, 4), (3, 6)]]\". It represents the code's part \"for (a, b[0]) in [(1, 2), (2, 4), (3, 6)]\"",
        )
        assertIs<ComprehensionExpression>(
            comprehensionExpression,
            "There is expected to be exactly one ComprehensionExpression in the list comprehension \"[a for (a, b[0]) in [(1, 2), (2, 4), (3, 6)]]\". It represents the code's part \"for (a, b[0]) in [(1, 2), (2, 4), (3, 6)]\"",
        )

        val tuple = comprehensionExpression.variable
        assertIs<InitializerListExpression>(
            tuple,
            "The variable of the ComprehensionExpression is the tuple \"(a, b[0])\" which is expected to be represented by an InitializerListExpression in the CPG.",
        )
        assertEquals(
            2,
            tuple.initializers.size,
            "The tuple \"(a, b[0])\" represented by an InitializerListExpression in the CPG is expected to have exactly two elements.",
        )
        val refA = tuple.initializers[0]
        assertIs<Reference>(
            refA,
            "The first element of the tuple \"(a, b[0])\" is expected to be represented by a Reference with localName \"a\" in the CPG.",
        )
        assertLocalName(
            "a",
            refA,
            "The first element of the tuple \"(a, b[0])\" is expected to be represented by a Reference with localName \"a\" in the CPG.",
        )
        val accessB0 = tuple.initializers[1]
        assertIs<SubscriptExpression>(
            accessB0,
            "The second element of the tuple \"(a, b[0])\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\"  and the subscriptExpression representing the index is a Literal<Int> with value \"0\".",
        )
        val refB = accessB0.arrayExpression
        assertIs<Reference>(
            refB,
            "The second element of the tuple \"(a, b[0])\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\"  and the subscriptExpression representing the index is a Literal<Int> with value \"0\" (with kotlin type Long ).",
        )
        assertLocalName(
            "b",
            refB,
            "The second element of the tuple \"(a, b[0])\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\"  and the subscriptExpression representing the index is a Literal<Int> with value \"0\" (with kotlin type Long ).",
        )
        val index = accessB0.subscriptExpression
        assertIs<Literal<Int>>(
            index,
            "The second element of the tuple \"(a, b[0])\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\"  and the subscriptExpression representing the index is a Literal<Int> with value \"0\" (with kotlin type Long ).",
        )
        assertLiteralValue(
            0L,
            index,
            "The second element of the tuple \"(a, b[0])\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\"  and the subscriptExpression representing the index is a Literal<Int> with value \"0\" (with kotlin type Long ).",
        )

        // Now the actually interesting part: We check for variables belonging to the references.
        assertEquals(
            1,
            refA.prevDFGEdges.size,
            "We expect that there's one DFG edge flowing into the reference \"a\" in the tuple. It should come from the InitializerListExpression and have the index \"0\"",
        )
        assertEquals(
            tuple,
            refA.prevDFG.single(),
            "We expect that there's one DFG edge flowing into the reference \"a\" in the tuple. It should come from the InitializerListExpression and have the index \"0\"",
        )
        val refAGranularity = refA.prevDFGEdges.single().granularity
        assertIs<IndexedDataflowGranularity>(
            refAGranularity,
            "We expect that there's one DFG edge flowing into the reference \"a\" in the tuple. It should come from the InitializerListExpression and have the index \"0\"",
        )
        assertEquals(
            0,
            refAGranularity.partialTarget,
            "We expect that there's one DFG edge flowing into the reference \"a\" in the tuple. It should come from the InitializerListExpression and have the index \"0\"",
        )
        val variableDeclarationA = refA.refersTo
        assertIs<VariableDeclaration>(
            variableDeclarationA,
            "We expect that the reference \"a\" refers to a VariableDeclaration with localName \"a\" which is not null and whose scope is the LocalScope of the list comprehension.",
        )
        assertIs<LocalScope>(
            variableDeclarationA.scope,
            "We expect that the reference \"a\" refers to a VariableDeclaration with localName \"a\" which is not null and whose scope is the LocalScope of the list comprehension.",
        )
        assertEquals(
            listComprehensionWithTupleAndAssignmentToListElement,
            variableDeclarationA.scope?.astNode,
            "We expect that the reference \"a\" refers to a VariableDeclaration with localName \"a\" which is not null and whose scope is the LocalScope of the list comprehension.",
        )
        assertRefersTo(
            refB,
            bDeclaration,
            "We expect that the reference \"b\" in the tuple refers to the VariableDeclaration of \"b\" which is added outside the list comprehension (in statement 0).",
        )
        val tupleToB0 = accessB0.prevDFGEdges.singleOrNull { it.start == tuple }
        assertNotNull(
            tupleToB0,
            "We expect that there's one DFG edge flowing into the reference \"b[0]\" in the tuple. It should come from the InitializerListExpression and have the index \"1\"",
        )
        val accessB0Granularity = tupleToB0.granularity
        assertIs<IndexedDataflowGranularity>(
            accessB0Granularity,
            "We expect that there's one DFG edge flowing into the reference \"b[0]\" in the tuple. It should come from the InitializerListExpression and have the index \"1\"",
        )
        assertEquals(
            1,
            accessB0Granularity.partialTarget,
            "We expect that there's one DFG edge flowing into the reference \"b[0]\" in the tuple. It should come from the InitializerListExpression and have the index \"1\"",
        )
    }

    @Test
    fun testComprehensionWithListAssignmentAndIndexVariable() {
        val comprehensionWithListAssignmentAndIndexVariableFunctionDeclaration =
            result.functions["comprehension_with_list_assignment_and_index_variable"]
        assertIs<FunctionDeclaration>(
            comprehensionWithListAssignmentAndIndexVariableFunctionDeclaration,
            "There must be a function called \"comprehension_with_list_assignment_and_index_variable\" in the file. It must be neither null nor any other class than a FunctionDeclaration.",
        )

        // Get the body
        val body = comprehensionWithListAssignmentAndIndexVariableFunctionDeclaration.body
        assertIs<Block>(
            body,
            "The body of each function is modeled as a Block in the CPG. This must also apply to the function \"comprehension_with_list_assignment_and_index_variable\".",
        )

        val listBInitialization = body.statements[0]
        assertIs<AssignExpression>(
            listBInitialization,
            "The first statement of the function \"comprehension_with_list_assignment_and_index_variable\" is expected to be the initialization of list \"b\" by the statement \"b = [0, 1, 2]\" which is expected to be represented by an AssignExpression in the CPG.",
        )
        val refBFirstStatement = listBInitialization.lhs[0]
        assertIs<Reference>(
            refBFirstStatement,
            "The left hand side of the assignment \"b = [0, 1, 2]\" is expected to be represented by a Reference with localName \"b\" in the CPG.",
        )
        assertLocalName(
            "b",
            refBFirstStatement,
            "The left hand side of the assignment \"b = [0, 1, 2]\" is expected to be represented by a Reference with localName \"b\" in the CPG.",
        )
        val bDeclaration = listBInitialization.variables["b"]
        assertIs<VariableDeclaration>(
            bDeclaration,
            "There must be a VariableDeclaration with the local name \"b\" inside the first statement of the function \"comprehension_with_list_assignment_and_index_variable\".",
        )
        assertRefersTo(
            refBFirstStatement,
            bDeclaration,
            "The reference \"b\" is expected to refer to the variable declaration \"b\" in the same statement.",
        )

        // Check if the AST of the list comprehension fits our expectations.
        val listComprehensionWithTupleAndAssignmentToListElement = body.statements[1]
        assertIs<CollectionComprehension>(
            listComprehensionWithTupleAndAssignmentToListElement,
            "The second statement of the function \"comprehension_with_list_assignment_and_index_variable\" is expected to be python's list comprehension \"[a for (a, b[a]) in [(0, 'this'), (1, 'is'), (2, 'fun')]]\" which is represented by a CollectionComprehension in the CPG",
        )

        val comprehensionExpression =
            listComprehensionWithTupleAndAssignmentToListElement.comprehensionExpressions
                .singleOrNull()
        assertEquals(
            1,
            listComprehensionWithTupleAndAssignmentToListElement.comprehensionExpressions.size,
            "There is expected to be exactly one CollectionComprehension in the list comprehension \"[a for (a, b[a]) in [(0, 'this'), (1, 'is'), (2, 'fun')]]\". It represents the code's part \"for (a, b[a]) in [(0, 'this'), (1, 'is'), (2, 'fun')]\"",
        )
        assertIs<ComprehensionExpression>(
            comprehensionExpression,
            "There is expected to be exactly one ComprehensionExpression in the list comprehension \"[a for (a, b[a]) in [(0, 'this'), (1, 'is'), (2, 'fun')]]\". It represents the code's part \"for (a, b[a]) in [(0, 'this'), (1, 'is'), (2, 'fun')]\"",
        )

        val tuple = comprehensionExpression.variable
        assertIs<InitializerListExpression>(
            tuple,
            "The variable of the ComprehensionExpression is the tuple \"(a, b[a])\" which is expected to be represented by an InitializerListExpression in the CPG.",
        )
        assertEquals(
            2,
            tuple.initializers.size,
            "The tuple \"(a, b[a])\" represented by an InitializerListExpression in the CPG is expected to have exactly two elements.",
        )
        val refA = tuple.initializers[0]
        assertIs<Reference>(
            refA,
            "The first element of the tuple \"(a, b[a])\" is expected to be represented by a Reference with localName \"a\" in the CPG.",
        )
        assertLocalName(
            "a",
            refA,
            "The first element of the tuple \"(a, b[a])\" is expected to be represented by a Reference with localName \"a\" in the CPG.",
        )
        val accessBA = tuple.initializers[1]
        assertIs<SubscriptExpression>(
            accessBA,
            "The second element of the tuple \"(a, b[a])\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\"  and the subscriptExpression representing the index is a Reference \"a\" which refers to the same variable as the tuple's first element.",
        )
        val refB = accessBA.arrayExpression
        assertIs<Reference>(
            refB,
            "The second element of the tuple \"(a, b[a])\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\"  and the subscriptExpression representing the index is a Reference \"a\" which refers to the same variable as the tuple's first element.",
        )
        assertLocalName(
            "b",
            refB,
            "The second element of the tuple \"(a, b[a])\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\"  and the subscriptExpression representing the index is Reference \"a\" which refers to the same variable as the tuple's first element.",
        )
        val index = accessBA.subscriptExpression
        assertIs<Reference>(
            index,
            "The second element of the tuple \"(a, b[a])\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\"  and the subscriptExpression representing the index is Reference \"a\" which refers to the same variable as the tuple's first element.",
        )
        assertLocalName(
            "a",
            index,
            "The second element of the tuple \"(a, b[a])\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\"  and the subscriptExpression representing the index is a Literal<Int> with value \"0\" (with kotlin type Long ).",
        )

        // Now the actually interesting part: We check for variables belonging to the references.
        assertEquals(
            1,
            refA.prevDFGEdges.size,
            "We expect that there's one DFG edge flowing into the reference \"a\" in the tuple. It should come from the InitializerListExpression and have the index \"0\"",
        )
        assertEquals(
            tuple,
            refA.prevDFG.single(),
            "We expect that there's one DFG edge flowing into the reference \"a\" in the tuple. It should come from the InitializerListExpression and have the index \"0\"",
        )
        val refAGranularity = refA.prevDFGEdges.single().granularity
        assertIs<IndexedDataflowGranularity>(
            refAGranularity,
            "We expect that there's one DFG edge flowing into the reference \"a\" in the tuple. It should come from the InitializerListExpression and have the index \"0\"",
        )
        assertEquals(
            0,
            refAGranularity.partialTarget,
            "We expect that there's one DFG edge flowing into the reference \"a\" in the tuple. It should come from the InitializerListExpression and have the index \"0\"",
        )
        val variableDeclarationA = refA.refersTo
        assertIs<VariableDeclaration>(
            variableDeclarationA,
            "We expect that the reference \"a\" in the first element of the tuple refers to a VariableDeclaration with localName \"a\" which is not null and whose scope is the LocalScope of the list comprehension.",
        )
        assertRefersTo(
            index,
            variableDeclarationA,
            "We expect that the reference \"a\" in the second element of the tuple refers to the same VariableDeclaration as the first element of the tuple.",
        )
        assertIs<LocalScope>(
            variableDeclarationA.scope,
            "We expect that the reference \"a\" refers to a VariableDeclaration with localName \"a\" which is not null and whose scope is the LocalScope of the list comprehension.",
        )
        assertEquals(
            listComprehensionWithTupleAndAssignmentToListElement,
            variableDeclarationA.scope?.astNode,
            "We expect that the reference \"a\" refers to a VariableDeclaration with localName \"a\" which is not null and whose scope is the LocalScope of the list comprehension.",
        )
        assertRefersTo(
            refB,
            bDeclaration,
            "We expect that the reference \"b\" in the tuple refers to the VariableDeclaration of \"b\" which is added outside the list comprehension (in statement 0).",
        )

        val tupleToBA = accessBA.prevDFGEdges.singleOrNull { it.start == tuple }
        assertNotNull(
            tupleToBA,
            "We expect that there's one DFG edge flowing into the reference \"b[a]\" in the tuple. It should come from the InitializerListExpression and have the index \"1\"",
        )
        val accessBAGranularity = tupleToBA.granularity
        assertIs<IndexedDataflowGranularity>(
            accessBAGranularity,
            "We expect that there's one DFG edge flowing into the reference \"b[a]\" in the tuple. It should come from the InitializerListExpression and have the index \"1\"",
        )
        assertEquals(
            1,
            accessBAGranularity.partialTarget,
            "We expect that there's one DFG edge flowing into the reference \"b[a]\" in the tuple. It should come from the InitializerListExpression and have the index \"1\"",
        )
    }

    @Ignore(
        "This test is ignored because the variable \"a\" in the tuple's first element is incorrectly resolved to the variable in the tuple's second element. This adds refersTo edges which should not exist and also causes the DFG to contain additional edges."
    )
    @Test
    fun testComprehensionWithListAssignmentAndIndexVariableReversed() {
        val comprehensionWithListAssignmentAndIndexVariableReversedFunctionDeclaration =
            result.functions["comprehension_with_list_assignment_and_index_variable_reversed"]
        assertIs<FunctionDeclaration>(
            comprehensionWithListAssignmentAndIndexVariableReversedFunctionDeclaration,
            "There must be a function called \"comprehension_with_list_assignment_and_index_variable_reversed\" in the file. It must be neither null nor any other class than a FunctionDeclaration.",
        )

        // Get the body
        val body = comprehensionWithListAssignmentAndIndexVariableReversedFunctionDeclaration.body
        assertIs<Block>(
            body,
            "The body of each function is modeled as a Block in the CPG. This must also apply to the function \"comprehension_with_list_assignment_and_index_variable_reversed\".",
        )

        val listBInitialization = body.statements[0]
        assertIs<AssignExpression>(
            listBInitialization,
            "The first statement of the function \"comprehension_with_list_assignment_and_index_variable_reversed\" is expected to be the initialization of list \"b\" by the statement \"b = [0, 1, 2]\" which is expected to be represented by an AssignExpression in the CPG.",
        )
        val refBFirstStatement = listBInitialization.lhs[0]
        assertIs<Reference>(
            refBFirstStatement,
            "The left hand side of the assignment \"b = [0, 1, 2]\" is expected to be represented by a Reference with localName \"b\" in the CPG.",
        )
        assertLocalName(
            "b",
            refBFirstStatement,
            "The left hand side of the assignment \"b = [0, 1, 2]\" is expected to be represented by a Reference with localName \"b\" in the CPG.",
        )
        val bDeclaration = listBInitialization.variables["b"]
        assertIs<VariableDeclaration>(
            bDeclaration,
            "There must be a VariableDeclaration with the local name \"b\" inside the first statement of the function \"comprehension_with_list_assignment_and_index_variable_reversed\".",
        )
        assertRefersTo(
            refBFirstStatement,
            bDeclaration,
            "The reference \"b\" is expected to refer to the variable declaration \"b\" in the same statement.",
        )

        val localAAssignment = body.statements[1]
        assertIs<AssignExpression>(
            localAAssignment,
            "The first statement of the function \"comprehension_with_list_assignment_and_index_variable_reversed\" is expected to be the initialization of the local variable \"a\" by the statement \"a = 1\" which is expected to be represented by an AssignExpression in the CPG.",
        )
        val localARef = localAAssignment.lhs[0]
        assertIs<Reference>(
            localARef,
            "The left hand side of the assignment \"a = 1\" is expected to be represented by a Reference with localName \"a\" in the CPG.",
        )
        val aDeclaration = localAAssignment.variables["a"]
        assertIs<VariableDeclaration>(
            aDeclaration,
            "There must be a VariableDeclaration with the local name \"a\" inside the first statement of the function \"comprehension_with_list_assignment_and_index_variable_reversed\".",
        )
        assertRefersTo(
            localARef,
            aDeclaration,
            "The reference \"a\" is expected to refer to the variable declaration \"a\" in the same statement.",
        )

        // Check if the AST of the list comprehension fits our expectations.
        val listComprehensionWithTupleAndAssignmentToListElement = body.statements[2]
        assertIs<CollectionComprehension>(
            listComprehensionWithTupleAndAssignmentToListElement,
            "The third statement of the function \"comprehension_with_list_assignment_and_index_variable_reversed\" is expected to be python's list comprehension \"[a for (b[a], a) in [('this', 0), ('is', 1), ('fun', 2)]]\" which is represented by a CollectionComprehension in the CPG",
        )

        val comprehensionExpression =
            listComprehensionWithTupleAndAssignmentToListElement.comprehensionExpressions
                .singleOrNull()
        assertEquals(
            1,
            listComprehensionWithTupleAndAssignmentToListElement.comprehensionExpressions.size,
            "There is expected to be exactly one CollectionComprehension in the list comprehension \"[a for (b[a], a) in [('this', 0), ('is', 1), ('fun', 2)]]\". It represents the code's part \"for (b[a], a) in [('this', 0), ('is', 1), ('fun', 2)]\"",
        )
        assertIs<ComprehensionExpression>(
            comprehensionExpression,
            "There is expected to be exactly one ComprehensionExpression in the list comprehension \"[a for (b[a], a) in [('this', 0), ('is', 1), ('fun', 2)]]\". It represents the code's part \"for (b[a], a) in [('this', 0), ('is', 1), ('fun', 2)]\"",
        )

        val tuple = comprehensionExpression.variable
        assertIs<InitializerListExpression>(
            tuple,
            "The variable of the ComprehensionExpression is the tuple \"(b[a], a)\" which is expected to be represented by an InitializerListExpression in the CPG.",
        )
        assertEquals(
            2,
            tuple.initializers.size,
            "The tuple \"(b[a], a)\" represented by an InitializerListExpression in the CPG is expected to have exactly two elements.",
        )
        val accessBA = tuple.initializers[0]
        assertIs<SubscriptExpression>(
            accessBA,
            "The first element of the tuple \"(b[a], a)\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\"  and the subscriptExpression representing the index is a Reference \"a\" which refers to no variable available at this point.",
        )
        val refB = accessBA.arrayExpression
        assertIs<Reference>(
            refB,
            "The first element of the tuple \"(b[a], a)\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\"  and the subscriptExpression representing the index is a Reference \"a\" which refers to no variable available at this point.",
        )
        assertLocalName(
            "b",
            refB,
            "The first element of the tuple \"(b[a], a)\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\"  and the subscriptExpression representing the index is a Reference \"a\" which refers to no variable available at this point.",
        )
        val indexA = accessBA.subscriptExpression
        assertIs<Reference>(
            indexA,
            "The first element of the tuple \"(b[a], a)\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\"  and the subscriptExpression representing the index is a Reference \"a\" which refers to no variable available at this point.",
        )
        assertLocalName(
            "a",
            indexA,
            "The first element of the tuple \"(b[a], a)\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\"  and the subscriptExpression representing the index is a Reference \"a\" which refers to no variable available at this point.",
        )
        val refA = tuple.initializers[1]
        assertIs<Reference>(
            refA,
            "The second element of the tuple \"(b[a], a)\" is expected to be represented by a Reference with localName \"a\" in the CPG.",
        )
        assertLocalName(
            "a",
            refA,
            "The second element of the tuple \"(b[a], a)\" is expected to be represented by a Reference with localName \"a\" in the CPG.",
        )

        // Now the actually interesting part: We check for variables belonging to the references.
        val innerVariableDeclarationA = refA.refersTo
        assertIs<VariableDeclaration>(
            innerVariableDeclarationA,
            "We expect that the reference \"a\" in the second element of the tuple refers to a VariableDeclaration with localName \"a\" which is not null and whose scope is the LocalScope of the list comprehension.",
        )
        assertIs<LocalScope>(
            innerVariableDeclarationA.scope,
            "We expect that the reference \"a\" refers to a VariableDeclaration with localName \"a\" which is not null and whose scope is the LocalScope of the list comprehension.",
        )
        assertEquals(
            1,
            refA.prevDFGEdges.size,
            "We expect that there's one DFG edge flowing into the reference \"a\" in the tuple. It should come from the InitializerListExpression and have the index \"0\"",
        )
        assertEquals(
            tuple,
            refA.prevDFG.single(),
            "We expect that there's one DFG edge flowing into the reference \"a\" in the tuple. It should come from the InitializerListExpression and have the index \"0\"",
        )
        val refAGranularity = refA.prevDFGEdges.single().granularity
        assertIs<IndexedDataflowGranularity>(
            refAGranularity,
            "We expect that there's one DFG edge flowing into the reference \"a\" in the tuple. It should come from the InitializerListExpression and have the index \"0\"",
        )
        assertEquals(
            1,
            refAGranularity.partialTarget,
            "We expect that there's one DFG edge flowing into the reference \"a\" in the tuple. It should come from the InitializerListExpression and have the index \"0\"",
        )
        assertEquals(
            listComprehensionWithTupleAndAssignmentToListElement,
            innerVariableDeclarationA.scope?.astNode,
            "We expect that the reference \"a\" refers to a VariableDeclaration with localName \"a\" which is not null and whose scope is the LocalScope of the list comprehension.",
        )

        val tupleToBA = accessBA.prevDFGEdges.singleOrNull { it.start == tuple }
        assertNotNull(
            tupleToBA,
            "We expect that there's one DFG edge flowing into the reference \"b[a]\" in the tuple. It should come from the InitializerListExpression and have the index \"0\"",
        )
        val accessBAGranularity = tupleToBA.granularity
        assertIs<IndexedDataflowGranularity>(
            accessBAGranularity,
            "We expect that there's one DFG edge flowing into the reference \"b[a]\" in the tuple. It should come from the InitializerListExpression and have the index \"0\"",
        )
        assertEquals(
            0,
            accessBAGranularity.partialTarget,
            "We expect that there's one DFG edge flowing into the reference \"b[a]\" in the tuple. It should come from the InitializerListExpression and have the index \"0\"",
        )
        assertRefersTo(
            refB,
            bDeclaration,
            "We expect that the reference \"b\" in the tuple refers to the VariableDeclaration of \"b\" which is added outside the list comprehension (in statement 0).",
        )
        assertEquals(
            0,
            indexA.prevDFG.size,
            "We expect that the reference \"a\" used as an index in the first element of the tuple does not have any incoming data flows which somewhat simulates that it's not initialized at this point in time which is also why python crashes.",
        )
        assertNotRefersTo(
            indexA,
            aDeclaration,
            "We expect that the reference \"a\" used as an index in the first element of the tuple does not refer to the same VariableDeclaration as the second element of the tuple nor to the local variable nor does it have an own VariableDeclaration since python would just crash.",
        )
        assertNotRefersTo(
            indexA,
            innerVariableDeclarationA,
            "We expect that the reference \"a\" used as an index in the first element of the tuple does not refer to the same VariableDeclaration as the second element of the tuple nor to the local variable nor does it have an own VariableDeclaration since python would just crash.",
        )
        assertNull(
            indexA.refersTo,
            "We expect that the reference \"a\" used as an index in the first element of the tuple does not refer to the same VariableDeclaration as the second element of the tuple nor to the local variable nor does it have an own VariableDeclaration since python would just crash.",
        )
    }

    @Test
    fun testComprehensionWithListAssignmentAndLocalIndexVariable() {
        val comprehensionWithListAssignmentAndLocalIndexVariableFunctionDeclaration =
            result.functions["comprehension_with_list_assignment_and_local_index_variable"]
        assertIs<FunctionDeclaration>(
            comprehensionWithListAssignmentAndLocalIndexVariableFunctionDeclaration,
            "There must be a function called \"comprehension_with_list_assignment_and_local_index_variable\" in the file. It must be neither null nor any other class than a FunctionDeclaration.",
        )

        // Get the body
        val body = comprehensionWithListAssignmentAndLocalIndexVariableFunctionDeclaration.body
        assertIs<Block>(
            body,
            "The body of each function is modeled as a Block in the CPG. This must also apply to the function \"comprehension_with_list_assignment_and_local_index_variable\".",
        )

        val listBInitialization = body.statements[0]
        assertIs<AssignExpression>(
            listBInitialization,
            "The first statement of the function \"comprehension_with_list_assignment_and_local_index_variable\" is expected to be the initialization of list \"b\" by the statement \"b = [0, 1, 2]\" which is expected to be represented by an AssignExpression in the CPG.",
        )
        val refBFirstStatement = listBInitialization.lhs[0]
        assertIs<Reference>(
            refBFirstStatement,
            "The left hand side of the assignment \"b = [0, 1, 2]\" is expected to be represented by a Reference with localName \"b\" in the CPG.",
        )
        assertLocalName(
            "b",
            refBFirstStatement,
            "The left hand side of the assignment \"b = [0, 1, 2]\" is expected to be represented by a Reference with localName \"b\" in the CPG.",
        )
        val bDeclaration = listBInitialization.variables["b"]
        assertIs<VariableDeclaration>(
            bDeclaration,
            "There must be a VariableDeclaration with the local name \"b\" inside the first statement of the function \"comprehension_with_list_assignment_and_local_index_variable\".",
        )
        assertRefersTo(
            refBFirstStatement,
            bDeclaration,
            "The reference \"b\" is expected to refer to the variable declaration \"b\" in the same statement.",
        )

        val localCAssignment = body.statements[1]
        assertIs<AssignExpression>(
            localCAssignment,
            "The first statement of the function \"comprehension_with_list_assignment_and_local_index_variable\" is expected to be the initialization of the local variable \"c\" by the statement \"c = 1\" which is expected to be represented by an AssignExpression in the CPG.",
        )
        val localCRef = localCAssignment.lhs[0]
        assertIs<Reference>(
            localCRef,
            "The left hand side of the assignment \"c = 1\" is expected to be represented by a Reference with localName \"c\" in the CPG.",
        )
        val cDeclaration = localCAssignment.variables["c"]
        assertIs<VariableDeclaration>(
            cDeclaration,
            "There must be a VariableDeclaration with the local name \"c\" inside the first statement of the function \"comprehension_with_list_assignment_and_local_index_variable\".",
        )
        assertRefersTo(
            localCRef,
            cDeclaration,
            "The reference \"c\" is expected to refer to the variable declaration \"c\" in the same statement.",
        )

        // Check if the AST of the list comprehension fits our expectations.
        val listComprehensionWithTupleAndAssignmentToListElement = body.statements[2]
        assertIs<CollectionComprehension>(
            listComprehensionWithTupleAndAssignmentToListElement,
            "The third statement of the function \"comprehension_with_list_assignment_and_local_index_variable\" is expected to be python's list comprehension \"[a for (b[c], a) in [('this', 0), ('is', 1), ('fun', 2)]]\" which is represented by a CollectionComprehension in the CPG",
        )

        val comprehensionExpression =
            listComprehensionWithTupleAndAssignmentToListElement.comprehensionExpressions
                .singleOrNull()
        assertEquals(
            1,
            listComprehensionWithTupleAndAssignmentToListElement.comprehensionExpressions.size,
            "There is expected to be exactly one CollectionComprehension in the list comprehension \"[a for (b[c], a) in [('this', 0), ('is', 1), ('fun', 2)]]\". It represents the code's part \"for (b[c], a) in [('this', 0), ('is', 1), ('fun', 2)]\"",
        )
        assertIs<ComprehensionExpression>(
            comprehensionExpression,
            "There is expected to be exactly one ComprehensionExpression in the list comprehension \"[a for (b[c], a) in [('this', 0), ('is', 1), ('fun', 2)]]\". It represents the code's part \"for (b[c], a) in [('this', 0), ('is', 1), ('fun', 2)]\"",
        )

        val tuple = comprehensionExpression.variable
        assertIs<InitializerListExpression>(
            tuple,
            "The variable of the ComprehensionExpression is the tuple \"(b[c], a)\" which is expected to be represented by an InitializerListExpression in the CPG.",
        )
        assertEquals(
            2,
            tuple.initializers.size,
            "The tuple \"(b[c], a)\" represented by an InitializerListExpression in the CPG is expected to have exactly two elements.",
        )
        val accessBA = tuple.initializers[0]
        assertIs<SubscriptExpression>(
            accessBA,
            "The first element of the tuple \"(b[c], a)\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\" and the subscriptExpression representing the index is a Reference \"c\" which refers to the local variable.",
        )
        val refB = accessBA.arrayExpression
        assertIs<Reference>(
            refB,
            "The first element of the tuple \"(b[c], a)\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\" and the subscriptExpression representing the index is a Reference \"c\" which refers to the local variable.",
        )
        assertLocalName(
            "b",
            refB,
            "The first element of the tuple \"(b[c], a)\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\" and the subscriptExpression representing the index is a Reference \"c\" which refers to the local variable.",
        )
        val index = accessBA.subscriptExpression
        assertIs<Reference>(
            index,
            "The first element of the tuple \"(b[c], a)\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\" and the subscriptExpression representing the index is a Reference \"c\" which refers to the local variable.",
        )
        assertLocalName(
            "c",
            index,
            "The first element of the tuple \"(b[c], a)\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\" and the subscriptExpression representing the index is a Reference \"c\" which refers to the local variable.",
        )
        val refA = tuple.initializers[1]
        assertIs<Reference>(
            refA,
            "The second element of the tuple \"(b[c], a)\" is expected to be represented by a Reference with localName \"a\" in the CPG.",
        )
        assertLocalName(
            "a",
            refA,
            "The second element of the tuple \"(b[c], a)\" is expected to be represented by a Reference with localName \"a\" in the CPG.",
        )

        // Now the actually interesting part: We check for variables belonging to the references.
        val variableDeclarationA = refA.refersTo
        assertIs<VariableDeclaration>(
            variableDeclarationA,
            "We expect that the reference \"a\" in the second element of the tuple refers to a VariableDeclaration with localName \"a\" which is not null and whose scope is the LocalScope of the list comprehension.",
        )
        assertIs<LocalScope>(
            variableDeclarationA.scope,
            "We expect that the reference \"a\" refers to a VariableDeclaration with localName \"a\" which is not null and whose scope is the LocalScope of the list comprehension.",
        )
        assertEquals(
            listComprehensionWithTupleAndAssignmentToListElement,
            variableDeclarationA.scope?.astNode,
            "We expect that the reference \"a\" refers to a VariableDeclaration with localName \"a\" which is not null and whose scope is the LocalScope of the list comprehension.",
        )
        assertRefersTo(
            refB,
            bDeclaration,
            "We expect that the reference \"b\" in the tuple refers to the VariableDeclaration of \"b\" which is added outside the list comprehension (in statement 0).",
        )
        assertRefersTo(
            index,
            cDeclaration,
            "We expect that the reference \"c\" used as an index in the first element of the tuple refers to the local variable \"c\" (in statement 1).",
        )
    }

    @Test
    fun testListComprehensionToListIndex() {
        val moreLoopVariablesFunctionDeclaration =
            result.functions["list_comprehension_to_list_index"]
        assertIs<FunctionDeclaration>(
            moreLoopVariablesFunctionDeclaration,
            "There must be a function called \"list_comprehension_to_list_index\" in the file. It must be neither null nor any other class than a FunctionDeclaration.",
        )

        // Get the body
        val body = moreLoopVariablesFunctionDeclaration.body
        assertIs<Block>(
            body,
            "The body of each function is modeled as a Block in the CPG. This must also apply to the function \"list_comprehension_to_list_index\".",
        )

        val listBInitialization = body.statements[0]
        assertIs<AssignExpression>(
            listBInitialization,
            "The first statement of the function \"list_comprehension_to_list_index\" is expected to be the initialization of list \"b\" by the statement \"b = [0, 1, 2]\" which is expected to be represented by an AssignExpression in the CPG.",
        )
        val refBFirstStatement = listBInitialization.lhs[0]
        assertIs<Reference>(
            refBFirstStatement,
            "The left hand side of the assignment \"b = [0, 1, 2]\" is expected to be represented by a Reference with localName \"b\" in the CPG.",
        )
        assertLocalName(
            "b",
            refBFirstStatement,
            "The left hand side of the assignment \"b = [0, 1, 2]\" is expected to be represented by a Reference with localName \"b\" in the CPG.",
        )
        val bDeclaration = listBInitialization.variables["b"]
        assertIs<VariableDeclaration>(
            bDeclaration,
            "There must be a VariableDeclaration with the local name \"b\" inside the first statement of the function \"list_comprehension_to_list_index\".",
        )
        assertRefersTo(
            refBFirstStatement,
            bDeclaration,
            "The reference \"b\" is expected to refer to the variable declaration \"b\" in the same statement.",
        )

        // Check if the AST of the list comprehension fits our expectations.
        val listComprehensionWithTupleAndAssignmentToListElement = body.statements[1]
        assertIs<CollectionComprehension>(
            listComprehensionWithTupleAndAssignmentToListElement,
            "The second statement of the function \"list_comprehension_to_list_index\" is expected to be python's list comprehension \"[b[0] for b[0] in ['this', 'is', 'fun']]\" which is represented by a CollectionComprehension in the CPG",
        )

        val comprehensionExpression =
            listComprehensionWithTupleAndAssignmentToListElement.comprehensionExpressions
                .singleOrNull()
        assertEquals(
            1,
            listComprehensionWithTupleAndAssignmentToListElement.comprehensionExpressions.size,
            "There is expected to be exactly one CollectionComprehension in the list comprehension \"[b[0] for b[0] in ['this', 'is', 'fun']]\". It represents the code's part \"for b[0] in ['this', 'is', 'fun']\"",
        )
        assertIs<ComprehensionExpression>(
            comprehensionExpression,
            "There is expected to be exactly one ComprehensionExpression in the list comprehension \"[b[0] for b[0] in ['this', 'is', 'fun']]\". It represents the code's part \"for b[0] in ['this', 'is', 'fun']\"",
        )

        val accessB0 = comprehensionExpression.variable
        assertIs<SubscriptExpression>(
            accessB0,
            "The control variable of the ComprehensionExpression is \"b[0]\" which is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\" and the subscriptExpression representing the index is a Literal<Int> with value \"0\".",
        )
        val refB = accessB0.arrayExpression
        assertIs<Reference>(
            refB,
            "The control variable \"b[0]\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\" and the subscriptExpression representing the index is a Literal<Int> with value \"0\" (with kotlin type Long ).",
        )
        assertLocalName(
            "b",
            refB,
            "The control variable \"b[0]\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\" and the subscriptExpression representing the index is a Literal<Int> with value \"0\" (with kotlin type Long ).",
        )
        val index = accessB0.subscriptExpression
        assertIs<Literal<Int>>(
            index,
            "The control variable \"b[0]\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\" and the subscriptExpression representing the index is a Literal<Int> with value \"0\" (with kotlin type Long ).",
        )
        assertLiteralValue(
            0L,
            index,
            "The control variable \"b[0]\" is expected to be represented by a SubscriptExpression with in the CPG. We expect that the base is a Reference with localName \"b\" and the subscriptExpression representing the index is a Literal<Int> with value \"0\" (with kotlin type Long ).",
        )

        // Now the actually interesting part: We check for variables belonging to the references.
        assertRefersTo(
            refB,
            bDeclaration,
            "We expect that the reference \"b\" in the control variable refers to the VariableDeclaration of \"b\" which is added outside the list comprehension (in statement 0).",
        )
        assertContains(
            accessB0.prevDFG,
            comprehensionExpression.iterable,
            "We expect that there's a DFG edge flowing into the control variable \"b[0]\" from the iterable",
        )
    }

    @Test
    fun testListComprehensionToField() {
        val listComprehensionToFieldFunctionDeclaration =
            result.functions["list_comprehension_to_field"]
        assertIs<FunctionDeclaration>(
            listComprehensionToFieldFunctionDeclaration,
            "There must be a function called \"list_comprehension_to_field\" in the file. It must be neither null nor any other class than a FunctionDeclaration.",
        )

        // Get the body
        val body = listComprehensionToFieldFunctionDeclaration.body
        assertIs<Block>(
            body,
            "The body of each function is modeled as a Block in the CPG. This must also apply to the function \"list_comprehension_to_field\".",
        )

        val listBInitialization = body.statements[0]
        assertIs<AssignExpression>(
            listBInitialization,
            "The first statement of the function \"list_comprehension_to_field\" is expected to be the initialization of list \"b\" by the statement \"b = Magic()\" which is expected to be represented by an AssignExpression in the CPG.",
        )
        val refBFirstStatement = listBInitialization.lhs[0]
        assertIs<Reference>(
            refBFirstStatement,
            "The left hand side of the assignment \"b = Magic()\" is expected to be represented by a Reference with localName \"b\" in the CPG.",
        )
        assertLocalName(
            "b",
            refBFirstStatement,
            "The left hand side of the assignment \"b = Magic())\" is expected to be represented by a Reference with localName \"b\" in the CPG.",
        )
        val bDeclaration = listBInitialization.variables["b"]
        assertIs<VariableDeclaration>(
            bDeclaration,
            "There must be a VariableDeclaration with the local name \"b\" inside the first statement of the function \"list_comprehension_to_field\".",
        )
        assertRefersTo(
            refBFirstStatement,
            bDeclaration,
            "The reference \"b\" is expected to refer to the variable declaration \"b\" in the same statement.",
        )

        // Check if the AST of the list comprehension fits our expectations.
        val listComprehensionWithTupleAndAssignmentToListElement = body.statements[1]
        assertIs<CollectionComprehension>(
            listComprehensionWithTupleAndAssignmentToListElement,
            "The second statement of the function \"list_comprehension_to_field\" is expected to be python's list comprehension \"[b.a for b.a in ['this', 'is', 'fun']]\" which is represented by a CollectionComprehension in the CPG.",
        )

        val comprehensionExpression =
            listComprehensionWithTupleAndAssignmentToListElement.comprehensionExpressions
                .singleOrNull()
        assertEquals(
            1,
            listComprehensionWithTupleAndAssignmentToListElement.comprehensionExpressions.size,
            "There is expected to be exactly one CollectionComprehension in the list comprehension \"[b.a for b.a in ['this', 'is', 'fun']]\". It represents the code's part \"for b.a in ['this', 'is', 'fun']\"",
        )
        assertIs<ComprehensionExpression>(
            comprehensionExpression,
            "There is expected to be exactly one ComprehensionExpression in the list comprehension \"[b.a for b.a in ['this', 'is', 'fun']]\". It represents the code's part \"for b.a in ['this', 'is', 'fun']\"",
        )

        val bMemberA = comprehensionExpression.variable
        assertIs<MemberExpression>(
            bMemberA,
            "The control variable \"b.a\" is expected to be represented by a MemberExpression in the CPG. We expect that the base is a Reference with localName \"b\" and the localName of the MemberExpression is \"a\".",
        )
        val refB = bMemberA.base
        assertIs<Reference>(
            refB,
            "The control variable \"b.a\" is expected to be represented by a MemberExpression in the CPG. We expect that the base is a Reference with localName \"b\" and the localName of the MemberExpression is \"a\".",
        )
        assertLocalName(
            "b",
            refB,
            "The control variable \"b.a\" is expected to be represented by a MemberExpression in the CPG. We expect that the base is a Reference with localName \"b\" and the localName of the MemberExpression is \"a\".",
        )
        assertLocalName(
            "a",
            bMemberA,
            "The control variable \"b.a\" is expected to be represented by a MemberExpression in the CPG. We expect that the base is a Reference with localName \"b\" and the localName of the MemberExpression is \"a\".",
        )

        // Now the actually interesting part: We check for variables belonging to the references.
        assertRefersTo(
            refB,
            bDeclaration,
            "We expect that the reference \"b\" used in the control variable refers to the VariableDeclaration of \"b\" which is added outside the list comprehension (in statement 0).",
        )

        val magicClass = result.records["Magic"]
        assertIs<RecordDeclaration>(
            magicClass,
            "There must be a class called \"Magic\" in the file. It must be neither null nor any other class than a RecordDeclaration which is expected to model python classes in the CPG.",
        )
        assertEquals(
            1,
            magicClass.fields.size,
            "We expect exactly one field inside the record declaration representing the class \"Magic\" and that's the field which we expect to represent the class' attribute \"a\".",
        )
        val fieldA = magicClass.fields["a"]
        assertIs<FieldDeclaration>(
            fieldA,
            "We expect exactly one field inside the record declaration representing the class \"Magic\" and that's the field which we expect to represent the class' attribute \"a\".",
        )

        assertRefersTo(
            bMemberA,
            fieldA,
            "We expect that the member expression \"b.a\" used as control variable refers to the FieldDeclaration \"a\" of the class \"Magic\".",
        )
        assertEquals(
            1,
            bMemberA.prevDFGEdges.size,
            "We expect that there's one DFG edge flowing into the control variable \"b.a\". The DFG should come from the iterable.",
        )
        assertEquals(
            comprehensionExpression.iterable,
            bMemberA.prevDFG.single(),
            "We expect that there's one DFG edge flowing into the control variable \"b.a\". The DFG should come from the iterable.",
        )
    }
}
