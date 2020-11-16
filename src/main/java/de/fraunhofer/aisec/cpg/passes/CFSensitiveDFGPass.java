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
 * multiple paths being able to run to trough the same Declared reference expression we have to keep
 * track of the set of values (assignments) associated to a variable at JoinPoints. If the set
 * reaching a Joinpoint is not adding new values to one variable the path does not have to be
 * further explored. This ensures that the algorithm terminates and scales with the number of
 * different paths in the program finally reaching a fixpoint.
 *
 * <p>This is only feasible because the values associate to a variable at fix-points is the location
 * assignment and not its symbolically executed value, in which case we could not ensure termination
 * for the algorithm.
 *
 * <p>We here do not solve the problem of Exception-Handling, for this we will need additional
 * semantics on Edges. --------
 */
public class CFSensitiveDFGPass extends Pass {

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
  private void removeValues(CFSensitiveDFGPass.FunctionLevelFixpointIterator fixDFGs) {
    for (Node currNode : fixDFGs.getRemoves().keySet()) {
      for (Node prev : fixDFGs.getRemoves().get(currNode)) {
        currNode.removePrevDFG(prev);
      }
    }
  }

  /**
   * ControlFlowSensitiveDFG Pass is perfomed on every Method
   *
   * @param node every node in the TranslationResult
   */
  public void handle(Node node) {
    if (node instanceof FunctionDeclaration) {

      CFSensitiveDFGPass.FunctionLevelFixpointIterator flfIterator =
          new CFSensitiveDFGPass.FunctionLevelFixpointIterator(node);
      flfIterator.handle();
      removeValues(flfIterator);
    }
  }

  private class FunctionLevelFixpointIterator {

    private Node eogRootNode;

    // A Node with refined DFG edges (key) is mapped to a set of nodes that were the previous
    // (unrefined) DFG edges and need to be removed later on
    private Map<Node, Set<Node>> removes;

    private Map<Node, Map<VariableDeclaration, Set<Node>>> joinPoints = new HashMap<>();

    public FunctionLevelFixpointIterator(Node node) {
      this.eogRootNode = node;
      this.removes = new HashMap<>();
    }

    public void handle() {
      iterateTillFixpoint(this.eogRootNode, new HashMap<>(), null, false);
      this.removes = new HashMap<>(); // Reseting removes, computing removes is not necessary in the
      // previous step, this can be removed
      propagateValues();
    }

    public Map<Node, Set<Node>> getRemoves() {
      return removes;
    }

