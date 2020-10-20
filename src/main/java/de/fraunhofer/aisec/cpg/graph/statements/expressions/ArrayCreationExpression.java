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
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.neo4j.ogm.annotation.Relationship;

/**
 * Expressions of the form <code>new Type[]</code> that represents the creation of an array, mostly
 * used in combination with a {@link VariableDeclaration}.
 */
public class ArrayCreationExpression extends Expression implements TypeListener {

  /**
   * The initializer of the expression, if present. Many languages, such as Java, either specify
   * {@link #dimensions} or an initializer.
   */
  @SubGraph("AST")
  private InitializerListExpression initializer;

  /**
   * Specifies the dimensions of the array that is to be created. Many languages, such as Java,
   * either explicitly specify dimensions or an {@link #initializer}, which is used to calculate
   * dimensions. In the graph, this will NOT be done.
   */
  @Relationship(value = "dimensions", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge> dimensions = new ArrayList<>();

  public InitializerListExpression getInitializer() {
    return initializer;
  }

  public void setInitializer(InitializerListExpression initializer) {
    if (this.initializer != null) {
      this.initializer.unregisterTypeListener(this);
      this.removePrevDFG(initializer);
    }
    this.initializer = initializer;
    if (initializer != null) {
      initializer.registerTypeListener(this);
      this.addPrevDFG(initializer);
    }
  }

  @NonNull
  public List<Expression> getDimensions() {
    List<Expression> targets = new ArrayList<>();
    for (PropertyEdge propertyEdge : this.dimensions) {
      targets.add((Expression) propertyEdge.getEnd());
    }
    return Collections.unmodifiableList(targets);
  }

  public void addDimension(Expression expression) {
    PropertyEdge propertyEdge = new PropertyEdge(this, expression);
    propertyEdge.addProperty(Properties.Index, this.dimensions.size());
    this.dimensions.add(propertyEdge);
  }

  @NonNull
  public List<PropertyEdge> getDimensionsPropertyEdge() {
    return this.dimensions;
  }

  public void setDimensions(List<Expression> dimensions) {
    this.dimensions = PropertyEdge.transformIntoPropertyEdgeList(dimensions, this, true);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ArrayCreationExpression)) {
      return false;
    }
    ArrayCreationExpression that = (ArrayCreationExpression) o;
    return super.equals(that)
        && Objects.equals(initializer, that.initializer)
        && Objects.equals(dimensions, that.dimensions)
        && Objects.equals(this.getDimensions(), that.getDimensions());
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
  public int hashCode() {
    return super.hashCode();
  }
}
