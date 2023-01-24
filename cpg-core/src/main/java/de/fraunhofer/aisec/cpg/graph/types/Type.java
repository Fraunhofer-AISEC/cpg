/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.types;

import de.fraunhofer.aisec.cpg.frontends.Language;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.Name;
import de.fraunhofer.aisec.cpg.graph.NameKt;
import de.fraunhofer.aisec.cpg.graph.Node;
import java.util.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.neo4j.ogm.annotation.Relationship;

/**
 * Abstract Type, describing all possible SubTypes, i.e. all different Subtypes are compliant with
 * this class. Contains information which is included in any Type such as name, storage, qualifier
 * and origin
 */
public abstract class Type extends Node {
  public static final String UNKNOWN_TYPE_STRING = "UNKNOWN";

  @Relationship(value = "SUPER_TYPE", direction = "OUTGOING")
  @NotNull
  protected Set<Type> superTypes = new HashSet<>();

  protected boolean primitive = false;

  protected Origin origin;

  public Type() {
    this.setName(new Name(EMPTY_NAME, null, this.getLanguage()));
  }

  public Type(String typeName) {
    this.setName(NameKt.parseName(this.getLanguage(), typeName));
    this.origin = Origin.UNRESOLVED;
  }

  public Type(Type type) {
    this.setName(type.getName().clone());
    this.origin = type.origin;
  }

  public Type(String typeName, Language<? extends LanguageFrontend> language) {
    if (this instanceof FunctionType) {
      this.setName(new Name(typeName, null, language));
    } else {
      this.setName(NameKt.parseName(language, typeName));
    }
    this.setLanguage(language);
    this.origin = Origin.UNRESOLVED;
  }

  public Type(Name fullTypeName, Language<? extends LanguageFrontend> language) {
    this.setName(fullTypeName.clone());
    this.origin = Origin.UNRESOLVED;
    this.setLanguage(language);
  }

  /**
   * All direct supertypes of this type.
   *
   * @return
   */
  @NotNull
  public Set<Type> getSuperTypes() {
    return superTypes;
  }

  public Origin getTypeOrigin() {
    return origin;
  }

  public void setTypeOrigin(Origin origin) {
    this.origin = origin;
  }

  public boolean isPrimitive() {
    return primitive;
  }

  /** Type Origin describes where the Type information came from */
  public enum Origin {
    RESOLVED,
    DATAFLOW,
    GUESSED,
    UNRESOLVED
  }

  /**
   * @param pointer Reason for the reference (array of pointer)
   * @return Returns a reference to the current Type. E.g. when creating a pointer to an existing
   *     ObjectType
   */
  public abstract Type reference(PointerType.PointerOrigin pointer);

  /**
   * @return Dereferences the current Type by resolving the reference. E.g. when dereferencing a
   *     pointer type we obtain the type the pointer is pointing towards
   */
  public abstract Type dereference();

  public void refreshNames() {}

  /**
   * Obtain the root Type Element for a Type Chain (follows Pointer and ReferenceTypes until a
   * Object-, Incomplete-, or FunctionPtrType is reached).
   *
   * @return root Type
   */
  public Type getRoot() {
    if (this instanceof SecondOrderType) {
      return ((SecondOrderType) this).getElementType().getRoot();
    } else {
      return this;
    }
  }

  public void setRoot(Type newRoot) {
    if (this instanceof SecondOrderType) {
      if (((SecondOrderType) this).getElementType() instanceof SecondOrderType) {
        ((SecondOrderType) ((SecondOrderType) this).getElementType()).setElementType(newRoot);
      } else {
        ((SecondOrderType) this).setElementType(newRoot);
      }
    }
  }

  /**
   * @return Creates an exact copy of the current type (chain)
   */
  public abstract Type duplicate();

  public String getTypeName() {
    return getName().toString();
  }

  /**
   * @return number of steps that are required in order to traverse the type chain until the root is
   *     reached
   */
  public int getReferenceDepth() {
    return 0;
  }

  /**
   * @return True if the Type parameter t is a FirstOrderType (Root of a chain) and not a Pointer or
   *     ReferenceType
   */
  public boolean isFirstOrderType() {
    return this instanceof ObjectType
        || this instanceof UnknownType
        || this instanceof FunctionType
        // TODO(oxisto): convert FunctionPointerType to second order type
        || this instanceof FunctionPointerType
        || this instanceof IncompleteType
        || this instanceof ParameterizedType;
  }

  /**
   * Required for possibleSubTypes to check if the new Type should be considered a subtype or not
   *
   * @param t other type the similarity is checked with
   * @return True if the parameter t is equal to the current type (this)
   */
  public boolean isSimilar(Type t) {
    return this.equals(t) || this.getTypeName().equals(t.getTypeName());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Type type)) return false;
    return Objects.equals(getName(), type.getName())
        && Objects.equals(getLanguage(), type.getLanguage());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getLanguage());
  }

  @NotNull
  @Override
  public String toString() {
    return new ToStringBuilder(this, TO_STRING_STYLE).append("name", getName()).toString();
  }
}
