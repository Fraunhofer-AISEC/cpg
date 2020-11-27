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
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import de.fraunhofer.aisec.cpg.helpers.Util;
import de.fraunhofer.aisec.cpg.passes.CallResolver;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.Relationship;

/**
 * Represents a call to a constructor, usually as an initializer.
 *
 * <ul>
 *   <li>In C++ this can be part of a variable declaration plus initialization, such as <code>
 *       int a(5);</code> or as part of a {@link NewExpression}.
 *   <li>In Java, it is the initializer of a {@link NewExpression}.
 * </ul>
 */
public class ConstructExpression extends Expression implements TypeListener {

  /**
   * The link to the {@link ConstructorDeclaration}. This is populated by the {@link
   * de.fraunhofer.aisec.cpg.passes.CallResolver} later.
   */
  @PopulatedByPass(CallResolver.class)
  private ConstructorDeclaration constructor;

  /** The {@link Declaration} of the type this expression instantiates. */
  @PopulatedByPass(CallResolver.class)
  private Declaration instantiates;

  /** The list of argument {@link Expression}s passed the constructor. */
  @Relationship(value = "ARGUMENTS", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge<Expression>> arguments = new ArrayList<>();

  public Declaration getInstantiates() {
    return instantiates;
  }

  public void setInstantiates(Declaration instantiates) {
    this.instantiates = instantiates;
    if (instantiates != null) {
      setType(TypeParser.createFrom(instantiates.getName(), true));
    }
  }

  public ConstructorDeclaration getConstructor() {
    return constructor;
  }

  public void setConstructor(ConstructorDeclaration constructor) {
    if (this.constructor != null) {
      this.constructor.unregisterTypeListener(this);
      Util.detachCallParameters(this.constructor, this.getArguments());
      this.removePrevDFG(this.constructor);
    }
    this.constructor = constructor;
    if (constructor != null) {
      constructor.registerTypeListener(this);
      Util.attachCallParameters(constructor, this.getArguments());
      this.addPrevDFG(constructor);
    }
  }

  public List<Expression> getArguments() {
    List<Expression> target = new ArrayList<>();
    for (PropertyEdge<Expression> propertyEdge : this.arguments) {
      target.add((Expression) propertyEdge.getEnd());
    }
    return Collections.unmodifiableList(target);
  }

  public List<PropertyEdge<Expression>> getArgumentsPropertyEdge() {
    return this.arguments;
  }

  public void setArguments(List<Expression> arguments) {
    this.arguments = PropertyEdge.transformIntoOutgoingPropertyEdgeList(arguments, this);
  }

  public void addArgument(Expression argument) {
    PropertyEdge<Expression> propertyEdge = new PropertyEdge<>(this, argument);
    propertyEdge.addProperty(Properties.INDEX, this.arguments.size());
    this.arguments.add(propertyEdge);
  }

  public List<Type> getSignature() {
    return getArguments().stream().map(Expression::getType).collect(Collectors.toList());
  }

  @Override
  public void typeChanged(HasType src, HasType root, Type oldType) {

    Type previous = this.type;
    setType(src.getPropagationType(), root);
    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Type.Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(HasType src, HasType root, Set<Type> oldSubTypes) {
    Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
    subTypes.addAll(src.getPossibleSubTypes());
    setPossibleSubTypes(subTypes, root);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("constructor", constructor)
        .append("instantiates", instantiates)
        .append("arguments", arguments)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ConstructExpression)) {
      return false;
    }
    ConstructExpression that = (ConstructExpression) o;
    return super.equals(that)
        && Objects.equals(constructor, that.constructor)
        && Objects.equals(arguments, that.arguments)
        && Objects.equals(this.getArguments(), that.getArguments());
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
