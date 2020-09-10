package de.fraunhofer.aisec.cpg.helpers;

import de.fraunhofer.aisec.cpg.graph.*;
import java.util.*;

public class ControlFlowSensitiveDFG {
  /*
   Map tracking every VariableDeclaration and all the possible Values. For the ControlFlowSensitiveDFG
   only VariableDeclarations are tracked, as we cannot determine the values for FieldDeclarations, since
   it depends on the external execution order.
  */
  private Map<VariableDeclaration, Set<Node>> variables;
  private Set<Node> visited = new HashSet<>();
  private Set<Node> visitedEOG;

  // A Node with refined DFG edges (key) is mapped to a set of nodes that were the previous
  // (unrefined) DFG edges and need to be removed later on
  private Map<Node, Set<Node>> removes;
  // Node where the ControlFlowSensitive analysis is started. On analysis start this will be the
  // MethodDeclaration, but
  // it can be any other node where the analysis is splitted (such as if of switch)
  private Node startNode;

  // Node where a splitted analysis is joined back together. If there is no split this is null.
  private Node endNode;

  public ControlFlowSensitiveDFG(
      Node startNode,
      Node endNode,
      Map<VariableDeclaration, Set<Node>> variables,
      Set<Node> visitedEOG) {
    this.variables = duplicateMap(variables);
    this.startNode = startNode;
    this.endNode = endNode;
    this.visitedEOG = new HashSet<>(visitedEOG);
    this.removes = new HashMap<>();
  }

