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
package de.fraunhofer.aisec.cpg.passes;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.types.Type;
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
 * This is needed e.g. for {@link
 * de.fraunhofer.aisec.cpg.graph.TypeManager#getCommonType(Collection)} to be re-evaluated at places
 * where it is crucial to have parsed all {@link RecordDeclaration}s. Otherwise, type information in
 * the graph might not be fully correct
 */
public class TypeHierarchyResolver extends Pass {

  protected final Map<String, RecordDeclaration> recordMap = new HashMap<>();
  protected final List<EnumDeclaration> enums = new ArrayList<>();

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

    translationResult.getTranslationUnits().forEach(SubgraphWalker::refreshType);
  }

  protected void findRecordsAndEnums(Node node) {
    if (node instanceof RecordDeclaration) {
      recordMap.putIfAbsent(node.getName(), (RecordDeclaration) node);
    } else if (node instanceof EnumDeclaration) {
      enums.add((EnumDeclaration) node);
    }
    for (var child : SubgraphWalker.getAstChildren(node)) {
      findRecordsAndEnums(child);
    }
  }

  protected List<MethodDeclaration> getAllMethodsFromSupertypes(
      Set<RecordDeclaration> supertypeRecords) {
    return supertypeRecords.stream()
        .map(RecordDeclaration::getMethods)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  protected Set<RecordDeclaration> findSupertypeRecords(RecordDeclaration record) {
    Set<RecordDeclaration> superTypeDeclarations =
        record.getSuperTypes().stream()
            .map(Type::getTypeName)
            .map(recordMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    record.setSuperTypeDeclarations(superTypeDeclarations);
    return superTypeDeclarations;
  }

  protected void analyzeOverridingMethods(
      RecordDeclaration declaration, List<MethodDeclaration> allMethodsFromSupertypes) {
    for (MethodDeclaration superMethod : allMethodsFromSupertypes) {
      List<MethodDeclaration> overrideCandidates =
          declaration.getMethods().stream()
              .filter(superMethod::isOverrideCandidate)
              .collect(Collectors.toList());
      superMethod.addOverriddenBy(overrideCandidates);
      overrideCandidates.forEach(o -> o.addOverrides(superMethod));
    }
  }

  @Override
  public void cleanup() {}
}
