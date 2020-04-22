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
import de.fraunhofer.aisec.cpg.graph.type.*;
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

  private Optional<Type> rewrapType(Type type, int depth) {
    if (depth > 0) {
      for (int i = 0; i < depth; i++) {
        type = type.reference();
      }
    }

    return Optional.of(type);
  }

  public Optional<Type> getCommonType(Collection<Type> types) {

    boolean sameType =
        types.stream().map(t -> t.getClass().getCanonicalName()).collect(Collectors.toSet()).size()
            == 1;
    if (!sameType) {
      // No commonType for different Types
      return Optional.empty();
    }

    // TODO SH add support for pointer/referencetype
    Set<Type> unwrappedTypes = new HashSet<>();
    int depth = 0;
    int counter = 0;
    boolean reference = false;
    for (Type t : types) {
      if (t instanceof PointerType) {
        if (counter == 0) {
          depth = t.getReferenceDepth();
          counter++;
        }
        if (t.getReferenceDepth() != depth) {
          return Optional.empty();
        }
        unwrappedTypes.add(t.getRoot());
      }

      if (t instanceof ReferenceType) {
        unwrappedTypes.add(((ReferenceType) t).getReference());
        reference = true;
      }
    }

    types = unwrappedTypes;

    if (types.isEmpty()) {
      return Optional.empty();
    } else if (types.size() == 1) {
      return rewrapType(types.iterator().next(), depth);
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
    Optional<Type> commonType = lca.map(a -> TypeParser.createFrom(a.getRecord().getName()));

    Type finalType;
    if (commonType.isPresent()) {
      finalType = commonType.get();
    } else {
      return commonType;
    }

    return rewrapType(finalType, depth);
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
