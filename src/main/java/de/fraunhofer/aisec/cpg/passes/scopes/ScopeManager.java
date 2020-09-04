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

package de.fraunhofer.aisec.cpg.passes.scopes;

import static de.fraunhofer.aisec.cpg.helpers.Util.errorWithFileLocation;

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.AssertStatement;
import de.fraunhofer.aisec.cpg.graph.BreakStatement;
import de.fraunhofer.aisec.cpg.graph.CatchClause;
import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.ContinueStatement;
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.DoStatement;
import de.fraunhofer.aisec.cpg.graph.EnumDeclaration;
import de.fraunhofer.aisec.cpg.graph.ForEachStatement;
import de.fraunhofer.aisec.cpg.graph.ForStatement;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.IfStatement;
import de.fraunhofer.aisec.cpg.graph.LabelStatement;
import de.fraunhofer.aisec.cpg.graph.NamespaceDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.ProblemDeclaration;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.Statement;
import de.fraunhofer.aisec.cpg.graph.SwitchStatement;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.TryStatement;
import de.fraunhofer.aisec.cpg.graph.TypedefDeclaration;
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.WhileStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
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
  private final Map<Node, Scope> scopeMap = new HashMap<>();

  private final Map<String, Scope> fqnScopeMap = new HashMap<>();

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

  public List<ValueDeclaration> getGlobals() {
    GlobalScope globalS = (GlobalScope) getFirstScopeThat(scope -> scope instanceof GlobalScope);
    if (globalS != null) {
      return globalS.getValueDeclarations();
    } else {
      return new ArrayList<>();
    }
  }

  public Scope getCurrentScope() {
    return this.currentScope;
  }

  public void addGlobal(VariableDeclaration global) {
    getGlobals().add(global);
  }

  public void enterScopeIfExists(Node nodeToScope) {
    if (scopeMap.containsKey(nodeToScope)) {
      currentScope = scopeMap.get(nodeToScope);
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
      } else if (nodeToScope instanceof TryStatement) {
        newScope = new TryScope(nodeToScope);
      } else if (nodeToScope instanceof NamespaceDeclaration) {
        newScope = new NameScope(nodeToScope, getCurrentNamePrefix(), lang.getNamespaceDelimiter());
      } else {
        LOGGER.error("No known scope for AST-nodes of type {}", nodeToScope.getClass());
        return;
      }
      pushScope(newScope);
    }
    currentScope = scopeMap.get(nodeToScope);
    if (newScope != null) {
      newScope.setScopedName(getCurrentNamePrefix());
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
        Scope scope = getScopeOfStatment(labelStatement.getSubStatement());
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
        Scope scope = getScopeOfStatment(labelStatement.getSubStatement());
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
            rec.getFields().remove(declaration);
            rec.getMethods().remove(declaration);
            rec.getConstructors().remove(declaration);
            rec.getRecords().remove(declaration);
          } else if (declScope.getAstNode() instanceof FunctionDeclaration) {
            ((FunctionDeclaration) declScope.getAstNode()).getParameters().remove(declaration);
          } else if (declScope.getAstNode() instanceof Statement) {
            ((Statement) declScope.getAstNode()).getLocals().remove(declaration);
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
    if (declaration instanceof ProblemDeclaration) {
      // directly add problems to the global scope
      var globalScope = (GlobalScope) getFirstScopeThat(scope -> scope instanceof GlobalScope);
      globalScope.addDeclaration(declaration);
    } else if (declaration instanceof ValueDeclaration) {
      ValueDeclarationScope scopeForValueDeclaration =
          (ValueDeclarationScope)
              getFirstScopeThat(scope -> scope instanceof ValueDeclarationScope);
      scopeForValueDeclaration.addValueDeclaration((ValueDeclaration) declaration);
    } else if (declaration instanceof RecordDeclaration
        || declaration instanceof NamespaceDeclaration
        || declaration instanceof EnumDeclaration) {
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

  @Nullable
  private ValueDeclaration resolve(Scope scope, DeclaredReferenceExpression ref) {
    if (scope instanceof ValueDeclarationScope) {
      for (ValueDeclaration valDecl : ((ValueDeclarationScope) scope).getValueDeclarations()) {
        if (valDecl.getName().equals(ref.getName())) return valDecl;
      }
    }
    return scope.getParent() != null ? resolve(scope.getParent(), ref) : null;
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
    List<String> namePath = Arrays.asList(astNodeName.split(lang.getNamespaceDelimiter()));
    List<String> currentPath =
        Arrays.asList(getCurrentNamePrefix().split(lang.getNamespaceDelimiter()));

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

  @Nullable
  public Declaration resolveInRecord(
      RecordDeclaration recordDeclaration, DeclaredReferenceExpression ref) {
    List<Declaration> members = new ArrayList<>();
    members.addAll(recordDeclaration.getFields());
    members.addAll(recordDeclaration.getMethods());
    members.addAll(recordDeclaration.getRecords());

    for (Declaration member : members) {
      if (member.getName().equals(ref.getName())) {
        return member;
      }
    }
    return null;
  }

  @Nullable
  public Declaration resolveInInheritanceHierarchy(
      RecordDeclaration recordDeclaration, DeclaredReferenceExpression ref) {
    Declaration resolved = resolveInRecord(recordDeclaration, ref);
    if (resolved != null) {
      return resolved;
    }
    // Here we resolve the member in the order the set returns the ancestors. As soon as we support
    // a Language that
    // allows diamond pattern style inheritance and member overloading, that would yield a ambiguous
    // declaration in
    // C++, algorithms like C3-Linearization have to be implemented
    for (RecordDeclaration ancestor : recordDeclaration.getSuperTypeDeclarations()) {
      resolved = resolveInInheritanceHierarchy(ancestor, ref);
      if (resolved != null) {
        return resolved;
      }
    }
    return null;
  }

  public Scope getScopeOfStatment(Node node) {
    return scopeMap.getOrDefault(node, null);
  }

  public void connectToLocal(DeclaredReferenceExpression referenceExpression) {
    if (isInBlock()) {
      CompoundStatement currentBlock = getCurrentBlock();
      if (expressionRefersToDeclaration(referenceExpression, currentBlock.getLocals())) {
        return;
      }
    }

    if (isInFunction()) {
      FunctionDeclaration currentFunction = getCurrentFunction();
      if (currentFunction != null
          && expressionRefersToDeclaration(referenceExpression, currentFunction.getParameters())) {
        return;
      }
    }

    if (isInRecord()) {
      RecordDeclaration currentRecord = getCurrentRecord();
      if (expressionRefersToDeclaration(referenceExpression, currentRecord.getFields())) {
        return;
      }
    }
    expressionRefersToDeclaration(referenceExpression, getGlobals());
  }

  private <T extends ValueDeclaration> boolean expressionRefersToDeclaration(
      DeclaredReferenceExpression referenceExpression, List<T> variables) {
    // look for a LOCAL with the same name
    Optional<T> any =
        variables.stream()
            .filter(param -> Objects.equals(param.getName(), referenceExpression.getName()))
            .findAny();

    if (any.isPresent()) {
      T declaration = any.get();

      referenceExpression.setRefersTo(declaration);
      referenceExpression.setType(declaration.getType());
      LOGGER.debug(
          "Connecting {} to method parameter {} of type {}",
          referenceExpression,
          declaration,
          declaration.getType());

      return true;
    }

    return false;
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
