/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.HasShortCircuitOperators
import de.fraunhofer.aisec.cpg.graph.BranchingNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.cyclomaticComplexity
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.statements.DoStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.LoopStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ComprehensionExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConditionalExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ShortCircuitOperator
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.helpers.functional.MapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass.BasicBlock
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import java.util.IdentityHashMap
import java.util.Objects
import kotlin.collections.component1
import kotlin.collections.component2

/** This pass builds the Control Dependence Graph (CDG) by iterating through the EOG. */
@DependsOn(EvaluationOrderGraphPass::class)
open class ControlDependenceGraphPass(ctx: TranslationContext) : EOGStarterPass(ctx) {

    class Configuration(
        /**
         * This specifies the maximum complexity (as calculated per
         * [de.fraunhofer.aisec.cpg.graph.statements.Statement.cyclomaticComplexity]) a
         * [FunctionDeclaration] must have in order to be considered.
         */
        var maxComplexity: Int? = null
    ) : PassConfiguration()

    override fun cleanup() {
        // Nothing to do
    }

    class BasicBlock(
        val ingoingEOGEdges: MutableSet<EvaluationOrder> = mutableSetOf(),
        val outgoingEOGEdges: MutableSet<EvaluationOrder> = mutableSetOf(),
        val nodes: MutableList<Node> = mutableListOf<Node>(),
        var startNode: Node,
    ) : Node() {
        val endNode: Node?
            get() = nodes.lastOrNull()

        val branchingNode: Node?
            get() =
                if (
                    endNode is BranchingNode ||
                        endNode is LoopStatement ||
                        endNode is ComprehensionExpression
                ) {
                    endNode as Node
                } else null

        override var location: PhysicalLocation? = null
            get() {
                return PhysicalLocation(
                    uri = startNode.location?.artifactLocation?.uri ?: URI(""),
                    region =
                        Region(
                            startLine =
                                nodes.mapNotNull { it.location?.region?.startLine }.minOrNull()
                                    ?: -1,
                            startColumn =
                                nodes.mapNotNull { it.location?.region?.startColumn }.minOrNull()
                                    ?: -1,
                            endLine =
                                nodes.mapNotNull { it.location?.region?.endLine }.maxOrNull() ?: -1,
                            endColumn =
                                nodes.mapNotNull { it.location?.region?.endColumn }.maxOrNull()
                                    ?: -1,
                        ),
                )
            }

        override fun hashCode(): Int {
            return Objects.hash(super.hashCode(), nodes)
        }

        override fun equals(other: Any?): Boolean {
            return other is BasicBlock &&
                super.equals(other) &&
                this.nodes == other.nodes &&
                this.startNode == other.startNode &&
                this.endNode == other.endNode
        }

        override fun toString(): String {
            return "$startNode - $endNode"
        }
    }

