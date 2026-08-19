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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.While
import de.fraunhofer.aisec.cpg.graph.types.BooleanType
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertInvokes
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SymbolResolverTest {

    @Test
    fun testExternVariables() {
        val file = File("src/test/resources/two_files/file.c")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        assertEquals(1, result.variables.filter { it.name.localName == "x" }.size)
    }

    @Test
    fun testExternVariablesReversedOrder() {
        val file = File("src/test/resources/cxx/extern/reversed_order.c")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        val variables = result.variables.filter { it.name.localName == "x" }
        assertEquals(1, variables.size)
        assertNotNull(variables.single().initializer)
    }

    @Test
    fun testExternVariablesTentativeDefinition() {
        val file = File("src/test/resources/cxx/extern/tentative_definition.c")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        val variables = result.variables.filter { it.name.localName == "x" }
        assertEquals(1, variables.size)
        assertNotNull(variables.single().initializer)
        result.refs { it.name.localName == "x" }.forEach { assertNotNull(it.refersTo) }
    }

    @Test
    fun testExternVariablesWithInitializer() {
        val file = File("src/test/resources/cxx/extern/extern_with_initializer.c")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        val variables = result.variables.filter { it.name.localName == "x" }
        assertEquals(1, variables.size)
        assertNotNull(variables.single().initializer)
    }

    @Test
    fun testExternVariablesCrossTranslationUnit() {
        val topLevel = File("src/test/resources/cxx/extern/cross_tu")
        val files = listOf(File(topLevel, "decl_only.c"), File(topLevel, "def.c"))
        val result =
            analyze(files, topLevel.toPath(), usePasses = true) {
                it.registerLanguage<CLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        val ref = result.refs.singleOrNull { it.name.localName == "x" }
        assertNotNull(ref, "expected exactly one reference to 'x' in decl_only.c")
        val resolved = ref.refersTo
        assertNotNull(
            resolved,
            "reference to 'x' declared extern in one TU and defined in " + "another should resolve",
        )
        assertNotNull((resolved as? Variable)?.initializer)
    }

    @Test
    fun testOnlyVariables() {
        val file = File("src/test/resources/cxx/symbols/only_variables.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CPPLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        assertNotNull(result)
        result.refs.forEach { assertNotNull(it.refersTo, "$it should not have an empty refersTo") }

        val ifCondition = result.ifs.firstOrNull()?.condition
        assertNotNull(ifCondition)
        assertIs<BooleanType>(ifCondition.type, "Type of if condition should be BooleanType")

        val unaryOp = result.allChildren<UnaryOperator>().firstOrNull()
        assertNotNull(unaryOp)
    }

    @Test
    fun testMemberCalls() {
        val file = File("src/test/resources/cxx/symbols/member_calls.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CPPLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        assertNotNull(result)
        result.refs.forEach { assertNotNull(it.refersTo) }
        result.mcalls.forEach { assertTrue(it.invokes.isNotEmpty()) }
    }

    @Test
    fun testSimpleCalls() {
        val file = File("src/test/resources/cxx/symbols/simple_calls.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CPPLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        assertNotNull(result)
        result.refs.forEach { assertNotNull(it.refersTo) }
        result.calls.forEach { assertTrue(it.invokes.isNotEmpty()) }
    }

    /**
     * This exercises [SymbolResolver.handleOverloadedOperator], which physically replaces a
     * [de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator] / [UnaryOperator] with an
     * [de.fraunhofer.aisec.cpg.graph.expressions.OperatorCall] via
     * [de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker.replace]. That replacement needs
     * a valid [SymbolResolver.walker] instance, which the EOG-worklist code path did not set up
     * before - this reuses the same fixture as
     * [de.fraunhofer.aisec.cpg.frontends.cxx.CXXDeclarationTest.testArithmeticOperator] to check
     * that overloaded operator resolution produces the same result under
     * [SymbolResolver.Configuration.experimentalEOGWorklist].
     */
    @Test
    fun testOverloadedOperators() {
        val file = File("src/test/resources/cxx/operators/arithmetic.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CPPLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        assertNotNull(result)

        val integer = result.records["Integer"]
        assertNotNull(integer)

        val plusplus = integer.operators["operator++"]
        assertNotNull(plusplus)

        val plus = integer.operators("operator+")
        assertEquals(2, plus.size)

        val main = result.functions["main"]
        assertNotNull(main)

        val unaryOp = main.operatorCalls["++"]
        assertNotNull(unaryOp)
        assertInvokes(unaryOp, plusplus)

        val binaryOp0 = main.operatorCalls("+").getOrNull(0)
        assertNotNull(binaryOp0)
        assertInvokes(binaryOp0, plus.getOrNull(0))

        val binaryOp1 = main.operatorCalls("+").getOrNull(1)
        assertNotNull(binaryOp1)
        assertInvokes(binaryOp1, plus.getOrNull(1))
    }

    /**
     * [SymbolResolver.accept] used to only route
     * [de.fraunhofer.aisec.cpg.graph.declarations.Function] EOG starters through
     * [SymbolResolver.acceptWithIterateEOG]; every other kind of EOG starter (e.g. a
     * [de.fraunhofer.aisec.cpg.graph.declarations.Field] with an initializer, which has its own
     * small, isolated EOG chain per
     * [de.fraunhofer.aisec.cpg.graph.declarations.Record.eogStarters]) always fell back to the
     * default resolver, even with [SymbolResolver.Configuration.experimentalEOGWorklist] enabled.
     * This checks that a field initializer calling a top-level function - resolved via the
     * (unaffected) global scope, not anything tracked per-EOG-starter - still resolves correctly
     * now that this restriction is lifted.
     */
    @Test
    fun testFieldInitializerCallsGlobalFunction() {
        val file = File("src/test/resources/cxx/symbols/field_initializer.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CPPLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        assertNotNull(result)

        val helper = result.functions["helper"]
        assertNotNull(helper)

        val field = result.fields["value"]
        assertNotNull(field)

        val call = field.initializer
        assertIs<Call>(call)
        assertInvokes(call, helper)
    }

    /**
     * None of the other tests in this file contain a loop. A loop's head is reached via more than
     * one incoming EOG edge (falling into the loop, and the back-edge from the end of the loop
     * body), so [de.fraunhofer.aisec.cpg.helpers.functional.Lattice.iterateEOG] has to run a real
     * fixpoint here (as opposed to the merge points after an `if`/`else`, which never feed back
     * into themselves). This checks that references inside the loop body/condition still resolve
     * correctly and that the analysis converges instead of timing out.
     */
    @Test
    fun testLoop() {
        val file = File("src/test/resources/cxx/symbols/loop.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CPPLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        assertNotNull(result)
        result.refs.forEach { assertNotNull(it.refersTo, "$it should not have an empty refersTo") }

        val whileStatement = result.allChildren<While>().firstOrNull()
        assertNotNull(whileStatement)
        assertIs<BooleanType>(
            whileStatement.condition?.type,
            "Type of while condition should be BooleanType",
        )
    }

    /**
     * [SymbolResolver.handleReference]'s implicit-receiver fallback (used when an unqualified
     * lookup finds nothing) reads [de.fraunhofer.aisec.cpg.ScopeManager.currentRecord], which in
     * turn reads [de.fraunhofer.aisec.cpg.ScopeManager.currentScope]. The default
     * [de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker] keeps that ambient state in
     * sync with the node it is currently visiting, but
     * [de.fraunhofer.aisec.cpg.helpers.functional.Lattice.iterateEOG] has no such notion by
     * itself - [acceptWithIterateEOG] has to update it explicitly. Without that, a call/reference
     * relying on an implicit receiver (no explicit `this->`) resolves against whatever scope was
     * last left behind by unrelated processing, rather than the record that lexically contains it.
     */
    @Test
    fun testImplicitReceiver() {
        val file = File("src/test/resources/cxx/symbols/implicit_receiver.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CPPLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        assertNotNull(result)

        val myClass = result.records["MyClass"]
        assertNotNull(myClass)
        val helper = myClass.methods["helper"]
        assertNotNull(helper)
        val value = myClass.fields["value"]
        assertNotNull(value)
        val caller = myClass.methods["caller"]
        assertNotNull(caller)

        val callToHelper = caller.calls.firstOrNull()
        assertNotNull(callToHelper)
        assertInvokes(callToHelper, helper)

        val valueRef = caller.refs("value").firstOrNull()
        assertNotNull(valueRef)
        assertEquals(value, valueRef.refersTo)
    }

    /**
     * A node reached by more than one incoming EOG edge - like the first node after an `if`/`else`
     * where neither branch terminates - is offered to
     * [de.fraunhofer.aisec.cpg.helpers.functional.Lattice.iterateEOG]'s transfer function once per
     * incoming edge, each with its own separate slice of lattice state. [acceptWithIterateEOG] used
     * to track "has SymbolResolver.handle already run for this node" in that per-edge state, which
     * doesn't catch this case (only loop back-edges, where the *same* edge is revisited). Calling
     * an inferring handler like [SymbolResolver.handleCall] twice on the very call that sits at
     * such a merge point used to create two separate inferred functions for the same undeclared
     * symbol instead of reusing the first one.
     */
    @Test
    fun testMergePointCallIsHandledOnce() {
        val file = File("src/test/resources/cxx/symbols/merge_point_call.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CPPLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        assertNotNull(result)

        val undeclaredFunctions = result.functions("undeclared_function")
        assertEquals(
            1,
            undeclaredFunctions.size,
            "Expected exactly one inferred declaration for the undeclared function, not one per incoming EOG edge.",
        )

        val call = result.calls("undeclared_function").firstOrNull()
        assertNotNull(call)
        assertInvokes(call, undeclaredFunctions.single())
    }
}
