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

package de.fraunhofer.aisec.cpg.passes;

import static de.fraunhofer.aisec.cpg.helpers.Util.warnWithFileLocation;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.type.FunctionPointerType;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import de.fraunhofer.aisec.cpg.graph.type.UnknownType;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates new connections between the place where a variable is declared and where it is used.
 *
 * <p>A field access is modeled with a {@link MemberExpression}. After AST building, its base and
 * member references are set to {@link DeclaredReferenceExpression} stubs. This pass resolves those
 * references and makes the member point to the appropriate {@link FieldDeclaration} and the base to
 * the "this" {@link FieldDeclaration} of the containing class. It is also capable of resolving
 * references to fields that are inherited from a superclass and thus not declared in the actual
 * base class. When base or member declarations are not found in the graph, a new "dummy" {@link
 * FieldDeclaration} is being created that is then used to collect all usages to the same unknown
 * declaration. {@link DeclaredReferenceExpression} stubs are removed from the graph after being
 * resolved.
 *
 * <p>A local variable access is modeled directly with a {@link DeclaredReferenceExpression}. This
 * step of the pass doesn't remove the {@link DeclaredReferenceExpression} nodes like in the field
 * usage case but rather makes their "refersTo" point to the appropriate {@link ValueDeclaration}.
 *
 * @author samuel
 */
public class VariableUsageResolver extends Pass {

  private static final Logger log = LoggerFactory.getLogger(VariableUsageResolver.class);
  private Map<Type, List<Type>> superTypesMap = new HashMap<>();
  private Map<Type, RecordDeclaration> recordMap = new HashMap<>();
  private Map<Type, EnumDeclaration> enumMap = new HashMap<>();
  private TranslationUnitDeclaration currTu;
  private ScopedWalker walker;

  @Override
  public void cleanup() {
    this.superTypesMap.clear();
    if (this.recordMap != null) {
      this.recordMap.clear();
    }
    this.enumMap.clear();
  }

  @Override
  public void accept(TranslationResult result) {
    walker = new ScopedWalker();

    for (TranslationUnitDeclaration tu : result.getTranslationUnits()) {
      currTu = tu;
      walker.clearCallbacks();
      walker.registerHandler((currClass, parent, currNode) -> walker.collectDeclarations(currNode));
      walker.registerHandler(this::findRecordsAndEnums);
      walker.iterate(currTu);
    }

    Map<Type, List<Type>> currSuperTypes =
        recordMap.values().stream()
            .collect(
                Collectors.toMap(
                    r -> TypeParser.createFrom(r.getName(), true),
                    RecordDeclaration::getSuperTypes));
    superTypesMap.putAll(currSuperTypes);

    for (TranslationUnitDeclaration tu : result.getTranslationUnits()) {
      walker.clearCallbacks();
      walker.registerHandler(this::resolve);
      walker.iterate(tu);
    }
  }

  private void findRecordsAndEnums(Node node, RecordDeclaration curClass) {
    if (node instanceof RecordDeclaration) {
      Type type = TypeParser.createFrom(node.getName(), true);
      recordMap.putIfAbsent(type, (RecordDeclaration) node);
    } else if (node instanceof EnumDeclaration) {
      Type type = TypeParser.createFrom(node.getName(), true);
      enumMap.putIfAbsent(type, (EnumDeclaration) node);
    }
  }

