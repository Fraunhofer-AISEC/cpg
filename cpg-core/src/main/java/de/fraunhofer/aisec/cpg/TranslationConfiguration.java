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
import static de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend.CXX_HEADER_EXTENSIONS;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import de.fraunhofer.aisec.cpg.frontends.CompilationDatabase;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend;
import de.fraunhofer.aisec.cpg.passes.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The configuration for the {@link TranslationManager} holds all information that is used during
 * the translation.
 */
public class TranslationConfiguration {

  private static final Logger log = LoggerFactory.getLogger(TranslationConfiguration.class);

  /** Set to true to generate debug output for the parser. */
  public final boolean debugParser;

  /**
   * Set to true to transitively load include files into the CPG.
   *
   * <p>If this value is set to false but includePaths are given, the parser will resolve
   * symbols/templates from these include, but do not load the parse tree into the CPG
   */
  public final boolean loadIncludes;

  /**
   * Paths to look for include files.
   *
   * <p>It is recommended to set proper include paths as otherwise unresolved symbols/templates will
   * result in subsequent parser mistakes, such as treating "{@literal <}" as a BinaryOperator in
   * the following example: {@literal <code> std::unique_ptr<Botan::Cipher_Mode> bla; </code>}
   *
   * <p>As long as loadIncludes is set to false, include files will only be parsed, but not loaded
   * into the CPG. *
   */
  public final String[] includePaths;

  /**
   * This acts as a white list for include files, if the array is not empty. Only the specified
   * includes files will be parsed and processed in the CPG, unless it is a port of the blacklist,
   * in which it will be ignored.
   */
  public final List<String> includeWhitelist;

  /**
   * This acts as a black list for include files, if the array is not empty. The specified includes
   * files will excluded from being parsed and processed in the CPG. The blacklist entries always
   * take priority over those in the whitelist.
   */
  public final List<String> includeBlacklist;

  /**
   * This map contains a list of language frontends classes and the file types they are registered
   * for.
   */
  private final Map<Class<? extends LanguageFrontend>, List<String>> frontends;

  /**
   * Switch off cleaning up TypeManager memory after analysis.
   *
   * <p>Set this to {@code true} only for testing.
   */
  public boolean disableCleanup = false;

  /** should the code of a node be shown as parameter in the node * */
  public final boolean codeInNodes;

  /** Set to true to process annotations or annotation-like elements. */
  public final boolean processAnnotations;

  /**
   * Should parser/translation fail on parse/resolving errors (true) or try to continue in a
   * best-effort manner (false).
   */
  final boolean failOnError;

  /** Definition of additional symbols, mostly useful for C++. */
  public final Map<String, String> symbols;

  /** Source code files to parse. */
  private final Map<String, List<File>> softwareComponents;

  private final File topLevel;

  /**
   * Only relevant for C++. A unity build refers to a build that consolidates all translation units
   * into a single one, which has the advantage that header files are only processed once, adding
   * far less duplicate nodes to the graph
   */
  final boolean useUnityBuild;

  /**
   * If true, the ASTs for the source files are parsed in parallel, but the passes afterwards will
   * still run in a single thread. This speeds up initial parsing but makes sure that further graph
   * enrichment algorithms remain correct.
   */
  final boolean useParallelFrontends;

  /**
   * If false, the type listener system is only activated once the frontends are done building the
   * initial AST structure. This avoids errors where the type of a node may depend on the order in
   * which the source files have been parsed.
   */
  final boolean typeSystemActiveInFrontend;

  /**
   * This is the data structure for storing the compilation database. It stores a mapping from the
   * File to the list of files that have to be included to their path, specified by the parameter in
   * the compilation database. This is currently only used by the {@link CXXLanguageFrontend}.
   *
   * <p>[{@link CompilationDatabase.Companion#fromFile(File)} can be used to construct a new
   * compilation database from a file.
   */
  final CompilationDatabase compilationDatabase;

  /**
   * If true the frontend shall use a heuristic matching of comments found in the source file to
   * match them to the closest AST node and save it in the comment property.
   */
  public final boolean matchCommentsToNodes;

  @NotNull private final List<Pass> passes;