  public ControlFlowSensitiveDFG(Node startNode) {
    this.variables = new HashMap<>();
    this.startNode = startNode;
    this.endNode = null;
    this.visitedEOG = new HashSet<>();
    this.removes = new HashMap<>();
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

  public Map<VariableDeclaration, Set<Node>> getVariables() {
    return variables;
  }

  public Set<Node> getVisitedEOG() {
    return visitedEOG;
  }

  private boolean checkVisited(Node node) {
    return this.visited.contains(node);
  }

  private static Map<VariableDeclaration, Set<Node>> duplicateMap(
      Map<VariableDeclaration, Set<Node>> in) {
    Map<VariableDeclaration, Set<Node>> duplicatedVariables = new HashMap<>();

    for (Map.Entry<VariableDeclaration, Set<Node>> e : in.entrySet()) {
      Set<Node> nodes = new HashSet<>(e.getValue());
      duplicatedVariables.put(e.getKey(), nodes);
    }

    return duplicatedVariables;
  }

  private void addVisitedToMap(VariableDeclaration variableDeclaration) {
    Set<Node> prevDFGSet = variableDeclaration.getPrevDFG();
    for (Node prev : prevDFGSet) {
      if (checkVisited(prev)) {
        if (variables.containsKey(variableDeclaration)) {
          variables.get(variableDeclaration).add(prev);
        } else {
          Set<Node> dfgSet = new HashSet<>();
          dfgSet.add(prev);
          variables.put(variableDeclaration, dfgSet);
        }
      }
    }
  }

  /**
   * Reverses the removal of prevDFG for VariableDeclarations perfomed by {@link
   * #addVisitedToMap(VariableDeclaration)}, when there a unique DFG path
   */
  private void addUniqueDFGs() {
    for (Map.Entry<VariableDeclaration, Set<Node>> entry : this.variables.entrySet()) {
      if (entry.getValue().size() == 1) {
        entry.getKey().addPrevDFG(entry.getValue().iterator().next());
      }
    }
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
   * @param node that has multiple outgoing EOG edges (e.g. IfStatement or SwitchStatement. Not
   *     applicable to TryCatch, since control flow is not fully mutually exclusive
   * @return the first node at which the execution of both paths are joined
   */
  private Node obtainJoinPoint(Node node) {
    Set<Node> nextEOG = new HashSet<>(node.getNextEOG());
    List<Set<Node>> eogs = new ArrayList<>();
    for (Node next : nextEOG) {
      eogs.add(eogTraversal(next));
    }

    // Calculate intersection to locate point in which the execution paths join
    Set<Node> intersection = new HashSet<>(eogs.get(0));

    for (Set<Node> eog : eogs) {
      intersection.retainAll(eog);
    }

    // Find first Element of EOG Traversal:
    Node element = null;
    while (intersection.size() > 1) {
      if (element != null) {
        intersection.remove(element);
      }
      Optional<Node> elementFromSet = intersection.stream().findFirst();
      if (elementFromSet.isPresent()) {
        element = elementFromSet.get();
        Set<Node> eog = eogTraversal(element);
        eog.remove(element);
        intersection.removeAll(eog);
      }
    }

    Optional<Node> joinNode = intersection.stream().findAny();
    return joinNode.orElse(null);
  }

  /**
   * Merges the variable Map when the analysis has been split and is joined together.
   *
   * @param dfgs List of all ControlFlowSensitiveDFGs that are necessary depending on the number of
   *     cases (e.g. if-else contains two dfgs, one for if and one for the else block.
   * @return merged Map with the VariableDeclaration to Value mappings of all the dfgs. Due to the
   *     merge it is possible to have multiple values for one VariableDeclaration (e.g. when a
   *     variable is set to some value x in the if block, and to a value y in the else block).
   */
  private Map<VariableDeclaration, Set<Node>> joinVariables(List<ControlFlowSensitiveDFG> dfgs) {
    Map<VariableDeclaration, Set<Node>> joindVariables = new HashMap<>();

    for (ControlFlowSensitiveDFG dfg : dfgs) {
      for (VariableDeclaration variableDeclaration : dfg.getVariables().keySet()) {
        if (!joindVariables.containsKey(variableDeclaration)) {
          Set<Node> values = new HashSet<>(dfg.getVariables().get(variableDeclaration));
          joindVariables.put(variableDeclaration, values);
        } else {
          joindVariables
              .get(variableDeclaration)
              .addAll(dfg.getVariables().get(variableDeclaration));
        }
      }
    }

    return joindVariables;
  }

  /**
   * @param dfgs exclusive DFG Paths
   * @return combination of all dfg paths that need to be removed for every exclusive DFG Path
   */
  private Map<Node, Set<Node>> joinRemoves(List<ControlFlowSensitiveDFG> dfgs) {
    Map<Node, Set<Node>> newRemoves = new HashMap<>();
    for (ControlFlowSensitiveDFG dfg : dfgs) {
      for (Node n : dfg.getRemoves().keySet()) {
        if (!newRemoves.containsKey(n)) {
          newRemoves.put(n, new HashSet<>());
        }
        newRemoves.get(n).addAll(dfg.getRemoves().get(n));
      }
    }
    return newRemoves;
  }

  /**
   * Stores the prevDFG to a VariableDeclaration of a node in the removes map and adds the values of
   * the VariableDeclaration as prevDFGs to the node
   *
   * @param currNode node that is analyzed
   */
  private void setIngoingDFG(Node currNode) {
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
   * If a Node has a DFG to a VariableDeclaration we need to remove the nextDFG and store the value
   * of the node in our tracking map
   *
   * @param currNode Node that is being analyzed
   */
  private void registerOutgoingDFG(Node currNode) {
    Set<Node> nextDFG = new HashSet<>(currNode.getNextDFG());
    for (Node next : nextDFG) {
      if (next instanceof VariableDeclaration && variables.containsKey(next)) {
        Set<Node> values = new HashSet<>(currNode.getPrevDFG());
        variables.replace((VariableDeclaration) next, values);
      }
    }
  }

  /**
   * Checks which is the next node that should be analyzed by EOG order and checking which have
   * already been analyzed
   *
   * @param currNode current node
   * @return node that has currNode as prevDFG and has not yet been analyzed or null if we reached
   *     the end
   */
  private Node getNextEOG(Node currNode) {
    for (Node next : currNode.getNextEOG()) {
      if (!visitedEOG.contains(next)) {
        return next;
      }
    }

    return null;
  }

  /**
   * Handles when there is a mutually exclusive split for the DFG
   *
   * @param currNode node where the split occcurs (e.g. IfStatement or SwitchStatement)
   * @return node at which the split is over and both execution paths are equal again
   */
  private Node handleDFGSplit(Node currNode) {
    // If an IfStatement or a SwitchStatement is found we split the ControlFlowSensitiveDFG for
    // every case and merge it, when the execution reaches the joinPoint
    Node joinNode = obtainJoinPoint(currNode);

    List<Node> eogList = currNode.getNextEOG();
    List<ControlFlowSensitiveDFG> dfgs = new ArrayList<>();
    for (Node n : eogList) {
      ControlFlowSensitiveDFG dfg =
          new ControlFlowSensitiveDFG(n, joinNode, variables, this.visitedEOG);
      dfgs.add(dfg);
      dfg.handle();
    }

    this.variables = joinVariables(dfgs);
    this.removes = joinRemoves(dfgs);

    for (ControlFlowSensitiveDFG dfg : dfgs) {
      this.visitedEOG.addAll(dfg.getVisitedEOG());
    }
    return joinNode;
  }

  /** Main method that performs the ControlFlowSensitveDFG analysis and transformation. */
  public void handle() {
    Node currNode = startNode;
    while (!visitedEOG.contains(currNode) && currNode != null && !currNode.equals(endNode)) {
      Node nextNode = null;
      visited.add(currNode);
      visitedEOG.add(currNode);

      List<Node> nextEOG = currNode.getNextEOG();
      if (nextEOG.isEmpty()) {
        break;
      }

      if (currNode instanceof VariableDeclaration) {
        // New VariableDeclaration is found, and we start the tracking
        addVisitedToMap((VariableDeclaration) currNode);
      } else if (currNode instanceof IfStatement || currNode instanceof SwitchStatement) {
        nextNode = handleDFGSplit(currNode);

      } else if (currNode instanceof DeclaredReferenceExpression) {
        // A DeclaredReferenceExpression makes use of one of the VariableDeclaration we are
        // tracking. Therefore we must modify the outgoing and ingoing DFG edges
        // Check for outgoing DFG edges
        registerOutgoingDFG(currNode);

        // Check for ingoing DFG edges
        setIngoingDFG(currNode);
      }

      // If the nextNode has not been set by a JoinPoint we take the nextEOG
      if (nextNode == null) {
        nextNode = getNextEOG(currNode);
      }

      currNode = nextNode;
    }

    if (this.startNode instanceof FunctionDeclaration) {
      addUniqueDFGs();
    }
  }
}
