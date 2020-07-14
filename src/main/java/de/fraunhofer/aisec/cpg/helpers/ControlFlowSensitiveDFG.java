package de.fraunhofer.aisec.cpg.helpers;

import de.fraunhofer.aisec.cpg.graph.*;
import java.util.*;

public class ControlFlowSensitiveDFG {

  private Map<VariableDeclaration, Set<Node>> variables;
  private Set<Node> visited = new HashSet<>();
  private Node startNode;
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
   *     applicable to TryCatch, since control flow is not mutually exclusive
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

  private void setIngoingDFG(Node currNode) {
    Set<Node> prevDFGs = currNode.getPrevDFG();
    for (Node prev : prevDFGs) {
      if (prev instanceof VariableDeclaration && variables.containsKey(prev)) {
        for (Node target : variables.get(prev)) {
          currNode.addPrevDFG(target);
        }
        currNode.removePrevDFG(prev);
      }
    }
  }

  private void registerOutgoingDFG(Node currNode) {
    Set<Node> nextDFG = currNode.getNextDFG();
    for (Node next : nextDFG) {
      if (next instanceof VariableDeclaration && variables.containsKey(next)) {
        Set<Node> values = new HashSet<>(currNode.getPrevDFG());
        variables.replace((VariableDeclaration) next, values);
        currNode.removeNextDFG(next);
      }
    }
  }

  public void handle() {
    Node currNode = startNode;
    while (!currNode.equals(endNode)) {
      Node nextNode = null;
      visited.add(currNode);

      List<Node> nextEOG = currNode.getNextEOG();
      if (nextEOG.size() == 0) {
        break;
      }

      if (currNode instanceof VariableDeclaration) {
        addVisitedToMap((VariableDeclaration) currNode);
      } else if (currNode instanceof IfStatement || currNode instanceof SwitchStatement) {
        Node joinNode = obtainJoinPoint(currNode);

        List<Node> eogList = currNode.getNextEOG();
        List<ControlFlowSensitiveDFG> dfgs = new ArrayList<>();
        for (Node n : eogList) {
          ControlFlowSensitiveDFG dfg = new ControlFlowSensitiveDFG(n, joinNode, variables);
          dfgs.add(dfg);
          dfg.handle();
        }

        this.variables = joinVariables(dfgs);
        nextNode = joinNode;

      } else if (currNode instanceof DeclaredReferenceExpression) {
        // Check for outgoing DFG edges
        registerOutgoingDFG(currNode);

        // Check for ingoing DFG edges
        setIngoingDFG(currNode);
      }

      if (nextNode == null) {
        nextNode = currNode.getNextEOG().get(0);
      }

      currNode = nextNode;
    }
  }
}