    fun collectBasicBlocks(
        startNode: Node
    ): Triple<BasicBlock, Collection<BasicBlock>, Map<Node, BasicBlock>> {
        val allBasicBlocks = mutableSetOf<BasicBlock>()
        val firstBB = BasicBlock(startNode = startNode)
        allBasicBlocks.add(firstBB)
        val worklist =
            mutableListOf<Triple<Node, EvaluationOrder?, BasicBlock>>(
                Triple(startNode, null, firstBB)
            )
        val alreadySeen = IdentityHashMap<Node, BasicBlock>()

        while (worklist.isNotEmpty()) {
            var (currentStartNode, reachingEOGEdge, basicBlock) = worklist.removeFirst()
            // If we have already seen this node, we can skip it.
            if (currentStartNode in alreadySeen) {
                val oldBB = alreadySeen[currentStartNode]
                // There must be some sort of merge point to arrive here twice, so we add the
                // reaching basic block to the BB this node belongs to.
                oldBB?.prevEOG += basicBlock
                continue
            }

            if (currentStartNode.prevEOG.size > 1 && currentStartNode != basicBlock.startNode) {
                // If the currentStartNode is reachable from multiple paths, it starts a new basic
                // block. currentStartNode is part of the new basic block, so we add it after this
                // if statement.
                // Set the end node of the old basic block to the last node on the path
                basicBlock =
                    BasicBlock(startNode = currentStartNode).apply {
                        ingoingEOGEdges.addAll(currentStartNode.prevEOGEdges)
                        // Save the relationships between the two basic blocks.
                        prevEOGEdges.add(basicBlock) {
                            this.branch = reachingEOGEdge?.branch
                            this.unreachable = reachingEOGEdge?.unreachable ?: false
                        }
                    }

                // Add the newly created basic block to the allBasicBlocks set
                allBasicBlocks.add(basicBlock)
            }
            // Add the basic block to the already seen map for this node
            alreadySeen[currentStartNode] = basicBlock

            basicBlock.nodes.add(currentStartNode)

            val shortCircuit = currentStartNode.astParent as? ShortCircuitOperator
            val nextRelevantEOGEdges =
                if (currentStartNode.nextEOGEdges.size > 1 && shortCircuit != null) {
                    // For ShortCircuitOperators, we select only the branch which is not a shortcut
                    // because it's not really a CDG-relevant node, and we want to save the branches
                    // it introduces.
                    currentStartNode.nextEOGEdges.filter {
                        (shortCircuit.language as? HasShortCircuitOperators)?.let {
                            shortCircuit.operatorCode in it.conjunctiveOperators
                        } == it.branch || it.branch == null
                    }
                } else {
                    currentStartNode.nextEOGEdges
                }

            if (nextRelevantEOGEdges.size > 1) {
                // If the currentStartNode splits up into multiple paths, the next nodes start a new
                // basic block. We already generate this here. But currentStartNode is still part of
                // the current basic block, so we add it before this if statement.
                basicBlock.outgoingEOGEdges.addAll(nextRelevantEOGEdges)
                worklist.addAll(
                    nextRelevantEOGEdges.mapNotNull {
                        if (it.end in alreadySeen) {
                            alreadySeen[it.end]?.prevEOGEdges?.add(basicBlock) {
                                this.branch = it.branch
                                this.unreachable = it.unreachable
                            }
                            null
                        } else {
                            Triple(
                                it.end,
                                it,
                                BasicBlock(startNode = it.end).apply {
                                    ingoingEOGEdges.add(it)
                                    // Save the relationships between the two basic blocks.
                                    prevEOGEdges.add(basicBlock) {
                                        this.branch = it.branch
                                        this.unreachable = it.unreachable
                                    }
                                    // Add the newly created basic block to the allBasicBlocks set
                                    allBasicBlocks.add(this)
                                },
                            )
                        }
                    }
                )
            } else if (nextRelevantEOGEdges.size == 1) {
                // If there's max. 1 incoming and max. 1 outgoing path, we can add the
                // currentStartNode to the current basic block.
                // If the currentStartNode has only one outgoing path, we can continue with this
                // path.
                val nextEdge = nextRelevantEOGEdges.single()
                worklist.add(Triple(nextEdge.end, nextEdge, basicBlock))
            } else {
                // List is empty, nothing to do.
            }
        }
        return Triple(firstBB, allBasicBlocks, alreadySeen)
    }

