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
package de.fraunhofer.aisec.cpg.passes;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import java.util.*;

/**
 * This pass tracks VariableDeclarations and values that are included in the graph by the DFG edge.
 * For this pass we traverse the EOG as it represents our understanding of the execution order and
 * can be used to remove the DFG edges that are not feasible.
 *
 * <p>Control Flow Sensitivity in the DFG is only performed on VariableDeclarations and not on
 * FieldDeclarations. The reason for this being the fact, that the value of a field might be
 * modified to a value that is not present in the method, thus it is not detected by our variable
 * tracking
 *
 * <p>This pass will split up at every branch in the EOG. Because of the existence of loops and
 * multiple paths being able to run to through the same Declared reference expression we have to
 * keep track of the set of values (assignments) associated to a variable at JoinPoints. If the set
 * reaching a Joinpoint is not adding new values to one variable the path does not have to be
 * further explored. This ensures that the algorithm terminates and scales with the number of
 * different paths in the program finally reaching a fixpoint.
 *
 * <p>This is only feasible because the values associate to a variable at fix-points is the location
 * assignment and not its symbolically executed value, in which case we could not ensure termination
 * for the algorithm.
 *
 * <p>We here do not solve the problem of Exception-Handling, for this we will need additional
 * semantics on Edges.
 */
public class ControlFlowSensitiveDFGPass extends Pass {

  @Override
  public void cleanup() {
    // Nothing to cleanup
  }

  @Override
  public void accept(TranslationResult translationResult) {
    SubgraphWalker.IterativeGraphWalker walker = new SubgraphWalker.IterativeGraphWalker();
    walker.registerOnNodeVisit(this::handle);
    for (TranslationUnitDeclaration tu : translationResult.getTranslationUnits()) {
      walker.iterate(tu);
    }
  }

  /**
   * Removes unrefined DFG edges
   *
   * @param fixDFGs ControlFlowSensitiveDFG of entire Method
   */
  protected void removeValues(ControlFlowSensitiveDFGPass.FunctionLevelFixpointIterator fixDFGs) {
    for (Node currNode : fixDFGs.getRemoves().keySet()) {
      for (Node prev : fixDFGs.getRemoves().get(currNode)) {
        currNode.removePrevDFG(prev);
      }
    }
  }

  /**
   * ControlFlowSensitiveDFG Pass is performed on every method.
   *
   * @param node every node in the TranslationResult
   */
  protected void handle(Node node) {
    if (node instanceof FunctionDeclaration || node instanceof StatementHolder) {
      ControlFlowSensitiveDFGPass.FunctionLevelFixpointIterator flfIterator =
          new ControlFlowSensitiveDFGPass.FunctionLevelFixpointIterator();
      flfIterator.handle(node);
      removeValues(flfIterator);
    }
  }

  protected interface IterationFunction {
    Map<VariableDeclaration, Set<Node>> iterate(
        Node node, Map<VariableDeclaration, Set<Node>> variables, Node endNode, boolean stopBefore);
  }

  protected class FunctionLevelFixpointIterator {

    /**
     * A Node with refined DFG edges (key) is mapped to a set of nodes that were the previous
     * (unrefined) DFG edges and need to be removed later on
     */
    protected Map<Node, Set<Node>> removes = new HashMap<>();

    protected final Map<Node, Map<VariableDeclaration, Set<Node>>> joinPoints = new HashMap<>();

    public void handle(Node functionRoot) {
      iterateTillFixpoint(functionRoot, new HashMap<>(), null, false);
      this.removes =
          new HashMap<>(); // Resetting removes, computing removes is not necessary in the
      // previous step, this can be removed
      propagateValues();
    }

    public Map<Node, Set<Node>> getRemoves() {
      return removes;
    }

    protected void addToRemoves(Node curr, Node prev) {
      if (!this.removes.containsKey(curr)) {
        this.removes.put(curr, new HashSet<>());
      }

      this.removes.get(curr).add(prev);
    }

    /**
     * Traverses the EOG starting at a node until there is no more outgoing EOGs to new nodes.
     *
     * @param node starting node
     * @return set containing all nodes that have been reached
     */
    protected Set<Node> eogTraversal(Node node) {
      Set<Node> eogReachableNodes = new HashSet<>();
      Set<Node> checkReachable = new HashSet<>();
      checkReachable.add(node);

      while (!checkReachable.isEmpty()) {
        Node n = checkReachable.iterator().next();
        checkReachable.addAll(n.getNextEOG());
        eogReachableNodes.add(n);
        checkReachable.removeAll(eogReachableNodes);
      }

      return eogReachableNodes;
    }