  private Set<ValueDeclaration> resolveFunctionPtr(
      Type containingClass, DeclaredReferenceExpression reference) {
    FunctionPointerType fptrType;
    if (reference.getType() instanceof FunctionPointerType) {
      fptrType = (FunctionPointerType) reference.getType();
    } else {
      log.error("Can't resolve a function pointer without a function pointer type!");
      return Collections.emptySet();
    }
    Optional<FunctionDeclaration> target = Optional.empty();
    String functionName = reference.getName();
    Matcher matcher =
        Pattern.compile("(?:(?<class>.*)(?:\\.|::))?(?<function>.*)").matcher(reference.getName());
    if (matcher.matches()) {
      String cls = matcher.group("class");
      functionName = matcher.group("function");
      String finalFunctionName = functionName;
      if (cls == null) {
        target =
            walker.getAllDeclarationsForScope(reference).stream()
                .filter(FunctionDeclaration.class::isInstance)
                .map(FunctionDeclaration.class::cast)
                .filter(
                    d ->
                        d.getName().equals(finalFunctionName)
                            && d.getType().equals(fptrType.getReturnType())
                            && d.hasSignature(fptrType.getParameters()))
                .findFirst();
      } else {
        containingClass = TypeParser.createFrom(cls, true);
        if (recordMap.containsKey(containingClass)) {
          target =
              recordMap.get(containingClass).getMethods().stream()
                  .map(FunctionDeclaration.class::cast)
                  .filter(
                      f ->
                          f.getName().equals(finalFunctionName)
                              && f.getType().equals(fptrType.getReturnType())
                              && f.hasSignature(fptrType.getParameters()))
                  .findFirst();
        }
      }
    }

    Set<ValueDeclaration> targets = new HashSet<>();

    if (target.isPresent()) {
      targets.add(target.get());
      return targets;
    }

    if (containingClass == null) {
      targets.add(handleUnknownMethod(functionName, reference.getType()));
    } else {
      MethodDeclaration resolved =
          handleUnknownClassMethod(containingClass, functionName, reference.getType());
      if (resolved != null) {
        targets.add(resolved);
      }
    }
    return targets;
  }

  private void resolve(RecordDeclaration currentClass, Node parent, Node current) {
    if (current instanceof DeclaredReferenceExpression) {
      DeclaredReferenceExpression ref = (DeclaredReferenceExpression) current;
      if (parent instanceof MemberCallExpression
          && current == ((MemberCallExpression) parent).getMember()
          && !(ref.getType() instanceof FunctionPointerType)) {
        // members of a MemberCallExpression are no variables to be resolved, unless we have a
        // function pointer call
        return;
      }
      Set<ValueDeclaration> refersTo = new HashSet<>();
      if (current instanceof MemberExpression) {
        MemberExpression memberExpression = (MemberExpression) current;
        resolve(currentClass, memberExpression, memberExpression.getBase());
        Type containingClass = ((MemberExpression) current).getBase().getType();
        resolveField(containingClass, ref).ifPresent(refersTo::add);
      } else {
        refersTo.addAll(resolveLocalVariable(currentClass, parent, current, ref));
      }

      if (!refersTo.isEmpty()) {
        ref.setRefersTo(refersTo);
      } else {
        warnWithFileLocation(current, log, "Did not find a declaration for {}", ref.getName());
      }
    }
  }

  private Set<ValueDeclaration> resolveLocalVariable(
      RecordDeclaration currentClass, Node parent, Node current, DeclaredReferenceExpression ref) {
    Set<ValueDeclaration> refersTo =
        walker
            .getDeclarationForScope(
                parent,
                v -> !(v instanceof FunctionDeclaration) && v.getName().equals(ref.getName()))
            .map(
                d -> {
                  Set<ValueDeclaration> set = new HashSet<>();
                  set.add(d);
                  return set;
                })
            .orElse(new HashSet<>());

    Type recordDeclType = null;
    if (currentClass != null) {
      recordDeclType = TypeParser.createFrom(currentClass.getName(), true);
    }

    if (ref.getType() instanceof FunctionPointerType && refersTo.isEmpty()) {
      refersTo = resolveFunctionPtr(recordDeclType, ref);
    }

    // only add new nodes for non-static unknown
    if (refersTo.isEmpty()
        && !(current instanceof StaticReferenceExpression)
        && recordDeclType != null
        && recordMap.containsKey(recordDeclType)) {
      // Maybe we are referring to a field instead of a local var
      Optional<FieldDeclaration> field =
          resolveField(recordDeclType, (DeclaredReferenceExpression) current);
      if (field.isPresent()) {
        Set<ValueDeclaration> resolvedMember = new HashSet<>();
        resolvedMember.add(field.get());
        refersTo = resolvedMember;
      }
    }
    return refersTo;
  }

