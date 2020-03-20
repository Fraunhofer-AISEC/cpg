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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Type {

  public static final String UNKNOWN_TYPE_STRING = "UNKNOWN";
  private static final Logger LOGGER = LoggerFactory.getLogger(Type.class);
  // Compile regex patterns once and for all.
  private static final Pattern DOUBLE_COLON = Pattern.compile("::");
  private static final Pattern START_WITH_BACKSLASH = Pattern.compile("\\*");
  private static final Pattern TYPE_FROM_STRING =
      Pattern.compile(
          "(?:(?<modifier>[a-zA-Z]*) )?(?<type>[a-zA-Z0-9_$.<>]*)(?<adjustment>[\\[\\]*&\\s]*)?");
  /** The type of the declaration. */
  protected String type = UNKNOWN_TYPE_STRING;
  /** Specifies whether this node has any type adjustments, such as a pointer or reference. */
  protected String typeAdjustment = "";
  /** Specifies whether this node has any type modifiers, such as const, final, ... */
  protected String typeModifier = "";
  /** Where does this type come from? Provided by a symbol solver, guessed from imports etc? */
  protected Origin typeOrigin = Origin.UNRESOLVED;
  /** Hint for resolving function pointer calls */
  protected boolean isFunctionPtr = false;

  @Id @GeneratedValue private Long id;

  private Type() {
    // type is initialized as unknown per default
  }

  public Type(String type) {
    setFrom(type);
  }

  public Type(String type, String typeAdjustment) {
    this(type);
    this.typeAdjustment = typeAdjustment;
  }

  public Type(String type, String typeAdjustment, boolean isFunctionPtr) {
    this.type = type;
    this.typeAdjustment = typeAdjustment;
    this.isFunctionPtr = isFunctionPtr;
  }

  public Type(String type, String typeAdjustment, Origin typeOrigin) {
    this(type, typeAdjustment);
    this.typeOrigin = typeOrigin;
  }

  public Type(String type, Origin typeOrigin) {
    this(type);
    this.typeOrigin = typeOrigin;
  }

  public Type(Type src) {
    setFrom(src);
  }

  public static Type getUnknown() {
    // we need to return a new type every time, as we cannot rely on the fact that nobody will
    // touch our global "UNKNOWN" type...
    return new Type();
  }

  private static String clean(String type) {
    if (type.contains("?")
        || type.contains("org.eclipse.cdt.internal.core.dom.parser.ProblemType@")) {
      return UNKNOWN_TYPE_STRING;
    }
    type = type.replaceAll("^struct ", "");
    type = type.replaceAll("^const struct ", "");
    type = type.replaceAll(" const ", " ");
    // remove artifacts from unidentified C++ namespaces
    type = type.replaceAll("\\{.*}::", "");
    // remove irrelevant array sizes cluttering the type name
    type = type.replaceAll("\\[[ \\d]*]", "[]");
    // remove function signature info
    type = type.replaceAll("\\(.*\\)", "");
    // unify separator
    type = type.replace("::", ".");
    return type.strip();
  }

  /**
   * Creates a new type from a string representation. This is basically syntactic sugar for calling
   * the constructor.
   *
   * @param string the string representation of the type
   * @return the type
   */
  public static Type createFrom(String string) {
    return new Type(string);
  }

  public String getTypeName() {
    return type;
  }

  public void setTypeName(String type) {
    this.type = type;
  }

  public void setFrom(Type src) {
    this.type = src.type;
    this.typeAdjustment = src.typeAdjustment;
    this.typeModifier = src.typeModifier;
    this.typeOrigin = src.typeOrigin;
    this.isFunctionPtr = src.isFunctionPtr;
  }

  private void setFrom(String string) {
    String cleaned = clean(string);
    Matcher matcher = TYPE_FROM_STRING.matcher(cleaned);
    if (matcher.matches()) {
      String typeName = matcher.group("type");
      String adjustment = matcher.group("adjustment");
      adjustment = adjustment == null ? "" : adjustment.replace(" ", "");
      String modifier = matcher.group("modifier");
      modifier = modifier == null ? "" : modifier;

      setTypeName(typeName);
      setTypeAdjustment(adjustment);
      setTypeModifier(modifier);
    } else {
      LOGGER.warn("Type regex does not match for {} (cleaned version of {})", cleaned, string);
      setTypeName(cleaned);
    }
    setFrom(TypeManager.getInstance().resolvePossibleTypedef(this));
  }

  public boolean hasTypeAdjustment() {
    return this.typeAdjustment != null;
  }

  public String getTypeAdjustment() {
    return typeAdjustment;
  }

  public void setTypeAdjustment(String typeAdjustment) {
    this.typeAdjustment = typeAdjustment;
  }

  /**
   * Sometimes the actual type adjustment might not be apparent (e.g. if intptr_t is defined as
   * int*, we do not have an adjustment explicitly set for intptr_t, but int* sure has one). So for
   * these cases we should just add any adjustments as appropriate, in order to not kill implicit
   * ones
   *
   * @param typeAdjustment
   */
  public void addTypeAdjustment(String typeAdjustment) {
    this.typeAdjustment += typeAdjustment;
  }

  public Origin getTypeOrigin() {
    return typeOrigin;
  }

  public void setTypeOrigin(Origin typeOrigin) {
    this.typeOrigin = typeOrigin;
  }

  public Type reference() {
    return new Type(this.type, "*" + this.typeAdjustment, this.isFunctionPtr);
  }

  public Type dereference() {
    // dereferencing an array results in basically the same as with a pointer
    return new Type(
        this.type, this.typeAdjustment.replaceFirst("(\\[])|(\\*)", ""), this.isFunctionPtr);
  }

  public void setFunctionPtr(boolean functionPtr) {
    isFunctionPtr = functionPtr;
  }

  public boolean isFunctionPtr() {
    return isFunctionPtr;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (hasTypeModifier()) {
      sb.append(getTypeModifier());
      sb.append(" ");
    }
    sb.append(getTypeName());
    if (hasTypeAdjustment()) {
      sb.append(getTypeAdjustment());
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Type
        && Objects.equals(((Type) other).type, this.type)
        && Objects.equals(((Type) other).typeModifier, this.typeModifier)
        && Objects.equals(((Type) other).typeAdjustment, this.typeAdjustment)
        && ((Type) other).isFunctionPtr == this.isFunctionPtr;
  }

  @Override
  public int hashCode() {
    int ret = 0;
    if (type != null) {
      ret += 17 * type.hashCode();
    }
    if (hasTypeModifier()) {
      ret += 17 * typeModifier.hashCode();
    }
    if (hasTypeAdjustment()) {
      ret += 19 * typeAdjustment.hashCode();
    }
    return ret;
  }

  public boolean hasTypeModifier() {
    return !typeModifier.isEmpty();
  }

  public String getTypeModifier() {
    return typeModifier;
  }

  public void setTypeModifier(String typeModifier) {
    this.typeModifier = typeModifier;
  }

  public enum Origin {
    RESOLVED,
    DATAFLOW,
    GUESSED,
    UNRESOLVED
  }
}
