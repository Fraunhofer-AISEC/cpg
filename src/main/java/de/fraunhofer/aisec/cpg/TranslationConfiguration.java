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

import de.fraunhofer.aisec.cpg.passes.*;
import java.io.File;
import java.util.*;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * The configuration for the {@link TranslationManager} holds all information that is used during
 * the translation.
 */
public class TranslationConfiguration {

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

  /** should the code of a node be shown as parameter in the node * */
  public final boolean codeInNodes;

  /**
   * Should parser/translation fail on parse/resolving errors (true) or try to continue in a
   * best-effort manner (false).
   */
  final boolean failOnError;

  /** Definition of additional symbols, mostly useful for C++. */
  public final Map<String, String> symbols;

  /** Source code files to parse. */
  private List<File> sourceLocations;

  private File topLevel;

  @NonNull private List<Pass> passes;

  private TranslationConfiguration(
      Map<String, String> symbols,
      List<File> sourceLocations,
      File topLevel,
      boolean debugParser,
      boolean failOnError,
      boolean loadIncludes,
      String[] includePaths,
      List<Pass> passes,
      boolean codeInNodes) {
    this.symbols = symbols;
    this.sourceLocations = sourceLocations;
    this.topLevel = topLevel;
    this.debugParser = debugParser;
    this.failOnError = failOnError;
    this.loadIncludes = loadIncludes;
    this.includePaths = includePaths;
    this.passes = passes != null ? passes : new ArrayList<>();
    // Make sure to init this AFTER sourceLocations has been set
    this.codeInNodes = codeInNodes;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Map<String, String> getSymbols() {
    return this.symbols;
  }

  public List<File> getSourceLocations() {
    return this.sourceLocations;
  }

  public File getTopLevel() {
    return topLevel;
  }

  public List<Pass> getRegisteredPasses() {
    return this.passes;
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
    private List<File> sourceLocations = new ArrayList<>();
    private File topLevel = null;
    private boolean debugParser = false;
    private boolean failOnError = false;
    private boolean loadIncludes = false;
    private Map<String, String> symbols = new HashMap<>();
    private List<String> includePaths = new ArrayList<>();
    private List<Pass> passes = new ArrayList<>();
    private boolean codeInNodes = true;

    public Builder symbols(Map<String, String> symbols) {
      this.symbols = symbols;
      return this;
    }

    /**
     * Files or directories containing the source code to analyze.
     *
     * @param sourceLocations
     * @return
     */
    public Builder sourceLocations(File... sourceLocations) {
      this.sourceLocations = Arrays.asList(sourceLocations);
      return this;
    }

    /**
     * Files or directories containing the source code to analyze
     *
     * @param sourceLocations
     * @return
     */
    public Builder sourceLocations(List<File> sourceLocations) {
      this.sourceLocations = sourceLocations;
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
     * Register an additional {@link Pass}.
     *
     * @param pass
     * @return
     */
    public Builder registerPass(@NonNull Pass pass) {
      this.passes.add(pass);
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
     * to be executed exactly in the specified order
     *
     * @return
     */
    public Builder defaultPasses() {
      registerPass(new FilenameMapper());
      registerPass(new TypeHierarchyResolver());
      registerPass(new JavaExternalTypeHierarchyResolver());
      registerPass(new ImportResolver());
      registerPass(new VariableUsageResolver());
      registerPass(new CallResolver()); // creates CG
      registerPass(new EvaluationOrderGraphPass()); // creates EOG
      registerPass(new TypeResolver());
      return this;
    }

    public Builder codeInNodes(boolean b) {
      this.codeInNodes = b;
      return this;
    }

    public TranslationConfiguration build() {
      String[] paths = new String[this.includePaths.size()];
      return new TranslationConfiguration(
          symbols,
          sourceLocations,
          topLevel,
          debugParser,
          failOnError,
          loadIncludes,
          includePaths.toArray(paths),
          passes,
          codeInNodes);
    }
  }
}