    /**
     * Stores the prevDFG to a VariableDeclaration of a node in the removes map and adds the values
     * of the VariableDeclaration as prevDFGs to the node
     *
     * @param currNode node that is analyzed
     */
    protected void setIngoingDFG(Node currNode, Map<VariableDeclaration, Set<Node>> variables) {
      Set<Node> prevDFGs = new HashSet<>(currNode.getPrevDFG());
      for (Node prev : prevDFGs) {
        if (prev instanceof VariableDeclaration && variables.containsKey(prev)) {
          for (Node target : variables.get(prev)) {
            currNode.addPrevDFG(target);
          }
          addToRemoves(currNode, prev);
        }
      }
    }

    /**
     * If a Node has a DFG to a VariableDeclaration we need to remove the nextDFG and store the
     * value of the node in our tracking map
     *
     * @param currNode Node that is being analyzed
     */
    protected void registerOutgoingDFG(
        Node currNode, Map<VariableDeclaration, Set<Node>> variables) {
      Set<Node> nextDFG = new HashSet<>(currNode.getNextDFG());
      for (Node next : nextDFG) {
        if (next instanceof VariableDeclaration && variables.containsKey(next)) {
          Set<Node> values = new HashSet<>(currNode.getPrevDFG());
          variables.replace((VariableDeclaration) next, values);
        }
      }
    }

    /**
     * Get node of the BinaryOperator where the assignment is finished (last node of the Assignment)
     *
     * @param node start node of the assignment LHS DeclaredReferenceExpression
     * @return return the last (in eog order) node of the assignment
     */
    protected Node obtainAssignmentNode(Node node) {
      Set<Node> nextEOG = new HashSet<>(node.getNextEOG());
      Set<Node> reachableEOGs = new HashSet<>();
      for (Node next : nextEOG) {
        reachableEOGs.addAll(eogTraversal(next));
      }

      return reachableEOGs.stream()
          .filter(
              n ->
                  n instanceof BinaryOperator
                      && Objects.equals(((BinaryOperator) n).getLhs(), node))
          .findAny()
          .orElse(null);
    }

    /**
     * Perform the actual modification of the DFG edges based on the values that are recorded in the
     * variables map for every VariableDeclaration
     *
     * @param currNode node whose dfg edges have to be replaced
     */
    protected void modifyDFGEdges(Node currNode, Map<VariableDeclaration, Set<Node>> variables) {
      // A DeclaredReferenceExpression makes use of one of the VariableDeclaration we are
      // tracking. Therefore, we must modify the outgoing and ingoing DFG edges
      // Check for outgoing DFG edges
      registerOutgoingDFG(currNode, variables);

      // Check for ingoing DFG edges
      setIngoingDFG(currNode, variables);
    }

