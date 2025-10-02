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
package de.fraunhofer.aisec.cpg.analysis.abstracteval.value

import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval
import de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleStateElement
import de.fraunhofer.aisec.cpg.analysis.abstracteval.intervalOf
import de.fraunhofer.aisec.cpg.graph.Node

/**
 * This class implements the [Value] interface for tracking the size of mutable collections. It
 * provides several operations which can then be used for concrete implementations.
 */
abstract class MutableCollectionSize : Value<LatticeInterval> {
    fun createEmptyCollection(): LatticeInterval {
        return LatticeInterval.Bounded(0, 0)
    }

    fun createWithElementsFromElement(
        element: Node,
        state: TupleStateElement<Any>,
    ): LatticeInterval {
        return state.intervalOf(element)
    }

    fun createWithElements(elements: List<Node>, state: TupleStateElement<Any>): LatticeInterval {
        return LatticeInterval.Bounded(elements.size, elements.size)
    }

    fun addSingleElementWithoutElementCheck(
        target: Node,
        state: TupleStateElement<Any>,
    ): LatticeInterval {
        return state.intervalOf(target) + LatticeInterval.Bounded(1, 1)
    }

    fun addSingleElementWithElementCheck(
        target: Node,
        state: TupleStateElement<Any>,
    ): LatticeInterval {
        return state.intervalOf(target) + LatticeInterval.Bounded(0, 1)
    }

    fun addMultipleElementsWithoutElementCheck(
        target: Node,
        element: Node,
        state: TupleStateElement<Any>,
    ): LatticeInterval {
        return state.intervalOf(target) + state.intervalOf(element)
    }

    fun addMultipleElementsWithElementCheck(
        target: Node,
        element: Node,
        state: TupleStateElement<Any>,
    ): LatticeInterval {
        val elementSize = state.intervalOf(element)
        // TODO: Do we want infinite or zero as upper bound or return bottom if we have bottom for
        // the element?
        val maxSize =
            (elementSize as? LatticeInterval.Bounded)?.upper ?: LatticeInterval.Bound.INFINITE
        return state.intervalOf(target) +
            LatticeInterval.Bounded(LatticeInterval.Bound.Value(0), maxSize)
    }

    fun addMultipleElementsWithoutElementCheck(
        target: Node,
        elements: List<Node>,
        state: TupleStateElement<Any>,
    ): LatticeInterval {
        return state.intervalOf(target) + LatticeInterval.Bounded(elements.size, elements.size)
    }

    fun addMultipleElementsWithElementCheck(
        target: Node,
        elements: List<Node>,
        state: TupleStateElement<Any>,
    ): LatticeInterval {
        return state.intervalOf(target) + LatticeInterval.Bounded(0, elements.size)
    }

    fun clearAllElements(): LatticeInterval {
        return LatticeInterval.Bounded(0, 0)
    }

    fun removeSingleElementWithoutElementCheck(
        target: Node,
        state: TupleStateElement<Any>,
    ): LatticeInterval {
        val result = state.intervalOf(target) - LatticeInterval.Bounded(1, 1)

        // If the lower bound is less than 0, we set it to 0 as negative sizes do not make sense
        if (result is LatticeInterval.Bounded && result.lower < LatticeInterval.Bound.Value(0)) {
            return LatticeInterval.Bounded(LatticeInterval.Bound.Value(0), result.upper)
        }
        return result
    }

    fun removeSingleElementWithElementCheck(
        target: Node,
        state: TupleStateElement<Any>,
    ): LatticeInterval {
        val result = state.intervalOf(target) - LatticeInterval.Bounded(0, 1)

        // If the lower bound is less than 0, we set it to 0 as negative sizes do not make sense
        if (result is LatticeInterval.Bounded && result.lower < LatticeInterval.Bound.Value(0)) {
            return LatticeInterval.Bounded(LatticeInterval.Bound.Value(0), result.upper)
        }
        return result
    }

    fun removeMultipleElementsWithoutElementCheck(
        target: Node,
        state: TupleStateElement<Any>,
    ): LatticeInterval {
        // We could remove all elements if all are similar, so we only know that the new size will
        // be somewhere between 0 and the current maximal size
        val result = state.intervalOf(target)

        if (result is LatticeInterval.Bounded) {
            return LatticeInterval.Bounded(LatticeInterval.Bound.Value(0), result.upper)
        }
        return result
    }

    fun removeMultipleElementsWithElementCheck(
        target: Node,
        elements: List<Node>,
        state: TupleStateElement<Any>,
    ): LatticeInterval {
        val result = state.intervalOf(target) - LatticeInterval.Bounded(0, elements.size)

        // If the lower bound is less than 0, we set it to 0 as negative sizes do not make sense
        if (result is LatticeInterval.Bounded && result.lower < LatticeInterval.Bound.Value(0)) {
            return LatticeInterval.Bounded(LatticeInterval.Bound.Value(0), result.upper)
        }
        return result
    }
}
