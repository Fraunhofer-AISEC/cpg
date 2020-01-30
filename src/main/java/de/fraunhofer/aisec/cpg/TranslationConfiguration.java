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

import de.fraunhofer.aisec.cpg.passes.CallResolver;
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass;
import de.fraunhofer.aisec.cpg.passes.FilenameMapper;
import de.fraunhofer.aisec.cpg.passes.ImportResolver;
import de.fraunhofer.aisec.cpg.passes.Pass;
import de.fraunhofer.aisec.cpg.passes.TypeHierarchyResolver;
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver;
import java.io.File;
import java.util.*;

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
  private List<File> sourceFiles;

  private File topLevel;
  private List<Pass> passes;

  private TranslationConfiguration(
      Map<String, String> symbols,
      List<File> sourceFiles,
      File topLevel,
      boolean debugParser,
      boolean failOnError,
      boolean loadIncludes,
      String[] includePaths,
      List<Pass> passes,
      boolean codeInNodes) {
    this.symbols = symbols;
    this.sourceFiles = sourceFiles;
    this.topLevel = topLevel;
    this.debugParser = debugParser;
    this.failOnError = failOnError;
    this.loadIncludes = loadIncludes;
    this.includePaths = includePaths;
    this.passes = passes != null ? passes : new ArrayList<>();
    // Make sure to init this AFTER sourceFiles has been set
    this.codeInNodes = codeInNodes;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Map<String, String> getSymbols() {
    return this.symbols;
  }

  public List<File> getSourceLocations() {
    return this.sourceFiles;
  }

  public File getTopLevel() {
    return topLevel;
  }

  public List<Pass> getRegisteredPasses() {
    return this.passes;
  }

  public static class Builder {
    private List<File> sourceFiles = new ArrayList<>();
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

    public Builder sourceFiles(File... sourceFiles) {
      this.sourceFiles = Arrays.asList(sourceFiles);
      return this;
    }

    public Builder topLevel(File topLevel) {
      this.topLevel = topLevel;
      return this;
    }

    public Builder debugParser(boolean debugParser) {
      this.debugParser = debugParser;
      return this;
    }

    public Builder failOnError(boolean failOnError) {
      this.failOnError = failOnError;
      return this;
    }

    public Builder loadIncludes(boolean loadIncludes) {
      this.loadIncludes = loadIncludes;
      return this;
    }

    public Builder includePath(String includePath) {
      this.includePaths.add(includePath);
      return this;
    }

    public Builder registerPass(Pass pass) {
      this.passes.add(pass);
      return this;
    }

    public Builder defaultPasses() {
      registerPass(new FilenameMapper());
      registerPass(new TypeHierarchyResolver());
      registerPass(new ImportResolver());
      registerPass(new VariableUsageResolver());
      registerPass(new CallResolver()); // creates CG
      registerPass(new EvaluationOrderGraphPass()); // creates EOG
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
          sourceFiles,
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
