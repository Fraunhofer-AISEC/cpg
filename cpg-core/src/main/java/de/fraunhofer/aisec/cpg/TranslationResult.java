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

import de.fraunhofer.aisec.cpg.graph.Component;
import de.fraunhofer.aisec.cpg.graph.Name;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.helpers.BenchmarkResults;
import de.fraunhofer.aisec.cpg.helpers.MeasurementHolder;
import de.fraunhofer.aisec.cpg.helpers.StatisticsHolder;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.util.*;
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
  public static final String APPLICATION_LOCAL_NAME = "application";
  private final TranslationManager translationManager;

  /**
   * Entry points to the CPG: "SoftwareComponent" refer to programs, application, other "bundles" of
   * software.
   */
  @SubGraph("AST")
  private final List<Component> components = new ArrayList<>();

  /** A free-for-use HashMap where passes can store whatever they want. */
  private final Map<String, Object> scratch = new ConcurrentHashMap<>();

  /**
   * A free-for-use collection of unique nodes. Nodes stored here will be exported to Neo4j, too.
   */
  private final Set<Node> additionalNodes = new HashSet<>();

  private final Set<MeasurementHolder> benchmarks = new LinkedHashSet<>();

  /**
   * The scope manager which comprises the complete translation result. In case of sequential
   * parsing, this scope manager is passed to the individual frontends one after another. In case of
   * sequential parsing, individual scope managers will be spawned by each language frontend and
   * then finally merged into this one.
   */
  @NotNull private final ScopeManager scopeManager;

  public TranslationResult(
      TranslationManager translationManager, @NotNull ScopeManager scopeManager) {
    this.translationManager = translationManager;
    this.scopeManager = scopeManager;
  }

  public boolean isCancelled() {
    return translationManager.isCancelled();
  }

  /**
   * Checks if only a single software component has been analyzed and returns its translation units.
   * For multiple software components, it aggregates the results.
   *
   * @return the list of all translation units.
   */
  public List<TranslationUnitDeclaration> getTranslationUnits() {
    if (this.components.size() == 1) {
      return Collections.unmodifiableList(this.components.get(0).getTranslationUnits());
    }
    List<TranslationUnitDeclaration> result = new ArrayList<>();
    for (var sc : components) {
      result.addAll(sc.getTranslationUnits());
    }
    return result;
  }

  /**
   * If no component exists, it generates a [Component] called "application" and adds [tu]. If a
   * component already exists, adds the tu to this component.
   *
   * @param tu The translation unit to add.
   * @deprecated This should not be used anymore. Instead, the corresponding component should be
   *     selected and the translation unit should be added there.
   */
  @Deprecated(since = "4.4.1")
  public synchronized void addTranslationUnit(TranslationUnitDeclaration tu) {
    Component swc = null;
    if (components.size() == 1) {
      // Only one component exists, so we take this one
      swc = components.get(0);
    } else if (components.isEmpty()) {
      // No component exists, so we create the new dummy component.
      swc = new Component();
      swc.setName(new Name(APPLICATION_LOCAL_NAME, null, ""));
      components.add(swc);
    } else {
      // Multiple components exist. As we don't know where to put the tu, we check if we have the
      // component we created and add it there or create a new one.
      for (var component : components) {
        if (component.getName().getLocalName().equals(APPLICATION_LOCAL_NAME)) {
          swc = component;
          break;
        }
      }

      if (swc == null) {
        swc = new Component();
        swc.setName(new Name(APPLICATION_LOCAL_NAME, null, ""));
        components.add(swc);
      }
    }

    swc.getTranslationUnits().add(tu);
  }

  /**
   * List of software components. Note that this list is immutable. Use {@link
   * #addComponent(Component)} if you wish to modify it.
   *
   * @return the list of software components
   */
  public List<Component> getComponents() {
    return Collections.unmodifiableList(this.components);
  }

  /**
   * Add a {@link Component} to this translation result in a thread safe way.
   *
   * @param sc The software component to add
   */
  public synchronized void addComponent(Component sc) {
    components.add(sc);
  }

  /**
   * Scratch storage that can be used by passes to store additional information in this result.
   * Callers must ensure that keys are unique. It is recommended to prefix them with the class name
   * of the Pass.
   *
   * @return the scratch storage
   */
  public Map<String, Object> getScratch() {
    return scratch;
  }

  public Set<Node> getAdditionalNodes() {
    return additionalNodes;
  }

  public TranslationManager getTranslationManager() {
    return translationManager;
  }

  @Override
  public void addBenchmark(@NotNull MeasurementHolder b) {
    this.benchmarks.add(b);
  }

  @NotNull
  public Set<MeasurementHolder> getBenchmarks() {
    return benchmarks;
  }

  @NotNull
  @Override
  public List<String> getTranslatedFiles() {
    List<String> result = new ArrayList<>();
    components.forEach(
        sc ->
            result.addAll(
                sc.getTranslationUnits().stream()
                    .map(TranslationUnitDeclaration::getName)
                    .map(Name::toString)
                    .collect(Collectors.toList())));
    return result;
  }

  @NotNull
  @Override
  public TranslationConfiguration getConfig() {
    return translationManager.getConfig();
  }

  @NotNull
  public BenchmarkResults getBenchmarkResults() {
    return StatisticsHolder.DefaultImpls.getBenchmarkResults(this);
  }

  @NotNull
  public ScopeManager getScopeManager() {
    return scopeManager;
  }
}
