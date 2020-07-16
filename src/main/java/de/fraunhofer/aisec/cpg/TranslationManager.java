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

package de.fraunhofer.aisec.cpg;

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontendFactory;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.helpers.Benchmark;
import de.fraunhofer.aisec.cpg.passes.Pass;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point for all source code translation for all language front-ends. */
public class TranslationManager {

  private static final Logger log = LoggerFactory.getLogger(TranslationManager.class);

  @NonNull private final TranslationConfiguration config;
  private final AtomicBoolean isCancelled = new AtomicBoolean(false);

  private ExecutorService executorService;

  private TranslationManager(@NonNull TranslationConfiguration config) {
    this.config = config;

    this.executorService = Executors.newCachedThreadPool();
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Kicks off the analysis.
   *
   * <p>This method orchestrates all passes that will do the main work.
   *
   * @return a {@link CompletableFuture} with the {@link TranslationResult}.
   */
  public CompletableFuture<TranslationResult> analyze() {
    TranslationResult result = new TranslationResult(this);

    // We wrap the analysis in a CompletableFuture, i.e. in an asynch task.
    return CompletableFuture.supplyAsync(
        () -> {
          ScopeManager scopesBuildForAnalysis = new ScopeManager();
          Benchmark outerBench =
              new Benchmark(TranslationManager.class, "Translation into full graph");

          Set<Pass> passesNeedCleanup = new HashSet<>();
          Set<LanguageFrontend> frontendsNeedCleanup = null;

          try {
            // Parse Java/C/CPP files
            Benchmark bench = new Benchmark(this.getClass(), "Frontend");
            frontendsNeedCleanup = runFrontends(result, this.config, scopesBuildForAnalysis);
            bench.stop();

            // TODO: Find a way to identify the right language during the execution of a pass (and
            // set the lang to the scope manager)
            // Apply passes
            for (Pass pass : config.getRegisteredPasses()) {
              passesNeedCleanup.add(pass);
              bench = new Benchmark(pass.getClass(), "Executing Pass");
              pass.accept(result);
              bench.stop();
              if (result.isCancelled()) {
                log.warn("Analysis interrupted, stopping Pass evaluation");
              }
            }
          } catch (TranslationException ex) {
            throw new CompletionException(ex);
          } finally {
            outerBench.stop();
            log.debug("Cleaning up {} Passes", passesNeedCleanup.size());
            passesNeedCleanup.forEach(Pass::cleanup);

            if (frontendsNeedCleanup != null) {
              log.debug("Cleaning up {} Frontends", frontendsNeedCleanup.size());
              frontendsNeedCleanup.forEach(LanguageFrontend::cleanup);
            }

            TypeManager.getInstance().cleanup();
          }
          return result;
        });
  }

  public List<Pass> getPasses() {
    return config.getRegisteredPasses();
  }

  public boolean isCancelled() {
    return isCancelled.get();
  }

  /**
   * Parses all language files using the respective {@link LanguageFrontend} and creates the initial
   * set of AST nodes.
   *
   * @param result the translation result that is being mutated
   * @param config the translation configuration
   * @throws TranslationException if the language front-end runs into an error and <code>failOnError
   * </code> is <code>true</code>.
   */
  private HashSet<LanguageFrontend> runFrontends(
      @NonNull TranslationResult result,
      @NonNull TranslationConfiguration config,
      @NonNull ScopeManager scopeManager)
      throws TranslationException {

    List<File> sourceLocations = new ArrayList<>(this.config.getSourceLocations());
    HashSet<LanguageFrontend> usedFrontends = new HashSet<>();

    List<Future<TranslationUnitDeclaration>> translationUnits = new ArrayList<>();
    for (int i = 0; i < sourceLocations.size(); i++) {
      File sourceLocation = sourceLocations.get(i);

      // Recursively add files in directories
      if (sourceLocation.isDirectory()) {
        try (Stream<Path> stream =
            Files.find(sourceLocation.toPath(), 999, (p, fileAttr) -> fileAttr.isRegularFile())) {
          sourceLocations.addAll(stream.map(Path::toFile).collect(Collectors.toSet()));
          continue;
        } catch (IOException e) {
          log.error(e.getMessage(), e);
        }
      }

      log.info("Parsing {}", sourceLocation.getAbsolutePath());
      LanguageFrontend frontend = null;
      try {
        frontend = LanguageFrontendFactory.getFrontend(sourceLocation, config, scopeManager);
        if (frontend == null) {
          log.error("Found no parser frontend for {}", sourceLocation.getName());

          if (config.failOnError) {
            throw new TranslationException(
                "Found no parser frontend for " + sourceLocation.getName());
          }
          continue;
        }
        for (LanguageFrontend previous : usedFrontends) {
          if (!previous.getClass().equals(frontend.getClass())) {
            log.error(
                "Different frontends are used for multiple files. This will very likely break the following passes.");
          }
        }
        usedFrontends.add(frontend);

        // remember which frontend parsed each file
        Map<String, String> sfToFe =
            (Map<String, String>)
                result
                    .getScratch()
                    .computeIfAbsent(
                        TranslationResult.SOURCE_LOCATIONS_TO_FRONTEND,
                        x -> new HashMap<String, String>());
        sfToFe.put(sourceLocation.getName(), frontend.getClass().getSimpleName());

        // result.getTranslationUnits().add(frontend.parse(sourceLocation));
        Future<TranslationUnitDeclaration> f = this.executorService.submit(frontend);

        translationUnits.add(f);
      } catch (TranslationException ex) {
        log.error(
            "An error occurred during parsing of {}: {}",
            sourceLocation.getName(),
            ex.getMessage());

        if (config.failOnError) {
          throw ex;
        }
      }

      // Set frontend so passes know what language they are working on.
      for (Pass pass : config.getRegisteredPasses()) {
        pass.setLang(frontend);
      }
    }

    for (Future<TranslationUnitDeclaration> f : translationUnits) {
      try {
        result.getTranslationUnits().add(f.get());
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }

    return usedFrontends;
  }

  /**
   * Returns the current (immutable) configuration of this TranslationManager.
   *
   * @return the configuration
   */
  @NonNull
  public TranslationConfiguration getConfig() {
    return this.config;
  }

  public static class Builder {

    private TranslationConfiguration config;

    private Builder() {}

    public Builder config(TranslationConfiguration config) {
      this.config = config;
      return this;
    }

    public TranslationManager build() {
      return new TranslationManager(this.config);
    }
  }
}