  /** This sub configuration object holds all information about inference and smart-guessing. */
  final InferenceConfiguration inferenceConfiguration;

  private TranslationConfiguration(
      Map<String, String> symbols,
      Map<String, List<File>> softwareComponents,
      File topLevel,
      boolean debugParser,
      boolean failOnError,
      boolean loadIncludes,
      String[] includePaths,
      List<String> includeWhitelist,
      List<String> includeBlacklist,
      List<Pass> passes,
      Map<Class<? extends LanguageFrontend>, List<String>> frontends,
      boolean codeInNodes,
      boolean processAnnotations,
      boolean disableCleanup,
      boolean useUnityBuild,
      boolean useParallelFrontends,
      boolean typeSystemActiveInFrontend,
      InferenceConfiguration inferenceConfiguration,
      CompilationDatabase compilationDatabase,
      boolean matchCommentsToNodes) {
    this.symbols = symbols;
    this.softwareComponents = softwareComponents;
    this.topLevel = topLevel;
    this.debugParser = debugParser;
    this.failOnError = failOnError;
    this.loadIncludes = loadIncludes;
    this.includePaths = includePaths;
    this.includeWhitelist = includeWhitelist;
    this.includeBlacklist = includeBlacklist;
    this.passes = passes != null ? passes : new ArrayList<>();
    this.frontends = frontends;
    // Make sure to init this AFTER sourceLocations has been set
    this.codeInNodes = codeInNodes;
    this.processAnnotations = processAnnotations;
    this.disableCleanup = disableCleanup;
    this.useUnityBuild = useUnityBuild;
    this.useParallelFrontends = useParallelFrontends;
    this.typeSystemActiveInFrontend = typeSystemActiveInFrontend;
    this.inferenceConfiguration = inferenceConfiguration;
    this.compilationDatabase = compilationDatabase;
    this.matchCommentsToNodes = matchCommentsToNodes;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Map<String, String> getSymbols() {
    return this.symbols;
  }

  /** Returns a list of all analyzed files. */
  public List<File> getSourceLocations() {
    List<File> sourceLocations = new ArrayList<>();
    for (var entry : softwareComponents.entrySet()) {
      sourceLocations.addAll(entry.getValue());
    }
    return sourceLocations;
  }

  public Map<String, List<File>> getSoftwareComponents() {
    return this.softwareComponents;
  }

  @Nullable
  public CompilationDatabase getCompilationDatabase() {
    return this.compilationDatabase;
  }

  public File getTopLevel() {
    return topLevel;
  }

  @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "name")
  @JsonIdentityReference(alwaysAsId = true)
  public List<Pass> getRegisteredPasses() {
    return this.passes;
  }

  public Map<Class<? extends LanguageFrontend>, List<String>> getFrontends() {
    return this.frontends;
  }

  public InferenceConfiguration getInferenceConfiguration() {
    return this.inferenceConfiguration;
  }

  /**
   * Builds a {@link TranslationConfiguration}.
   *
   * <p>Example:
   *
   * <pre>{@code
   * TranslationManager.builder()
   *    .config( TranslationConfiguration.builder()
   *        .sourceLocations(new File("example.cpp"))
   *        .defaultPasses()
   *        .debugParser(true)
   *        .build())
   * .build();
   * }</pre>
   */
  public static class Builder {
    private Map<String, List<File>> softwareComponents = new HashMap<>();
    private final Map<Class<? extends LanguageFrontend>, List<String>> frontends = new HashMap<>();
    private File topLevel = null;
    private boolean debugParser = false;
    private boolean failOnError = false;
    private boolean loadIncludes = false;
    private Map<String, String> symbols = new HashMap<>();
    private final List<String> includePaths = new ArrayList<>();
    private final List<String> includeWhitelist = new ArrayList<>();
    private final List<String> includeBlacklist = new ArrayList<>();
    private final List<Pass> passes = new ArrayList<>();
    private boolean codeInNodes = true;
    private boolean processAnnotations = false;
    private boolean disableCleanup = false;
    private boolean useUnityBuild = false;
    private boolean useParallelFrontends = false;
    private boolean typeSystemActiveInFrontend = true;
    private InferenceConfiguration inferenceConfiguration =
        new InferenceConfiguration.Builder().build();
    private CompilationDatabase compilationDatabase;
    private boolean matchCommentsToNodes = false;

