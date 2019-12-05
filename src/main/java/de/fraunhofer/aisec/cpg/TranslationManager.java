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
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.helpers.Benchmark;
import de.fraunhofer.aisec.cpg.passes.Pass;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point for all source code translation for all language front-ends. */
public class TranslationManager {

  private static final Logger log = LoggerFactory.getLogger(TranslationManager.class);

  private TranslationConfiguration config;
  private AtomicBoolean isCancelled = new AtomicBoolean(false);

  private TranslationManager(TranslationConfiguration config) {
    this.config = config;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Kicks off the analysis.
   *
   * <p>This method orchestrates all passes that will do the main work.
   */
  public CompletableFuture<TranslationResult> analyze() {
    TranslationResult result = new TranslationResult(this);

    // We wrap the analysis in a CompletableFuture, i.e. in an asynch task.
    return CompletableFuture.supplyAsync(
        () -> {
          Benchmark outerBench =
              new Benchmark(TranslationManager.class, "Translation into full graph");

          HashSet<Pass> passesNeedCleanup = new HashSet<>();
          HashSet<LanguageFrontend> frontendsNeedCleanup = null;

          try {
            // Parse Java/C/CPP files
            Benchmark bench = new Benchmark(this.getClass(), "Frontend");
            frontendsNeedCleanup = runFrontends(result, this.config);
            bench.stop();

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
   *     </code> is <code>true</code>.
   * @return
   */
  private HashSet<LanguageFrontend> runFrontends(
      TranslationResult result, TranslationConfiguration config) throws TranslationException {

    List<File> sourceFiles = this.config.getSourceFiles();
    HashSet<LanguageFrontend> usedFrontends = new HashSet<>();
    for (File sourceFile : sourceFiles) {
      log.info("Parsing {}", sourceFile.getAbsolutePath());
      LanguageFrontend frontend = null;
      try {
        frontend =
            LanguageFrontendFactory.getFrontend(
                sourceFile.getName().substring(sourceFile.getName().lastIndexOf('.')).toLowerCase(),
                config);

        if (frontend == null) {
          log.error("Found no parser frontend for {}", sourceFile.getName());

          if (config.failOnError) {
            throw new TranslationException("Found no parser frontend for " + sourceFile.getName());
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
        HashMap<String, String> sfToFe =
            (HashMap<String, String>)
                result
                    .getScratch()
                    .computeIfAbsent(
                        TranslationResult.SOURCEFILESTOFRONTEND,
                        x -> new HashMap<String, String>());
        sfToFe.put(sourceFile.getName(), frontend.getClass().getSimpleName());

        result.getTranslationUnits().add(frontend.parse(sourceFile));
      } catch (TranslationException ex) {
        log.error(
            "An error occurred during parsing of {}: {}", sourceFile.getName(), ex.getMessage());

        if (config.failOnError) {
          throw ex;
        }
      } finally {
        // this only sets one frontend. once more frontends are allowed in parallel, this needs to
        // change
        for (Pass pass : config.getRegisteredPasses()) {
          pass.setLang(frontend);
        }
      }
    }
    return usedFrontends;
  }

  /**
   * Returns the current (immutable) configuration of this TranslationManager.
   *
   * @return
   */
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
