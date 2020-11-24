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

import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.ToStringBuilder;
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
  protected List<PropertyEdge> invokes = new ArrayList<>();
  /** The list of arguments. */
  @Relationship(value = "ARGUMENTS", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge> arguments = new ArrayList<>();
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
    for (PropertyEdge propertyEdge : this.arguments) {
      targets.add((Expression) propertyEdge.getEnd());
    }
    return Collections.unmodifiableList(targets);
  }

  @NonNull
  public List<PropertyEdge> getArgumentsPropertyEdge() {
    return this.arguments;
  }

  public void addArgument(Expression expression) {
    PropertyEdge propertyEdge = new PropertyEdge(this, expression);
    propertyEdge.addProperty(Properties.INDEX, this.arguments.size());
    this.arguments.add(propertyEdge);
  }

  public void setArguments(List<Expression> arguments) {
    this.arguments = PropertyEdge.transformIntoPropertyEdgeList(arguments, this, true);
  }

  @NonNull
  public List<FunctionDeclaration> getInvokes() {
    List<FunctionDeclaration> targets = new ArrayList<>();
    for (PropertyEdge propertyEdge : this.invokes) {
      targets.add((FunctionDeclaration) propertyEdge.getEnd());
    }
    return Collections.unmodifiableList(targets);
  }

  public List<PropertyEdge> getInvokesPropertyEdge() {
    return this.invokes;
  }

  public void setInvokes(List<FunctionDeclaration> invokes) {
    PropertyEdge.getTarget(this.invokes, true)
        .forEach(
            i -> {
              ((FunctionDeclaration) i).unregisterTypeListener(this);
              Util.detachCallParameters((FunctionDeclaration) i, this.getArguments());
              this.removePrevDFG(i);
            });
    this.invokes = PropertyEdge.transformIntoPropertyEdgeList(invokes, this, true);
    invokes.forEach(
        i -> {
          i.registerTypeListener(this);
          Util.attachCallParameters(i, this.getArguments());
          this.addPrevDFG(i);
        });
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
              .map(pe -> (FunctionDeclaration) pe.getEnd())
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
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("base", base)
        .toString();
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
        && Objects.equals(arguments, that.arguments)
        && Objects.equals(this.getArguments(), that.getArguments())
        && Objects.equals(invokes, that.invokes)
        && Objects.equals(this.getInvokes(), that.getInvokes())
        && Objects.equals(base, that.base);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