    public Builder symbols(Map<String, String> symbols) {
      this.symbols = symbols;
      return this;
    }

    /**
     * Files or directories containing the source code to analyze. Generates a dummy software
     * component called "application".
     *
     * @param sourceLocations The files with the source code
     * @return this
     */
    public Builder sourceLocations(File... sourceLocations) {
      this.softwareComponents.put("application", Arrays.asList(sourceLocations));
      return this;
    }

    /**
     * Files or directories containing the source code to analyze. Generates a dummy software
     * component called "application".
     *
     * @param sourceLocations The files with the source code
     * @return this
     */
    public Builder sourceLocations(List<File> sourceLocations) {
      this.softwareComponents.put("application", sourceLocations);
      return this;
    }

    /**
     * Files or directories containing the source code to analyze organized by different components
     *
     * @param softwareComponents A map holding the different components with their files
     * @return this
     */
    public Builder softwareComponents(Map<String, List<File>> softwareComponents) {
      this.softwareComponents = softwareComponents;
      return this;
    }

    public Builder useCompilationDatabase(CompilationDatabase compilationDatabase) {
      this.compilationDatabase = compilationDatabase;
      return this;
    }

    public Builder topLevel(File topLevel) {
      this.topLevel = topLevel;
      return this;
    }

    /**
     * Dump parser debug output to the logs (Caution: this will generate a lot of output).
     *
     * @param debugParser
     * @return
     */
    public Builder debugParser(boolean debugParser) {
      this.debugParser = debugParser;
      return this;
    }

    /**
     * Match comments found in source files to nodes according to a heuristic.
     *
     * @param matchCommentsToNodes
     * @return
     */
    public Builder matchCommentsToNodes(boolean matchCommentsToNodes) {
      this.matchCommentsToNodes = matchCommentsToNodes;
      return this;
    }

    /**
     * Fail analysis on first error. Try to continue otherwise.
     *
     * @param failOnError
     * @return
     */
    public Builder failOnError(boolean failOnError) {
      this.failOnError = failOnError;
      return this;
    }

    /**
     * Load C/C++ include headers before the analysis.
     *
     * <p>Required for macro expansion.
     *
     * @param loadIncludes
     * @return
     */
    public Builder loadIncludes(boolean loadIncludes) {
      this.loadIncludes = loadIncludes;
      return this;
    }

    /**
     * Directory containing include headers.
     *
     * @param includePath
     * @return
     */
    public Builder includePath(String includePath) {
      this.includePaths.add(includePath);
      return this;
    }

    /**
     * Adds the specified file to the include whitelist. Relative and absolute paths are supported.
     *
     * @param includeFile
     * @return
     */
    public Builder includeWhitelist(String includeFile) {
      this.includeWhitelist.add(includeFile);
      return this;
    }

    public Builder disableCleanup() {
      this.disableCleanup = true;
      return this;
    }

    /**
     * Adds the specified file to the include blacklist. Relative and absolute paths are supported.
     *
     * @param includeFile
     * @return
     */
    public Builder includeBlacklist(String includeFile) {
      this.includeBlacklist.add(includeFile);
      return this;
    }

    /**
     * Register an additional {@link Pass}.
     *
     * @param pass
     * @return
     */
    public Builder registerPass(@NotNull Pass pass) {
      this.passes.add(pass);
      return this;
    }

    /** Registers an additional {@link de.fraunhofer.aisec.cpg.frontends.LanguageFrontend}. */
    public Builder registerLanguage(
        @NotNull Class<? extends LanguageFrontend> frontend, List<String> fileTypes) {
      this.frontends.put(frontend, fileTypes);
      return this;
    }

    /** Unregisters a registered {@link de.fraunhofer.aisec.cpg.frontends.LanguageFrontend}. */
    public Builder unregisterLanguage(@NotNull Class<? extends LanguageFrontend> frontend) {
      this.frontends.remove(frontend);
      return this;
    }

