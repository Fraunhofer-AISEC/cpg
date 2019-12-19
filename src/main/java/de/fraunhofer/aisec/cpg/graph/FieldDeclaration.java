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

package de.fraunhofer.aisec.cpg.graph;

import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener;
import de.fraunhofer.aisec.cpg.graph.Type.Origin;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Declaration of a field within a {@link RecordDeclaration}. It contains the modifiers associated
 * with the field as well as an initializer {@link Expression} which provides an initial value for
 * the field.
 */
public class FieldDeclaration extends ValueDeclaration implements TypeListener {

  @SubGraph("AST")
  @Nullable
  private Expression initializer;

  private List<String> modifiers = new ArrayList<>();

  public FieldDeclaration() {}

  private FieldDeclaration(VariableDeclaration declaration) {
    this.name = declaration.getName();
    this.code = declaration.getCode();
    this.region = declaration.getRegion();
    this.type = declaration.getType();
    this.initializer = declaration.getInitializer();
  }

  public static FieldDeclaration from(VariableDeclaration declaration) {
    return new FieldDeclaration(declaration);
  }

  @Nullable
  public Expression getInitializer() {
    return initializer;
  }

  public void setInitializer(Expression initializer) {
    if (this.initializer != null) {
      this.initializer.unregisterTypeListener(this);
      this.removePrevDFG(this.initializer);
    }
    this.initializer = initializer;
    if (initializer != null) {
      initializer.registerTypeListener(this);
      this.addPrevDFG(initializer);
    }
  }

  public List<String> getModifiers() {
    return modifiers;
  }

  public void setModifiers(List<String> modifiers) {
    this.modifiers = modifiers;
  }

  @Override
  public void typeChanged(HasType src, Type oldType) {
    Type previous = this.type;
    setType(src.getType());
    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(HasType src, Set<Type> oldSubTypes) {
    Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
    subTypes.addAll(src.getPossibleSubTypes());
    setPossibleSubTypes(subTypes);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("initializer", initializer)
        .append("modifiers", modifiers)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FieldDeclaration)) {
      return false;
    }
    FieldDeclaration that = (FieldDeclaration) o;
    return super.equals(that)
        && Objects.equals(initializer, that.initializer)
        && Objects.equals(modifiers, that.modifiers);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
