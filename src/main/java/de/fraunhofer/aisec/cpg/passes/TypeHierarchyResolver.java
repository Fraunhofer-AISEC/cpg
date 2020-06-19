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

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Transitively {@link RecordDeclaration} nodes with their supertypes' records.
 *
 * <p>Supertypes are all interfaces a class implements and the superclass it inherits from,
 * including all of their respective supertypes). The JavaParser provides us with initial info about
 * direct ancestors' names. This pass then recursively maps those and their own supertypes to the
 * correct {@link RecordDeclaration} (if available).
 *
 * <p>After determining the ancestors of a class, all inherited methods are scanned to find out
 * which of them are overridden/implemented in the current class. See {@link
 * MethodDeclaration#getOverriddenBy()}
 *
 * <p><b>Attention:</b> Needs to be run before other analysis passes, as it triggers a type refresh.
 * This is needed e.g. for {@link TypeManager#getCommonType(Collection)} to be re-evaluated at
 * places where it is crucial to have parsed all {@link RecordDeclaration}s. Otherwise, type
 * information in the graph might not be fully correct
 */
public class TypeHierarchyResolver extends Pass {

  private Map<String, RecordDeclaration> recordMap = new HashMap<>();
  private List<EnumDeclaration> enums = new ArrayList<>();
  private Map<String, RecordDeclaration> unknownTypes = new HashMap<>();

  @Override
  public LanguageFrontend getLang() {
    return null;
  }

  @Override
  public void setLang(LanguageFrontend lang) {}

  @Override
  public void accept(TranslationResult translationResult) {
    for (TranslationUnitDeclaration tu : translationResult.getTranslationUnits()) {
      findRecordsAndEnums(tu);
    }

    for (RecordDeclaration record : recordMap.values()) {
      Set<RecordDeclaration> supertypeRecords = findSupertypeRecords(record);
      List<MethodDeclaration> allMethodsFromSupertypes =
          getAllMethodsFromSupertypes(supertypeRecords);
      analyzeOverridingMethods(record, allMethodsFromSupertypes);
    }

    for (EnumDeclaration enumDecl : enums) {
      Set<RecordDeclaration> directSupertypeRecords =
          enumDecl.getSuperTypes().stream()
              .map(s -> recordMap.getOrDefault(s.toString(), null))
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
      Set<RecordDeclaration> allSupertypes =
          directSupertypeRecords.stream()
              .map(this::findSupertypeRecords)
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());
      enumDecl.setSuperTypeDeclarations(allSupertypes);
    }

    if (!unknownTypes.isEmpty()) {
      // Get the translation unit holding all unknown declarations, or create a new one if necessary
      TranslationUnitDeclaration unknownDeclarations =
          translationResult.getTranslationUnits().stream()
              .filter(tu -> tu.getName().equals("unknown declarations"))
              .findFirst()
              .orElseGet(() -> createUnknownTranslationUnit(translationResult));
      unknownDeclarations.setDeclarations(new ArrayList<>(unknownTypes.values()));
      recordMap.putAll(unknownTypes);
    }

    translationResult.getTranslationUnits().forEach(this::refreshType);
  }

  private void findRecordsAndEnums(Node node) {
    if (node instanceof RecordDeclaration) {
      recordMap.putIfAbsent(node.getName(), (RecordDeclaration) node);
    } else if (node instanceof EnumDeclaration) {
      enums.add((EnumDeclaration) node);
    }
    for (Node child : SubgraphWalker.getAstChildren(node)) {
      findRecordsAndEnums(child);
    }
  }

  private void refreshType(Node node) {
    for (Node child : SubgraphWalker.getAstChildren(node)) {
      refreshType(child);
    }
    if (node instanceof HasType) {
      ((HasType) node).refreshType();
    }
  }

  private List<MethodDeclaration> getAllMethodsFromSupertypes(
      Set<RecordDeclaration> supertypeRecords) {
    return supertypeRecords.stream()
        .map(RecordDeclaration::getMethods)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private Set<RecordDeclaration> findSupertypeRecords(RecordDeclaration record) {
    Set<RecordDeclaration> localSuperTypeDeclarations =
        record.getSuperTypes().stream()
            .map(
                t -> {
                  if (recordMap.containsKey(t.getTypeName())) {
                    return recordMap.get(t.getTypeName());
                  } else {
                    if (!unknownTypes.containsKey(t.getTypeName())) {
                      unknownTypes.put(
                          t.getTypeName(),
                          NodeBuilder.newRecordDeclaration(
                              t.getTypeName(), Collections.emptyList(), "class", ""));
                    }
                    return unknownTypes.get(t.getTypeName());
                  }
                })
            .collect(Collectors.toSet());
    HashSet<RecordDeclaration> allSupertypeRecords = new HashSet<>(localSuperTypeDeclarations);
    for (RecordDeclaration superType : localSuperTypeDeclarations) {
      allSupertypeRecords.addAll(findSupertypeRecords(superType));
    }

    record.setSuperTypeDeclarations(allSupertypeRecords);
    List<Type> superTypeNames =
        allSupertypeRecords.stream()
            .map(RecordDeclaration::getName)
            .map(t -> TypeParser.createFrom(t, true))
            .distinct()
            .collect(Collectors.toList());
    record.setSuperTypes(superTypeNames);
    return allSupertypeRecords;
  }

  private void analyzeOverridingMethods(
      RecordDeclaration declaration, List<MethodDeclaration> allMethodsFromSupertypes) {
    for (MethodDeclaration superMethod : allMethodsFromSupertypes) {
      List<MethodDeclaration> overrideCandidates =
          declaration.getMethods().stream()
              .filter(superMethod::isOverrideCandidate)
              .collect(Collectors.toList());
      superMethod.getOverriddenBy().addAll(overrideCandidates);
      overrideCandidates.forEach(o -> o.getOverrides().add(superMethod));
    }
  }

  @Override
  public void cleanup() {
    this.unknownTypes.clear();
  }
}
