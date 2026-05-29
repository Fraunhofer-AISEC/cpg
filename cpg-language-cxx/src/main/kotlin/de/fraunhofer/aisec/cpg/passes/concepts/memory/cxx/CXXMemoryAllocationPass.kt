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
package de.fraunhofer.aisec.cpg.passes.concepts.memory.cxx

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.memory.Memory
import de.fraunhofer.aisec.cpg.graph.concepts.memory.MemoryManagementMode
import de.fraunhofer.aisec.cpg.graph.concepts.memory.newAllocate
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.newBinaryOperator
import de.fraunhofer.aisec.cpg.passes.Description
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptPass

/**
 * Recognises C/C++ standard-library allocator calls (`malloc`, `calloc`, `realloc`) and attaches an
 * [Allocate] memory operation to each, populated with the size expression and (when derivable) the
 * variable being allocated.
 *
 * What this means for downstream analyses: instead of every consumer having to special-case
 * allocator function names, they query `call.overlays.filterIsInstance<Allocate>()` and read
 * `.size` / `.what` — see e.g. `ArrayValue.getSize`.
 *
 * Not yet covered: `new` / `new[]` (different AST shape in CDT) and `aligned_alloc` /
 * `posix_memalign` (size lives in different argument positions). Adding them is a one-line
 * extension to the when-branch below plus the size-extraction helper.
 */
@Description(
    "Recognises C/C++ allocator calls (malloc, calloc, realloc) and attaches Allocate concepts."
)
class CXXMemoryAllocationPass(ctx: TranslationContext) : ConceptPass(ctx) {

    override fun handleNode(node: Node, tu: TranslationUnit) {
        if (node is Call) handleCall(node, tu)
    }

    private fun handleCall(call: Call, tu: TranslationUnit) {
        val size = allocationSizeOf(call) ?: return
        val concept =
            tu.getConceptOrCreate<Memory> { Memory(mode = MemoryManagementMode.UNMANAGED) }
        newAllocate(
            underlyingNode = call,
            concept = concept,
            what = targetVariable(call),
            size = size,
            connect = true,
        )
    }

    /**
     * The expression that determines how many bytes the call allocates, or `null` if the call isn't
     * a recognised allocator. For `calloc(M, N)` the total is the product `M * N`, so we synthesise
     * a [BinaryOperator] (`*`) over the two arguments — `ValueEvaluator` will constant-fold it when
     * both operands are literals.
     */
    private fun allocationSizeOf(call: Call): Expression? =
        when (call.name.toString()) {
            "malloc" -> call.arguments.getOrNull(0)
            "calloc" -> {
                val count = call.arguments.getOrNull(0)
                val elemSize = call.arguments.getOrNull(1)
                if (count != null && elemSize != null) {
                    call.newBinaryOperator("*").apply {
                        lhs = count
                        rhs = elemSize
                    }
                } else null
            }
            "realloc" -> call.arguments.getOrNull(1)
            else -> null
        }

    /**
     * The [Variable] whose memory is being allocated, if obvious from the call's context: the LHS
     * of `Variable v = malloc(...)` (Variable's initializer holds the Call) or `v = malloc(...)`
     * (the Call is the RHS of an Assign whose LHS resolves to a Variable). Returns `null` when the
     * call's result isn't directly bound to a variable; downstream analyses can still find the
     * buffer via DFG without this hint.
     */
    private fun targetVariable(call: Call): Node? {
        val parent = call.astParent
        return when {
            parent is Variable && parent.initializer === call -> parent
            parent is Assign && parent.rhs.singleOrNull() === call ->
                (parent.lhs.singleOrNull() as? Reference)?.refersTo
            else -> null
        }
    }
}
