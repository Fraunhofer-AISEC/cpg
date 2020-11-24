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

package de.fraunhofer.aisec.cpg.graph.declarations;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.Relationship;

public class EnumDeclaration extends Declaration {

  @Relationship(value = "ENTRIES", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge> entries = new ArrayList<>();

  @Relationship(value = "SUPER_TYPES", direction = "OUTGOING")
  private List<PropertyEdge> superTypes = new ArrayList<>();

  @Relationship private Set<RecordDeclaration> superTypeDeclarations = new HashSet<>();

  public List<PropertyEdge> getEntriesPropertyEdge() {
    return this.entries;
  }

  public List<EnumConstantDeclaration> getEntries() {
    List<EnumConstantDeclaration> target = new ArrayList<>();
    for (PropertyEdge propertyEdge : this.entries) {
      target.add((EnumConstantDeclaration) propertyEdge.getEnd());
    }
    return Collections.unmodifiableList(target);
  }

  public void setEntries(List<EnumConstantDeclaration> entries) {
    this.entries = PropertyEdge.transformIntoPropertyEdgeList(entries, this, true);
  }

  public List<Type> getSuperTypes() {
    List<Type> target = new ArrayList<>();
    for (PropertyEdge propertyEdge : this.superTypes) {
      target.add((Type) propertyEdge.getEnd());
    }
    return Collections.unmodifiableList(target);
  }

  public List<PropertyEdge> getSuperTypesPropertyEdge() {
    return this.superTypes;
  }

  public void setSuperTypes(List<Type> superTypes) {
    this.superTypes = PropertyEdge.transformIntoPropertyEdgeList(superTypes, this, true);
  }

  public Set<RecordDeclaration> getSuperTypeDeclarations() {
    return superTypeDeclarations;
  }

  public void setSuperTypeDeclarations(Set<RecordDeclaration> superTypeDeclarations) {
    this.superTypeDeclarations = superTypeDeclarations;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("entries", entries)
        .toString();
  }
}
