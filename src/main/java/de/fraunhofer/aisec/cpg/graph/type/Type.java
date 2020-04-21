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

package de.fraunhofer.aisec.cpg.graph.type;

import de.fraunhofer.aisec.cpg.graph.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.neo4j.ogm.annotation.typeconversion.Convert;

/**
 * Abstract Type, describing all possible SubTypes, i.e. all different Subtypes are complient with
 * this class. Contains information which is included in any Type such as name, storage, qualifier
 * and origin
 */
public abstract class Type extends Node {
  public static final String UNKNOWN_TYPE_STRING = "UNKNOWN";

  /**
   * Describes Storage specifier of variables. Depending on the storage specifier, variables are
   * stored in different parts e.g. in C/CPP AUTO stores the variable on the stack whereas static in
   * the bss section
   */
  public enum Storage {
    AUTO,
    EXTERN,
    STATIC,
    REGISTER
  }

  public Storage getStorage() {
    return storage;
  }

  public void setStorage(Storage storage) {
    this.storage = storage;
  }

  public Qualifier getQualifier() {
    return qualifier;
  }

  public void setQualifier(Qualifier qualifier) {
    this.qualifier = qualifier;
  }

  public Origin getTypeOrigin() {
    return origin;
  }

  public void setTypeOrigin(Origin origin) {
    this.origin = origin;
  }

  /** Type Origin describes where the Type information came from */
  public enum Origin {
    RESOLVED,
    DATAFLOW,
    GUESSED,
    UNRESOLVED
  }

  /**
   * Describes possible qualifiers that can be added to the type in order to modify its behavior.
   * Supported: const (final), volatile, restrict, atomic
   */
  public static class Qualifier {
    private boolean isConst; // C, C++, Java
    private boolean isVolatile; // C, C++, Java
    private boolean isRestrict; // C
    private boolean isAtomic; // C

    public Qualifier(boolean isConst, boolean isVolatile, boolean isRestrict, boolean isAtomic) {
      this.isConst = isConst;
      this.isVolatile = isVolatile;
      this.isRestrict = isRestrict;
      this.isAtomic = isAtomic;
    }

    public Qualifier() {
      this.isConst = false;
      this.isVolatile = false;
      this.isRestrict = false;
      this.isAtomic = false;
    }

    public boolean isConst() {
      return isConst;
    }

    public void setConst(boolean aConst) {
      isConst = aConst;
    }

    public boolean isVolatile() {
      return isVolatile;
    }

    public void setVolatile(boolean aVolatile) {
      isVolatile = aVolatile;
    }

    public boolean isRestrict() {
      return isRestrict;
    }

    public void setRestrict(boolean restrict) {
      isRestrict = restrict;
    }

    public boolean isAtomic() {
      return isAtomic;
    }

    public void setAtomic(boolean atomic) {
      isAtomic = atomic;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Qualifier)) return false;
      Qualifier qualifier = (Qualifier) o;
      return isConst == qualifier.isConst
          && isVolatile == qualifier.isVolatile
          && isRestrict == qualifier.isRestrict
          && isAtomic == qualifier.isAtomic;
    }

    @Override
    public int hashCode() {
      return Objects.hash(isConst, isVolatile, isRestrict, isAtomic);
    }

    @Override
    public String toString() {
      return "Qualifier{"
          + "isConst="
          + isConst
          + ", isVolatile="
          + isVolatile
          + ", isRestrict="
          + isRestrict
          + ", isAtomic="
          + isAtomic
          + '}';
    }
  }

  protected Storage
      storage; // auto, extern, static, register --> consider auto as modifier or auto to
  // automatically infer the value

  @Convert(QualifierConverter.class)
  protected Qualifier qualifier;

  protected Origin origin;

  public Type() {
    this.name = "";
    this.storage = Storage.AUTO;
    this.qualifier = new Qualifier(false, false, false, false);
  }

  public Type(String typeName) {
    this.name = typeName;
  }

  public Type(Type type) {
    this.storage = type.storage;
    this.name = type.name;
    this.qualifier =
        new Qualifier(
            type.qualifier.isConst,
            type.qualifier.isVolatile,
            type.qualifier.isRestrict,
            type.qualifier.isAtomic);
    this.origin = type.origin;
  }

  public Type(String typeName, Storage storage, Qualifier qualifier) {
    this.name = typeName;
    this.storage = storage;
    this.qualifier = qualifier;
    this.origin = Origin.UNRESOLVED;
  }

  /**
   * @return Creates a reference to the current Type. E.g. when creating a pointer to an existing
   *     ObjectType
   */
  public abstract Type reference();

  /**
   * @return Dereferences the current Type by resolving the reference. E.g. when dereferencing an
   *     pointer type we obtain the type the pointer is pointing towards
   */
  public abstract Type dereference();

  /** @return Resolves one dereference Level, i.e. the Type is dereferenced one time */
  public abstract Type getFollowingLevel();

  /**
   * Obtain the root Type Element for a Type Chain (follows Pointer and ReferenceTypes until Object-
   * Incomplete- FunctionPtrType is reached
   *
   * @return root Type
   */
  public abstract Type getRoot();

  public void setRoot(Type newRoot) {
    return;
  }

  /** @return Creates an exact copy of the curent type (chain) */
  public abstract Type duplicate();

  public String getTypeName() {
    return name;
  }

  /**
   * @return number of steps that are required in order to traverse the type chain until the root is
   *     reached
   */
  public int getReferenceDepth() {
    return 0;
  }

  public void setAdditionalTypeKeywords(String keywords) {
    List<String> separatedKeywords = TypeParser.separate(keywords);
    for (String keyword : separatedKeywords) {
      if (TypeParser.isKnownSpecifier(keyword)) {
        if (TypeParser.isStorageSpecifier(keyword)) {
          List<String> specifiers = new ArrayList<>();
          specifiers.add(keyword);
          this.setStorage(TypeParser.calcStorage(specifiers));
        } else if (TypeParser.isQualifierSpecifier(keyword)) {
          List<String> qualifiers = new ArrayList<>();
          qualifiers.add(keyword);
          this.setQualifier(TypeParser.calcQualifier(qualifiers, this.getQualifier()));
        }
      }
    }
  }

  /**
   * @return True if the Type parameter t is a FirstOrderType (Root of a chain) -> not a Pointer or
   *     RefrenceType
   */
  public boolean isFirstOrderType() {
    return this instanceof ObjectType
        || this instanceof UnknownType
        || this instanceof FunctionPointerType
        || this instanceof IncompleteType;
  }

  /**
   * Required for possibleSubTypes to check if the new Type should be considered a subtype or not
   *
   * @param t
   * @return True if the parameter t is equal to the current type (this)
   */
  public boolean isSimilar(Type t) {
    if (this.equals(t)) {
      return true;
    }

    return this.getTypeName().equals(t.getTypeName());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Type)) return false;
    Type type = (Type) o;
    return Objects.equals(name, type.name)
        && storage == type.storage
        && Objects.equals(qualifier, type.qualifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, storage, qualifier);
  }

  @Override
  public String toString() {
    return "Type{"
        + "typeName='"
        + name
        + '\''
        + ", storage="
        + storage
        + ", qualifier="
        + qualifier
        + ", origin="
        + origin
        + '}';
  }
}
