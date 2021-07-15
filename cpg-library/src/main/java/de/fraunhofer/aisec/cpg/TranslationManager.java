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

import static de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend.CXX_EXTENSIONS;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.frontends.golang.GoLanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.helpers.Benchmark;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.helpers.Util;
import de.fraunhofer.aisec.cpg.helpers.incremental.ChangeMapping;
import de.fraunhofer.aisec.cpg.passes.Pass;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point for all source code translation for all language front-ends. */
public class TranslationManager {

  private static final Logger log = LoggerFactory.getLogger(TranslationManager.class);

  @NonNull private TranslationConfiguration config;
  private AtomicBoolean isCancelled = new AtomicBoolean(false);

  private TranslationManager(@NonNull TranslationConfiguration config) {
    this.config = config;
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
            doCleanup(frontendsNeedCleanup, passesNeedCleanup);
          }
          result.setScopeManager(scopesBuildForAnalysis);
          return result;
        });
  }

  /**
   * Like {@link #analyze()} but allows the user to pass a previous {@link TranslationResult} that
   * is used to incrementally construct the current result whenever possible. This may speed up the
   * analysis in case there have only been small changes to the analyzed source code.
   *
   * @param previousResult The result of a previous run of {@link #analyze()}
   * @return The updated result that matches the current state of the source code
   */
  public TranslationResult analyze(TranslationResult previousResult) throws TranslationException {
    Benchmark outerBench =
        new Benchmark(TranslationManager.class, "Incremental graph construction");
    TranslationResult result = new TranslationResult(this);

    Set<File> sourceLocations = new HashSet<>();

    for (File sourceLocation : config.getSourceLocations()) {
      // Recursively add files in directories
      if (sourceLocation.isDirectory()) {
        try (Stream<Path> stream =
            Files.find(sourceLocation.toPath(), 999, (p, fileAttr) -> fileAttr.isRegularFile())) {
          sourceLocations.addAll(stream.map(Path::toFile).collect(Collectors.toSet()));
          sourceLocations.remove(sourceLocation);
        } catch (IOException e) {
          log.error(e.getMessage(), e);
        }
      } else if (sourceLocation.isFile()) {
        sourceLocations.add(sourceLocation);
      }
    }

    List<File> changed = getChangedFiles(previousResult, result, sourceLocations);

    Benchmark bench = new Benchmark(TranslationManager.class, "Frontends");
    config.setSourceLocations(changed);
    Set<LanguageFrontend> frontendsNeedCleanup =
        runFrontends(result, config, previousResult.getScopeManager());
    bench.stop();

    removeImplicitDeclarations(previousResult.getImplicitDeclarations());

    bench = new Benchmark(TranslationManager.class, "Incremental change processing");
    boolean passesNotNeeded =
        ChangeMapping.processChanges(
            previousResult.getTranslationUnits(), result.getTranslationUnits());
    bench.stop();

    Set<Pass> passesNeedCleanup = runPasses(result, passesNotNeeded);
    doCleanup(frontendsNeedCleanup, passesNeedCleanup);

    result.setScopeManager(previousResult.getScopeManager());

    outerBench.stop();

    return result;
  }

  private void removeImplicitDeclarations(Map<Node, List<Declaration>> implicitDeclarations) {
    for (Entry<Node, List<Declaration>> entry : implicitDeclarations.entrySet()) {
      if (entry.getKey() instanceof TranslationUnitDeclaration) {
        entry.getValue().forEach(((TranslationUnitDeclaration) entry.getKey())::removeDeclaration);
      } else if (entry.getKey() instanceof RecordDeclaration) {
        RecordDeclaration record = (RecordDeclaration) entry.getKey();
        for (Declaration implicitDeclaration : entry.getValue()) {
          if (implicitDeclaration instanceof FieldDeclaration) {
            record.removeField((FieldDeclaration) implicitDeclaration);
          } else if (implicitDeclaration instanceof ConstructorDeclaration) {
            record.removeConstructor((ConstructorDeclaration) implicitDeclaration);
          } else if (implicitDeclaration instanceof MethodDeclaration) {
            record.removeMethod((MethodDeclaration) implicitDeclaration);
          } else if (implicitDeclaration instanceof TemplateDeclaration) {
            record.removeTemplate((TemplateDeclaration) implicitDeclaration);
          }
        }
      }
      entry.getValue().forEach(Node::disconnectFromGraph);
    }
  }

  private Set<Pass> runPasses(TranslationResult result, boolean skipIfPossible) {
    Benchmark bench;
    Set<Pass> passesNeedCleanup = new HashSet<>();
    for (Pass pass : config.getRegisteredPasses()) {
      if (skipIfPossible && pass.canBeSkippedIncremental()) {
        continue;
      }
      passesNeedCleanup.add(pass);
      bench = new Benchmark(pass.getClass(), "Executing Pass");
      pass.accept(result);
      bench.stop();
      if (result.isCancelled()) {
        log.warn("Analysis interrupted, stopping Pass evaluation");
      }
    }
    return passesNeedCleanup;
  }

  private void doCleanup(Set<LanguageFrontend> frontendsNeedCleanup, Set<Pass> passesNeedCleanup) {
    if (!this.config.disableCleanup) {
      log.debug("Cleaning up {} Passes", passesNeedCleanup.size());
      passesNeedCleanup.forEach(Pass::cleanup);

      if (frontendsNeedCleanup != null) {
        log.debug("Cleaning up {} Frontends", frontendsNeedCleanup.size());
        frontendsNeedCleanup.forEach(LanguageFrontend::cleanup);
      }

      TypeManager.getInstance().cleanup();
    }
  }

  @NotNull
  private List<File> getChangedFiles(
      TranslationResult previousResult, TranslationResult result, Set<File> sourceLocations)
      throws TranslationException {
    List<File> changed = new ArrayList<>();

    for (File file : sourceLocations) {
      Optional<TranslationUnitDeclaration> previousTU =
          previousResult.getTranslationUnits().stream()
              .filter(tu -> tu.getName().equals(file.toString()))
              .findAny();
      if (previousTU.isEmpty()) {
        changed.add(file);
        continue;
      }

      try {
        HashCode sha256 = com.google.common.io.Files.asByteSource(file).hash(Hashing.sha256());
        if (sha256.equals(previousTU.get().getSha256())) {
          result.addTranslationUnit(previousTU.get());
        } else {
          changed.add(file);
        }
      } catch (IOException e) {
        String message = "Could not calculate file hash of " + file.getName();
        log.error("{}: {}", message, e.getMessage());

        if (config.failOnError) {
          throw new TranslationException(message, e);
        }

        changed.add(file);
      }
    }
    return changed;
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
  private Set<LanguageFrontend> runFrontends(
      @NonNull TranslationResult result,
      @NonNull TranslationConfiguration config,
      @NonNull ScopeManager scopeManager)
      throws TranslationException {

    List<File> sourceLocations = new ArrayList<>(this.config.getSourceLocations());

    if (config.useUnityBuild) {
      try {
        File tmpFile = Files.createTempFile("compile", ".cpp").toFile();
        tmpFile.deleteOnExit();

        try (var writer = new PrintWriter(tmpFile)) {
          for (int i = 0; i < sourceLocations.size(); i++) {
            File sourceLocation = sourceLocations.get(i);
            // Recursively add files in directories
            if (sourceLocation.isDirectory()) {
              try (Stream<Path> stream =
                  Files.find(
                      sourceLocation.toPath(), 999, (p, fileAttr) -> fileAttr.isRegularFile())) {
                sourceLocations.addAll(stream.map(Path::toFile).collect(Collectors.toSet()));
              }
            } else {
              if (CXX_EXTENSIONS.contains(Util.getExtension(sourceLocation))) {
                if (config.getTopLevel() != null) {
                  Path topLevel = config.getTopLevel().toPath();
                  writer.write(
                      "#include \"" + topLevel.relativize(sourceLocation.toPath()) + "\"\n");
                } else {
                  writer.write("#include \"" + sourceLocation.getAbsolutePath() + "\"\n");
                }
              }
            }
          }
        }

        sourceLocations = List.of(tmpFile);
      } catch (IOException e) {
        throw new TranslationException(e);
      }
    }

    boolean useParallelFrontends = config.useParallelFrontends;

    for (int i = 0; i < sourceLocations.size(); i++) {
      File sourceLocation = sourceLocations.get(i);

      // Recursively add files in directories
      if (sourceLocation.isDirectory()) {
        try (Stream<Path> stream =
            Files.find(sourceLocation.toPath(), 999, (p, fileAttr) -> fileAttr.isRegularFile())) {
          sourceLocations.addAll(stream.map(Path::toFile).collect(Collectors.toSet()));
          sourceLocations.remove(sourceLocation);
        } catch (IOException e) {
          log.error(e.getMessage(), e);
        }
      } else {
        if (useParallelFrontends
            && getFrontendClass(Util.getExtension(sourceLocation)) == GoLanguageFrontend.class) {
          log.warn("Parallel frontends are not yet supported for Go");
          useParallelFrontends = false;
        }
      }
    }

    TypeManager.setTypeSystemActive(config.typeSystemActiveInFrontend);

    Set<LanguageFrontend> usedFrontends;
    if (useParallelFrontends) {
      usedFrontends = parseParallel(result, scopeManager, sourceLocations);
    } else {
      usedFrontends = parseSequentially(result, scopeManager, sourceLocations);
    }

    if (!config.typeSystemActiveInFrontend) {
      TypeManager.setTypeSystemActive(true);
      result.getTranslationUnits().forEach(tu -> SubgraphWalker.activateTypes(tu, scopeManager));
    }

    return usedFrontends;
  }

  private Set<LanguageFrontend> parseParallel(
      @NonNull TranslationResult result,
      @NonNull ScopeManager originalScopeManager,
      Collection<File> sourceLocations) {
    Set<LanguageFrontend> usedFrontends = new HashSet<>();

    log.info("Parallel parsing started");
    List<CompletableFuture<Optional<LanguageFrontend>>> futures = new ArrayList<>();
    List<ScopeManager> parallelScopeManagers = new ArrayList<>();
    Map<CompletableFuture<Optional<LanguageFrontend>>, File> futureToFile = new IdentityHashMap<>();

    for (File sourceLocation : sourceLocations) {
      ScopeManager scopeManager = new ScopeManager();
      parallelScopeManagers.add(scopeManager);
      CompletableFuture<Optional<LanguageFrontend>> future =
          getParsingFuture(result, scopeManager, sourceLocation);
      futures.add(future);
      futureToFile.put(future, sourceLocation);
    }

    for (CompletableFuture<Optional<LanguageFrontend>> future : futures) {
      try {
        future
            .get()
            .ifPresent(f -> handleCompletion(result, usedFrontends, futureToFile.get(future), f));
      } catch (InterruptedException | ExecutionException e) {
        log.error("Error parsing " + futureToFile.get(future), e);
        Thread.currentThread().interrupt();
      }
    }
    originalScopeManager.mergeFrom(parallelScopeManagers);
    usedFrontends.forEach(f -> f.setScopeManager(originalScopeManager));

    log.info("Parallel parsing completed");

    return usedFrontends;
  }

  private CompletableFuture<Optional<LanguageFrontend>> getParsingFuture(
      @NonNull TranslationResult result, @NonNull ScopeManager scopeManager, File sourceLocation) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return parse(result, scopeManager, sourceLocation);
          } catch (TranslationException e) {
            throw new RuntimeException("Error parsing " + sourceLocation, e);
          }
        });
  }

  private Set<LanguageFrontend> parseSequentially(
      @NonNull TranslationResult result,
      @NonNull ScopeManager scopeManager,
      Collection<File> sourceLocations)
      throws TranslationException {
    Set<LanguageFrontend> usedFrontends = new HashSet<>();

    for (File sourceLocation : sourceLocations) {

      log.info("Parsing {}", sourceLocation.getAbsolutePath());
      parse(result, scopeManager, sourceLocation)
          .ifPresent(f -> handleCompletion(result, usedFrontends, sourceLocation, f));
    }

    return usedFrontends;
  }

  private void handleCompletion(
      @NonNull TranslationResult result,
      Set<LanguageFrontend> usedFrontends,
      File sourceLocation,
      LanguageFrontend f) {
    usedFrontends.add(f);
    if (usedFrontends.stream().map(Object::getClass).distinct().count() > 1) {
      log.error(
          "Different frontends are used for multiple files. This will very likely break the following passes.");
    }

    // remember which frontend parsed each file
    Map<String, String> sfToFe =
        (Map<String, String>)
            result
                .getScratch()
                .computeIfAbsent(
                    TranslationResult.SOURCE_LOCATIONS_TO_FRONTEND,
                    x -> new HashMap<String, String>());
    sfToFe.put(sourceLocation.getName(), f.getClass().getSimpleName());

    // Set frontend so passes know what language they are working on.
    for (Pass pass : config.getRegisteredPasses()) {
      pass.setLang(f);
    }
  }

  private Optional<LanguageFrontend> parse(
      @NotNull TranslationResult result, @NotNull ScopeManager scopeManager, File sourceLocation)
      throws TranslationException {
    LanguageFrontend frontend = null;
    try {
      frontend = getFrontend(Util.getExtension(sourceLocation), scopeManager);
      if (frontend == null) {
        log.error("Found no parser frontend for {}", sourceLocation.getName());

        if (config.failOnError) {
          throw new TranslationException(
              "Found no parser frontend for " + sourceLocation.getName());
        }
        return Optional.empty();
      }

      TranslationUnitDeclaration tu = frontend.parse(sourceLocation);
      tu.setSha256(com.google.common.io.Files.asByteSource(sourceLocation).hash(Hashing.sha256()));
      result.addTranslationUnit(tu);
    } catch (TranslationException ex) {
      log.error(
          "An error occurred during parsing of {}: {}", sourceLocation.getName(), ex.getMessage());

      if (config.failOnError) {
        throw ex;
      }
    } catch (IOException ex) {
      String message = "Could not calculate file hash of " + sourceLocation.getName();
      log.error("{}: {}", message, ex.getMessage());

      if (config.failOnError) {
        throw new TranslationException(message, ex);
      }
    }

    return Optional.ofNullable(frontend);
  }

  @Nullable
  private LanguageFrontend getFrontend(String extension, ScopeManager scopeManager) {
    Class<? extends LanguageFrontend> clazz = getFrontendClass(extension);

    if (clazz != null) {
      try {
        return clazz
            .getConstructor(TranslationConfiguration.class, ScopeManager.class)
            .newInstance(this.config, scopeManager);
      } catch (InstantiationException
          | IllegalAccessException
          | InvocationTargetException
          | NoSuchMethodException e) {
        log.error("Could not instantiate language frontend {}", clazz.getName(), e);
        return null;
      }
    }

    return null;
  }

  @Nullable
  private Class<? extends LanguageFrontend> getFrontendClass(String extension) {
    var clazz =
        this.config.getFrontends().entrySet().stream()
            .filter(entry -> entry.getValue().contains(extension))
            .map(Entry::getKey)
            .findAny()
            .orElse(null);
    return clazz;
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
