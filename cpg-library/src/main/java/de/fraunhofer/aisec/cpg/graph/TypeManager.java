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

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.cpp2.CXXLanguageFrontend2;
import de.fraunhofer.aisec.cpg.frontends.golang.GoLanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.llvm.LLVMIRLanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.typescript.TypeScriptLanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TemplateDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TypedefDeclaration;
import de.fraunhofer.aisec.cpg.graph.types.*;
import de.fraunhofer.aisec.cpg.helpers.Util;
import de.fraunhofer.aisec.cpg.passes.scopes.RecordScope;
import de.fraunhofer.aisec.cpg.passes.scopes.Scope;
import de.fraunhofer.aisec.cpg.passes.scopes.TemplateScope;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeManager {

  private static final Logger log = LoggerFactory.getLogger(TypeManager.class);

  private static final List<String> primitiveTypeNames =
      List.of("byte", "short", "int", "long", "float", "double", "boolean", "char");
  private static final Pattern funPointerPattern =
      Pattern.compile("\\(?\\*(?<alias>[^()]+)\\)?\\(.*\\)");
  @NonNull private static TypeManager INSTANCE = new TypeManager();
  private static boolean typeSystemActive = true;

  public enum Language {
    JAVA,
    CXX,
    GO,
    PYTHON,
    TYPESCRIPT,
    LLVM_IR,
    UNKNOWN
  }

  @NonNull
  private final Map<HasType, List<Type>> typeCache =
      Collections.synchronizedMap(new IdentityHashMap<>());

  @NonNull
  private Map<String, RecordDeclaration> typeToRecord =
      Collections.synchronizedMap(new HashMap<>());

  /**
   * Stores the relationship between parameterized RecordDeclarations (e.g. Classes using Generics)
   * to the ParameterizedType to be able to resolve the Type of the fields, since ParameterizedTypes
   * are unique to the RecordDeclaration and are not merged.
   */
  @NonNull
  private final Map<RecordDeclaration, List<ParameterizedType>> recordToTypeParameters =
      Collections.synchronizedMap(new HashMap<>());

  @NonNull
  private final Map<TemplateDeclaration, List<ParameterizedType>> templateToTypeParameters =
      Collections.synchronizedMap(new HashMap<>());

  @NonNull
  private final Map<Type, List<Type>> typeState =
      Collections.synchronizedMap(new HashMap<>()); // Stores all the unique types ObjectType as
  // Key and
  // Reference-/PointerTypes
  // as Values

  private final Set<Type> firstOrderTypes = Collections.synchronizedSet(new HashSet<>());
  private final Set<Type> secondOrderTypes = Collections.synchronizedSet(new HashSet<>());

  /**
   * The language frontend that is currently active. This can be null, e.g. if we are executed in
   * tests.
   */
  @org.jetbrains.annotations.Nullable private LanguageFrontend frontend;

  private boolean noFrontendWarningIssued = false;

  public static void reset() {
    INSTANCE = new TypeManager();
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
        if (parameterizedType.getName().equals(name)) {
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
        if (parameterizedType.getName().equals(name)) {
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
  @NonNull
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
      TemplateDeclaration template = (TemplateDeclaration) scope.getAstNode();
      ParameterizedType parameterizedType = getTypeParameter(template, name);
      if (parameterizedType != null) {
        return parameterizedType;
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
      TemplateDeclaration templateDeclaration, String typeName) {
    ParameterizedType parameterizedType = getTypeParameter(templateDeclaration, typeName);
    if (parameterizedType != null) {
      return parameterizedType;
    } else {
      parameterizedType = new ParameterizedType(typeName);
      addTypeParameter(templateDeclaration, parameterizedType);
      return parameterizedType;
    }
  }

  @NonNull
  public Map<Type, List<Type>> getTypeState() {
    return typeState;
  }

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
    return firstOrderTypes.stream().anyMatch(type -> type.getRoot().getName().equals(name));
  }

  private TypeManager() {}

  public static TypeManager getInstance() {
    return INSTANCE;
  }

  public static boolean isTypeSystemActive() {
    return typeSystemActive;
  }

  public static void setTypeSystemActive(boolean active) {
    typeSystemActive = active;
  }

  public Map<HasType, List<Type>> getTypeCache() {
    return typeCache;
  }

  public synchronized void cacheType(HasType node, Type type) {
    if (!isUnknown(type)) {
      var list = typeCache.computeIfAbsent(node, n -> new ArrayList<>());

      // make sure, that the list is really modifiable and not an empty immutable list
      if (list.isEmpty()) {
        list = new ArrayList<>();
        typeCache.put(node, list);
      }

      if (!list.contains(type)) {
        list.add(type);
      }
    }
  }

  public void setLanguageFrontend(@NonNull LanguageFrontend frontend) {
    this.frontend = frontend;
  }

  public boolean isPrimitive(Type type) {
    return primitiveTypeNames.contains(type.getTypeName());
  }

  public boolean isUnknown(Type type) {
    return type instanceof UnknownType;
  }

  /**
   * @param generics
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
    if (type instanceof ObjectType
        && newType instanceof ObjectType
        && ((ObjectType) type).getGenerics() != null
        && ((ObjectType) newType).getGenerics() != null
        && type.getName().equals(newType.getName())) {
      return containsParameterizedType(((ObjectType) newType).getGenerics())
          && !(containsParameterizedType(((ObjectType) type).getGenerics()));
    }
    return false;
  }

  private Optional<Type> rewrapType(
      Type type,
      int depth,
      PointerType.PointerOrigin pointerOrigin,
      boolean reference,
      ReferenceType referenceType) {
    if (depth > 0) {
      for (int i = 0; i < depth; i++) {
        type = type.reference(pointerOrigin);
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
    int depth = 0;
    int counter = 0;
    boolean reference = false;
    PointerType.PointerOrigin pointerOrigin = null;
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
        pointerOrigin = ((PointerType) t).getPointerOrigin();
      }
    }

    wrapState.setDepth(depth);
    wrapState.setPointerOrigin(pointerOrigin);
    wrapState.setReference(reference);
    wrapState.setReferenceType(referenceType);

    if (unwrappedTypes.isEmpty() && !original.isEmpty()) {
      return original;
    } else {
      return unwrappedTypes;
    }
  }

  @NonNull
  public Optional<Type> getCommonType(@NonNull Collection<Type> types) {

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
          wrapState.getDepth(),
          wrapState.getPointerOrigin(),
          wrapState.isReference(),
          wrapState.getReferenceType());
    }
    typeToRecord =
        frontend
            .getScopeManager()
            .filterScopesDistinctBy(RecordScope.class::isInstance, s -> s.getAstNode().getName())
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
    Optional<Type> commonType = lca.map(a -> TypeParser.createFrom(a.getRecord().getName(), true));

    Type finalType;
    if (commonType.isPresent()) {
      finalType = commonType.get();
    } else {
      return commonType;
    }

    return rewrapType(
        finalType,
        wrapState.getDepth(),
        wrapState.getPointerOrigin(),
        wrapState.isReference(),
        wrapState.getReferenceType());
  }

  private Set<Ancestor> getAncestors(RecordDeclaration recordDeclaration, int depth) {
    if (recordDeclaration.getSuperTypes().isEmpty()) {
      HashSet<Ancestor> ret = new HashSet<>();
      ret.add(new Ancestor(recordDeclaration, depth));
      return ret;
    }
    Set<Ancestor> ancestors =
        recordDeclaration.getSuperTypes().stream()
            .map(s -> typeToRecord.getOrDefault(s.getTypeName(), null))
            .filter(Objects::nonNull)
            .map(s -> getAncestors(s, depth + 1))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    ancestors.add(new Ancestor(recordDeclaration, depth));
    return ancestors;
  }

  @NonNull
  public Language getLanguage() {
    if (frontend instanceof JavaLanguageFrontend) {
      return Language.JAVA;
    } else if (frontend instanceof CXXLanguageFrontend
        || frontend instanceof CXXLanguageFrontend2) {
      return Language.CXX;
    } else if (frontend instanceof GoLanguageFrontend) {
      return Language.GO;
    } else if (frontend instanceof PythonLanguageFrontend) {
      return Language.PYTHON;
    } else if (frontend instanceof TypeScriptLanguageFrontend) {
      return Language.TYPESCRIPT;
    } else if (frontend instanceof LLVMIRLanguageFrontend) {
      return Language.LLVM_IR;
    }

    log.error(
        "Unknown language (frontend: {})",
        frontend != null ? frontend.getClass().getSimpleName() : null);
    return Language.UNKNOWN;
  }

  @Nullable
  public LanguageFrontend getFrontend() {
    return frontend;
  }

  public boolean isSupertypeOf(Type superType, Type subType) {
    if (superType.getReferenceDepth() != subType.getReferenceDepth()) {
      return false;
    }

    // arrays and pointers match in C++
    if (checkArrayAndPointer(superType, subType)) {
      return true;
    }

    // ObjectTypes can be passed as ReferenceTypes
    if (superType instanceof ReferenceType) {
      return isSupertypeOf(((ReferenceType) superType).getElementType(), subType);
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
      return first.getTypeName().equals(second.getTypeName()) && first.isSimilar(second);
    } else {
      return false;
    }
  }

  public void cleanup() {
    this.frontend = null;
    this.typeToRecord.clear();
  }

  private Type getTargetType(Type currTarget, String alias) {
    if (alias.contains("(") && alias.contains("*")) {
      // function pointer
      return TypeParser.createFrom(currTarget.getName() + " " + alias, true);
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

  private Type getAlias(String alias) {
    if (alias.contains("(") && alias.contains("*")) {
      // function pointer
      Matcher matcher = funPointerPattern.matcher(alias);
      if (matcher.find()) {
        return TypeParser.createIgnoringAlias(matcher.group("alias"));
      } else {
        log.error("Could not find alias name in function pointer typedef: {}", alias);
        return TypeParser.createIgnoringAlias(alias);
      }
    } else {
      alias = alias.split("\\[")[0];
      alias = alias.replace("*", "");
      return TypeParser.createIgnoringAlias(alias);
    }
  }

  /**
   * Handles type defs. It is necessary to specify which language frontend this belongs to, because
   * in a parallel run, the {@link TypeManager} does not have access to the "current" one.
   *
   * @param frontend
   * @param rawCode
   */
  public void handleTypedef(LanguageFrontend frontend, String rawCode) {
    String cleaned = rawCode.replaceAll("(typedef|;)", "").strip();
    if (cleaned.startsWith("struct")) {
      handleStructTypedef(frontend, rawCode, cleaned);
    } else if (Util.containsOnOuterLevel(cleaned, ',')) {
      handleMultipleAliases(frontend, rawCode, cleaned);
    } else {
      List<String> parts = Util.splitLeavingParenthesisContents(cleaned, " \t\r\n");
      if (parts.size() < 2) {
        log.error("Typedef contains no whitespace to split on: {}", rawCode);
        return;
      }
      // typedefs can be wildly mixed around, but the last item is always the alias to be defined
      Type target =
          TypeParser.createFrom(
              Util.removeRedundantParentheses(String.join(" ", parts.subList(0, parts.size() - 1))),
              true);
      handleSingleAlias(frontend, rawCode, target, parts.get(parts.size() - 1));
    }
  }

  private void handleMultipleAliases(LanguageFrontend frontend, String rawCode, String cleaned) {
    List<String> parts = Util.splitLeavingParenthesisContents(cleaned, ",");
    String[] splitFirst = parts.get(0).split("\\s+");
    if (splitFirst.length < 2) {
      log.error("Cannot find out target type for {}", rawCode);
      return;
    }
    Type target = TypeParser.createFrom(splitFirst[0], true);
    parts.set(0, parts.get(0).substring(splitFirst[0].length()).strip());
    for (String part : parts) {
      handleSingleAlias(frontend, rawCode, target, part);
    }
  }

  private void handleStructTypedef(LanguageFrontend frontend, String rawCode, String cleaned) {
    int endOfStruct = cleaned.lastIndexOf('}');
    if (endOfStruct + 1 < cleaned.length()) {
      List<String> parts =
          Util.splitLeavingParenthesisContents(cleaned.substring(endOfStruct + 1), ",");
      Optional<String> name =
          parts.stream().filter(p -> !p.contains("*") && !p.contains("[")).findFirst();
      if (name.isPresent()) {
        Type target = TypeParser.createIgnoringAlias(name.get());
        for (String part : parts) {
          if (!part.equals(name.get())) {
            handleSingleAlias(frontend, rawCode, target, part);
          }
        }
      } else {
        log.error("Could not identify struct name: {}", rawCode);
      }
    } else {
      log.error("No alias found for struct typedef: {}", rawCode);
    }
  }

  public void handleSingleAlias(
      LanguageFrontend frontend, String rawCode, Type target, String aliasString) {
    String cleanedPart = Util.removeRedundantParentheses(aliasString);
    Type currTarget = getTargetType(target, cleanedPart);
    Type alias = getAlias(cleanedPart);

    if (alias instanceof SecondOrderType) {
      Type chain = alias.duplicate();
      chain.setRoot(currTarget);
      currTarget = chain;
      currTarget.refreshNames();
      alias = alias.getRoot();
    }

    TypedefDeclaration typedef = NodeBuilder.newTypedefDeclaration(currTarget, alias, rawCode);

    if (frontend == null) {
      if (!noFrontendWarningIssued) {
        log.warn("No frontend available. Be aware that typedef resolving cannot currently be done");
        noFrontendWarningIssued = true;
      }
      return;
    }

    frontend.getScopeManager().addTypedef(typedef);
  }

  public Type resolvePossibleTypedef(Type alias) {
    if (frontend == null) {
      if (!noFrontendWarningIssued) {
        log.warn("No frontend available. Be aware that typedef resolving cannot currently be done");
        noFrontendWarningIssued = true;
      }
      return alias;
    }

    Type toCheck = alias.getRoot();

    Type finalToCheck = toCheck;
    Optional<Type> applicable =
        frontend.getScopeManager().getCurrentTypedefs().stream()
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