    /**
     * Computes the CDG for the given [startNode]. It performs the following steps:
     * 1) Compute the "parent branching node" for each node and through which path the node is
     *    reached
     * 2) Find out which branch of a [BranchingNode] is actually conditional. The other ones aren't.
     * 3) For each node: 3.a) Check if the node is reachable through an unconditional path of its
     *    parent [BranchingNode] or through all the conditional paths. 3.b) Move the node "one layer
     *    up" by finding the parent node of the current [BranchingNode] and changing it to this
     *    parent node and the path(s) through which the [BranchingNode] node is reachable. 3.c)
     *    Repeat step 3) until you cannot move the node upwards in the CDG anymore.
     */
    override fun accept(startNode: Node) {
        // For now, we only execute this for function declarations, we will support all EOG starters
        // in the future.
        if (startNode !is FunctionDeclaration) {
            return
        }
        val max = passConfig<Configuration>()?.maxComplexity
        val c = startNode.body?.cyclomaticComplexity ?: 0
        if (max != null && c > max) {
            log.info(
                "Ignoring function ${startNode.name} because its complexity (${c}) is greater than the configured maximum (${max})"
            )
            return
        }

        log.trace("Creating CDG for {} with complexity {}", startNode.name, c)

        val (firstBasicBlock, basicBlocks, nodeToBBMap) = collectBasicBlocks(startNode)

        log.trace("Retrieved network of BBs for {}", startNode.name)

        val prevEOGState =
            PrevEOGState(innerLattice = PrevEOGLattice(innerLattice = PowersetLattice()))

        // Maps nodes to their "cdg parent" (i.e. the dominator) and also has the information
        // through which path it is reached. If all outgoing paths of the basicBlock's dominator
        // result in the basicBlock, we use the dominator's state instead (i.e., we move the
        // basicBlock one layer upwards)
        var startState: PrevEOGStateElement = prevEOGState.bottom
        startState =
            prevEOGState.push(
                startState,
                firstBasicBlock,
                PrevEOGLatticeElement(startNode to PowersetLattice.Element(firstBasicBlock)),
                true,
            )
        log.trace("Iterating EOG of {}", firstBasicBlock)
        val finalState =
            prevEOGState.iterateEOG(firstBasicBlock.nextEOGEdges, startState, ::transfer)
        log.trace("Done iterating EOG for {}. Generating the edges now.", startNode.name)

        // branchingNodeConditionals is a map organized as follows:
        //   BranchingNode -> Set of BasicBlocks where, if we visited all of these, the
        //      branchingNode does not dominate us anymore (we are after the merge point).
        val branchingNodeConditionals =
            getBranchingNodeConditions(startNode, basicBlocks, nodeToBBMap)

        // final state is a map organized as follows:
        //   BasicBlock -> Map<Node, Set<BasicBlock>> with
        //    branchingNode -> Set of BasicBlocks taken right after the branchingNode.

        // Collect the information, identify merge points, etc. This is not really efficient yet :(
        for ((basicBlock, dominatorPaths) in finalState) {
            var finalDominators =
                dominatorPaths.entries.map { (k, v) -> Pair(k, v.toMutableSet()) }.toMutableList()

            // Remove all entries where the basicBlock is reachable through all branches of a
            // branchingNode.
            finalDominators.removeIf {
                branchingNodeConditionals[it.first]?.let { elements ->
                    it.second.containsAll(elements)
                } == true
            }
            // Remove all entries where the basicBlock is reachable through its own branchingNode.
            // This indicates a loop, and this part seems to be in the unconditional part executed
            // before the loop starts (e.g., this affects all nodes in the condition)
            finalDominators.removeIf { basicBlock.branchingNode == it.first }
            // Try to remove transitive relationships, i.e., if a basicBlock is in our dominators
            // but also dominates one of our (remaining) dominators, we remove it.
            val transitiveDominators =
                finalDominators
                    .mapNotNull {
                        // Get the dominator of this dominator
                        val transitiveBB = nodeToBBMap[it.first]
                        transitiveBB
                            ?.let { finalState[it] }
                            ?.entries
                            ?.mapNotNull { (k, v) ->
                                if (k != transitiveBB.branchingNode) k to v else null
                            }
                    }
                    .flatten()
            finalDominators = finalDominators.minus(transitiveDominators).toMutableList()

            // After deleting a bunch of stuff, we have two options: 1) there are no dominators
            // left, and we assign the function declaration, or 2) there is one or multiple
            // dominators left.
            if (finalDominators.isEmpty()) {
                basicBlock.nodes.forEach { it.prevCDG += startNode }
            } else {
                // We have one or multiple dominators left.
                finalDominators.forEach { (finalDominator, reachingBB) ->
                    // Which branches are relevant for the CDG edge? We compute this by checking
                    // which branch properties are set between the dominator and reachingBB.
                    val branchesSet =
                        finalDominator.nextEOGEdges
                            .filter { edge -> edge.end in reachingBB.flatMap { it.nodes } }
                            .mapNotNull { it.branch }
                            .toSet()

                    basicBlock.nodes.forEach { node ->
                        node.prevCDGEdges.add(finalDominator) {
                            branches =
                                when {
                                    branchesSet.isNotEmpty() -> {
                                        branchesSet
                                    }

                                    finalDominator is IfStatement &&
                                        (branchingNodeConditionals[finalDominator]?.size ?: 0) >
                                            1 -> { // Note: branchesSet must be empty here The if
                                        // statement has only a then branch but there's a way
                                        // to "jump out" of this branch. In this case, we
                                        // want to set the false property here.
                                        setOf(false)
                                    }

                                    else -> setOf()
                                }
                        }
                    }
                }
            }
        }
    }

