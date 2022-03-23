/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.helpers;

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper class for graph walking: Walking through ast-, cfg-, ...- edges */
public class SubgraphWalker {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubgraphWalker.class);

  private static final HashMap<String, List<Field>> fieldCache = new HashMap<>();

  // hide ctor
  private SubgraphWalker() {}

  /**
   * Returns all the fields for a specific class type. Because this information is static during
   * runtime, we do cache this information in {@link #fieldCache} for performance reasons.
   *
   * @param classType the class type
   * @return its fields, including the ones from its superclass
   */
  private static Collection<Field> getAllFields(Class<?> classType) {
    if (classType.getSuperclass() != null) {
      var cacheKey = classType.getName();

      // Note: we cannot use computeIfAbsent here, because we are calling our function
      // recursively and this would result in a ConcurrentModificationException
      if (fieldCache.containsKey(cacheKey)) {
        return fieldCache.get(cacheKey);
      }

      var fields = new ArrayList<Field>();
      fields.addAll(getAllFields(classType.getSuperclass()));
      fields.addAll(Arrays.asList(classType.getDeclaredFields()));

      // update the cache
      fieldCache.put(cacheKey, fields);

      return fields;
    }

    return new ArrayList<>();
  }

  /**
   * Retrieves a list of AST children of the specified node by iterating all fields that are
   * annotated with the {@link SubGraph} annotation and its value "AST".
   *
   * @param node the start node
   * @return a list of children from the node's AST
   */
  public static List<Node> getAstChildren(Node node) {
    var children = new ArrayList<Node>();
    if (node == null) return children;

    Class<?> classType = node.getClass();
    for (Field field : getAllFields(classType)) {
      SubGraph subGraph = field.getAnnotation(SubGraph.class);
      if (subGraph != null && Arrays.asList(subGraph.value()).contains("AST")) {
        try {
          // disable access mechanisms
          field.trySetAccessible();

          Object obj = field.get(node);

          // restore old state
          field.setAccessible(false);

          // skip, if null
          if (obj == null) {
            continue;
          }

          boolean outgoing = true; // default
          if (field.getAnnotation(Relationship.class) != null) {
            outgoing = field.getAnnotation(Relationship.class).direction().equals("OUTGOING");
          }

          if (PropertyEdge.checkForPropertyEdge(field, obj)) {
            obj = PropertyEdge.unwrap((List<PropertyEdge<Node>>) obj, outgoing);
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
   * @param n the node which contains the ast children to flatten
   * @return the flattened nodes
   */
  public static List<Node> flattenAST(Node n) {
    if (n == null) {
      return new ArrayList<>();
    }

    Set<Node> list = new HashSet<>();

    flattenASTInternal(list, n);

    List<Node> ret = new ArrayList<>(list);

    // sort it
    ret.sort(new NodeComparator());

    return ret;
  }

  private static void flattenASTInternal(@NonNull Set<Node> list, @NonNull Node n) {
    // add the node itself
    list.add(n);

    for (Node child : SubgraphWalker.getAstChildren(n)) {
      if (!list.contains(child)) {
        flattenASTInternal(list, child);
      }
    }
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

  public static void refreshType(Node node) {
    for (Node child : getAstChildren(node)) {
      refreshType(child);
    }
    if (node instanceof HasType) {
      ((HasType) node).refreshType();
    }
  }

  public static void activateTypes(Node node, ScopeManager scopeManager) {
    AtomicInteger num = new AtomicInteger();

    Map<HasType, Set<Type>> typeCache = TypeManager.getInstance().getTypeCache();
    IterativeGraphWalker walker = new IterativeGraphWalker();
    walker.registerOnNodeVisit(scopeManager::enterScopeIfExists);
    walker.registerOnScopeExit(
        n -> {
          if (n instanceof HasType) {
            HasType typeNode = (HasType) n;
            typeCache
                .getOrDefault(typeNode, Collections.emptySet())
                .forEach(
                    t -> {
                      t = TypeManager.getInstance().resolvePossibleTypedef(t);
                      ((HasType) n).setType(t);
                    });
            typeCache.remove((HasType) n);
            num.getAndIncrement();
          }
        });
    walker.iterate(node);

    LOGGER.debug("Activated {} nodes for {}", num, node.getName());

    // For some nodes it may happen that they are not reachable via AST, but we still need to set
    // their type to the requested value
    typeCache.forEach(
        (n, types) ->
            types.forEach(
                t -> {
                  t = TypeManager.getInstance().resolvePossibleTypedef(t);
                  n.setType(t);
                }));
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
    private final List<Consumer<Node>> onNodeVisit = new ArrayList<>();

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
    private final List<Consumer<Node>> onScopeExit = new ArrayList<>();

    /**
     * The core iterative AST traversal algorithm: In a depth-first way we descend into the tree,
     * providing callbacks for graph modification.
     *
     * @param root The node where we should start
     */
    public void iterate(Node root) {
      todo = new ArrayDeque<>();
      backlog = new ArrayDeque<>();
      Set<Node> seen = new LinkedHashSet<>();

      todo.push(root);
      while (!todo.isEmpty()) {
        Node current = todo.pop();
        if (!backlog.isEmpty() && backlog.peek().equals(current)) {
          Node exiting = backlog.pop();
          onScopeExit.forEach(c -> c.accept(exiting));
        } else {
          // re-place the current node as a marker for the above check to find out when we need to
          // exit a scope
          todo.push(current);
          onNodeVisit.forEach(c -> c.accept(current));

          var unseenChildren =
              SubgraphWalker.getAstChildren(current).stream()
                  .filter(Predicate.not(seen::contains))
                  .collect(Collectors.toList());
          seen.addAll(unseenChildren);
          Util.reverse(unseenChildren.stream()).forEach(todo::push);
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
    private final Map<Node, Pair<Node, List<ValueDeclaration>>>
        nodeToParentBlockAndContainedValueDeclarations = new IdentityHashMap<>();
    private final Deque<RecordDeclaration> currentClass = new ArrayDeque<>();
    private IterativeGraphWalker walker;

    private final LanguageFrontend lang;

    public ScopedWalker(LanguageFrontend lang) {
      this.lang = lang;
    }

    /**
     * Callback function(s) getting three arguments: the type of the class we're currently in, the
     * root node of the current declaration scope, the currently visited node. The declaration scope
     * root can be passed to {@link ScopedWalker#getAllDeclarationsForScope} in order to retrieve
     * the currently available declarations.
     */
    private final List<TriConsumer<RecordDeclaration, Node, Node>> handlers = new ArrayList<>();

    public void clearCallbacks() {
      handlers.clear();
    }

    public void registerHandler(TriConsumer<RecordDeclaration, Node, Node> handler) {
      handlers.add(handler);
    }

    public void registerHandler(BiConsumer<Node, RecordDeclaration> handler) {
      handlers.add((currClass, parent, currNode) -> handler.accept(currNode, currClass));
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

    private void handleNode(Node current, TriConsumer<RecordDeclaration, Node, Node> handler) {
      lang.getScopeManager().enterScopeIfExists(current);

      Node parent = walker.getBacklog().peek();

      if (current instanceof RecordDeclaration && current != currentClass.peek()) {
        currentClass.push(
            (RecordDeclaration)
                current); // we can be in an inner class, so we remember this as a stack
      }

      // TODO: actually we should not handle this in handleNode but have something similar to
      // onScopeEnter because the method declaration already correctly sets the scope

      // methods can also contain record scopes
      if (current instanceof MethodDeclaration) {
        RecordDeclaration recordDeclaration = ((MethodDeclaration) current).getRecordDeclaration();
        if (recordDeclaration != null && recordDeclaration != currentClass.peek()) {
          currentClass.push(recordDeclaration);
        }
      }

      handler.accept(currentClass.peek(), parent, current);
    }

    private void leaveScope(Node exiting) {
      if (exiting instanceof RecordDeclaration) { // leave a class
        currentClass.pop();
      }

      lang.getScopeManager().leaveScope(exiting);
    }

    @Nullable
    public RecordDeclaration getCurrentClass() {
      return currentClass.isEmpty() ? null : currentClass.peek();
    }

    public void collectDeclarations(Node current) {
      Node parentBlock = null;

      // get containing Record or Compound
      for (Node node : walker.getBacklog()) {
        if (node instanceof RecordDeclaration
            || node instanceof CompoundStatement
            || node instanceof FunctionDeclaration
            // can also be a translationunit for global (c) functions
            || node instanceof TranslationUnitDeclaration) {
          parentBlock = node;
          break;
        }
      }
      nodeToParentBlockAndContainedValueDeclarations.put(
          current, new MutablePair<>(parentBlock, new ArrayList<>()));

      if (current instanceof ValueDeclaration) {

        LOGGER.trace("Adding variable {}", current.getCode());
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
          if (val instanceof FunctionDeclaration || !scopedVars.contains(val.getName())) {
            result.add(val);
            scopedVars.add(val.getName());
          }
        }
        currentScope = entry.getLeft();
      }
      return result;
    }

    public Optional<? extends ValueDeclaration> getDeclarationForScope(
        Node scope, Predicate<ValueDeclaration> predicate) {
      Node currentScope = scope;

      // iterate all declarations from the current scope and all its parent scopes
      while (currentScope != null
          && nodeToParentBlockAndContainedValueDeclarations.containsKey(scope)) {
        Pair<Node, List<ValueDeclaration>> entry =
            nodeToParentBlockAndContainedValueDeclarations.get(currentScope);
        for (ValueDeclaration val : entry.getRight()) {
          if (predicate.test(val)) {
            return Optional.of(val);
          }
        }
        currentScope = entry.getLeft();
      }
      return Optional.empty();
    }
  }
}
