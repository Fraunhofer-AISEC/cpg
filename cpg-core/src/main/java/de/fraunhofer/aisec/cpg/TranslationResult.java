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
package de.fraunhofer.aisec.cpg;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.helpers.Benchmark;
import de.fraunhofer.aisec.cpg.helpers.StatisticsHolder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * The global (intermediate) result of the translation. A {@link
 * de.fraunhofer.aisec.cpg.frontends.LanguageFrontend} will initially populate it and a {@link
 * de.fraunhofer.aisec.cpg.passes.Pass} can extend it.
 */
public class TranslationResult extends Node implements StatisticsHolder {
  public static final String SOURCE_LOCATIONS_TO_FRONTEND = "sourceLocationsToFrontend";
  private final TranslationManager translationManager;

  /** Entry points to the CPG: "TranslationUnits" refer to source files. */
  @SubGraph("AST")
  private final List<TranslationUnitDeclaration> translationUnits = new ArrayList<>();

  /** A free-for-use HashMap where passes can store whatever they want. */
  private final Map<String, Object> scratch = new ConcurrentHashMap<>();

  private final List<Benchmark> benchmarks = new ArrayList<>();

  public TranslationResult(TranslationManager translationManager) {
    this.translationManager = translationManager;
  }

  public boolean isCancelled() {
    return translationManager.isCancelled();
  }

  /**
   * List of translation units. Note that this list is immutable. Use {@link
   * #addTranslationUnit(TranslationUnitDeclaration)} if you wish to modify it.
   *
   * @return the list of translation units
   */
  public List<TranslationUnitDeclaration> getTranslationUnits() {
    return Collections.unmodifiableList(this.translationUnits);
  }

  /**
   * Add a {@link TranslationUnitDeclaration} to this translation result in a thread safe way.
   *
   * @param tu The translation unit to add
   */
  public synchronized void addTranslationUnit(TranslationUnitDeclaration tu) {
    translationUnits.add(tu);
  }

  /**
   * Scratch storage that can be used by passes to store additional information in this result.
   * Callers must ensure that keys are unique. It is recommended to prefix them with the class name
   * of the Pass.
   *
   * @return the scratch storage
   */
  //  public Scene getScene() {
  //    return this.scene;
  //  }

  public Map<String, Object> getScratch() {
    return scratch;
  }

  public TranslationManager getTranslationManager() {
    return translationManager;
  }

  @Override
  public void addBenchmark(@NotNull Benchmark b) {
    this.benchmarks.add(b);
  }

  public List<Benchmark> getBenchmarks() {
    return benchmarks;
  }

  @Override
  public void printBenchmark() {
    StatisticsHolder.DefaultImpls.printBenchmark(this);
  }

  @NotNull
  @Override
  public List<String> getTranslatedFiles() {
    return translationUnits.stream()
        .map(TranslationUnitDeclaration::getName)
        .collect(Collectors.toList());
  }

  @NotNull
  @Override
  public TranslationConfiguration getConfig() {
    return translationManager.getConfig();
  }
}
