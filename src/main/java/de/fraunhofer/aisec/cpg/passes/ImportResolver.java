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
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.EnumDeclaration;
import de.fraunhofer.aisec.cpg.graph.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.Region;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.Type;
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportResolver implements Pass {

  private List<RecordDeclaration> records = new ArrayList<>();
  private Map<String, Declaration> importables = new HashMap<>();
  private Map<String, Declaration> unknownTypes = new HashMap<>();

  @Override
  public LanguageFrontend getLang() {
    return null;
  }

  @Override
  public void setLang(LanguageFrontend lang) {}

  @Override
  public void cleanup() {
    records.clear();
    records = null;
    importables.clear();
    importables = null;
    unknownTypes.clear();
    unknownTypes = null;
  }

  @Override
  public void accept(TranslationResult result) {
    for (TranslationUnitDeclaration tu : result.getTranslationUnits()) {
      findImportables(tu);
    }

    for (RecordDeclaration record : records) {
      Set<Declaration> imports = getDeclarationsForTypeNames(record.getImportStatements());
      record.setImports(imports);
      Set<ValueDeclaration> staticImports = getStaticImports(record);
      record.setStaticImports(staticImports);
    }

    if (!unknownTypes.isEmpty()) {
      // Get the translation unit holding all unknown declarations, or create a new one if necessary
      TranslationUnitDeclaration unknownDeclarations =
          result.getTranslationUnits().stream()
              .filter(tu -> tu.getName().equals("unknown declarations"))
              .findFirst()
              .orElseGet(() -> createUnknownTranslationUnit(result));
      unknownDeclarations.setDeclarations(new ArrayList<>(unknownTypes.values()));
      result.getTranslationUnits().add(unknownDeclarations);
      importables.putAll(unknownTypes);
    }
  }

  private Set<ValueDeclaration> getStaticImports(RecordDeclaration record) {
    Map<Boolean, List<String>> partitioned =
        record.getStaticImportStatements().stream()
            .collect(Collectors.partitioningBy(i -> i.endsWith("*")));

    Set<ValueDeclaration> staticImports = new HashSet<>();
    Pattern importPattern = Pattern.compile("(?<base>.*)\\.(?<member>.*)");
    for (String specificStaticImport : partitioned.get(false)) {
      Matcher matcher = importPattern.matcher(specificStaticImport);
      if (!matcher.matches()) {
        continue;
      }
      Declaration base = getOrCreateDeclaration(matcher.group("base"));
      Set<ValueDeclaration> members = new HashSet<>();
      if (base instanceof RecordDeclaration) {
        members = getOrCreateMembers((RecordDeclaration) base, matcher.group("member"));
      } else if (base instanceof EnumDeclaration) {
        members = getOrCreateMembers((EnumDeclaration) base, matcher.group("member"));
      }
      staticImports.addAll(members);
    }

    for (String asteriskImport : partitioned.get(true)) {
      Declaration base = getOrCreateDeclaration(asteriskImport.replace(".*", ""));
      if (base instanceof RecordDeclaration) {
        RecordDeclaration baseRecord = (RecordDeclaration) base;
        staticImports.addAll(
            Stream.concat(Stream.of(baseRecord), baseRecord.getSuperTypeDeclarations().stream())
                .map(RecordDeclaration::getMethods)
                .flatMap(Collection::stream)
                .filter(MethodDeclaration::isStatic)
                .collect(Collectors.toList()));
        staticImports.addAll(
            Stream.concat(Stream.of(baseRecord), baseRecord.getSuperTypeDeclarations().stream())
                .map(RecordDeclaration::getFields)
                .flatMap(Collection::stream)
                .filter(f -> f.getModifiers().contains("static"))
                .collect(Collectors.toList()));
      } else if (base instanceof EnumDeclaration) {
        EnumDeclaration baseEnum = (EnumDeclaration) base;
        staticImports.addAll(baseEnum.getEntries());
      }
    }

    return staticImports;
  }

  private Set<Declaration> getDeclarationsForTypeNames(List<String> targetTypes) {
    return targetTypes.stream().map(this::getOrCreateDeclaration).collect(Collectors.toSet());
  }

  private Declaration getOrCreateDeclaration(String name) {
    if (importables.containsKey(name)) {
      return importables.get(name);
    } else {
      // Create stubs for unknown imports
      if (!unknownTypes.containsKey(name)) {
        unknownTypes.put(
            name, NodeBuilder.newRecordDeclaration(name, Collections.emptyList(), "class", ""));
      }
      return unknownTypes.get(name);
    }
  }

  private Set<ValueDeclaration> getOrCreateMembers(EnumDeclaration base, String name) {
    Set<ValueDeclaration> entries =
        base.getEntries().stream()
            .filter(e -> e.getName().equals(name))
            .collect(Collectors.toSet());
    return entries;
  }

  private Set<ValueDeclaration> getOrCreateMembers(RecordDeclaration base, String name) {
    Set<MethodDeclaration> memberMethods =
        base.getMethods().stream()
            .filter(m -> m.getName().endsWith(name))
            .collect(Collectors.toSet());
    // add methods from superclasses
    memberMethods.addAll(
        base.getSuperTypeDeclarations().stream()
            .map(RecordDeclaration::getMethods)
            .flatMap(Collection::stream)
            .filter(m -> m.getName().endsWith(name))
            .collect(Collectors.toSet()));

    Set<FieldDeclaration> memberFields =
        base.getFields().stream().filter(f -> f.getName().equals(name)).collect(Collectors.toSet());
    // add fields from superclasses
    memberFields.addAll(
        base.getSuperTypeDeclarations().stream()
            .map(RecordDeclaration::getFields)
            .flatMap(Collection::stream)
            .filter(f -> f.getName().equals(name))
            .collect(Collectors.toSet()));

    // now it gets weird: you can import a field and a number of methods that have the same name,
    // all with a *single* static import...
    Set<ValueDeclaration> result =
        Stream.concat(memberMethods.stream(), memberFields.stream())
            .map(ValueDeclaration.class::cast)
            .collect(Collectors.toSet());
    if (result.isEmpty()) {
      // the target might be a field or a method, we don't know. Thus we need to create both
      FieldDeclaration targetField =
          NodeBuilder.newFieldDeclaration(
              name, Type.getUnknown(), new ArrayList<>(), "", new Region(-1, -1, -1, -1), null);
      targetField.setDummy(true);
      MethodDeclaration targetMethod = NodeBuilder.newMethodDeclaration(name, "", true);
      targetMethod.setDummy(true);

      base.getFields().add(targetField);
      base.getMethods().add(targetMethod);
      result.add(targetField);
      result.add(targetMethod);
    }

    return result;
  }

  private void findImportables(Node node) {
    if (node instanceof RecordDeclaration) {
      records.add((RecordDeclaration) node);
      importables.putIfAbsent(node.getName(), (RecordDeclaration) node);
    } else if (node instanceof EnumDeclaration) {
      importables.putIfAbsent(node.getName(), (EnumDeclaration) node);
    }
    for (Node child : SubgraphWalker.getAstChildren(node)) {
      findImportables(child);
    }
  }
}
