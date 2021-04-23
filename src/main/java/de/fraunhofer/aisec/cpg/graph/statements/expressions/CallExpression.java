/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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

package de.fraunhofer.aisec.cpg.graph.statements.expressions;

import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.util.*;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.neo4j.ogm.annotation.Relationship;

/**
 * An expression, which calls another function. It has a list of arguments (list of {@link
 * Expression}s) and is connected via the INVOKES edge to its {@link FunctionDeclaration}.
 */
public class CallExpression extends Expression implements TypeListener {

  /**
   * Connection to its {@link FunctionDeclaration}. This will be populated by the {@link
   * de.fraunhofer.aisec.cpg.passes.CallResolver}.
   */
  @Relationship(value = "INVOKES", direction = "OUTGOING")
  protected List<PropertyEdge<FunctionDeclaration>> invokes = new ArrayList<>();

  /** The list of arguments. */
  @Relationship(value = "ARGUMENTS", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge<Expression>> arguments = new ArrayList<>();

  /**
   * The base object. This is marked as an AST child, because this is required for {@link
   * MemberCallExpression}. Be aware that for simple calls the implicit "this" base is not part of
   * the original AST, but we treat it as such for better consistency
   */
  @SubGraph("AST")
  private Node base;

  private String fqn;

  public Node getBase() {
    return base;
  }

  public void setBase(Node base) {
    if (this.base instanceof HasType) {
      ((HasType) this.base).unregisterTypeListener(this);
    }
    this.base = base;
    if (base instanceof HasType) {
      ((HasType) base).registerTypeListener(this);
    }
  }

  @NonNull
  public List<Expression> getArguments() {
    List<Expression> targets = new ArrayList<>();
    for (PropertyEdge<Expression> propertyEdge : this.arguments) {
      targets.add(propertyEdge.getEnd());
    }
    return Collections.unmodifiableList(targets);
  }

  public void setArguments(List<Expression> arguments) {
    this.arguments = PropertyEdge.transformIntoOutgoingPropertyEdgeList(arguments, this);
  }

  public void setArgument(int index, Expression argument) {
    this.arguments.get(index).setEnd(argument);
  }

  @NonNull
  public List<PropertyEdge<Expression>> getArgumentsPropertyEdge() {
    return this.arguments;
  }

  public void addArgument(Expression expression) {
    PropertyEdge<Expression> propertyEdge = new PropertyEdge<>(this, expression);
    propertyEdge.addProperty(Properties.INDEX, this.arguments.size());
    this.arguments.add(propertyEdge);
  }

  @NonNull
  public List<FunctionDeclaration> getInvokes() {
    List<FunctionDeclaration> targets = new ArrayList<>();
    for (PropertyEdge<FunctionDeclaration> propertyEdge : this.invokes) {
      targets.add(propertyEdge.getEnd());
    }
    return Collections.unmodifiableList(targets);
  }

  public void setInvokes(List<FunctionDeclaration> invokes) {
    PropertyEdge.getTarget(this.invokes)
        .forEach(
            i -> {
              i.unregisterTypeListener(this);
              Util.detachCallParameters(i, this.getArguments());
              this.removePrevDFG(i);
            });
    this.invokes = PropertyEdge.transformIntoOutgoingPropertyEdgeList(invokes, this);
    invokes.forEach(
        i -> {
          i.registerTypeListener(this);
          Util.attachCallParameters(i, this.getArguments());
          this.addPrevDFG(i);
        });
  }

  public List<PropertyEdge<FunctionDeclaration>> getInvokesPropertyEdge() {
    return this.invokes;
  }

  public List<Type> getSignature() {
    return getArguments().stream().map(Expression::getType).collect(Collectors.toList());
  }

  @Override
  public void typeChanged(HasType src, HasType root, Type oldType) {
    if (src == base) {
      setFqn(src.getType().getRoot().getTypeName() + "." + this.getName());
    } else {
      Type previous = this.type;
      List<Type> types =
          invokes.stream()
              .map(pe -> pe.getEnd())
              .map(FunctionDeclaration::getType)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
      Type alternative = !types.isEmpty() ? types.get(0) : null;
      Type commonType = TypeManager.getInstance().getCommonType(types).orElse(alternative);
      Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
      subTypes.remove(oldType);
      subTypes.addAll(types);

      setType(commonType, root);
      setPossibleSubTypes(subTypes, root);

      if (!previous.equals(this.type)) {
        this.type.setTypeOrigin(Type.Origin.DATAFLOW);
      }
    }
  }

  @Override
  public void possibleSubTypesChanged(HasType src, HasType root, Set<Type> oldSubTypes) {
    if (src != base) {
      Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
      subTypes.addAll(src.getPossibleSubTypes());
      setPossibleSubTypes(subTypes, root);
    }
  }

  @Override
  public String toString() {
    return "["
        + getClass().getSimpleName()
        + (isImplicit() ? "*" : "")
        + "]"
        + (base == null ? "" : base.getName() + ".")
        + getName()
        + "("
        + getSignature().stream().map(Type::getName).collect(Collectors.joining(", "))
        + ") -> "
        + getType().getName();
  }

  public String getFqn() {
    return fqn;
  }

  public void setFqn(String fqn) {
    this.fqn = fqn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CallExpression)) {
      return false;
    }
    CallExpression that = (CallExpression) o;
    return super.equals(that)
        && Objects.equals(this.getArguments(), that.getArguments())
        && PropertyEdge.propertyEqualsList(arguments, that.arguments)
        && Objects.equals(this.getInvokes(), that.getInvokes())
        && PropertyEdge.propertyEqualsList(invokes, that.invokes)
        && Objects.equals(base, that.base);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
