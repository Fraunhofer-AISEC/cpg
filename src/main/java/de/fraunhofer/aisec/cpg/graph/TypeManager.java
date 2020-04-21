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

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import de.fraunhofer.aisec.cpg.graph.type.UnknownType;
import de.fraunhofer.aisec.cpg.passes.scopes.RecordScope;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class TypeManager {

  private static final List<String> primitiveTypeNames =
      List.of("byte", "short", "int", "long", "float", "double", "boolean", "char");
  private static TypeManager INSTANCE = new TypeManager();

  public enum Language {
    JAVA,
    CXX
  }

  private Map<String, RecordDeclaration> typeToRecord = new HashMap<>();
  private Map<Type, List<Type>> typeState =
      new HashMap<>(); // Stores all the unique types ObjectType as Key and Reference-/PointerTypes
  // as Values
  private List<Type> firstOrderTypes = new ArrayList<>();
  private List<Type> secondOrderTypes = new ArrayList<>();
  private LanguageFrontend frontend;

  public Map<Type, List<Type>> getTypeState() {
    return typeState;
  }

  /**
   * Ensures that two different Types that are created at different Points are still the same object
   * in order to only store one node into the database
   *
   * @param type newly created Type
   * @return If the same type was already stored in the typeState Map the stored one is returned. In
   *     the other case the parameter type is stored into the map and the parameter type is returned
   */
  public Type obtainType(Type type) {
    Type root = type.getRoot();
    if (root.equals(type) && typeState.containsKey(type)) {
      for (Type t : typeState.keySet()) {
        if (t.equals(type)) {
          return t;
        }
      }
    } else {
      addType(type);
      return type;
    }

    if (typeState.containsKey(root)) {
      List<Type> references = typeState.get(root);
      for (Type r : references) {
        if (r.equals(type)) {
          return r;
        }
      }
      addType(type);
      return type;
    }

    addType(type);
    return type;
  }

  public Type registerType(Type t) {
    if (t.isFirstOrderType()) {
      this.firstOrderTypes.add(t);
    } else {
      this.secondOrderTypes.add(t);
      registerType(t.getFollowingLevel());
    }
    return t;
  }

  public List<Type> getFirstOrderTypes() {
    return firstOrderTypes;
  }

  public List<Type> getSecondOrderTypes() {
    return secondOrderTypes;
  }

  /**
   * Responsible for storing new types into typeState
   *
   * @param type new type
   */
  private void addType(Type type) {
    Type root = type.getRoot();
    if (root.equals(type)) {
      // This is a rootType and is included in the map as key with empty references
      if (!typeState.containsKey(type)) {
        typeState.put(type, new ArrayList<>());
        return;
      }
    }

    // ReferencesTypes
    if (typeState.containsKey(root)) {
      if (!typeState.get(root).contains(type)) {
        typeState.get(root).add(type);
        addType(type.getFollowingLevel());
      }

    } else {
      addType(type.getRoot());
      addType(type);
    }
  }

  private TypeManager() {}

  public static TypeManager getInstance() {
    return INSTANCE;
  }

  public void setLanguageFrontend(LanguageFrontend frontend) {
    this.frontend = frontend;
  }

  public boolean isPrimitive(Type type) {
    return primitiveTypeNames.contains(type.getTypeName());
  }

  public boolean isUnknown(Type type) {
    return type instanceof UnknownType;
  }

  public Optional<Type> getCommonType(Collection<Type> types) {

    // TODO SH handle pointer

    if (types.isEmpty()) {
      return Optional.empty();
    } else if (types.size() == 1) {
      return Optional.of(types.iterator().next());
    }
    typeToRecord =
        frontend.getScopeManager()
            .getUniqueScopesThat(RecordScope.class::isInstance, s -> s.getAstNode().getName())
            .stream()
            .map(s -> (RecordDeclaration) s.getAstNode())
            .collect(Collectors.toMap(RecordDeclaration::getName, Function.identity()));

    List<Set<Ancestor>> allAncestors =
        types.stream()
            .map(t -> typeToRecord.getOrDefault(t.getTypeName(), null))
            .filter(Objects::nonNull)
            .map(r -> getAncestors(r, 0))
            .collect(Collectors.toList());

    // normalize/reverse depth: roots start at 0, increasing on each level
    for (Set<Ancestor> ancestors : allAncestors) {
      Optional<Ancestor> farthest =
          ancestors.stream().max(Comparator.comparingInt(Ancestor::getDepth));
      if (farthest.isPresent()) {
        int maxDepth = farthest.get().getDepth();
        ancestors.forEach(a -> a.setDepth(maxDepth - a.getDepth()));
      }
    }

    Set<Ancestor> commonAncestors = new HashSet<>();
    for (int i = 0; i < allAncestors.size(); i++) {
      if (i == 0) {
        commonAncestors.addAll(allAncestors.get(i));
      } else {
        commonAncestors.retainAll(allAncestors.get(i));
      }
    }

    Optional<Ancestor> lca =
        commonAncestors.stream().max(Comparator.comparingInt(Ancestor::getDepth));
    return lca.map(a -> TypeParser.createFrom(a.getRecord().getName()));
  }

  private Set<Ancestor> getAncestors(RecordDeclaration record, int depth) {
    if (record.getSuperTypes().isEmpty()) {
      return Set.of(new Ancestor(record, depth));
    }
    Set<Ancestor> ancestors =
        record.getSuperTypes().stream()
            .map(s -> typeToRecord.getOrDefault(s.getTypeName(), null))
            .filter(Objects::nonNull)
            .map(s -> getAncestors(s, depth + 1))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    ancestors.add(new Ancestor(record, depth));
    return ancestors;
  }

  public Language getLanguage() {
    if (frontend instanceof JavaLanguageFrontend) {
      return Language.JAVA;
    } else {
      return Language.CXX;
    }
  }

  public boolean isSupertypeOf(Type superType, Type subType) {
    if (superType.getReferenceDepth() != subType.getReferenceDepth()) {
      return false;
    }

    Optional<Type> commonType = getCommonType(new HashSet<>(List.of(superType, subType)));
    if (commonType.isPresent()) {
      return commonType.get().equals(superType);
    } else {
      // If array depth matches: check whether these are types from the standard library
      try {
        Class superCls = Class.forName(superType.getTypeName());
        Class subCls = Class.forName(subType.getTypeName());
        return superCls.isAssignableFrom(subCls);
      } catch (ClassNotFoundException e) {
        // Not in the class path, can't help here
        return false;
      }
    }
  }

  public void cleanup() {
    this.frontend = null;
    this.typeToRecord.clear();
  }

  private class Ancestor {

    private RecordDeclaration record;
    private int depth;

    public Ancestor(RecordDeclaration record, int depth) {
      this.record = record;
      this.depth = depth;
    }

    public RecordDeclaration getRecord() {
      return record;
    }

    public int getDepth() {
      return depth;
    }

    public void setDepth(int depth) {
      this.depth = depth;
    }

    @Override
    public int hashCode() {
      return Objects.hash(record);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Ancestor) {
        return ((Ancestor) obj).getRecord().equals(this.getRecord())
            && ((Ancestor) obj).getDepth() == this.getDepth();
      } else {
        return false;
      }
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this, Node.TO_STRING_STYLE)
          .append("record", record.getName())
          .append("depth", depth)
          .toString();
    }
  }
}
