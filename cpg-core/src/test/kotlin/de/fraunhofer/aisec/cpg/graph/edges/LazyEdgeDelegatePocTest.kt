/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.edges

import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField
import kotlin.test.*

/**
 * Proof-of-concept and feasibility check for "option 1b": replacing the manual three-member edge
 * pattern
 *
 * ```
 * private var _fooEdges: FooEdges? = null           // backing field
 * @Relationship var fooEdges: FooEdges get()/set()  // persisted edge property
 * @DoNotPersist var foos: MutableSet<Node> get()/set() // unwrapped view
 * ```
 *
 * with a single-line Kotlin property delegate (`var fooEdges by lazyEdge { ... }`).
 *
 * The delegate is attractive: it is one line and it is genuinely lazy (the container is only built
 * on first access). This test exists to answer, empirically, whether it can actually be adopted,
 * because two independent reflection mechanisms read these members and they read them differently:
 * - **Persistence** ([de.fraunhofer.aisec.cpg.persistence], `isRelationship`) keys off the Kotlin
 *   *property return type*. A delegated edge property therefore still looks like a relationship —
 *   *except* that `isRelationship` explicitly drops any property whose backing field type name
 *   contains `"Delegate"`.
 * - **AST/DFG traversal** ([SubgraphWalker.getAllEdgeFields]) reads the raw Java *field* whose name
 *   contains `"Edge"` and expects it to hold an `EdgeCollection`. A delegated property's backing
 *   field holds the *delegate object*, not the container.
 *
 * The assertions below confirm the conclusion: a delegate is transparent to persistence but opaque
 * to `getAllEdgeFields`, so it silently removes the edges from AST/DFG traversal. That is why the
 * production code keeps the manual field and only shortens the getter via a factory (option 1a). A
 * delegate could only be adopted if [SubgraphWalker.getAllEdgeFields] were first taught to unwrap
 * delegates.
 */
class LazyEdgeDelegatePocTest {

    /**
     * Minimal lazy read/write delegate: build the value via [factory] on first read, cache it, and
     * allow it to be overwritten. This is the shape a real `lazyEdge` delegate would take.
     */
    private class LazyEdge<T : Any>(val factory: () -> T) : ReadWriteProperty<Any?, T> {
        var backing: T? = null
            private set

        override fun getValue(thisRef: Any?, property: KProperty<*>): T =
            backing ?: factory().also { backing = it }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            backing = value
        }
    }

    /** Sample using the delegate. Note the single-line declaration that replaces the triplet. */
    private class DelegatedSample(counter: IntArray) {
        var fooEdges: MutableList<String> by LazyEdge {
            counter[0]++
            mutableListOf()
        }
    }

    /** Sample using the manual backing-field pattern that the real nodes use today. */
    @Suppress("unused")
    private class ManualSample {
        private var _fooEdges: MutableList<String>? = null
        var fooEdges: MutableList<String>
            get() = _fooEdges ?: mutableListOf<String>().also { _fooEdges = it }
            set(value) {
                _fooEdges = value
            }
    }

    @Test
    fun testDelegateIsLazyAndCached() {
        val counter = intArrayOf(0)
        val sample = DelegatedSample(counter)

        // The container is not built until first access.
        assertEquals(0, counter[0])

        sample.fooEdges.add("a")
        assertEquals(1, counter[0])

        // Second access reuses the cached container instead of rebuilding it.
        sample.fooEdges.add("b")
        assertEquals(1, counter[0])
        assertEquals(listOf("a", "b"), sample.fooEdges)
    }

    @Test
    fun testTraversalCannotSeeDelegatedContainer() {
        // getAllEdgeFields selects Java fields whose *name* contains "Edge".
        val fooField =
            SubgraphWalker.getAllEdgeFields(DelegatedSample::class.java).single {
                it.name.startsWith("fooEdges")
            }

        // The backing field is the synthetic delegate holder, not the container itself...
        assertEquals("fooEdges\$delegate", fooField.name)
        // ...and its type is the delegate, so getAstChildren's `is EdgeCollection` branch would not
        // match (it throws AnnotationFormatError for a field that is not an edge/edge collection).
        assertEquals(LazyEdge::class.java, fooField.type)
        assertNotEquals(MutableList::class.java, fooField.type)

        // By contrast, the manual pattern exposes a field that directly holds the container, which
        // is exactly what the traversal expects.
        val manualField =
            SubgraphWalker.getAllEdgeFields(ManualSample::class.java).single {
                it.name.contains("Edge")
            }
        assertEquals("_fooEdges", manualField.name)
    }

    @Test
    fun testPersistenceSeesReturnTypeNotDelegate() {
        val prop = DelegatedSample::fooEdges

        // Persistence's isRelationship keys off the property's RETURN type, which is transparent to
        // the delegate: it sees MutableList (a Collection), not the LazyEdge wrapper. So the
        // relationship path itself is unaffected by delegation.
        assertNotEquals(LazyEdge::class, prop.returnType.classifier)

        // The decisive guard is the *backing field* type: isRelationship drops any property whose
        // field type simpleName contains "Delegate". Our delegate is deliberately not named
        // "*Delegate" (so this assertion passes), but it shows what that guard keys on: naming the
        // delegate class conventionally (…Delegate) would silently drop the relationship.
        val fieldTypeName = prop.javaField?.type?.simpleName
        assertEquals("LazyEdge", fieldTypeName)
        assertFalse(
            fieldTypeName!!.contains("Delegate"),
            "a delegate named *Delegate would be excluded from persistence by isRelationship",
        )
    }
}
