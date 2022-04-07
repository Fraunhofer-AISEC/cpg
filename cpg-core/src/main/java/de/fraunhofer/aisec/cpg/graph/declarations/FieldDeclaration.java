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

import de.fraunhofer.aisec.cpg.graph.HasInitializer;
import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.Nullable;
import org.neo4j.ogm.annotation.Relationship;

/**
 * Declaration of a field within a {@link RecordDeclaration}. It contains the modifiers associated
 * with the field as well as an initializer {@link Expression} which provides an initial value for
 * the field.
 */
public class FieldDeclaration extends ValueDeclaration implements TypeListener, HasInitializer {

  @SubGraph("AST")
  @Nullable
  private Expression initializer;

  /** Specifies, whether this field declaration is also a definition, i.e. has an initializer. */
  private boolean isDefinition = false;

  /** If this is only a declaration, this provides a link to the definition of the field. */
  @Relationship(value = "DEFINES")
  private FieldDeclaration definition = this;

  /** @see VariableDeclaration#implicitInitializerAllowed */
  private boolean implicitInitializerAllowed = false;

  public boolean isImplicitInitializerAllowed() {
    return implicitInitializerAllowed;
  }

  public void setImplicitInitializerAllowed(boolean implicitInitializerAllowed) {
    this.implicitInitializerAllowed = implicitInitializerAllowed;
  }

  private boolean isArray = false;

  public boolean isArray() {
    return isArray;
  }

  public void setIsArray(boolean isArray) {
    this.isArray = isArray;
  }

  private List<String> modifiers = new ArrayList<>();

  @Nullable
  public Expression getInitializer() {
    return initializer;
  }

  public void setInitializer(Expression initializer) {
    if (this.initializer != null) {
      setIsDefinition(true);
      this.initializer.unregisterTypeListener(this);
      this.removePrevDFG(this.initializer);
      if (this.initializer instanceof TypeListener) {
        this.unregisterTypeListener((TypeListener) this.initializer);
      }
    }
    this.initializer = initializer;
    if (initializer != null) {
      initializer.registerTypeListener(this);
      this.addPrevDFG(initializer);
      if (initializer instanceof TypeListener) {
        this.registerTypeListener((TypeListener) initializer);
      }
    }
  }

  public FieldDeclaration getDefinition() {
    return isDefinition ? this : definition;
  }

  public boolean isDefinition() {
    return isDefinition;
  }

  public void setIsDefinition(boolean definition) {
    this.isDefinition = definition;
  }

  public void setDefinition(FieldDeclaration definition) {
    this.definition = definition;
  }

  public List<String> getModifiers() {
    return modifiers;
  }

  public void setModifiers(List<String> modifiers) {
    this.modifiers = modifiers;
  }

  @Override
  public void typeChanged(HasType src, Collection<HasType> root, Type oldType) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }
    if (!TypeManager.getInstance().isUnknown(this.type)
        && src.getPropagationType().equals(oldType)) {
      return;
    }

    Type previous = this.type;
    Type newType;
    if (src == initializer && initializer instanceof InitializerListExpression) {
      // Init list is seen as having an array type, but can be used ambiguously. It can be either
      // used to initialize an array, or to initialize some objects. If it is used as an
      // array initializer, we need to remove the array/pointer layer from the type, otherwise it
      // can be ignored once we have a type
      if (isArray) {
        newType = src.getType();
      } else if (!TypeManager.getInstance().isUnknown(this.type)) {
        return;
      } else {
        newType = src.getType().dereference();
      }
    } else {
      newType = src.getPropagationType();
    }

    setType(newType, root);
    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Type.Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(
      HasType src, Collection<HasType> root, Set<Type> oldSubTypes) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }
    Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
    subTypes.addAll(src.getPossibleSubTypes());
    setPossibleSubTypes(subTypes, root);
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