    private void addToRemoves(Node curr, Node prev) {
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
    private Set<Node> eogTraversal(Node node) {
      Set<Node> eogReachableNodes = new HashSet<>();
      Set<Node> checkRechable = new HashSet<>();
      checkRechable.add(node);

      while (!checkRechable.isEmpty()) {
        Node n = checkRechable.iterator().next();
        checkRechable.addAll(n.getNextEOG());
        eogReachableNodes.add(n);
        checkRechable.removeAll(eogReachableNodes);
      }

      return eogReachableNodes;
    }

    /**
     * Stores the prevDFG to a VariableDeclaration of a node in the removes map and adds the values
     * of the VariableDeclaration as prevDFGs to the node
     *
     * @param currNode node that is analyzed
     */
    private void setIngoingDFG(Node currNode, Map<VariableDeclaration, Set<Node>> variables) {
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
    private void registerOutgoingDFG(Node currNode, Map<VariableDeclaration, Set<Node>> variables) {
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
    private Node obtainAssignmentNode(Node node) {
      Set<Node> nextEOG = new HashSet<>(node.getNextEOG());
      Set<Node> rechableEOGs = new HashSet<>();
      for (Node next : nextEOG) {
        rechableEOGs.addAll(eogTraversal(next));
      }

      return rechableEOGs.stream()
          .filter(n -> n instanceof BinaryOperator && ((BinaryOperator) n).getLhs().equals(node))
          .findAny()
          .orElse(null);
    }

    /**
     * Perform the actual modification of the DFG edges based on the values that are recorded in the
     * variables map for every VariableDeclaration
     *
     * @param currNode node whose dfg edges have to be replaced
     */
    private void modifyDFGEdges(Node currNode, Map<VariableDeclaration, Set<Node>> variables) {
      // A DeclaredReferenceExpression makes use of one of the VariableDeclaration we are
      // tracking. Therefore we must modify the outgoing and ingoing DFG edges
      // Check for outgoing DFG edges
      registerOutgoingDFG(currNode, variables);

      // Check for ingoing DFG edges
      setIngoingDFG(currNode, variables);
    }

    /**
     * Merge the removes Map from the current object with another instance of ContolFlowSensitiveDFG
     *
     * @param newRemoves remove map of the other ControlFlowSensitiveDFG
     */
    private void mergeRemoves(Map<Node, Set<Node>> newRemoves) {
      for (Map.Entry<Node, Set<Node>> entry : newRemoves.entrySet()) {
        if (this.removes.containsKey(entry.getKey())) {
          this.removes.get(entry.getKey()).addAll(entry.getValue());
        } else {
          this.removes.put(entry.getKey(), entry.getValue());
        }
      }
    }

    public Map<VariableDeclaration, Set<Node>> iterateTillFixpoint(
        Node node,
        Map<VariableDeclaration, Set<Node>> variables,
        Node endNode,
        boolean stopBefore) {
      if (node == null) return variables;
      do {
        if (node.getPrevEOG().size() != 1) {
          // We only want to keep track of variables where they can change due to multiple incoming
          // EOG-Edges or at the root of a EOG-path
          if (!joinPoints.containsKey(node)) {
            joinPoints.put(node, createShallowCopy(variables));

          } else {
            Map currentJoinpoint = joinPoints.get(node);
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
          node = handleDeclaredReferenceExpression((DeclaredReferenceExpression) node, variables);
        }

        if (node.equals(endNode) && !stopBefore) {
          return variables;
        }

        // We use recursion when a eog path splits, if we can find a non-recursive variation of this
        // algorithm it may avoid some problems with scaling
        if (node.getNextEOG().size() > 1) {
          for (Node next : node.getNextEOG()) {
            if (next instanceof VariableDeclaration) {
              addDFGToMap((VariableDeclaration) next, node, variables);
            }
            iterateTillFixpoint(next, createShallowCopy(variables), endNode, stopBefore);
          }
          return variables;
        } else if (node.getNextEOG().size() == 0) {
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
    private Node handleDeclaredReferenceExpression(
        DeclaredReferenceExpression currNode, Map<VariableDeclaration, Set<Node>> variables) {
      if (currNode.getAccess().equals(AccessValues.WRITE)) {
        // This is an assignment -> DeclaredReferenceExpression + Write Access
        Node binaryOperator =
            obtainAssignmentNode(
                currNode); // Search for = BinaryOperator as it marks the end of the assignment

        Node nextEOG =
            currNode
                .getNextEOG()
                .get(
                    0); // Only one outgoing eog edge from an assignment DeclaredReferenceExpression
        iterateTillFixpoint(nextEOG, variables, binaryOperator, true);

        // Todo check if this causes problems when the lhs of an assignment is non trivial and also
        // contains state chainging effects else we have to change this check to if(parent instance
        // of Assignment-Statment)

        // Perform Delayed DFG modifications (after having processed the entire assignment)
        modifyDFGEdges(currNode, variables);

        // Update values of DFG Pass until the end of the assignment
        // this.variables = joinVariables(dfgs);
        // mergeRemoves(joinRemoves(dfgs));
        return binaryOperator
            .getPrevEOG()
            .get(0); // Still has to compute the joinPoints at the assignment, we take one of its
        // predecessors to ensure it running through the loop once
      } else {
        // Other DeclaredReferenceExpression that do not have a write assignment we do not have to
        // delay the replacement of the value in the VariableDeclaration
        modifyDFGEdges(currNode, variables);
        return currNode; // It is necessary for it to return the already processed node
      }
    }

    private Node probagateAtDeclaredReferenceExpression(
        DeclaredReferenceExpression currNode, Map<VariableDeclaration, Set<Node>> variables) {
      if (currNode.getAccess().equals(AccessValues.WRITE)) {
        // This is an assignment -> DeclaredReferenceExpression + Write Access
        Node binaryOperator =
            obtainAssignmentNode(
                currNode); // Search for = BinaryOperator as it marks the end of the assignment

        Node nextEOG =
            currNode
                .getNextEOG()
                .get(
                    0); // Only one outgoing eog edge from an assignment DeclaredReferenceExpression
        propagateFromJoinPoints(nextEOG, variables, binaryOperator, true);

        // Todo check if this causes problems when the lhs of an assignment is non trivial and also
        // contains state chainging effects else we have to change this check to if(parent instance
        // of Assignment-Statment)

        // Perform Delayed DFG modifications (after having processed the entire assignment)
        modifyDFGEdges(currNode, variables);

        return binaryOperator.getPrevEOG().get(0); // Continue the EOG traversal at the assignment
      } else {
        // Other DeclaredReferenceExpression that do not have a write assignment we do not have to
        // delay the replacement of the value in the VariableDeclaration
        modifyDFGEdges(currNode, variables);
        return currNode; // It is necessary for it to return the already processed node
      }
    }

    public void propagateValues() {
      for (Node joinPoint : this.joinPoints.keySet()) {
        propagateFromJoinPoints(joinPoint, this.joinPoints.get(joinPoint), null, true);
      }
    }

    public Map<VariableDeclaration, Set<Node>> propagateFromJoinPoints(
        Node node,
        Map<VariableDeclaration, Set<Node>> variables,
        Node endNode,
        boolean stopBefore) {
      if (node == null) return variables;
      do {
        if (node.equals(endNode) && stopBefore) {
          return variables;
        }

        if (node instanceof DeclaredReferenceExpression) {
          node =
              probagateAtDeclaredReferenceExpression((DeclaredReferenceExpression) node, variables);
        }

        if (node.equals(endNode) && !stopBefore) {
          return variables;
        }

        // We use recursion when a eog path splits, if we can find a non-recursive variation of this
        // algorithm it may avoid some problems with scaling
        if (node.getNextEOG().size() > 1) {
          for (Node next : node.getNextEOG()) {
            if (next instanceof VariableDeclaration) {
              addDFGToMap((VariableDeclaration) next, node, variables);
            }
            if (!joinPoints.containsKey(
                next)) { // As we are propagating from joinpoints we stop when we reach the next
              // joinpoint
              propagateFromJoinPoints(next, createShallowCopy(variables), endNode, stopBefore);
            }
          }
          return variables;
        } else if (node.getNextEOG().size() == 0) {
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

    private void addDFGToMap(
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

    public boolean mergeStates(
        Map<VariableDeclaration, Set<Node>> currentJoinpoint,
        Map<VariableDeclaration, Set<Node>> variables) {
      boolean changed = false;
      // This merging may be more efficiently done with some native function
      for (VariableDeclaration key : variables.keySet()) {
        Set<Node> newAssignments = variables.get(key);
        if (currentJoinpoint.containsKey(key)) {
          Set<Node> existing = currentJoinpoint.get(key);
          for (Node assignment : newAssignments) {
            if (!existing.contains(assignment)) {
              existing.add(assignment);
              changed = true;
            }
          }
        } else {
          currentJoinpoint.put(
              key,
              new LinkedHashSet<>(
                  variables.get(
                      key))); // We create a copy of the set to avoid changes when processing other
          // paths
          changed = true;
        }
      }
      return changed;
    }

    /**
     * Creates a shallow copy to the depth of the nodes. References to nodes are not copyied as new
     * objects. Only the collections are created sd new Objects.
     *
     * @param state
     * @return
     */
    public Map<VariableDeclaration, Set<Node>> createShallowCopy(
        Map<VariableDeclaration, Set<Node>> state) {
      Map<VariableDeclaration, Set<Node>> shallowCopy = new LinkedHashMap<>();
      for (VariableDeclaration key : state.keySet()) {
        shallowCopy.put(key, new LinkedHashSet<>(state.get(key)));
      }
      return shallowCopy;
    }
  }
}
