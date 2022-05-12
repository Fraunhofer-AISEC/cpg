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
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ImportResolver extends Pass {

  protected final List<RecordDeclaration> records = new ArrayList<>();
  protected final Map<String, Declaration> importables = new HashMap<>();

  @Override
  @Nullable
  public LanguageFrontend getLang() {
    return null;
  }

  @Override
  public void setLang(LanguageFrontend lang) {}

  @Override
  public void cleanup() {
    records.clear();
    importables.clear();
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
  }

  protected Set<ValueDeclaration> getStaticImports(RecordDeclaration record) {
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
      Declaration base = importables.get((matcher.group("base")));
      Set<ValueDeclaration> members = new HashSet<>();
      if (base instanceof RecordDeclaration) {
        members = getOrCreateMembers((RecordDeclaration) base, matcher.group("member"));
      } else if (base instanceof EnumDeclaration) {
        members = getOrCreateMembers((EnumDeclaration) base, matcher.group("member"));
      }
      staticImports.addAll(members);
    }

    for (String asteriskImport : partitioned.get(true)) {
      Declaration base = importables.get(asteriskImport.replace(".*", ""));
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

  protected Set<Declaration> getDeclarationsForTypeNames(List<String> targetTypes) {
    return targetTypes.stream()
        .map(importables::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  protected Set<ValueDeclaration> getOrCreateMembers(EnumDeclaration base, String name) {
    Set<ValueDeclaration> entries =
        base.getEntries().stream()
            .filter(e -> e.getName().equals(name))
            .collect(Collectors.toSet());
    return entries;
  }

  protected Set<ValueDeclaration> getOrCreateMembers(RecordDeclaration base, String name) {
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
              name, UnknownType.getUnknownType(), new ArrayList<>(), "", null, null, false);
      targetField.setInferred(true);
      MethodDeclaration targetMethod = NodeBuilder.newMethodDeclaration(name, "", true, base);
      targetMethod.setInferred(true);

      base.addField(targetField);
      base.addMethod(targetMethod);
      result.add(targetField);
      result.add(targetMethod);
    }

    return result;
  }

  protected void findImportables(Node node) {
    if (node instanceof RecordDeclaration) {
      records.add((RecordDeclaration) node);
      importables.putIfAbsent(node.getName(), (RecordDeclaration) node);
    } else if (node instanceof EnumDeclaration) {
      importables.putIfAbsent(node.getName(), (EnumDeclaration) node);
    }
    for (var child : SubgraphWalker.getAstChildren(node)) {
      findImportables(child);
    }
  }
}
