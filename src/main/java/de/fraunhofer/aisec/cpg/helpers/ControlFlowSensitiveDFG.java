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
  private Set<Node> visitedEOG = new HashSet<>();
  // Node where the ControlFlowSensitive analysis is started. On analysis start this will be the
  // MethodDeclaration, but
  // it can be any other node where the analysis is splitted (such as if of switch)
  private Node startNode;

  // Node where a splitted analysis is joined back together. If there is no split this is null.
  private Node endNode;

  public ControlFlowSensitiveDFG(
      Node startNode, Node endNode, Map<VariableDeclaration, Set<Node>> variables) {
    this.variables = duplicateMap(variables);
    this.startNode = startNode;
    this.endNode = endNode;
  }

  public ControlFlowSensitiveDFG(Node startNode) {
    this.variables = new HashMap<>();
    this.startNode = startNode;
    this.endNode = null;
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

    for (VariableDeclaration v : in.keySet()) {
      Set<Node> nodes = new HashSet<>(in.get(v));
      duplicatedVariables.put(v, nodes);
    }

    return duplicatedVariables;
  }

  private void addVisitedToMap(VariableDeclaration variableDeclaration) {
    Set<Node> prevDFGSet = variableDeclaration.getPrevDFG();
    Set<Node> removePrevDFG = new HashSet<>();
    for (Node prev : prevDFGSet) {
      if (checkVisited(prev)) {
        if (variables.containsKey(variableDeclaration)) {
          variables.get(variableDeclaration).add(prev);
        } else {
          Set<Node> DFGSet = new HashSet<>();
          DFGSet.add(prev);
          variables.put(variableDeclaration, DFGSet);
        }
        removePrevDFG.add(prev);
      }
    }

    for (Node prev : removePrevDFG) {
      variableDeclaration.removePrevDFG(prev);
    }
  }

  /**
   * Traverses the EOG starting at a node until there is no more outgoing EOGs to new nodes.
   *
   * @param node starting node
   * @param eogReachableNodes set containing all nodes that have been reached (loop prevention)
   * @return set containing all nodes that have been reached
   */
  private Set<Node> eogTraversal(Node node, Set<Node> eogReachableNodes) {
    if (eogReachableNodes.contains(node)) {
      return eogReachableNodes;
    }

    eogReachableNodes.add(node);

    Set<Node> nextEog = new HashSet<>(node.getNextEOG());
    for (Node n : nextEog) {
      eogReachableNodes.addAll(eogTraversal(n, eogReachableNodes));
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
      Set<Node> rechableEOG = new HashSet<>();
      eogs.add(eogTraversal(next, rechableEOG));
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
        Set<Node> eog = eogTraversal(element, new HashSet<>());
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
   * Removes the prevDFG to a VariableDeclaration of a node and adds the values of the
   * VariableDeclaration as prevDFGs to the node
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
        currNode.removePrevDFG(prev);
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
        currNode.removeNextDFG(next);
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

  /** Main method that performs the ControlFlowSensitveDFG analysis and transformation. */
  public void handle() {
    Node currNode = startNode;
    while (currNode != null && !currNode.equals(endNode)) {
      Node nextNode = null;
      visited.add(currNode);
      visitedEOG.add(currNode);

      List<Node> nextEOG = currNode.getNextEOG();
      if (nextEOG.size() == 0) {
        break;
      }

      if (currNode instanceof VariableDeclaration) {
        // New VariableDeclaration is found, and we start the tracking
        addVisitedToMap((VariableDeclaration) currNode);
      } else if (currNode instanceof IfStatement || currNode instanceof SwitchStatement) {
        // TODO Currently we treat the CaseStatements in SwitchStatements as if they were mutually exclusive. This must be updated once we have an order for the cases to determine the target of a fall through
        // If an IfStatement or a SwitchStatement is found we split the ControlFlowSensitiveDFG for
        // every case and merge it, when the execution reaches the joinPoint
        Node joinNode = obtainJoinPoint(currNode);

        List<Node> eogList = currNode.getNextEOG();
        List<ControlFlowSensitiveDFG> dfgs = new ArrayList<>();
        for (Node n : eogList) {
          ControlFlowSensitiveDFG dfg = new ControlFlowSensitiveDFG(n, joinNode, variables);
          dfgs.add(dfg);
          dfg.handle();
        }

        this.variables = joinVariables(dfgs);

        for (ControlFlowSensitiveDFG dfg : dfgs) {
          this.visitedEOG.addAll(dfg.getVisitedEOG());
        }

        nextNode = joinNode;

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
  }
}
