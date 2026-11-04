/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Namespace
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import de.fraunhofer.aisec.cpg.test.*
import de.fraunhofer.aisec.cpg.test.GraphExamples
import kotlin.test.*

internal class SubgraphWalkerTest : BaseTest() {
    @Test
    fun testLoopDetection() {
        // Let's create an intentional loop
        val tu = TranslationUnit()
        val name = Namespace()
        val func = Function()
        name.addDeclaration(tu)
        name.addDeclaration(func)
        tu.addDeclaration(name)

        val flat = SubgraphWalker.flattenAST(tu)

        assertEquals(listOf<Node>(tu, name, func), flat)
    }

    @Test
    @Throws(Exception::class)
    fun testASTChildrenGetter() {
        val tu =
            GraphExamples.getVisitorTest(
                    TranslationConfiguration.builder()
                        .loadIncludes(true)
                        .disableCleanup()
                        .debugParser(true)
                        .failOnError(true)
                        .useParallelFrontends(true)
                        .registerLanguage<TestLanguage>()
                        .defaultPasses()
                        .build()
                )
                .components
                .first()
                .translationUnits
                .first()
        val namespace = tu.namespaces["compiling"]
        assertNotNull(namespace)

        val recordDeclaration = namespace.records["compiling.SimpleClass"]
        assertNotNull(recordDeclaration)

        // This calls SubgraphWalker.getAstChildren()
        val ast = recordDeclaration.astChildren
        assertFalse(ast.isEmpty())

        // should contain 3 AST nodes, 1 field, 1 method, 1 constructor
        assertEquals(3, ast.size)
    }

    @Test
    fun testReplaceInput() {
        with(testFrontend { it.registerLanguage<TestLanguage>() }) {
            val walker = SubgraphWalker.ScopedWalker(scopeManager, Strategy::AST_FORWARD)

            fun assertReplacesInput(
                parent: Expression,
                getInput: () -> Expression,
                setInput: (Expression) -> Unit,
            ) {
                val old = newReference("old")
                setInput(old)
                parent.access = AccessValues.WRITE

                val new = newReference("new")
                assertEquals(AccessValues.READ, new.access)
                assertTrue(walker.replace(parent, old, new))
                assertSame(new, getInput())
                assertEquals(parent, new.astParent)
                assertEquals(AccessValues.WRITE, new.access)

                // Replacing a node that is not the input must fail
                assertFalse(walker.replace(parent, old, newReference("other")))
                assertSame(new, getInput())
            }

            val unary = newUnaryOperator("-", postfix = false, prefix = true)
            assertReplacesInput(unary, { unary.input }, { unary.input = it })

            val deref = newPointerDereference("*old")
            assertReplacesInput(deref, { deref.input }, { deref.input = it })

            val ref = newPointerReference("&old")
            assertReplacesInput(ref, { ref.input }, { ref.input = it })
        }
    }

    /**
     * Regression test for a bug where [SubgraphWalker.replace] threw a
     * [java.util.ConcurrentModificationException] while migrating [HasType.typeObservers] from the
     * replaced node to the replacement: the old node's `typeObservers` is an [IdentitySet] backed
     * by an [java.util.IdentityHashMap], and the migration loop used to call
     * `old.typeObservers.forEach { old.unregisterTypeObserver(it); ... }`, mutating the very set it
     * was iterating over. This only manifests once a node has 2 or more observers, since
     * [java.util.IdentityHashMap]'s iterator only detects the comodification on the *next* call to
     * `next()`.
     *
     * This test does not reliably reproduce an actual data race (there isn't one - the exception is
     * fully deterministic and single-threaded), it just exercises the previously-broken code path
     * with multiple observers registered.
     */
    @Test
    fun testReplaceMigratesMultipleTypeObservers() {
        with(testFrontend { it.registerLanguage<TestLanguage>() }) {
            val walker = SubgraphWalker.ScopedWalker(scopeManager, Strategy::AST_FORWARD)

            class RecordingObserver : HasType.TypeObserver {
                var lastType: Type? = null

                override fun typeChanged(newType: Type, src: HasType) {
                    lastType = newType
                }

                override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {}
            }

            val unary = newUnaryOperator("-", postfix = false, prefix = true)
            val old = newReference("old")
            unary.input = old

            // Register (at least) two observers on the old node. A single observer would not have
            // triggered the bug.
            val observer1 = RecordingObserver()
            val observer2 = RecordingObserver()
            old.registerTypeObserver(observer1)
            old.registerTypeObserver(observer2)

            val new = newReference("new")

            // This must not throw a ConcurrentModificationException.
            assertTrue(walker.replace(unary, old, new))

            // Neither observer must still be registered on the old (now disconnected) node...
            assertFalse(old.typeObservers.contains(observer1))
            assertFalse(old.typeObservers.contains(observer2))
            // ... and both must have been migrated to the new node. (new.typeObservers may also
            // contain further observers, e.g. "unary" itself, which started observing "new" as a
            // side effect of "parent.input = new" above; that is unrelated existing behavior.)
            assertTrue(new.typeObservers.contains(observer1))
            assertTrue(new.typeObservers.contains(observer2))
        }
    }
}
