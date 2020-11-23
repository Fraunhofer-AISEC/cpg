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

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.Relationship;

public class DesignatedInitializerExpression extends Expression {

  @SubGraph("AST")
  private Expression rhs;

  @Relationship(value = "lhs", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge> lhs;

  public Expression getRhs() {
    return rhs;
  }

  public void setRhs(Expression rhs) {
    this.rhs = rhs;
  }

  public List<Expression> getLhs() {
    List<Expression> target = new ArrayList<>();
    for (PropertyEdge propertyEdge : this.lhs) {
      target.add((Expression) propertyEdge.getEnd());
    }
    return Collections.unmodifiableList(target);
  }

  public List<PropertyEdge> getLhsPropertyEdge() {
    return this.lhs;
  }

  public void setLhs(List<Expression> lhs) {
    this.lhs = PropertyEdge.transformIntoPropertyEdgeList(lhs, this, true);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("lhr", lhs)
        .append("rhs", rhs)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DesignatedInitializerExpression)) {
      return false;
    }
    DesignatedInitializerExpression that = (DesignatedInitializerExpression) o;
    return super.equals(that)
        && Objects.equals(rhs, that.rhs)
        && Objects.equals(lhs, that.lhs)
        && Objects.equals(this.getLhs(), that.getLhs());
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