    /**
     * Register all default {@link Pass}es.
     *
     * <p>This will register
     *
     * <ol>
     *   <li>FilenameMapper
     *   <li>TypeHierarchyResolver
     *   <li>ImportResolver
     *   <li>VariableUsageResolver
     *   <li>CallResolver
     *   <li>EvaluationOrderGraphPass
     *   <li>TypeResolver
     * </ol>
     *
     * to be executed exactly in the specified order.
     *
     * @return
     */
    public Builder defaultPasses() {
      registerPass(new TypeHierarchyResolver());
      registerPass(new JavaExternalTypeHierarchyResolver());
      registerPass(new ImportResolver());
      registerPass(new VariableUsageResolver());
      registerPass(new CallResolver()); // creates CG
      registerPass(new EvaluationOrderGraphPass()); // creates EOG
      registerPass(new TypeResolver());
      registerPass(new ControlFlowSensitiveDFGPass());
      registerPass(new FilenameMapper());
      return this;
    }

    /** Register extra passes declared by a frontend with [PassRegisterExtraPass] */
    private void registerExtraFrontendPasses() {

      for (Class<? extends LanguageFrontend> frontend : frontends.keySet()) {
        if (frontend.isAnnotationPresent(PassRegisterExtraPass.class)) {
          PassRegisterExtraPass[] extraPasses =
              frontend.getAnnotationsByType(PassRegisterExtraPass.class);
          for (PassRegisterExtraPass p : extraPasses) {
            try {
              var clazz = p.value();
              registerPass(clazz.getConstructor().newInstance());
              log.info(
                  "Registered an extra (frontend dependent) default dependency: {}", p.value());

            } catch (NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException ignored) {
            }
          }
        }
      }
    }

    /**
     * Register all default languages.
     *
     * @return
     */
    public Builder defaultLanguages() {
      registerLanguage(
          CXXLanguageFrontend.class,
          Lists.newArrayList(Iterables.concat(CXX_EXTENSIONS, CXX_HEADER_EXTENSIONS)));
      registerLanguage(JavaLanguageFrontend.class, JavaLanguageFrontend.JAVA_EXTENSIONS);

      // do not register experimental languages by default until we have a release strategy
      // registerLanguage(GoLanguageFrontend.class, GoLanguageFrontend.GOLANG_EXTENSIONS);
      return this;
    }

    public Builder codeInNodes(boolean b) {
      this.codeInNodes = b;
      return this;
    }

    /**
     * Specifies, whether annotations should be process or not. By default, they are not processed,
     * since they might populate the graph too much.
     *
     * @param b the new value
     * @return
     */
    public Builder processAnnotations(boolean b) {
      this.processAnnotations = b;
      return this;
    }

    /**
     * Only relevant for C++. A unity build refers to a build that consolidates all translation
     * units into a single one, which has the advantage that header files are only processed once,
     * adding far less duplicate nodes to the graph
     *
     * @param b the new value
     * @return
     */
    public Builder useUnityBuild(boolean b) {
      this.useUnityBuild = b;
      return this;
    }

    /**
     * If true, the ASTs for the source files are parsed in parallel, but the passes afterwards will
     * still run in a single thread. This speeds up initial parsing but makes sure that further
     * graph enrichment algorithms remain correct. Please make sure to also set {@link
     * #typeSystemActiveInFrontend(boolean)} to false to avoid probabilistic errors that appear
     * depending on the parsing order.
     *
     * @param b the new value
     * @return
     */
    public Builder useParallelFrontends(boolean b) {
      this.useParallelFrontends = b;
      return this;
    }

    /**
     * If false, the type system is only activated once the frontends are done building the initial
     * AST structure. This avoids errors where the type of a node may depend on the order in which
     * the source files have been parsed.
     *
     * @param b the new value
     * @return
     */
    public Builder typeSystemActiveInFrontend(boolean b) {
      this.typeSystemActiveInFrontend = b;
      return this;
    }

    public Builder inferenceConfiguration(InferenceConfiguration configuration) {
      this.inferenceConfiguration = configuration;
      return this;
    }

