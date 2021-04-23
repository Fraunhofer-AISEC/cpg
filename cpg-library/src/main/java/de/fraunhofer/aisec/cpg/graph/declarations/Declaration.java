/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.declarations;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression;
import java.util.HashSet;
import java.util.Set;
import org.neo4j.ogm.annotation.Relationship;

/**
 * Represents a single declaration or definition, i.e. of a variable ({@link VariableDeclaration})
 * or function ({@link FunctionDeclaration}).
 *
 * <p>Note: We do NOT (currently) distinguish between the definition and the declaration of a
 * function. This means, that if a function is first declared and later defined with a function
 * body, we will currently have two {@link FunctionDeclaration} nodes. This is very similar to the
 * behaviour of clang, however clang does establish a connection between those nodes, we currently
 * do not.
 */
// TODO: expressionRefersToDeclaration definition and declaration nodes and introduce a field if its
// declaration only
public class Declaration extends Node {

  @Relationship(value = "REFERS_TO", direction = "INCOMING")
  private Set<DeclaredReferenceExpression> incomingReferences = new HashSet<>();

  @Relationship(value = "INVOKES", direction = "INCOMING")
  private Set<CallExpression> incomingInvokes = new HashSet<>();

  @Relationship(value = "CONSTRUCTOR", direction = "INCOMING")
  private Set<ConstructExpression> incomingConstructorCalls = new HashSet<>();

  public void addIncomingReference(DeclaredReferenceExpression user) {
    incomingReferences.add(user);
  }

  public void removeIncomingReference(DeclaredReferenceExpression user) {
    incomingReferences.remove(user);
  }

  public Set<DeclaredReferenceExpression> getIncomingReferences() {
    return new HashSet<>(incomingReferences);
  }

  public void addIncomingInvokes(CallExpression caller) {
    incomingInvokes.add(caller);
  }

  public void removeIncomingInvokes(CallExpression caller) {
    incomingInvokes.remove(caller);
  }

  public Set<CallExpression> getIncomingInvokes() {
    return new HashSet<>(incomingInvokes);
  }

  public void addIncomingConstructorCall(ConstructExpression caller) {
    incomingConstructorCalls.add(caller);
  }

  public void removeIncomingConstructorCall(ConstructExpression caller) {
    incomingConstructorCalls.remove(caller);
  }

  public Set<ConstructExpression> getIncomingConstructorCalls() {
    return new HashSet<>(incomingConstructorCalls);
  }

  @Override
  public String toString() {
    return "[" + getClass().getSimpleName() + (isImplicit() ? "*" : "") + "] " + getName();
  }
}
