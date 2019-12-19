/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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

package de.fraunhofer.aisec.cpg.helpers;

import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.Type;
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.swing.border.Border;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper class for graph walking: Walking through ast-, cfg-, ...- edges */
public class SubgraphWalker {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubgraphWalker.class);

  // hide ctor
  private SubgraphWalker() {}

  private static Collection<Field> getAllFields(Class<?> classType) {
    if (classType.getSuperclass() != null) {
      Collection<Field> fields = getAllFields(classType.getSuperclass());
      fields.addAll(Arrays.asList(classType.getDeclaredFields()));

      return fields;
    }

    return new ArrayList<>();
  }

  /**
   * Calls handler function of all super-classes of the current node to get the AST children of the
   * node.
   *
   * @param node - Node to get the children from the AST tree structure
   * @return a set of children from the nodes member variables
   */
  public static Set<Node> getAstChildren(Node node) {
    HashSet<Node> children = new HashSet<>(); // Set for duplicate elimination
    if (node == null) return children;

    Class classType = node.getClass();
    for (Field field : getAllFields(classType)) {
      SubGraph subGraph = field.getAnnotation(SubGraph.class);
      if (subGraph != null && Arrays.asList(subGraph.value()).contains("AST")) {
        try {
          // disable access mechanisms
          field.setAccessible(true);

          Object obj = field.get(node);

          // restore old state
          field.setAccessible(false);

          // skip, if null
          if (obj == null) {
            continue;
          }

          if (obj instanceof Node) {
            children.add((Node) obj);
          } else if (obj instanceof Collection) {
            Collection<? extends Node> astChildren = (Collection<? extends Node>) obj;
            astChildren.removeIf(Objects::isNull);
            children.addAll(astChildren);
          } else {
            throw new AnnotationFormatError(
                "Found @SubGraph(\"AST\") on field of type "
                    + obj.getClass()
                    + " but can only used with node graph classes or collections of graph nodes");
          }
        } catch (IllegalAccessException ex) {
          LOGGER.error("Error while retrieving AST children: {}", ex.getMessage());
        }
      }
    }
    return children;
  }

  /**
   * Flattens the tree, starting at Node n into a list.
   *
   * @param n
   * @return
   */
  public static List<Node> flattenAST(Node n) {
    if (n == null) {
      return new ArrayList<>();
    }
    List<Node> list = new ArrayList<>();
    list.add(n);
    SubgraphWalker.getAstChildren(n).forEach(node -> list.addAll(flattenAST(node)));
    list.sort(new NodeComparator());
    return list;
  }

  /**
   * Function returns two lists in a list. The first list contains all eog nodes with no predecesor
   * in the subgraph with root 'n'. The second list contains eog edges that have no successor in the
   * subgraph with root 'n'. The first List marks the entry and the second marks the exit nodes of
   * the cfg in this subgraph.
   *
   * @param n - root of the subgraph.
   * @return Two lists, list 1 contains all eog entries and list 2 contains all exits.
   */
  public static Border getEOGPathEdges(Node n) {
    Border border = new Border();
    List<Node> flattedASTTree = flattenAST(n);
    List<Node> eogNodes =
        flattedASTTree.stream()
            .filter(node -> !node.getPrevEOG().isEmpty() || !node.getNextEOG().isEmpty())
            .collect(Collectors.toList());
    // Nodes that are incoming edges, no other node
    border.entries =
        eogNodes.stream()
            .filter(node -> node.getPrevEOG().stream().anyMatch(prev -> !eogNodes.contains(prev)))
            .collect(Collectors.toList());
    border.exits =
        eogNodes.stream()
            .filter(node -> node.getNextEOG().stream().anyMatch(next -> !eogNodes.contains(next)))
            .collect(Collectors.toList());
    return border;
  }

  /**
   * Visit all nodes.
   *
   * @param stmt
   */
  public static void visit(Node stmt, Consumer<Node> visitor) {
    List<Node> nodes = flattenAST(stmt);
    for (Node n : nodes) {
      visitor.accept(n);
    }
  }

  /**
   * For better readability: <code>result.entries</code> instead of <code>result.get(0)</code> when
   * working with getEOGPathEdges. Can be used for all subgraphs in subgraphs, e.g. AST entries and
   * exits in a EOG subgraph, EOG entries and exits in a CFG subgraph.
   */
  public static class Border {
    private List<Node> entries = new ArrayList<>();
    private List<Node> exits = new ArrayList<>();

    public List<Node> getEntries() {
      return entries;
    }

    public List<Node> getExits() {
      return exits;
    }
  }

  public static class IterativeGraphWalker {

    private Deque<Node> todo;
    private Deque<Node> backlog;

    /**
     * This callback is triggered whenever a new node is visited for the first time. This is the
     * place where usual graph manipulation will happen. The current node is the single argument
     * passed to the function
     */
    private List<Consumer<Node>> onNodeVisit = new ArrayList<>();

    /**
     * The callback that is designed to tell the user when we leave the current scope. The exited
     * node is passed as an argument to the callback function. Consider the following AST:
     *
     * <p>.........(1) parent
     *
     * <p>........./........\
     *
     * <p>(2) child1....(4) child2
     *
     * <p>........|
     *
     * <p>(3) subchild
     *
     * <p>Once "parent" has been visited, we continue descending into its children. First into
     * "child1", followed by "subchild". Once we are done there, we return to "child1". At this
     * point, the exit handler notifies the user that "subchild" is being exited. Afterwards we exit
     * "child1", and after "child2" is done, "parent" is exited. This callback is important for
     * tracking declaration scopes, as e.g. anything declared in "child1" is also visible to
     * "subchild", but not to "child2".
     */
    private List<Consumer<Node>> onScopeExit = new ArrayList<>();

    /**
     * The core iterative AST traversal algorithm: In a depth-first way we descend into the tree,
     * providing callbacks for graph modification.
     *
     * @param root The node where we should start
     */
    public void iterate(Node root) {
      todo = new ArrayDeque<>();
      backlog = new ArrayDeque<>();
      Set<Node> seen = new HashSet<>();

      todo.push(root);
      while (!todo.isEmpty()) {
        Node current = todo.pop();
        if (!backlog.isEmpty() && backlog.peek().equals(current)) {
          onScopeExit.forEach(c -> c.accept(backlog.pop()));
        } else {
          // re-place the current node as a marker for the above check to find out when we need to
          // exit a scope
          todo.push(current);

          onNodeVisit.forEach(c -> c.accept(current));

          Set<Node> unseenChildren =
              SubgraphWalker.getAstChildren(current).stream()
                  .filter(Predicate.not(seen::contains))
                  .collect(Collectors.toSet());
          seen.addAll(unseenChildren);
          unseenChildren.forEach(todo::push);
          backlog.push(current);
        }
      }
    }

    public void registerOnNodeVisit(Consumer<Node> callback) {
      onNodeVisit.add(callback);
    }

    public void registerOnScopeExit(Consumer<Node> callback) {
      onScopeExit.add(callback);
    }

    public void clearCallbacks() {
      onNodeVisit.clear();
      onScopeExit.clear();
    }

    public Deque<Node> getTodo() {
      return todo;
    }

    public Deque<Node> getBacklog() {
      return backlog;
    }
  }

  /**
   * Handles declaration scope monitoring for iterative traversals. If this is not required, use
   * {@link IterativeGraphWalker} for less overhead.
   *
   * <p>Declaration scopes are similar to {@link de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager}
   * scopes: {@link ValueDeclaration}s located inside a scope (i.e. are children of the scope root)
   * are visible to any children of the scope root. Scopes can be layered, where declarations from
   * parent scopes are visible to the children but not the other way around.
   */
  public static class ScopedWalker {

    // declarationScope -> (parentScope, declarations)
    private Map<Node, Pair<Node, List<ValueDeclaration>>>
        nodeToParentBlockAndContainedValueDeclarations = new IdentityHashMap<>();
    @Nullable private Type currentClass = null;
    private IterativeGraphWalker walker;

    /**
     * Callback function(s) getting three arguments: the type of the class we're currently in, the
     * root node of the current declaration scope, the currently visited node. The declaration scope
     * root can be passed to {@link ScopedWalker#getAllDeclarationsForScope} in order to retrieve
     * the currently available declarations.
     */
    private List<TriConsumer<Type, Node, Node>> handlers = new ArrayList<>();

    public void clearCallbacks() {
      handlers.clear();
    }

    public void registerHandler(TriConsumer<Type, Node, Node> handler) {
      handlers.add(handler);
    }

    public void registerHandler(Consumer<Node> handler) {
      handlers.add((currClass, currScope, currNode) -> handler.accept(currNode));
    }

    /**
     * Wraps {@link IterativeGraphWalker} to handle declaration scopes.
     *
     * @param root The node where AST descent is started
     */
    public void iterate(Node root) {
      walker = new IterativeGraphWalker();
      handlers.forEach(h -> walker.registerOnNodeVisit(n -> handleNode(n, h)));
      walker.registerOnScopeExit(this::leaveScope);
      walker.iterate(root);
    }

    private void handleNode(Node current, TriConsumer<Type, Node, Node> handler) {

      Node currentScope = walker.getBacklog().peek();

      if (current instanceof RecordDeclaration) {
        currentClass = new Type(current.getName());
      }

      handler.accept(currentClass, currentScope, current);
    }

    private void leaveScope(Node exiting) {
      if (exiting instanceof RecordDeclaration) { // leave a class
        currentClass = null;
      }
    }

    public void collectDeclarations(Node root, Node current) {
      Node parentBlock = null;

      // get containing Record or Compound
      for (Node node : walker.getBacklog()) {
        if (node instanceof RecordDeclaration
            || node instanceof CompoundStatement
            || node instanceof FunctionDeclaration) {
          parentBlock = node;
          break;
        }
      }
      nodeToParentBlockAndContainedValueDeclarations.put(
          current, new MutablePair<>(parentBlock, new ArrayList<>()));

      if (current instanceof ValueDeclaration) {

        LOGGER.info("Adding variable {}", current.getCode());
        if (parentBlock == null) {
          LOGGER.warn("Parent block is empty during subgraph run");
        } else {
          nodeToParentBlockAndContainedValueDeclarations
              .get(parentBlock)
              .getRight()
              .add((ValueDeclaration) current);
        }
      }
    }

    public List<ValueDeclaration> getAllDeclarationsForScope(Node scope) {
      List<ValueDeclaration> result = new ArrayList<>();
      Node currentScope = scope;

      Set<String> scopedVars = new HashSet<>();

      // get all declarations from the current scope and all its parent scopes
      while (currentScope != null
          && nodeToParentBlockAndContainedValueDeclarations.containsKey(scope)) {
        Pair<Node, List<ValueDeclaration>> entry =
            nodeToParentBlockAndContainedValueDeclarations.get(currentScope);
        for (ValueDeclaration val : entry.getRight()) {
          // make sure that we only add the variable for the current scope.
          // if the var is already added, all outside vars with this name are shadowed inside a
          // scope and we do not add them here
          if (!scopedVars.contains(val.getName())) {
            result.add(val);
            scopedVars.add(val.getName());
          }
        }
        currentScope = entry.getLeft();
      }
      return result;
    }

    public Optional<? extends ValueDeclaration> getDeclarationForScope(Node scope, String name) {
      Node currentScope = scope;

      // iterate all declarations from the current scope and all its parent scopes
      while (currentScope != null
          && nodeToParentBlockAndContainedValueDeclarations.containsKey(scope)) {
        Pair<Node, List<ValueDeclaration>> entry =
            nodeToParentBlockAndContainedValueDeclarations.get(currentScope);
        for (ValueDeclaration val : entry.getRight()) {
          if (val.getName().equals(name)) {
            return Optional.of(val);
          }
        }
        currentScope = entry.getLeft();
      }
      return Optional.empty();
    }
  }
}
