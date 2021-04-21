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
package de.fraunhofer.aisec.cpg.graph.statements;

import static de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.unwrap;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.neo4j.ogm.annotation.Relationship;

/**
 * A {@link Statement}, which contains a single or multiple {@link Declaration}s. Usually these
 * statements occur if one defines a variable within a function body. A function body is a {@link
 * CompoundStatement}, which can only contain other statements, but not declarations. Therefore
 * declarations are wrapped in a {@link DeclarationStatement}.
 */
public class DeclarationStatement extends Statement {

  /**
   * The list of declarations declared or defined by this statement. It is always a list, even if it
   * only contains a single {@link Declaration}.
   */
  @Relationship(value = "DECLARATIONS", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge<Declaration>> declarations = new ArrayList<>();

  public Declaration getSingleDeclaration() {
    return isSingleDeclaration() ? this.declarations.get(0).getEnd() : null;
  }

  public boolean isSingleDeclaration() {
    return this.declarations.size() == 1;
  }

  public void setSingleDeclaration(Declaration declaration) {
    this.declarations.clear();
    PropertyEdge<Declaration> propertyEdge = new PropertyEdge<>(this, declaration);
    propertyEdge.addProperty(Properties.INDEX, 0);
    this.declarations.add(propertyEdge);
  }

  public <T extends Declaration> T getSingleDeclarationAs(Class<T> clazz) {
    return clazz.cast(this.getSingleDeclaration());
  }

  @NonNull
  public List<Declaration> getDeclarations() {
    return unwrap(this.declarations, true);
  }

  public List<PropertyEdge<Declaration>> getDeclarationsPropertyEdge() {
    return this.declarations;
  }

  public void setDeclarations(List<Declaration> declarations) {
    this.declarations = PropertyEdge.transformIntoOutgoingPropertyEdgeList(declarations, this);
  }

  public void addToPropertyEdgeDeclaration(@NonNull Declaration declaration) {
    PropertyEdge<Declaration> propertyEdge = new PropertyEdge<>(this, declaration);
    propertyEdge.addProperty(Properties.INDEX, this.declarations.size());
    this.declarations.add(propertyEdge);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("declarations", declarations)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DeclarationStatement)) {
      return false;
    }
    DeclarationStatement that = (DeclarationStatement) o;
    return super.equals(that)
        && Objects.equals(this.getDeclarations(), that.getDeclarations())
        && PropertyEdge.propertyEqualsList(declarations, that.declarations);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