    /*
     * For a branching node, we identify which path(s) have to be found to be in a "merging point".
     * There are two options:
     *   1) There's a path which is executed independent of the branch (e.g. this is the case for an if-statement without an else-branch).
     *   2) A node can be reached from all conditional branches.
     *
     * This method collects the merging points. It also includes the function declaration itself.
     */
    private fun getBranchingNodeConditions(
        functionDeclaration: FunctionDeclaration,
        allBasicBlocks: Collection<BasicBlock>,
        nodeToBBMap: Map<Node, BasicBlock>,
    ) =
        mapOf(
            // For the function declaration, there's only the path through the function declaration
            // itself.
            Pair(functionDeclaration, setOfNotNull(nodeToBBMap[functionDeclaration])),
            *allBasicBlocks
                .mapNotNull {
                    it.branchingNode?.let { branchingNode ->
                        val mergingPoints =
                            if (branchingNode.nextEOGEdges.any { !it.isConditionalBranch() }) {
                                // There's an unconditional path (case 1), so when reaching this
                                // branch, we're done. Collect all (=1) unconditional branches.
                                branchingNode.nextEOGEdges
                                    .filter { !it.isConditionalBranch() }
                                    .map { it.end }
                                    .toSet()
                            } else {
                                // All branches are executed based on some condition (case 2), so we
                                // collect all these branches.
                                branchingNode.nextEOGEdges.map { it.end }.toSet()
                            }
                        branchingNode to mergingPoints.mapNotNull { nodeToBBMap[it] }
                    }
                }
                .toTypedArray(),
        )
}

/**
 * This method is executed for each EOG edge which is in the worklist. [currentEdge] is the edge to
 * process, [currentState] contains the state which was observed before arriving here.
 *
 * This method modifies the state for the next eog edge as follows:
 * - If [currentEdge] starts in a [BranchingNode], the end node depends on the start node. We modify
 *   the state to express that "the end node depends on the start node and is reachable through the
 *   path starting at the end node".
 * - For all other starting nodes, we copy the state of the start node to the end node.
 *
 * Returns the updated state and true because we always expect an update of the state.
 */
fun transfer(
    lattice: Lattice<PrevEOGStateElement>,
    currentEdge: EvaluationOrder,
    currentState: PrevEOGStateElement,
): PrevEOGStateElement {
    val lattice = lattice as? PrevEOGState ?: return currentState
    var newState = currentState

    val currentStart =
        currentEdge.start as? BasicBlock
            ?: throw IllegalArgumentException(
                "Current edge start must be a BasicBlock, but was ${currentEdge.start}"
            )
    val currentEnd =
        currentEdge.end as? BasicBlock
            ?: throw IllegalArgumentException(
                "Current edge end must be a BasicBlock, but was ${currentEdge.end}"
            )

    // Check if we start in a branching node and if this edge leads to the conditional
    // branch. In this case, the next node will move "one layer downwards" in the CDG.
    val branchingNode = currentStart.branchingNode
    if (branchingNode != null) {
        // We start in a branching node and end in one of the branches, so we have thes
        // following state:
        // for the branching node "start", we have a path through "end".
        val prevPathLattice =
            newState[currentStart]
                ?.filter { (k, _) -> k != branchingNode }
                ?.let { PrevEOGLatticeElement(it) } ?: PrevEOGLatticeElement()

        val map = PrevEOGLatticeElement(branchingNode to PowersetLattice.Element(currentEnd))

        val newPath = lattice.innerLattice.lub(map, prevPathLattice, true)
        newState = lattice.push(newState, currentEnd, newPath, true)
    } else {
        // We did not start in a branching node, so for the next node, we have the same path
        // (last branching + first end node) as for the start node of this edge.
        // If there is no state for the start node (most likely, this is the case for the
        // first edge in a function), we generate a new state where we start in "start" end
        // have "end" as the first node in the "branch".
        val state =
            newState[currentStart]?.let { PrevEOGLatticeElement(it) }
                ?: PrevEOGLatticeElement(currentStart to PowersetLattice.Element(currentEnd))
        newState = lattice.push(newState, currentEnd, state, true)
    }
    return newState
}

