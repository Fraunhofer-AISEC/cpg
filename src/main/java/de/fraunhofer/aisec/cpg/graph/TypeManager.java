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
import de.fraunhofer.aisec.cpg.passes.scopes.RecordScope;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class TypeManager {

  private static final List<String> primitiveTypeNames =
      List.of("byte", "short", "int", "long", "float", "double", "boolean", "char");
  private static TypeManager INSTANCE = new TypeManager();

  private Map<String, RecordDeclaration> typeToRecord = new HashMap<>();
  private LanguageFrontend frontend;

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
    return isUnknown(type.getTypeName());
  }

  public boolean isUnknown(String type) {
    return type.contains(Type.UNKNOWN_TYPE_STRING)
        || type.contains("?")
        || type.equals("var")
        || type.equals("");
  }

  public Optional<Type> getCommonType(Collection<Type> types) {
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
    return lca.map(a -> new Type(a.getRecord().getName()));
  }

  private Set<Ancestor> getAncestors(RecordDeclaration record, int depth) {
    if (record.getSuperTypes().isEmpty()) {
      HashSet<Ancestor> ret = new HashSet<>();
      ret.add(new Ancestor(record, depth));
      return ret;
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

  public boolean isSupertypeOf(Type superType, Type subType) {
    if (!superType.getTypeAdjustment().equals(subType.getTypeAdjustment())) {
      return false;
    }

    // arrays and pointers match in C++
    if (checkArrayAndPointer(superType, subType) || checkArrayAndPointer(subType, superType)) {
      return true;
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

  private boolean checkArrayAndPointer(Type first, Type second) {
    int secondArrayDepth = StringUtils.countMatches(second.getTypeAdjustment(), "[]");
    int firstPointerDepth = StringUtils.countMatches(first.getTypeAdjustment(), '*');
    if (secondArrayDepth == firstPointerDepth) {
      return first.getTypeName().equals(second.getTypeName());
    } else {
      return false;
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
