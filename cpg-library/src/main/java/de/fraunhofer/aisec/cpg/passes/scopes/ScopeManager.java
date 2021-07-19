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
package de.fraunhofer.aisec.cpg.passes.scopes;

import static de.fraunhofer.aisec.cpg.helpers.Util.errorWithFileLocation;

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.statements.*;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The scope manager builds a multitree-structure of scopes associated to a scope. These Scopes
 * capture the are of validity of certain (Variable-, Field-, Record-)declarations but are also used
 * to identify outer scopes that should be target of a jump (continue, break, throw).
 *
 * <p>enterScope(Node) and leaveScope(Node) can be used to enter the Tree of scopes and then sitting
 * at a path, access the currently valid "stack" of scopes.
 */
public class ScopeManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScopeManager.class);

  /** Allows to map the AST nodes to the associated scope */
  private final Map<Node, Scope> scopeMap = new IdentityHashMap<>();

  private final Map<String, Scope> fqnScopeMap = new IdentityHashMap<>();

  private Scope currentScope = null;
  private LanguageFrontend lang;

  public ScopeManager() {
    pushScope(new GlobalScope());
  }

  public LanguageFrontend getLang() {
    return lang;
  }

  public void setLang(LanguageFrontend lang) {
    this.lang = lang;
  }

  /**
   * Combine the state of several scope managers into this one.
   *
   * @param toMerge The scope managers to merge into this one
   */
  public void mergeFrom(Collection<ScopeManager> toMerge) {
    List<GlobalScope> globalScopes =
        toMerge.stream()
            .map(s -> s.scopeMap.get(null))
            .filter(GlobalScope.class::isInstance)
            .map(GlobalScope.class::cast)
            .collect(Collectors.toList());
    Scope currGlobalScope = this.scopeMap.get(null);
    if (!(currGlobalScope instanceof GlobalScope)) {
      LOGGER.error("Scope for null node is not a GlobalScope!");
    } else {
      ((GlobalScope) currGlobalScope).mergeFrom(globalScopes);
    }

    for (ScopeManager manager : toMerge) {
      this.scopeMap.putAll(manager.scopeMap);
      this.fqnScopeMap.putAll(manager.fqnScopeMap);
    }

    this.scopeMap.put(null, currGlobalScope);
  }

  private void pushScope(Scope scope) {
    if (scopeMap.containsKey(scope.astNode)) {
      LOGGER.error(
          "Node cannot be scoped twice. A node at most one associated scope apart from the parent scopes.");
      return;
    }
    scopeMap.put(scope.astNode, scope);

    if (scope instanceof NameScope || scope instanceof RecordScope) {
      fqnScopeMap.put(scope.getAstNode().getName(), scope);
    }

    if (currentScope != null) {
      currentScope.getChildren().add(scope);
      scope.setParent(currentScope);
    }
    currentScope = scope;
  }

  public boolean isInBlock() {
    return this.getFirstScopeThat(scope -> scope instanceof BlockScope) != null;
  }

  public boolean isInFunction() {
    return this.getFirstScopeThat(scope -> scope instanceof FunctionScope) != null;
  }

  public boolean isInRecord() {
    return this.getFirstScopeThat(scope -> scope instanceof RecordScope) != null;
  }

  public Map<String, Scope> getFqnScopeMap() {
    return fqnScopeMap;
  }

  @Nullable
  public CompoundStatement getCurrentBlock() {
    Scope blockScope = this.getFirstScopeThat(scope -> scope instanceof BlockScope);
    if (blockScope == null) {
      LOGGER.error("Cannot get current block. No scope.");
      return null;
    }

    Node node = blockScope.getAstNode();
    if (!(node instanceof CompoundStatement)) {
      LOGGER.error("Cannot get current block. No AST node {}", blockScope.toString());
      return null;
    }
    return (CompoundStatement) node;
  }

  @Nullable
  public FunctionDeclaration getCurrentFunction() {
    Scope functionScope = getFirstScopeThat(scope -> scope instanceof FunctionScope);
    if (functionScope == null) {
      LOGGER.error("Cannot get current function. No scope.");
      return null;
    }

    Node node = functionScope.getAstNode();
    if (!(node instanceof FunctionDeclaration)) {
      LOGGER.error("Cannot get current function. No AST node {}", functionScope.toString());
      return null;
    }
    return (FunctionDeclaration) node;
  }

  @Nullable
  public RecordDeclaration getCurrentRecord() {
    Scope recordScope = getFirstScopeThat(scope -> scope instanceof RecordScope);
    if (recordScope == null) {
      LOGGER.error("Cannot get current Record. No scope.");
      return null;
    }

    Node node = recordScope.getAstNode();
    if (!(node instanceof RecordDeclaration)) {
      LOGGER.error("Cannot get current Record. No AST node {}", recordScope);
      return null;
    }
    return (RecordDeclaration) node;
  }

  public Scope getCurrentScope() {
    return this.currentScope;
  }

  public void enterScopeIfExists(Node nodeToScope) {
    if (scopeMap.containsKey(nodeToScope)) {
      var scope = scopeMap.get(nodeToScope);

      // we need a special handling of name spaces, because
      // thy are associated to more than one AST node
      if (scope instanceof NameScope) {
        // update AST (see enterScope for an explanation)
        scope.astNode = nodeToScope;
      }

      currentScope = scope;
    }
  }

  @Nullable
  public Scope leaveScopeIfExists(Node nodeToLeave) {
    Scope leaveScope = scopeMap.getOrDefault(nodeToLeave, null);
    if (leaveScope != null) {
      currentScope = leaveScope.parent;
    }
    return leaveScope;
  }

  public void enterScope(Node nodeToScope) {
    Scope newScope = null;
    if (!scopeMap.containsKey(nodeToScope)) {
      if (nodeToScope instanceof CompoundStatement) {
        newScope = new BlockScope((CompoundStatement) nodeToScope);
      } else if (nodeToScope instanceof WhileStatement
          || nodeToScope instanceof DoStatement
          || nodeToScope instanceof AssertStatement) {
        newScope = new LoopScope((Statement) nodeToScope);
      } else if (nodeToScope instanceof ForStatement || nodeToScope instanceof ForEachStatement) {
        newScope = new LoopScope((Statement) nodeToScope);
      } else if (nodeToScope instanceof SwitchStatement) {
        newScope = new SwitchScope((SwitchStatement) nodeToScope);
      } else if (nodeToScope instanceof FunctionDeclaration) {
        newScope = new FunctionScope((FunctionDeclaration) nodeToScope);
      } else if (nodeToScope instanceof IfStatement) {
        newScope = new ValueDeclarationScope(nodeToScope);
      } else if (nodeToScope instanceof CatchClause) {
        newScope = new ValueDeclarationScope(nodeToScope);
      } else if (nodeToScope instanceof RecordDeclaration) {
        newScope =
            new RecordScope(nodeToScope, getCurrentNamePrefix(), lang.getNamespaceDelimiter());
      } else if (nodeToScope instanceof TemplateDeclaration) {
        newScope =
            new TemplateScope(nodeToScope, getCurrentNamePrefix(), lang.getNamespaceDelimiter());
      } else if (nodeToScope instanceof TryStatement) {
        newScope = new TryScope(nodeToScope);
      } else if (nodeToScope instanceof NamespaceDeclaration) {
        // this is a little workaround to solve issues around namespaces

        // the challenge is, that if we have two files that have functions
        // belonging to the same namespace, they need to end up in the same NameScope,
        // otherwise the call resolver will not find them. But we still need to be able
        // to treat the namespace declaration as an AST node unique to each file. So in
        // the example we want to up with two namespace declaration that point to the same
        // name scope in the end.

        // First, check if the namespace already exists in our scope
        // TODO: proper resolving of the namespace according to the syntax?
        var existing =
            ((StructureDeclarationScope) currentScope)
                .getStructureDeclarations().stream()
                    .filter(x -> Objects.equals(x.getName(), nodeToScope.getName()))
                    .findFirst();

        if (existing.isPresent()) {
          var oldNode = existing.get();
          var oldScope = scopeMap.get(oldNode);

          // might still be non-existing in some cases because this is hacky
          if (oldScope != null) {
            // update the AST node to this namespace declaration
            oldScope.astNode = nodeToScope;

            // set current scope
            currentScope = oldScope;

            // make it also available in the scope map, otherwise, we cannot leave the scope
            scopeMap.put(oldScope.astNode, oldScope);
          } else {
            newScope =
                new NameScope(nodeToScope, getCurrentNamePrefix(), lang.getNamespaceDelimiter());
          }
        } else {
          newScope =
              new NameScope(nodeToScope, getCurrentNamePrefix(), lang.getNamespaceDelimiter());
        }
      } else {
        LOGGER.error("No known scope for AST-nodes of type {}", nodeToScope.getClass());
        return;
      }
    }

    if (newScope != null) {
      pushScope(newScope);
      newScope.setScopedName(getCurrentNamePrefix());
    } else {
      currentScope = scopeMap.get(nodeToScope);
    }
  }

  public boolean isBreakable(Scope scope) {
    return scope instanceof LoopScope || scope instanceof SwitchScope;
  }

  public boolean isContinuable(Scope scope) {
    return scope instanceof LoopScope;
  }

  /**
   * Remove all scopes above the specified one including the specified one.
   *
   * @param nodeToLeave - The scope is defined by its astNode
   * @return the scope is returned for processing
   */
  @Nullable
  public Scope leaveScope(@NonNull Node nodeToLeave) {
    // Check to return as soon as we know that there is no associated scope, this check could be
    // omitted
    // but will increase runtime if leaving a node without scope will happen often.
    if (!scopeMap.containsKey(nodeToLeave)) {
      return null;
    }
    Scope leaveScope = getFirstScopeThat(scope -> Objects.equals(scope.astNode, nodeToLeave));
    if (leaveScope == null) {
      if (scopeMap.containsKey(nodeToLeave)) {
        errorWithFileLocation(
            nodeToLeave,
            LOGGER,
            "Node of type {} has a scope but is not active in the moment.",
            nodeToLeave.getClass());
      } else {
        errorWithFileLocation(
            nodeToLeave,
            LOGGER,
            "Node of type {} is not associated with a scope.",
            nodeToLeave.getClass());
      }
      return null;
    }
    currentScope = leaveScope.parent;
    return leaveScope;
  }

  @Nullable
  public Scope getFirstScopeThat(Predicate<Scope> predicate) {
    return getFirstScopeThat(currentScope, predicate);
  }

  @Nullable
  public Scope getFirstScopeThat(Scope searchScope, Predicate<Scope> predicate) {
    while (searchScope != null) {
      if (predicate.test(searchScope)) {
        return searchScope;
      }
      searchScope = searchScope.parent;
    }
    return null;
  }

  public List<Scope> getScopesThat(Predicate<Scope> predicate) {
    List<Scope> scopes = new ArrayList<>();
    for (Scope scope : scopeMap.values()) if (predicate.test(scope)) scopes.add(scope);
    return scopes;
  }

  public <T> List<Scope> getUniqueScopesThat(
      Predicate<Scope> predicate, Function<Scope, T> uniqueProperty) {
    List<Scope> scopes = new ArrayList<>();
    Set<T> seen = new HashSet<>();
    for (Scope scope : scopeMap.values()) {
      if (predicate.test(scope) && seen.add(uniqueProperty.apply(scope))) {
        scopes.add(scope);
      }
    }
    return scopes;
  }

  public void addBreakStatement(BreakStatement breakStatement) {
    if (breakStatement.getLabel() == null) {
      Scope scope = getFirstScopeThat(this::isBreakable);
      if (scope == null) {
        LOGGER.error(
            "Break inside of unbreakable scope. The break will be ignored, but may lead "
                + "to an incorrect graph. The source code is not valid or incomplete.");
        return;
      }
      ((IBreakable) scope).addBreakStatement(breakStatement);
    } else {
      LabelStatement labelStatement = getLabelStatement(breakStatement.getLabel());
      if (labelStatement != null) {
        Scope scope = getScopeOfStatement(labelStatement.getSubStatement());
        ((IBreakable) scope).addBreakStatement(breakStatement);
      }
    }
  }

  public void addContinueStatement(ContinueStatement continueStatement) {
    if (continueStatement.getLabel() == null) {
      Scope scope = getFirstScopeThat(this::isContinuable);
      if (scope == null) {
        LOGGER.error(
            "Continue inside of not continuable scope. The continue will be ignored, but may lead "
                + "to an incorrect graph. The source code is not valid or incomplete.");
        return;
      }
      ((IContinuable) scope).addContinueStatement(continueStatement);
    } else {
      LabelStatement labelStatement = getLabelStatement(continueStatement.getLabel());
      if (labelStatement != null) {
        Scope scope = getScopeOfStatement(labelStatement.getSubStatement());
        ((IContinuable) scope).addContinueStatement(continueStatement);
      }
    }
  }

  public void addLabelStatement(LabelStatement labelStatement) {
    currentScope.addLabelStatement(labelStatement);
  }

  @Nullable
  public LabelStatement getLabelStatement(String labelString) {
    LabelStatement labelStatement;
    Scope searchScope = currentScope;
    while (searchScope != null) {
      labelStatement = searchScope.getLabelStatements().getOrDefault(labelString, null);
      if (labelStatement != null) {
        return labelStatement;
      }
      searchScope = searchScope.parent;
    }
    return null;
  }

  /**
   * TO remove a valueDeclaration in the cases were the declaration gets replaced by something else
   *
   * @param declaration
   */
  public void removeDeclaration(Declaration declaration) {
    Scope toIterate = currentScope;
    do {

      if (toIterate instanceof ValueDeclarationScope) {
        ValueDeclarationScope declScope = (ValueDeclarationScope) toIterate;
        if (declScope.getValueDeclarations().contains(declaration)) {
          declScope.getValueDeclarations().remove(declaration);
          if (declScope.getAstNode() instanceof RecordDeclaration) {
            RecordDeclaration rec = (RecordDeclaration) declScope.getAstNode();
            rec.removeField((FieldDeclaration) declaration);
            rec.removeMethod((MethodDeclaration) declaration);
            rec.removeConstructor((ConstructorDeclaration) declaration);
            rec.removeRecord((RecordDeclaration) declaration);
          } else if (declScope.getAstNode() instanceof FunctionDeclaration) {
            ((FunctionDeclaration) declScope.getAstNode())
                .removeParameter((ParamVariableDeclaration) declaration);
          } else if (declScope.getAstNode() instanceof Statement) {
            if (declaration instanceof VariableDeclaration) {
              ((Statement) declScope.getAstNode()).removeLocal((VariableDeclaration) declaration);
            }
          } else if (declScope.getAstNode() instanceof EnumDeclaration) {
            ((EnumDeclaration) declScope.getAstNode()).getEntries().remove(declaration);
          }
        }
      }

      toIterate = toIterate.getParent();
    } while (toIterate != null);
  }

  public void resetToGlobal(TranslationUnitDeclaration declaration) {
    GlobalScope global = (GlobalScope) getFirstScopeThat(scope -> scope instanceof GlobalScope);
    if (global != null) {
      // update the AST node to this translation unit declaration
      global.astNode = declaration;

      currentScope = global;
    }
  }

  /**
   * Adds a declaration to the CPG by taking into account the currently active scope, and add the
   * Declaration to the appropriate node. This function will keep the declaration in the Scopes and
   * allows the ScopeManager by himself to resolve ValueDeclarations through {@link
   * ScopeManager#resolve(DeclaredReferenceExpression)}.
   *
   * @param declaration
   */
  public void addDeclaration(Declaration declaration) {
    if (declaration instanceof ProblemDeclaration || declaration instanceof IncludeDeclaration) {
      // directly add problems and includes to the global scope
      var globalScope = (GlobalScope) getFirstScopeThat(scope -> scope instanceof GlobalScope);
      globalScope.addDeclaration(declaration);
    } else if (declaration instanceof ValueDeclaration) {
      ValueDeclarationScope scopeForValueDeclaration =
          (ValueDeclarationScope)
              getFirstScopeThat(scope -> scope instanceof ValueDeclarationScope);
      scopeForValueDeclaration.addValueDeclaration((ValueDeclaration) declaration);
    } else if (declaration instanceof RecordDeclaration
        || declaration instanceof NamespaceDeclaration
        || declaration instanceof EnumDeclaration
        || declaration instanceof TemplateDeclaration) {
      StructureDeclarationScope scopeForStructureDeclaration =
          (StructureDeclarationScope)
              getFirstScopeThat(scope -> scope instanceof StructureDeclarationScope);
      scopeForStructureDeclaration.addDeclaration(declaration);
    }
  }

  public void addTypedef(TypedefDeclaration typedef) {
    ValueDeclarationScope scope =
        (ValueDeclarationScope) getFirstScopeThat(ValueDeclarationScope.class::isInstance);
    if (scope == null) {
      LOGGER.error("Cannot add typedef. Not in declaration scope.");
      return;
    }
    scope.addTypedef(typedef);
    if (scope.astNode == null) {
      lang.getCurrentTU().addTypedef(typedef);
    } else {
      scope.astNode.addTypedef(typedef);
    }
  }

  public List<TypedefDeclaration> getCurrentTypedefs() {
    return getCurrentTypedefs(currentScope);
  }

  private List<TypedefDeclaration> getCurrentTypedefs(Scope scope) {
    List<TypedefDeclaration> curr = new ArrayList<>();

    if (scope instanceof ValueDeclarationScope) {
      curr.addAll(((ValueDeclarationScope) scope).getTypedefs());
    }

    if (scope.getParent() != null) {
      for (TypedefDeclaration parentTypedef : getCurrentTypedefs(scope.getParent())) {
        if (curr.stream()
            .map(TypedefDeclaration::getAlias)
            .noneMatch(parentTypedef.getAlias()::equals)) {
          curr.add(parentTypedef);
        }
      }
    }
    return curr;
  }

  public String getCurrentNamePrefix() {
    Scope namedScope =
        getFirstScopeThat(scope -> scope instanceof NameScope || scope instanceof RecordScope);
    if (namedScope instanceof NameScope) return ((NameScope) namedScope).getNamePrefix();
    if (namedScope instanceof RecordScope) return namedScope.getAstNode().getName();
    return "";
  }

  public String getCurrentNamePrefixWithDelimiter() {
    String namePrefix = getCurrentNamePrefix();
    if (!namePrefix.isEmpty()) {
      namePrefix += lang.getNamespaceDelimiter();
    }
    return namePrefix;
  }

  @Nullable
  public ValueDeclaration resolve(DeclaredReferenceExpression ref) {
    return resolve(currentScope, ref);
  }

  public List<FunctionDeclaration> resolveFunction(CallExpression call) {
    return resolveFunction(currentScope, call);
  }

  public List<FunctionTemplateDeclaration> resolveFunctionTemplateDeclaration(CallExpression call) {
    return resolveFunctionTemplateDeclaration(currentScope, call);
  }

  public List<FunctionDeclaration> resolveFunctionStopScopeTraversalOnDefinition(
      CallExpression call) {
    return resolveFunctionStopScopeTraversalOnDefinition(currentScope, call);
  }

  /**
   * Resolves only references to Values in the current scope, static references to other visible
   * records are not resolved over the ScopeManager.
   *
   * <p>TODO: We should merge this function with {@link #resolveFunction(Scope, CallExpression)}
   *
   * @param scope
   * @param ref
   * @return
   */
  @Nullable
  private ValueDeclaration resolve(Scope scope, Node ref) {
    if (scope instanceof ValueDeclarationScope) {
      for (ValueDeclaration valDecl : ((ValueDeclarationScope) scope).getValueDeclarations()) {
        if (valDecl.getName().equals(ref.getName())) {

          // If the reference seems to point to a function the entire signature is checked for
          // equality
          if (ref instanceof HasType
              && ((HasType) ref).getType() instanceof FunctionPointerType
              && valDecl instanceof FunctionDeclaration) {
            FunctionPointerType fptrType = (FunctionPointerType) ((HasType) ref).getType();
            FunctionDeclaration d = (FunctionDeclaration) valDecl;
            if (d.getType().equals(fptrType.getReturnType())
                && d.hasSignature(fptrType.getParameters())) {
              return valDecl;
            }
          } else {
            return valDecl;
          }
        }
      }
    }
    return scope.getParent() != null ? resolve(scope.getParent(), ref) : null;
  }

  /**
   * Traverses the scope and looks for Declarations of type c which matches f
   *
   * @param scope
   * @param p predicate the element must match to
   * @param c class of the object we want to find by traversing
   * @param <T>
   * @return
   */
  @NonNull
  private <T> List<T> resolveValueDeclaration(Scope scope, Predicate<T> p, Class<T> c) {
    if (scope instanceof ValueDeclarationScope) {
      var list =
          ((ValueDeclarationScope) scope)
              .getValueDeclarations().stream()
                  .filter(c::isInstance)
                  .map(c::cast)
                  .filter(p)
                  .collect(Collectors.toList());

      if (!list.isEmpty()) {
        return list;
      }
    }

    return scope.getParent() != null
        ? resolveValueDeclaration(scope.getParent(), p, c)
        : new ArrayList<>();
  }

  /**
   * Traverses the scope and looks for Declarations of type c which matches f
   *
   * @param scope
   * @param p predicate the element must match to
   * @param c class of the object we want to find by traversing
   * @param <T>
   * @return
   */
  @NonNull
  private <T> List<T> resolveStructureDeclaration(Scope scope, Predicate<T> p, Class<T> c) {
    if (scope instanceof StructureDeclarationScope) {
      var list =
          ((StructureDeclarationScope) scope)
              .getStructureDeclarations().stream()
                  .filter(c::isInstance)
                  .map(c::cast)
                  .filter(p)
                  .collect(Collectors.toList());

      if (list.isEmpty()) {
        for (Declaration declaration :
            ((StructureDeclarationScope) scope).getStructureDeclarations()) {
          if (declaration instanceof RecordDeclaration) {
            list =
                ((RecordDeclaration) declaration)
                    .getTemplates().stream()
                        .filter(c::isInstance)
                        .map(c::cast)
                        .filter(p)
                        .collect(Collectors.toList());
          }
        }
      }

      if (!list.isEmpty()) {
        return list;
      }
    }

    return scope.getParent() != null
        ? resolveStructureDeclaration(scope.getParent(), p, c)
        : new ArrayList<>();
  }

  /**
   * @param scope where we are searching for the FunctionTemplateDeclarations
   * @param call CallExpression we want to resolve an invocation target for
   * @return List of FunctionTemplateDeclaration that match the name provided in the CallExpression
   *     and therefore are invocation candidates
   */
  private List<FunctionTemplateDeclaration> resolveFunctionTemplateDeclaration(
      Scope scope, CallExpression call) {
    return resolveStructureDeclaration(
        scope, c -> c.getName().equals(call.getName()), FunctionTemplateDeclaration.class);
  }

  /**
   * Resolves a function reference of a call expression.
   *
   * @param scope
   * @param call
   * @return
   */
  @NonNull
  private List<FunctionDeclaration> resolveFunction(Scope scope, CallExpression call) {
    return resolveValueDeclaration(
        scope,
        f -> f.getName().equals(call.getName()) && f.hasSignature(call.getSignature()),
        FunctionDeclaration.class);
  }

  /**
   * Resolves a function reference of a call expression, but stops the scope traversal when a
   * FunctionDeclaration with matching name has been found
   *
   * @param scope
   * @param call
   * @return
   */
  @NonNull
  private List<FunctionDeclaration> resolveFunctionStopScopeTraversalOnDefinition(
      Scope scope, CallExpression call) {
    return resolveValueDeclaration(
        scope, f -> f.getName().equals(call.getName()), FunctionDeclaration.class);
  }

  /**
   * This function tries to resolve a FQN to a scope. The name is the name of the AST-Node
   * associated to a scope. The Name may be the FQN-name or a relative name that with the currently
   * active namespace gives the AST-Nodes, FQN. If the provided name and the current namespace
   * overlap ,they are merged and the FQN is resolved. If there is no node with the merged FQN-name
   * null is returned. This is due to the behaviour of C++ when resolving names for AST-elements
   * that are definitions of exiting declarations.
   *
   * @param astNodeName relative (to the current Namespace) or fqn-Name of an entity associated to a
   *     scope.
   * @return The scope that the resolved name is associated to.
   */
  private Scope resolveScopeWithPath(@Nullable String astNodeName) {
    if (astNodeName == null || astNodeName.isEmpty()) {
      return currentScope;
    }
    List<String> namePath =
        Arrays.asList(astNodeName.split(Pattern.quote(lang.getNamespaceDelimiter())));
    List<String> currentPath =
        Arrays.asList(getCurrentNamePrefix().split(Pattern.quote(lang.getNamespaceDelimiter())));

    // Last index because the inner name has preference
    int nameIndexInCurrent = currentPath.lastIndexOf(namePath.get(0));

    if (nameIndexInCurrent >= 0) {
      // Overlapping relative resolution
      List<String> mergedPath = currentPath.subList(0, nameIndexInCurrent);
      mergedPath.addAll(namePath);
      return this.fqnScopeMap.getOrDefault(
          String.join(lang.getNamespaceDelimiter(), mergedPath), null);
    } else {
      // Absolute name of the node by concatenating the current namespace and the relative name
      String relativeToAbsolute =
          getCurrentNamePrefixWithDelimiter()
              + lang.getNamespaceDelimiter()
              + String.join(lang.getNamespaceDelimiter(), namePath);
      // Relative resolution
      Scope scope = this.fqnScopeMap.getOrDefault(relativeToAbsolute, null);
      if (scope != null) {
        return scope;
      } else {
        // Absolut resolution: The name is used as absolut name.
        return this.fqnScopeMap.getOrDefault(astNodeName, null);
      }
    }
  }

  @Nullable
  private ValueDeclaration resolveInSingleScope(Scope scope, DeclaredReferenceExpression ref) {
    if (scope instanceof ValueDeclarationScope) {
      for (ValueDeclaration valDecl : ((ValueDeclarationScope) scope).getValueDeclarations()) {
        if (valDecl.getName().equals(ref.getName())) return valDecl;
      }
    }
    return null;
  }

  public Scope getScopeOfStatement(Node node) {
    return scopeMap.getOrDefault(node, null);
  }

  /**
   * Retrieves the {@link RecordDeclaration} for the given name in the given scope.
   *
   * @param scope the scope
   * @param name the name
   * @return the declaration, or null if it does not exist
   */
  @Nullable
  public RecordDeclaration getRecordForName(Scope scope, String name) {
    Optional<RecordDeclaration> o = Optional.empty();

    // check current scope first
    if (scope instanceof StructureDeclarationScope) {
      o =
          ((StructureDeclarationScope) scope)
              .getStructureDeclarations().stream()
                  .filter(d -> d instanceof RecordDeclaration && Objects.equals(d.getName(), name))
                  .map(d -> (RecordDeclaration) d)
                  .findFirst();
    }

    if (o.isPresent()) {
      return o.get();
    }

    // no parent left
    if (scope.getParent() == null) {
      return null;
    }

    return getRecordForName(scope.getParent(), name);
  }

  ///// End copied over for now ///////

}