/**
 * For all types I've seen so far, the "true" branch is executed conditionally.
 *
 * For if-statements, the BRANCH property is set to "false" for the "else" branch (which is also
 * executed conditionally) and is not set in the code after an if-statement if there's no else
 * branch (which is also always executed). For all other nodes, the "false" branch is the code after
 * the loop or so (i.e., the unconditionally executed path).
 *
 * Note: This method does not account for return statements in the conditional part or endless loops
 * where the other branch is actually also conditionally executed (or not). It should be easy to
 * change this if we do not want this behavior (just remove the condition on the start node of the
 * "false" branch).
 */
private fun EvaluationOrder.isConditionalBranch(): Boolean {
    val startNode = (this.start as? ControlDependenceGraphPass.BasicBlock)?.endNode ?: this.start
    return if (branch == true) {
        true
    } else
        (startNode is IfStatement ||
            startNode is DoStatement ||
            startNode is ComprehensionExpression ||
            (startNode.astParent is ComprehensionExpression &&
                startNode == (startNode.astParent as ComprehensionExpression).iterable) ||
            startNode is ConditionalExpression ||
            startNode is ShortCircuitOperator) && branch == false ||
            (startNode is IfStatement &&
                !startNode.allBranchesFromMyThenBranchGoThrough(startNode.nextUnconditionalNode))
}

private val IfStatement.nextUnconditionalNode: Node?
    get() = this.nextEOGEdges.firstOrNull { it.branch == null }?.end

private fun IfStatement.allBranchesFromMyThenBranchGoThrough(node: Node?): Boolean {
    if (this.thenStatement.allChildren<ReturnStatement>().isNotEmpty()) return false

    if (node == null) return true

    val alreadySeen = mutableSetOf<Node>()
    val nextNodes = this.nextEOGEdges.filter { it.branch == true }.map { it.end }.toMutableList()

    while (nextNodes.isNotEmpty()) {
        val nextNode = nextNodes.removeFirst()
        if (nextNode == node) {
            continue
        } else if (nextNode.nextEOG.isEmpty()) {
            // We're at the end of the EOG but didn't see "node" on this path. Fail
            return false
        }
        alreadySeen.add(nextNode)
        nextNodes.addAll(nextNode.nextEOG.filter { it !in alreadySeen })
    }

    return true
}

typealias PrevEOGLatticeElement = MapLattice.Element<Node, PowersetLattice.Element<BasicBlock>>

typealias PrevEOGLattice = MapLattice<Node, PowersetLattice.Element<BasicBlock>>

typealias PrevEOGStateElement = MapLattice.Element<BasicBlock, PrevEOGLatticeElement>

typealias PrevEOGState = MapLattice<BasicBlock, PrevEOGLatticeElement>

fun PrevEOGState.push(
    currentElement: PrevEOGStateElement,
    newNode: BasicBlock,
    newEOGLattice: PrevEOGLatticeElement,
    allowModify: Boolean,
): PrevEOGStateElement {
    return this.lub(currentElement, PrevEOGStateElement(newNode to newEOGLattice), allowModify)
}