    /**
     * This function collects the set of variable definitions valid for the VariableDeclarations
     * defined in the program when reaching a join-point, a node reached by more than one incoming
     * EOG-Edge. The state is computed whenever a write access to a variable is encountered. A node
     * may be passed by multiple paths and therefore the states have to be merged at these
     * join-points. However, the number of paths is finite and scales well enough to make a fixpoint
     * iteration of states at join-points is therefore terminating and feasible.
     *
     * <p>This function iterates over the entire EOG starting at a fixed node. If the execution of
     * this function is started with the function-Node which also represents the EOG-Root-Node all
     * Nodes that are part of a valid execution path will be traversed.
     *
     * @param node - Start of the fixpoint iteration
     * @param variables - The state, composed of a mapping from variables to expression that were
     *     prior written to it.
     * @param endNode - A node where the iteration shall stop, if null the iteration stops at a
     *     point once a fix-point no outgoing eog edge is reached
     * @param stopBefore - denotes whether the iteration shall stop before or after processing the
     *     reached node.
     * @return The state after reaching on of the terminating conditions
     */
    protected Map<VariableDeclaration, Set<Node>> iterateTillFixpoint(
        Node node,
        Map<VariableDeclaration, Set<Node>> variables,
        Node endNode,
        boolean stopBefore) {
      if (node == null) {
        return variables;
      }
      do {
        if (node.getPrevEOG().size() != 1) {
          // We only want to keep track of variables where they can change due to multiple incoming
          // EOG-Edges or at the root of a EOG-path
          if (!joinPoints.containsKey(node)) {
            joinPoints.put(node, createShallowCopy(variables));

          } else {
            Map<VariableDeclaration, Set<Node>> currentJoinpoint = joinPoints.get(node);
            if (!mergeStates(currentJoinpoint, variables)) {
              return currentJoinpoint; // Stop when we get to a joinpoint that does not get a
              // broader state through this path this ensures termination
            }
            // Progress execution with the updated JoinPoint set
            variables = createShallowCopy(currentJoinpoint);
          }
        }
        if (node.equals(endNode) && stopBefore) {
          return variables;
        }

        if (node instanceof DeclaredReferenceExpression) {
          node =
              handleDeclaredReferenceExpression(
                  (DeclaredReferenceExpression) node, variables, this::iterateTillFixpoint);
        }

        if (node.equals(endNode) && !stopBefore) {
          return variables;
        }

        // We use recursion when a eog path splits, if we can find a non-recursive variation of this
        // algorithm it may avoid some problems with scaling
        if (node.getNextEOG().size() > 1) {
          Map<VariableDeclaration, Set<Node>> updatedVariables = new HashMap<>();
          for (Node next : node.getNextEOG()) {
            if (next instanceof VariableDeclaration) {
              addDFGToMap((VariableDeclaration) next, node, variables);
            }
            mergeStates(
                updatedVariables,
                iterateTillFixpoint(next, createShallowCopy(variables), endNode, stopBefore));
          }
          return updatedVariables;
        } else if (node.getNextEOG().isEmpty()) {
          return variables;
        } else {
          Node next = node.getNextEOG().get(0);
          if (next instanceof VariableDeclaration) {
            addDFGToMap((VariableDeclaration) next, node, variables);
          }
          node = next;
        }
      } while (node != null);
      return variables;
    }

    /**
     * Method that handles DeclaredReferenceExpressions when the EOG is traversed
     *
     * @param currNode DeclaredReferenceExpression that is found in
     * @return Node where the EOG traversal should continue
     */
    protected Node handleDeclaredReferenceExpression(
        DeclaredReferenceExpression currNode,
        Map<VariableDeclaration, Set<Node>> variables,
        IterationFunction iterationFunction) {
      if (currNode.getAccess().equals(AccessValues.WRITE)) {
        // This is an assignment -> DeclaredReferenceExpression + Write Access
        Node binaryOperator =
            obtainAssignmentNode(
                currNode); // Search for = BinaryOperator as it marks the end of the assignment

        if (binaryOperator != null) {
          Node nextEOG =
              currNode.getNextEOG().get(0); // Only one outgoing eog edge from an assignment
          // DeclaredReferenceExpression
          Map<VariableDeclaration, Set<Node>> updatedVariables =
              iterationFunction.iterate(
                  nextEOG, createShallowCopy(variables), binaryOperator, false);

          // necessary to return the updated variables state to the calling function as the return
          // value returns the next node
          variables.clear();
          variables.putAll(updatedVariables);

          // Perform Delayed DFG modifications (after having processed the entire assignment)
          modifyDFGEdges(currNode, variables);

          // Update values of DFG Pass until the end of the assignment
          return binaryOperator;
        }
      }
      // Other DeclaredReferenceExpression that do not have a write assignment we do not have to
      // delay the replacement of the value in the VariableDeclaration
      modifyDFGEdges(currNode, variables);
      return currNode; // It is necessary for it to return the already processed node
    }

    /**
     * Iterates over all join-points and propagates the state that is valid for the sum of incoming
     * eog-Paths to refine the dfg edges at variable usage points.
     */
    protected void propagateValues() {
      for (Map.Entry<Node, Map<VariableDeclaration, Set<Node>>> joinPoint :
          this.joinPoints.entrySet()) {
        propagateFromJoinPoints(joinPoint.getKey(), joinPoint.getValue(), null, true);
      }
    }