  private Optional<FieldDeclaration> resolveField(
      Type containingClass, DeclaredReferenceExpression reference) {
    if (lang instanceof JavaLanguageFrontend
        && reference.getName().matches("(?<class>.+\\.)?super")) {
      // if we have a "super" on the member side, this is a member call. We need to resolve this
      // in the call resolver instead
      return Optional.empty();
    }

    Optional<FieldDeclaration> member = Optional.empty();
    if (!(containingClass instanceof UnknownType) && recordMap.containsKey(containingClass)) {
      member =
          recordMap.get(containingClass).getFields().stream()
              .filter(f -> f.getName().equals(reference.getName()))
              .findFirst();
    }

    if (member.isEmpty()) {
      member =
          superTypesMap.getOrDefault(containingClass, Collections.emptyList()).stream()
              .map(recordMap::get)
              .filter(Objects::nonNull)
              .flatMap(r -> r.getFields().stream())
              .filter(f -> f.getName().equals(reference.getName()))
              .findFirst();
    }
    // Attention: using orElse instead of orElseGet will always invoke unknown declaration handling!
    return member.or(
        () ->
            Optional.ofNullable(
                handleUnknownField(containingClass, reference.getName(), reference.getType())));
  }

  private FieldDeclaration handleUnknownField(Type base, String name, Type type) {
    if (!recordMap.containsKey(base)) {
      return null;
    }
    // fields.putIfAbsent(base, new ArrayList<>());
    List<FieldDeclaration> declarations = recordMap.get(base).getFields();
    Optional<FieldDeclaration> target =
        declarations.stream().filter(f -> f.getName().equals(name)).findFirst();
    if (target.isEmpty()) {
      FieldDeclaration declaration =
          NodeBuilder.newFieldDeclaration(
              name, type, Collections.emptyList(), "", null, null, false);
      declarations.add(declaration);
      declaration.setImplicit(true);
      // lang.getScopeManager().addValueDeclaration(declaration);
      return declaration;
    } else {
      return target.get();
    }
  }

  private MethodDeclaration handleUnknownClassMethod(Type base, String name, Type type) {
    if (!recordMap.containsKey(base)) {
      return null;
    }
    RecordDeclaration containingRecord = recordMap.get(base);
    List<MethodDeclaration> declarations = containingRecord.getMethods();
    Optional<MethodDeclaration> target =
        declarations.stream().filter(f -> f.getName().equals(name)).findFirst();
    if (target.isEmpty()) {
      MethodDeclaration declaration =
          NodeBuilder.newMethodDeclaration(name, "", false, containingRecord);
      declaration.setType(type);
      declarations.add(declaration);
      declaration.setImplicit(true);
      return declaration;
    } else {
      return target.get();
    }
  }

  private FunctionDeclaration handleUnknownMethod(String name, Type type) {
    Optional<FunctionDeclaration> target =
        currTu.getDeclarations().stream()
            .filter(FunctionDeclaration.class::isInstance)
            .map(FunctionDeclaration.class::cast)
            .filter(f -> f.getName().equals(name))
            .filter(f -> f.hasSignature(((FunctionPointerType) type).getParameters()))
            .findFirst();
    if (target.isEmpty()) {
      FunctionDeclaration declaration = NodeBuilder.newFunctionDeclaration(name, "");
      if (type instanceof FunctionPointerType) {
        declaration.setType(((FunctionPointerType) type).getReturnType());
        declaration.setParameters(
            Util.createParameters(((FunctionPointerType) type).getParameters()));
      } else {
        declaration.setType(type);
      }
      currTu.add(declaration);
      declaration.setImplicit(true);
      return declaration;
    } else {
      return target.get();
    }
  }
}