    public TranslationConfiguration build() throws ConfigurationException {
      if (useParallelFrontends && typeSystemActiveInFrontend) {
        log.warn(
            "Not disabling the type system during the frontend "
                + "phase is not recommended when using the parallel frontends feature! "
                + "This may result in erroneous results.");
      }

      registerExtraFrontendPasses();

      return new TranslationConfiguration(
          symbols,
          softwareComponents,
          topLevel,
          debugParser,
          failOnError,
          loadIncludes,
          includePaths.toArray(new String[] {}),
          includeWhitelist,
          includeBlacklist,
          orderPasses(),
          frontends,
          codeInNodes,
          processAnnotations,
          disableCleanup,
          useUnityBuild,
          useParallelFrontends,
          typeSystemActiveInFrontend,
          inferenceConfiguration,
          compilationDatabase,
          matchCommentsToNodes);
    }

    /**
     * Collects the requested passes stored in [passes] and generates a [PassOrderingWorkingList]
     * consisting of pairs of passes and their dependencies.
     *
     * @return A populated [PassOrderingWorkingList] derived from [passes].
     */
    private PassOrderingWorkingList collectInitialPasses() {
      PassOrderingWorkingList workingList = new PassOrderingWorkingList();
      for (Pass p : passes) {
        boolean passFound = false;
        for (PassOrderingPassWithDependencies wl : workingList.getWorkingList()) {
          if (wl.getPass().getClass() == p.getClass()) {
            passFound = true;
            break;
          }
        }
        if (!passFound) {
          Set<Class<? extends Pass>> deps = new HashSet<>();
          deps.addAll(p.getHardDependencies());
          deps.addAll(p.getSoftDependencies());
          workingList.addToWorkingList(new PassOrderingPassWithDependencies(p, deps));
        }
      }
      return workingList;
    }

    /**
     * This function reorders passes in order to meet their dependency requirements.
     *
     * <ul>
     *   <li>soft dependencies [PassRegisterSoftDependency]: all passes registered as soft
     *       dependency will be executed before the current pass if they are registered
     *   <li>hard dependencies [PassRegisterHardDependency]: all passes registered as hard
     *       dependency will be executed before the current pass (hard dependencies will be
     *       registered even if the user did not register them)
     *   <li>first pass [PassIsFirstPass]: a pass registered as first pass will be executed in the
     *       beginning
     *   <li>last pass [PassIsLastPass]: a pass registered as last pass will be executed at the end
     * </ul>
     *
     * <p>This function uses a very simple (and inefficient) logic to meet the requirements above:
     *
     * <ol>
     *   <li>A list of all registered passes and their dependencies is build [workingList]
     *   <li>All missing hard dependencies [PassRegisterHardDependency] are added to the
     *       [workingList]
     *   <li>The first pass [PassIsFirstPass] is added to the result and removed from the other
     *       passes dependencies
     *   <li>The first pass in the [workingList] without dependencies is added to the result and it
     *       is removed from the other passes dependencies
     *   <li>The above step is repeated until all passes are added to the result
     * </ol>
     *
     * @return a sorted list of passes
     */
    private List<Pass> orderPasses() throws ConfigurationException {

      log.info("Passes before enforcing order: {}", passes);

      List<Pass> result = new ArrayList<>();

      // Create a local copy of all passes and their "current" dependencies without possible
      // duplicates
      PassOrderingWorkingList workingList = collectInitialPasses();

      log.debug("Working list after initial scan: {}", workingList);

      workingList.addMissingDependencies();
      log.debug("Working list after adding missing dependencies: {}", workingList);

      if (workingList.getFirstPasses().size() > 1) {
        log.error(
            "Too many passes require to be executed as first pass: {}",
            workingList.getWorkingList());
        throw new ConfigurationException("Too many passes require to be executed as first pass.");
      }

      if (workingList.getLastPasses().size() > 1) {
        log.error(
            "Too many passes require to be executed as last pass: {}", workingList.getLastPasses());
        throw new ConfigurationException("Too many passes require to be executed as last pass.");
      }

      while (!workingList.isEmpty()) {
        Pass p = workingList.getAndRemoveFirstPassWithoutDependencies();
        if (p != null) {
          result.add(p);
        } else {
          // failed to find a pass that can be added to the result -> deadlock :(
          throw new ConfigurationException("Failed to satisfy ordering requirements.");
        }
      }

      log.info("Passes after enforcing order: {}", result);
      return result;
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
  }
}
