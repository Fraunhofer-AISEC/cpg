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
package de.fraunhofer.aisec.cpg.graph;

import static de.fraunhofer.aisec.cpg.graph.DeclarationBuilderKt.newTypedefDeclaration;

import de.fraunhofer.aisec.cpg.ScopeManager;
import de.fraunhofer.aisec.cpg.frontends.Language;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.cpp.CLanguage;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope;
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope;
import de.fraunhofer.aisec.cpg.graph.scopes.Scope;
import de.fraunhofer.aisec.cpg.graph.scopes.TemplateScope;
import de.fraunhofer.aisec.cpg.graph.types.*;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeManager {

  private static final Logger log = LoggerFactory.getLogger(TypeManager.class);

  // TODO: document/remove this regexp, merge with other pattern
  private static final Pattern funPointerPattern =
      Pattern.compile("\\(?\\*(?<alias>[^()]+)\\)?\\(.*\\)");
  @NotNull private static TypeManager instance = new TypeManager();
  private static boolean typeSystemActive = true;

  @NotNull
  private final Map<HasType, List<Type>> typeCache =
      Collections.synchronizedMap(new IdentityHashMap<>());

  @NotNull
  private final Map<Type, RecordDeclaration> typeToRecord =
      Collections.synchronizedMap(new HashMap<>());

  /**
   * Stores the relationship between parameterized RecordDeclarations (e.g. Classes using Generics)
   * to the ParameterizedType to be able to resolve the Type of the fields, since ParameterizedTypes
   * are unique to the RecordDeclaration and are not merged.
   */
  @NotNull
  private final Map<RecordDeclaration, List<ParameterizedType>> recordToTypeParameters =
      Collections.synchronizedMap(new HashMap<>());

  @NotNull
  private final Map<TemplateDeclaration, List<ParameterizedType>> templateToTypeParameters =
      Collections.synchronizedMap(new HashMap<>());

  @NotNull
  private final Map<Type, List<Type>> typeState =
      Collections.synchronizedMap(new HashMap<>()); // Stores all the unique types ObjectType as
  // Key and
  // Reference-/PointerTypes
  // as Values

  private final Set<Type> firstOrderTypes = Collections.synchronizedSet(new HashSet<>());
  private final Set<Type> secondOrderTypes = Collections.synchronizedSet(new HashSet<>());

  public static void reset() {
    instance = new TypeManager();
  }

  /**
   * @param recordDeclaration that is instantiated by a template containing parameterizedtypes
   * @param name of the ParameterizedType we want to get
   * @return ParameterizedType if there is a parameterized type defined in the recordDeclaration
   *     with matching name, null instead
   */
  @Nullable
  public ParameterizedType getTypeParameter(RecordDeclaration recordDeclaration, String name) {
    if (this.recordToTypeParameters.containsKey(recordDeclaration)) {
      for (ParameterizedType parameterizedType :
          this.recordToTypeParameters.get(recordDeclaration)) {
        if (parameterizedType.getName().toString().equals(name)) {
          return parameterizedType;
        }
      }
    }

    return null;
  }

  /**
   * Adds a List of ParameterizedType to {@link TypeManager#recordToTypeParameters}
   *
   * @param recordDeclaration will be stored as key for the map
   * @param typeParameters List containing all ParameterizedTypes used by the recordDeclaration and
   *     will be stored as value in the map
   */
  public void addTypeParameter(
      RecordDeclaration recordDeclaration, List<ParameterizedType> typeParameters) {
    this.recordToTypeParameters.put(recordDeclaration, typeParameters);
  }

  /**
   * Searches {@link TypeManager#templateToTypeParameters} for ParameterizedTypes that were defined
   * in a template matching the provided name
   *
   * @param templateDeclaration that includes the ParameterizedType we are looking for
   * @param name name of the ParameterizedType we are looking for
   * @return
   */
  @Nullable
  public ParameterizedType getTypeParameter(TemplateDeclaration templateDeclaration, String name) {
    if (this.templateToTypeParameters.containsKey(templateDeclaration)) {
      for (ParameterizedType parameterizedType :
          this.templateToTypeParameters.get(templateDeclaration)) {
        if (parameterizedType.getName().toString().equals(name)) {
          return parameterizedType;
        }
      }
    }
    return null;
  }

  /**
   * @param templateDeclaration
   * @return List containing all ParameterizedTypes the templateDeclaration defines. If the
   *     templateDeclaration is not registered, an empty list is returned.
   */
  @NotNull
  public List<ParameterizedType> getAllParameterizedType(TemplateDeclaration templateDeclaration) {
    if (this.templateToTypeParameters.containsKey(templateDeclaration)) {
      return this.templateToTypeParameters.get(templateDeclaration);
    }
    return new ArrayList<>();
  }

  /**
   * Searches for ParameterizedType if the scope is a TemplateScope. If not we search the parent
   * scope until we reach the top.
   *
   * @param scope in which we are searching for the defined ParameterizedTypes
   * @param name of the ParameterizedType
   * @return ParameterizedType that is found within the scope (or any parent scope) and matches the
   *     provided name. Null if we reach the top of the scope without finding a matching
   *     ParameterizedType
   */
  public ParameterizedType searchTemplateScopeForDefinedParameterizedTypes(
      Scope scope, String name) {
    if (scope instanceof TemplateScope) {
      var node = scope.getAstNode();

      // We need an additional check here, because of parsing or other errors, the AST node might
      // not necessarily be a template declaration.
      if (node instanceof TemplateDeclaration templateDeclaration) {
        ParameterizedType parameterizedType = getTypeParameter(templateDeclaration, name);
        if (parameterizedType != null) {
          return parameterizedType;
        }
      }
    }

    return scope.getParent() != null
        ? searchTemplateScopeForDefinedParameterizedTypes(scope.getParent(), name)
        : null;
  }

  /**
   * Adds ParameterizedType to the {@link TypeManager#templateToTypeParameters} to be able to
   * resolve this type when it is used
   *
   * @param templateDeclaration key for {@link TypeManager#templateToTypeParameters}
   * @param typeParameter ParameterizedType we want to register
   */
  public void addTypeParameter(
      TemplateDeclaration templateDeclaration, ParameterizedType typeParameter) {
    if (this.templateToTypeParameters.containsKey(templateDeclaration)) {
      this.templateToTypeParameters.get(templateDeclaration).add(typeParameter);
    } else {
      List<ParameterizedType> typeParameters = new ArrayList<>();
      typeParameters.add(typeParameter);
      this.templateToTypeParameters.put(templateDeclaration, typeParameters);
    }
  }

  /**
   * Check if a ParameterizedType with name typeName is already registered. If so we return the
   * already created ParameterizedType. If not, we create and return a new ParameterizedType
   *
   * @param templateDeclaration in which the ParameterizedType is defined
   * @param typeName name of the ParameterizedType
   * @return
   */
  public ParameterizedType createOrGetTypeParameter(
      TemplateDeclaration templateDeclaration,
      String typeName,
      Language<? extends LanguageFrontend> language) {
    ParameterizedType parameterizedType = getTypeParameter(templateDeclaration, typeName);
    if (parameterizedType != null) {
      return parameterizedType;
    } else {
      parameterizedType = new ParameterizedType(typeName, language);
      addTypeParameter(templateDeclaration, parameterizedType);
      return parameterizedType;
    }
  }

  @NotNull
  public Map<Type, List<Type>> getTypeState() {
    return typeState;
  }

  @NotNull
  public <T extends Type> T registerType(T t) {
    if (t.isFirstOrderType()) {
      this.firstOrderTypes.add(t);
    } else {
      this.secondOrderTypes.add(t);
      registerType(((SecondOrderType) t).getElementType());
    }
    return t;
  }

  public Set<Type> getFirstOrderTypes() {
    return firstOrderTypes;
  }

  public Set<Type> getSecondOrderTypes() {
    return secondOrderTypes;
  }

  public boolean typeExists(String name) {
    return firstOrderTypes.stream()
        .anyMatch(type -> type.getRoot().getName().toString().equals(name));
  }

  private TypeManager() {}

  public static @NotNull TypeManager getInstance() {
    return instance;
  }

  public static boolean isTypeSystemActive() {
    return typeSystemActive;
  }

  public static void setTypeSystemActive(boolean active) {
    typeSystemActive = active;
  }

  @NotNull
  public Map<HasType, List<Type>> getTypeCache() {
    return typeCache;
  }

  public synchronized void cacheType(HasType node, Type type) {
    if (!isUnknown(type)) {
      List<Type> types = typeCache.computeIfAbsent(node, n -> new ArrayList<>());
      if (!types.contains(type)) {
        types.add(type);
      }
    }
  }

  public static boolean isPrimitive(Type type, Language<? extends LanguageFrontend> language) {
    return language.getPrimitiveTypeNames().contains(type.getTypeName());
  }

  public boolean isUnknown(Type type) {
    return type instanceof UnknownType;
  }

  /**
   * @param generics the list of parameter types
   * @return true if the generics contain parameterized Types
   */
  public boolean containsParameterizedType(List<Type> generics) {
    for (Type t : generics) {
      if (t instanceof ParameterizedType) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param type oldType that we want to replace
   * @param newType newType
   * @return true if an objectType with instantiated generics is replaced by the same objectType
   *     with parameterizedTypes as generics false otherwise
   */
  public boolean stopPropagation(Type type, Type newType) {
    if (type instanceof ObjectType typeObjectType
        && newType instanceof ObjectType newObjectType
        && type.getName().equals(newType.getName())) {
      return containsParameterizedType(newObjectType.getGenerics())
          && !(containsParameterizedType(typeObjectType.getGenerics()));
    }
    return false;
  }

  private Optional<Type> rewrapType(
      Type type,
      int depth,
      PointerType.PointerOrigin[] pointerOrigins,
      boolean reference,
      ReferenceType referenceType) {
    if (depth > 0) {
      for (int i = depth - 1; i >= 0; i--) {
        type = type.reference(pointerOrigins[i]);
      }
    }
    if (reference) {
      referenceType.setElementType(type);
      return Optional.of(referenceType);
    }
    return Optional.of(type);
  }

  private Set<Type> unwrapTypes(Collection<Type> types, WrapState wrapState) {
    // TODO Performance: This method is called very often (for each setType()) and does four
    // iterations over "types". Reduce number of iterations.
    Set<Type> original = new HashSet<>(types);
    Set<Type> unwrappedTypes = new HashSet<>();
    PointerType.PointerOrigin[] pointerOrigins = new PointerType.PointerOrigin[0];
    int depth = 0;
    int counter = 0;
    boolean reference = false;
    ReferenceType referenceType = null;

    Type t1 = types.stream().findAny().orElse(null);

    if (t1 instanceof ReferenceType) {
      for (Type t : types) {
        referenceType = (ReferenceType) t;
        if (!referenceType.isSimilar(t)) {
          return Collections.emptySet();
        }
        unwrappedTypes.add(((ReferenceType) t).getElementType());
        reference = true;
      }
      types = unwrappedTypes;
    }

    Type t2 = types.stream().findAny().orElse(null);

    if (t2 instanceof PointerType) {
      for (Type t : types) {
        if (counter == 0) {
          depth = t.getReferenceDepth();
          counter++;
        }
        if (t.getReferenceDepth() != depth) {
          return Collections.emptySet();
        }
        unwrappedTypes.add(t.getRoot());

        pointerOrigins = new PointerType.PointerOrigin[depth];
        var containedType = t2;
        int i = 0;
        pointerOrigins[i] = ((PointerType) containedType).getPointerOrigin();
        while (containedType instanceof PointerType) {
          containedType = ((PointerType) containedType).getElementType();
          if (containedType instanceof PointerType) {
            pointerOrigins[++i] = ((PointerType) containedType).getPointerOrigin();
          }
        }
      }
    }

    wrapState.depth = depth;
    wrapState.setPointerOrigin(pointerOrigins);
    wrapState.setReference(reference);
    wrapState.referenceType = referenceType;

    if (unwrappedTypes.isEmpty() && !original.isEmpty()) {
      return original;
    } else {
      return unwrappedTypes;
    }
  }

  /**
   * This function is a relict from the old ages. It iterates through a collection of types and
   * returns the type they have in *common*. For example, if two types `A` and `B` both derive from
   * the interface `C`` then `C` would be returned. Because this contains some legacy code that does
   * crazy stuff, we need access to scope information, so we can build a map between type
   * information and their record declarations. We want to get rid of that in the future.
   *
   * @param types the types to compare
   * @param provider a {@link ScopeProvider}.
   * @return the common type
   */
  @NotNull
  public Optional<Type> getCommonType(@NotNull Collection<Type> types, ScopeProvider provider) {
    // TODO: Documentation needed.
    boolean sameType =
        types.stream().map(t -> t.getClass().getCanonicalName()).collect(Collectors.toSet()).size()
            == 1;
    if (!sameType) {
      // No commonType for different Types
      return Optional.empty();
    }
    WrapState wrapState = new WrapState();

    types = unwrapTypes(types, wrapState);

    if (types.isEmpty()) {
      return Optional.empty();
    } else if (types.size() == 1) {
      return rewrapType(
          types.iterator().next(),
          wrapState.depth,
          wrapState.pointerOrigins,
          wrapState.isReference(),
          wrapState.referenceType);
    }

    var scope = provider.getScope();

    if (scope == null) {
      return Optional.empty();
    }

    // We need to find the global scope
    var globalScope = provider.getScope().getGlobalScope();
    if (globalScope == null) {
      return Optional.empty();
    }

    for (var child : globalScope.getChildren()) {
      if (child instanceof RecordScope && child.getAstNode() instanceof RecordDeclaration) {
        typeToRecord.put(
            ((RecordDeclaration) child.getAstNode()).toType(),
            (RecordDeclaration) child.getAstNode());
      }

      // HACKY HACK HACK
      if (child instanceof NameScope) {
        for (var child2 : child.getChildren()) {
          if (child2 instanceof RecordScope && child2.getAstNode() instanceof RecordDeclaration) {
            typeToRecord.put(
                ((RecordDeclaration) child2.getAstNode()).toType(),
                (RecordDeclaration) child2.getAstNode());
          }
        }
      }
    }

    List<Set<Ancestor>> allAncestors =
        types.stream()
            .map(t -> typeToRecord.getOrDefault(t, null))
            .filter(Objects::nonNull)
            .map(r -> getAncestors(r, 0))
            .toList();

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
        Set<Ancestor> others = allAncestors.get(i);
        Set<Ancestor> newCommonAncestors = new HashSet<>();
        // like Collection#retainAll but swaps relevant items out if the other set's matching
        // ancestor has a higher depth
        for (Ancestor curr : commonAncestors) {
          Optional<Ancestor> toRetain =
              others.stream()
                  .filter(a -> a.equals(curr))
                  .map(a -> curr.getDepth() >= a.getDepth() ? curr : a)
                  .findFirst();
          toRetain.ifPresent(newCommonAncestors::add);
        }
        commonAncestors = newCommonAncestors;
      }
    }

    Optional<Ancestor> lca =
        commonAncestors.stream().max(Comparator.comparingInt(Ancestor::getDepth));
    Optional<Type> commonType =
        lca.map(a -> TypeParser.createFrom(a.getRecord().getName(), a.getRecord().getLanguage()));

    Type finalType;
    if (commonType.isPresent()) {
      finalType = commonType.get();
    } else {
      return commonType;
    }

    return rewrapType(
        finalType,
        wrapState.depth,
        wrapState.pointerOrigins,
        wrapState.isReference(),
        wrapState.referenceType);
  }

  private Set<Ancestor> getAncestors(RecordDeclaration recordDeclaration, int depth) {
    if (recordDeclaration.getSuperTypes().isEmpty()) {
      HashSet<Ancestor> ret = new HashSet<>();
      ret.add(new Ancestor(recordDeclaration, depth));
      return ret;
    }
    Set<Ancestor> ancestors =
        recordDeclaration.getSuperTypes().stream()
            .map(s -> typeToRecord.getOrDefault(s, null))
            .filter(Objects::nonNull)
            .map(s -> getAncestors(s, depth + 1))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    ancestors.add(new Ancestor(recordDeclaration, depth));
    return ancestors;
  }

  public boolean isSupertypeOf(Type superType, Type subType, MetadataProvider provider) {
    Language<?> language = null;

    if (superType instanceof UnknownType && subType instanceof UnknownType) return true;

    if (superType.getReferenceDepth() != subType.getReferenceDepth()) {
      return false;
    }

    if (provider instanceof LanguageProvider languageProvider) {
      language = languageProvider.getLanguage();
    }

    // arrays and pointers match in C/C++
    // TODO: Make this independent from the specific language
    if (language instanceof CLanguage && checkArrayAndPointer(superType, subType)) {
      return true;
    }

    // ObjectTypes can be passed as ReferenceTypes
    if (superType instanceof ReferenceType referenceType) {
      return isSupertypeOf(referenceType.getElementType(), subType, provider);
    }

    // We cannot proceed without a scope provider
    if (!(provider instanceof ScopeProvider scopeProvider)) {
      return false;
    }

    Optional<Type> commonType =
        getCommonType(new HashSet<>(List.of(superType, subType)), scopeProvider);
    if (commonType.isPresent()) {
      return commonType.get().equals(superType);
    } else {
      // If array depth matches: check whether these are types from the standard library
      try {
        Class<?> superCls = Class.forName(superType.getTypeName());
        Class<?> subCls = Class.forName(subType.getTypeName());
        return superCls.isAssignableFrom(subCls);
      } catch (ClassNotFoundException | NoClassDefFoundError e) {
        // Not in the class path or other linkage exception, can't help here
        return false;
      }
    }
  }

  public boolean checkArrayAndPointer(Type first, Type second) {
    int firstDepth = first.getReferenceDepth();
    int secondDepth = second.getReferenceDepth();
    if (firstDepth == secondDepth) {
      return first.getRoot().getName().equals(second.getRoot().getName())
          && first.isSimilar(second);
    } else {
      return false;
    }
  }

  public void cleanup() {
    this.typeToRecord.clear();
  }

  private Type getTargetType(Type currTarget, String alias) {
    if (alias.contains("(") && alias.contains("*")) {
      // function pointer
      return TypeParser.createFrom(currTarget.getName() + " " + alias, currTarget.getLanguage());
    } else if (alias.endsWith("]")) {
      // array type
      return currTarget.reference(PointerType.PointerOrigin.ARRAY);
    } else if (alias.contains("*")) {
      // pointer
      int depth = StringUtils.countMatches(alias, '*');
      for (int i = 0; i < depth; i++) {
        currTarget = currTarget.reference(PointerType.PointerOrigin.POINTER);
      }
      return currTarget;
    } else {
      return currTarget;
    }
  }

  private Type getAlias(String alias, @NotNull Language<? extends LanguageFrontend> language) {
    if (alias.contains("(") && alias.contains("*")) {
      // function pointer
      Matcher matcher = funPointerPattern.matcher(alias);
      if (matcher.find()) {
        return TypeParser.createIgnoringAlias(matcher.group("alias"), language);
      } else {
        log.error("Could not find alias name in function pointer typedef: {}", alias);
        return TypeParser.createIgnoringAlias(alias, language);
      }
    } else {
      alias = alias.split("\\[")[0];
      alias = alias.replace("*", "");
      return TypeParser.createIgnoringAlias(alias, language);
    }
  }

  /**
   * Creates a typedef / type alias in the form of a {@link TypedefDeclaration} to the scope manager
   * and returns it.
   *
   * @param frontend the language frontend
   * @param rawCode the raw code
   * @param target the target type
   * @param aliasString the alias / name of the typedef
   * @return the typedef declaration
   */
  @NotNull
  public Declaration createTypeAlias(
      @NotNull LanguageFrontend frontend, String rawCode, Type target, String aliasString) {
    String cleanedPart = Util.removeRedundantParentheses(aliasString);
    Type currTarget = getTargetType(target, cleanedPart);
    Type alias;
    alias = getAlias(cleanedPart, frontend.getLanguage());

    if (alias instanceof SecondOrderType) {
      Type chain = alias.duplicate();
      chain.setRoot(currTarget);
      currTarget = chain;
      currTarget.refreshNames();
      alias = alias.getRoot();
    }

    TypedefDeclaration typedef = newTypedefDeclaration(frontend, currTarget, alias, rawCode);

    frontend.getScopeManager().addTypedef(typedef);

    return typedef;
  }

  public Type resolvePossibleTypedef(Type alias, ScopeManager scopeManager) {
    Type finalToCheck = alias.getRoot();
    Optional<Type> applicable =
        scopeManager.getCurrentTypedefs().stream()
            .filter(t -> t.getAlias().getRoot().equals(finalToCheck))
            .findAny()
            .map(TypedefDeclaration::getType);

    if (applicable.isEmpty()) {
      return alias;
    } else {
      return TypeParser.reWrapType(alias, applicable.get());
    }
  }

  private static class Ancestor {

    private final RecordDeclaration recordDeclaration;
    private int depth;

    public Ancestor(RecordDeclaration recordDeclaration, int depth) {
      this.recordDeclaration = recordDeclaration;
      this.depth = depth;
    }

    public RecordDeclaration getRecord() {
      return recordDeclaration;
    }

    public int getDepth() {
      return depth;
    }

    public void setDepth(int depth) {
      this.depth = depth;
    }

    @Override
    public int hashCode() {
      return Objects.hash(recordDeclaration);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Ancestor)) {
        return false;
      }
      Ancestor ancestor = (Ancestor) o;
      return Objects.equals(recordDeclaration, ancestor.recordDeclaration);
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this, Node.TO_STRING_STYLE)
          .append("record", recordDeclaration.getName())
          .append("depth", depth)
          .toString();
    }
  }
}
