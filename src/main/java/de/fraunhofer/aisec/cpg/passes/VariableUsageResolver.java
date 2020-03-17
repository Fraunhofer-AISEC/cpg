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
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker;
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
                Collectors.toMap(r -> new Type(r.getName()), RecordDeclaration::getSuperTypes));
    superTypesMap.putAll(currSuperTypes);

    for (TranslationUnitDeclaration tu : result.getTranslationUnits()) {
      walker.clearCallbacks();
      walker.registerHandler(this::resolveFieldUsages);
      walker.iterate(tu);
    }
    for (TranslationUnitDeclaration tu : result.getTranslationUnits()) {
      walker.clearCallbacks();
      walker.registerHandler(this::resolveLocalVarUsage);
      walker.iterate(tu);
    }
  }

  private void findRecordsAndEnums(Node node, RecordDeclaration curClass) {
    if (node instanceof RecordDeclaration) {
      Type type = new Type(node.getName());
      recordMap.putIfAbsent(type, (RecordDeclaration) node);
    } else if (node instanceof EnumDeclaration) {
      Type type = new Type(node.getName());
      enumMap.putIfAbsent(type, (EnumDeclaration) node);
    }
  }

  private Set<ValueDeclaration> resolveFunctionPtr(
      Type containingClass, DeclaredReferenceExpression reference) {
    Set<ValueDeclaration> targets = new HashSet<>();
    String functionName = reference.getName();
    Matcher matcher =
        Pattern.compile("(?:(?<class>.*)(?:\\.|::))?(?<function>.*)").matcher(reference.getName());
    if (matcher.matches()) {
      String cls = matcher.group("class");
      functionName = matcher.group("function");
      String finalFunctionName = functionName;
      if (cls == null) {
        targets =
            walker.getAllDeclarationsForScope(reference).stream()
                .filter(FunctionDeclaration.class::isInstance)
                .filter(d -> d.getName().equals(finalFunctionName))
                .collect(Collectors.toCollection(HashSet::new));
      } else {
        containingClass = new Type(cls);
        if (recordMap.containsKey(containingClass)) {
          targets =
              recordMap.get(containingClass).getMethods().stream()
                  .filter(f -> f.getName().equals(finalFunctionName))
                  .map(ValueDeclaration.class::cast)
                  .collect(Collectors.toCollection(HashSet::new));
        }
      }
    }

    if (targets.isEmpty()) {
      if (containingClass == null) {
        Set<ValueDeclaration> unknownMethod = new HashSet<>();
        unknownMethod.add(handleUnknownMethod(functionName, reference.getType()));
        return unknownMethod;
      } else {
        Set<ValueDeclaration> unknownClass = new HashSet<>();
        unknownClass.add(
            handleUnknownClassMethod(containingClass, functionName, reference.getType()));
        return unknownClass;
      }
    } else {
      return targets;
    }
  }

  private void resolveLocalVarUsage(RecordDeclaration currentClass, Node parent, Node current) {
    if (current instanceof DeclaredReferenceExpression) {
      DeclaredReferenceExpression ref = (DeclaredReferenceExpression) current;
      Set<ValueDeclaration> refersTo =
          walker
              .getDeclarationForScope(parent, ref.getName())
              .map(
                  d -> {
                    Set<ValueDeclaration> set = new HashSet<>();
                    set.add(d);
                    return set;
                  })
              .orElse(new HashSet<>());

      Type recordDeclType = null;
      if (currentClass != null) {
        recordDeclType = new Type(currentClass.getName());
      }

      if (ref.getType().isFunctionPtr()
          && (refersTo.isEmpty()
              || refersTo.stream().anyMatch(FunctionDeclaration.class::isInstance))) {
        // If we already found something, this might either be a function pointer variable or a
        // function that would match the name. If we found a function, discard this finding, as
        // it is most likely not correct yet
        refersTo = resolveFunctionPtr(recordDeclType, ref);
      }

      // only add new nodes for non-static unknown
      if (refersTo.isEmpty()
          && !(current instanceof StaticReferenceExpression)
          && recordDeclType != null
          && recordMap.containsKey(recordDeclType)) {
        // Maybe we are referring to a field instead of a local var
        Set<ValueDeclaration> resolvedMember = new HashSet<>();
        resolvedMember.add(resolveMember(recordDeclType, (DeclaredReferenceExpression) current));
        refersTo = resolvedMember;
      }

      if (!refersTo.isEmpty()) {
        ref.setRefersTo(refersTo);
      } else {
        warnWithFileLocation(current, log, "Did not find a declaration for {}");
      }
    }
  }

  private void resolveFieldUsages(Node current, RecordDeclaration curClass) {
    if (current instanceof MemberExpression) {
      MemberExpression memberExpression = (MemberExpression) current;
      Node base = memberExpression.getBase();
      Node member = memberExpression.getMember();
      if (base instanceof DeclaredReferenceExpression) {
        base = resolveBase((DeclaredReferenceExpression) memberExpression.getBase());
      }
      if (member instanceof DeclaredReferenceExpression) {
        if (base instanceof EnumDeclaration) {
          String name = member.getName();
          member =
              ((EnumDeclaration) base)
                  .getEntries().stream()
                      .filter(e -> e.getName().equals(name))
                      .findFirst()
                      .orElse(null);
        } else {
          Type baseType = Type.getUnknown();
          if (base instanceof HasType) {
            baseType = ((HasType) base).getType();
          }
          if (base instanceof RecordDeclaration) {
            baseType = new Type(base.getName());
          }
          member =
              base == null
                  ? null
                  : resolveMember(
                      baseType, (DeclaredReferenceExpression) memberExpression.getMember());
          if (member != null) {
            HasType typedMember = (HasType) member;
            typedMember.setType(memberExpression.getType());
            Set<Type> subTypes = new HashSet<>(typedMember.getPossibleSubTypes());
            subTypes.addAll(memberExpression.getPossibleSubTypes());
            typedMember.setPossibleSubTypes(subTypes);
          }
        }
      }

      if (base != null && member != null) {
        if (base != memberExpression.getBase()) {
          memberExpression.getBase().disconnectFromGraph();
        }
        if (member != memberExpression.getMember()) {
          memberExpression.getMember().disconnectFromGraph();
        }
        memberExpression.setBase(base);
        memberExpression.setMember(member);
      } else {
        log.warn("Unexpected: null base or member in field usage: {}", current);
      }
    }
  }

  private Declaration resolveBase(DeclaredReferenceExpression reference) {
    // check if this refers to an enum
    if (enumMap.containsKey(reference.getType())) {
      return enumMap.get(reference.getType());
    }

    if (recordMap.containsKey(reference.getType())) {
      RecordDeclaration recordDeclaration = recordMap.get(reference.getType());
      if (reference instanceof StaticReferenceExpression) {
        return recordDeclaration;
      } else {
        // check if we have this type as a class in our graph. If so, we can refer to its "this"
        // field
        if (recordDeclaration.getThis() != null) {
          return recordDeclaration.getThis();
        } else {
          return recordDeclaration;
        }
      }
    } else {
      log.info(
          "Type declaration for {} not found in graph, using dummy to collect all " + "usages",
          reference.getType());
      return handleUnknownField(reference.getType(), reference);
    }
  }

  private ValueDeclaration resolveMember(
      Type containingClass, DeclaredReferenceExpression reference) {

    Optional<FieldDeclaration> member = Optional.empty();
    if (!TypeManager.getInstance().isUnknown(containingClass)) {
      if (recordMap.containsKey(containingClass)) {
        member =
            recordMap.get(containingClass).getFields().stream()
                .filter(f -> f.getName().equals(reference.getName()))
                .findFirst();
      }
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
    return member.orElseGet(() -> handleUnknownField(containingClass, reference));
  }

  private FieldDeclaration handleUnknownField(Type base, DeclaredReferenceExpression reference) {
    recordMap.putIfAbsent(
        base,
        NodeBuilder.newRecordDeclaration(
            base.getTypeName(),
            new ArrayList<>(),
            Type.UNKNOWN_TYPE_STRING,
            Type.UNKNOWN_TYPE_STRING));
    // fields.putIfAbsent(base, new ArrayList<>());
    List<FieldDeclaration> declarations = recordMap.get(base).getFields();
    Optional<FieldDeclaration> target =
        declarations.stream().filter(f -> f.getName().equals(reference.getName())).findFirst();
    if (target.isEmpty()) {
      FieldDeclaration declaration =
          NodeBuilder.newFieldDeclaration(
              reference.getName(), reference.getType(), Collections.emptyList(), "", null, null);
      declarations.add(declaration);
      declaration.setImplicit(true);
      // lang.getScopeManager().addValueDeclaration(declaration);
      return declaration;
    } else {
      return target.get();
    }
  }

  private MethodDeclaration handleUnknownClassMethod(Type base, String name, Type type) {
    recordMap.putIfAbsent(
        base,
        NodeBuilder.newRecordDeclaration(
            base.getTypeName(),
            new ArrayList<>(),
            Type.UNKNOWN_TYPE_STRING,
            Type.UNKNOWN_TYPE_STRING));
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
            .findFirst();
    if (target.isEmpty()) {
      FunctionDeclaration declaration = NodeBuilder.newFunctionDeclaration(name, "");
      declaration.setType(type);
      currTu.getDeclarations().add(declaration);
      declaration.setImplicit(true);
      return declaration;
    } else {
      return target.get();
    }
  }
}
