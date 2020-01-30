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

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.AssertStatement;
import de.fraunhofer.aisec.cpg.graph.BreakStatement;
import de.fraunhofer.aisec.cpg.graph.CatchClause;
import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.ConstructorDeclaration;
import de.fraunhofer.aisec.cpg.graph.ContinueStatement;
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.DoStatement;
import de.fraunhofer.aisec.cpg.graph.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.ForEachStatement;
import de.fraunhofer.aisec.cpg.graph.ForStatement;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.IfStatement;
import de.fraunhofer.aisec.cpg.graph.LabelStatement;
import de.fraunhofer.aisec.cpg.graph.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.NamespaceDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.ParamVariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.Statement;
import de.fraunhofer.aisec.cpg.graph.SwitchStatement;
import de.fraunhofer.aisec.cpg.graph.TryStatement;
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.WhileStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScopeManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScopeManager.class);

  private final Map<Node, Scope> scopeMap = new HashMap<>();
  private Scope currentScope = null;
  private LanguageFrontend lang;

  public ScopeManager(LanguageFrontend lang) {
    this.lang = lang;
    pushScope(new GlobalScope());
  }

  private void pushScope(Scope scope) {
    if (scopeMap.containsKey(scope.astNode)) {
      LOGGER.error(
          "Node cannot be scoped twice. A node at most one associated scope apart from the parent scopes.");
      return;
    }
    scopeMap.put(scope.astNode, scope);
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
      LOGGER.error("Cannot get current function. No scope.");
      return null;
    }

    Node node = recordScope.getAstNode();
    if (!(node instanceof RecordDeclaration)) {
      LOGGER.error("Cannot get current function. No AST node {}", recordScope.toString());
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
    if (scopeMap.containsKey(nodeToScope)) currentScope = scopeMap.get(nodeToScope);
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
    if (!scopeMap.containsKey(nodeToScope)) {
      Scope newScope;
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
        newScope = new DeclarationScope(nodeToScope);
      } else if (nodeToScope instanceof CatchClause) {
        newScope = new DeclarationScope(nodeToScope);
      } else if (nodeToScope instanceof RecordDeclaration) {
        newScope = new RecordScope(nodeToScope);
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
  public Scope leaveScope(Node nodeToLeave) {
    Scope leaveScope = getFirstScopeThat(scope -> scope.astNode.equals(nodeToLeave));
    if (leaveScope == null) {
      if (scopeMap.containsKey(nodeToLeave)) {
        LOGGER.error(
            "Node of type {} has a scope but is not active in the moment.", nodeToLeave.getClass());
      } else {
        LOGGER.error("Node of type {} is not associated with a scope.", nodeToLeave.getClass());
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

  public void addValueDeclaration(VariableDeclaration variableDeclaration) {
    DeclarationScope dScope =
        (DeclarationScope) getFirstScopeThat(scope -> scope instanceof DeclarationScope);
    if (dScope == null) {
      LOGGER.error("Cannot add VariableDeclaration. Not in declaration scope.");
      return;
    }
    dScope.addValueDeclaration(variableDeclaration);
    if (dScope.astNode instanceof Statement) {
      ((Statement) dScope.astNode).getLocals().add(variableDeclaration);
    }
  }

  public void addValueDeclaration(ParamVariableDeclaration paramDeclaration) {
    FunctionScope fScope =
        (FunctionScope) getFirstScopeThat(scope -> scope instanceof FunctionScope);
    if (fScope == null) {
      LOGGER.error("Cannot add ParamVariableDeclaration. Not in function scope.");
      return;
    }
    fScope.addValueDeclaration(paramDeclaration);
    List<ParamVariableDeclaration> params =
        ((FunctionDeclaration) fScope.getAstNode()).getParameters();
    if (!params.contains(paramDeclaration)) {
      params.add(paramDeclaration);
    }
  }

  public void addValueDeclaration(FieldDeclaration fieldDeclaration) {
    RecordScope rScope = (RecordScope) getFirstScopeThat(scope -> scope instanceof RecordScope);
    if (rScope == null) {
      LOGGER.error("Cannot add FieldDeclaration. Not in record scope.");
      return;
    }
    rScope.addValueDeclaration(fieldDeclaration);
    List<FieldDeclaration> fields = ((RecordDeclaration) rScope.getAstNode()).getFields();
    if (!fields.contains(fieldDeclaration)) {
      fields.add(fieldDeclaration);
    }
  }

  public void addValueDeclaration(FunctionDeclaration functionDeclaration) {
    DeclarationScope scopeForFunction =
        (DeclarationScope) getFirstScopeThat(scope -> scope instanceof RecordScope);
    if (scopeForFunction == null) {
      scopeForFunction =
          (DeclarationScope) getFirstScopeThat(scope -> scope instanceof GlobalScope);
    }
    if (scopeForFunction == null) {
      LOGGER.error("Cannot add FunctionDeclaration. Not in record or global scope.");
      return;
    }
    scopeForFunction.addValueDeclaration(functionDeclaration);
    if (scopeForFunction.getAstNode() != null) {
      RecordDeclaration rDecl = (RecordDeclaration) scopeForFunction.getAstNode();
      List<FunctionDeclaration> functions = new ArrayList<>();
      if (functionDeclaration instanceof ConstructorDeclaration) {
        functions =
            rDecl.getConstructors().stream()
                .map(m -> (FunctionDeclaration) m)
                .collect(Collectors.toList());
      } else if (functionDeclaration instanceof MethodDeclaration) {
        functions =
            rDecl.getMethods().stream()
                .map(m -> (FunctionDeclaration) m)
                .collect(Collectors.toList());
      }
      if (!functions.contains(functionDeclaration)) functions.add(functionDeclaration);
    }
  }

  public String getCurrentNamePrefix() {
    Scope namedScope =
        getFirstScopeThat(scope -> scope instanceof NameScope || scope instanceof RecordScope);
    if (namedScope instanceof NameScope) return ((NameScope) namedScope).getNamePrefix();
    if (namedScope instanceof RecordScope) return namedScope.getAstNode().getName();
    return "";
  }

  public String getFullNamePrefix() {
    Scope searchScope = currentScope;
    StringBuilder fullname = new StringBuilder();
    do {
      if (searchScope instanceof NameScope || searchScope instanceof RecordScope) {
        if (searchScope instanceof NameScope) {
          fullname.insert(0, ((NameScope) searchScope).getNamePrefix() + ".");
        }
        if (searchScope instanceof RecordScope) {
          fullname.insert(0, searchScope.getAstNode().getName() + ".");
        }
      }
      searchScope = searchScope.parent;
    } while (searchScope != null);
    if (fullname.length() > 0) {
      return fullname.substring(0, fullname.length() - 1); // remove last .
    } else {
      return "";
    }
  }

  @Nullable
  public ValueDeclaration resolve(DeclaredReferenceExpression ref) {
    return resolve(currentScope, ref);
  }

  @Nullable
  private ValueDeclaration resolve(Scope scope, DeclaredReferenceExpression ref) {
    if (scope instanceof DeclarationScope) {
      for (ValueDeclaration valDecl : ((DeclarationScope) scope).getValueDeclarations()) {
        if (valDecl.getName().equals(ref.getName())) return valDecl;
        /*
        if(valDecl instanceof ParamVariableDeclaration){
          ParamVariableDeclaration param = (ParamVariableDeclaration)valDecl;
          if(param.getName().equals(ref.getName())) return param;
        }else if(valDecl instanceof VariableDeclaration){
          VariableDeclaration variable = (VariableDeclaration) valDecl;
          if(variable.getName().equals(ref.getName())) return variable;
        }else if(valDecl instanceof FieldDeclaration){
          FieldDeclaration field = (FieldDeclaration) valDecl;

        }else if(valDecl instanceof FunctionDeclaration){
          FunctionDeclaration function = (FunctionDeclaration) valDecl;

        }*/
      }
    }
    return scope.getParent() != null ? resolve(scope.getParent(), ref) : null;
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

  ///// Copied over for now - not used but maybe necessary at some point ///////

  @Nullable
  public Declaration getDeclarationForName(String name) {
    // first, check locals
    Declaration declaration;
    if (isInBlock()) {
      CompoundStatement currentBlock = getCurrentBlock();
      declaration = getForName(currentBlock.getLocals(), name);

      if (declaration != null) {
        return declaration;
      }
    }
    if (isInFunction()) {
      FunctionDeclaration currentFunction = getCurrentFunction();
      declaration = getForName(currentFunction.getParameters(), name);

      if (declaration != null) {
        return declaration;
      }
    }
    if (isInRecord()) {
      RecordDeclaration currentRecord = getCurrentRecord();
      declaration = getForName(currentRecord.getFields(), name);

      if (declaration != null) {
        return declaration;
      }
    }

    // lastly, check globals
    declaration = getForName(getGlobals(), name);

    return declaration;

    // TODO: also check for function definitions?
  }

  @Nullable
  private <T extends ValueDeclaration> Declaration getForName(List<T> variables, String name) {
    Optional<T> any =
        variables.stream().filter(param -> Objects.equals(param.getName(), name)).findAny();

    return any.orElse(null);
  }

  ///// End copied over for now ///////

}