    /**
     * Propagates the state from the join-point until the next join-point is reached.
     *
     * @param node - Start of the propagation
     * @param variables - The state, composed of a mapping from variables to expression that were
     *     prior written to it collected at some join-point.
     * @param endNode - A node where the iteration shall stop, if null the iteration stops at a
     *     point once a fix-point no outgoing eog edge is reached
     * @param stopBefore - denotes whether the iteration shall stop before or after processing the
     *     reached node.
     * @return The state after reaching on of the terminating conditions
     */
    protected Map<VariableDeclaration, Set<Node>> propagateFromJoinPoints(
        Node node,
        Map<VariableDeclaration, Set<Node>> variables,
        Node endNode,
        boolean stopBefore) {
      if (node == null) {
        return variables;
      }
      do {
        if (node.equals(endNode) && stopBefore) {
          return variables;
        }

        if (node instanceof DeclaredReferenceExpression) {
          node =
              handleDeclaredReferenceExpression(
                  (DeclaredReferenceExpression) node, variables, this::propagateFromJoinPoints);
        }

        if (node.equals(endNode) && !stopBefore) {
          return variables;
        }

        // We use recursion when a eog path splits, if we can find a non-recursive variation of this
        // algorithm it may avoid some problems with scaling
        if (node.getNextEOG().size() > 1) {
          Map<VariableDeclaration, Set<Node>> updatedVariables = new HashMap<>();
          for (Node next : node.getNextEOG()) {
            if (next instanceof VariableDeclaration) {
              addDFGToMap((VariableDeclaration) next, node, variables);
            }
            if (!joinPoints.containsKey(
                next)) { // As we are propagating from joinpoints we stop when we reach the next
              // joinpoint
              mergeStates(
                  updatedVariables,
                  iterateTillFixpoint(next, createShallowCopy(variables), endNode, stopBefore));
            }
          }
          return updatedVariables;
        } else if (node.getNextEOG().isEmpty()) {
          return variables;
        } else {
          Node next = node.getNextEOG().get(0);
          if (next instanceof VariableDeclaration) {
            addDFGToMap((VariableDeclaration) next, node, variables);
          }
          node = next;
          if (joinPoints.containsKey(node)) {
            break; // As we are propagating from joinpoints we stop when we reach the next joinpoint
          }
        }
      } while (!node.getNextEOG().isEmpty());
      return variables;
    }

    /**
     * Adds a DFG-Edge to the Mapping of variables. Updating the state.
     *
     * @param variableDeclaration - for which a new valid definition has to be added to the state
     * @param prev - the defining expression to the variable
     * @param variables - the state that is updated by the DFG edge
     */
    protected void addDFGToMap(
        VariableDeclaration variableDeclaration,
        Node prev,
        Map<VariableDeclaration, Set<Node>> variables) {
      Set<Node> prevDFGSet = variableDeclaration.getPrevDFG();
      if (prevDFGSet.contains(
          prev)) { // ensure that the prev EOG node is also one of its dfg predecessors
        if (variables.containsKey(variableDeclaration)) {
          variables.get(variableDeclaration).add(prev);
        } else {
          Set<Node> dfgSet = new HashSet<>();
          dfgSet.add(prev);
          variables.put(variableDeclaration, dfgSet);
        }
      }
    }

    /**
     * Merges two states assuming that both states come from valid paths. All the definition for
     * variables are collected into the current state represented by {@code currentJoinpoint}
     *
     * @param currentJoinpoint - The state we are merging into
     * @param variables - The state we are merging from
     * @return whether or not the merging resulted into an update to {@code currentJoinpoint}
     */
    protected boolean mergeStates(
        Map<VariableDeclaration, Set<Node>> currentJoinpoint,
        Map<VariableDeclaration, Set<Node>> variables) {
      boolean changed = false;
      // This merging may be more efficiently done with some native function
      for (Map.Entry<VariableDeclaration, Set<Node>> entry : variables.entrySet()) {
        Set<Node> newAssignments = entry.getValue();
        if (currentJoinpoint.containsKey(entry.getKey())) {
          Set<Node> existing = currentJoinpoint.get(entry.getKey());
          for (Node assignment : newAssignments) {
            if (!existing.contains(assignment)) {
              existing.add(assignment);
              changed = true;
            }
          }
        } else {
          currentJoinpoint.put(
              entry.getKey(),
              new LinkedHashSet<>(
                  entry
                      .getValue())); // We create a copy of the set to avoid changes when processing
          // other paths
          changed = true;
        }
      }
      return changed;
    }

    /**
     * Creates a shallow copy to the depth of the nodes. References to nodes are not copied as new
     * objects. Only the collections are created sd new Objects.
     *
     * @param state The state to copy
     * @return The copy
     */
    protected Map<VariableDeclaration, Set<Node>> createShallowCopy(
        Map<VariableDeclaration, Set<Node>> state) {
      Map<VariableDeclaration, Set<Node>> shallowCopy = new LinkedHashMap<>();
      for (Map.Entry<VariableDeclaration, Set<Node>> entry : state.entrySet()) {
        shallowCopy.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
      }
      return shallowCopy;
    }
  }
}
