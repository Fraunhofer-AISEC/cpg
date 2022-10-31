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
package de.fraunhofer.aisec.cpg.frontends;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.graph.CodeAndLocationProvider;
import de.fraunhofer.aisec.cpg.graph.LanguageProvider;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import de.fraunhofer.aisec.cpg.sarif.Region;
import java.io.File;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LanguageFrontend extends ProcessedListener
    implements CodeAndLocationProvider, LanguageProvider {

  // Allow non-Java frontends to access the logger (i.e. jep)
  public static final Logger log = LoggerFactory.getLogger(LanguageFrontend.class);

  protected Language<? extends LanguageFrontend> language;

  protected final TranslationConfiguration config;

  protected ScopeManager scopeManager;

  private final String namespaceDelimiter;
  protected TranslationUnitDeclaration currentTU = null;

  // Todo Moving this to scope manager and add listeners and processedMappings to specified scopes.

  public LanguageFrontend(
      @NotNull Language<? extends LanguageFrontend> language,
      @NotNull TranslationConfiguration config,
      ScopeManager scopeManager,
      String namespaceDelimiter) {
    this.language = language;
    this.config = config;
    this.namespaceDelimiter = namespaceDelimiter;
    this.scopeManager = scopeManager;
    this.scopeManager.setLang(this);
  }

  public List<TranslationUnitDeclaration> parseAll() throws TranslationException {
    ArrayList<TranslationUnitDeclaration> units = new ArrayList<>();
    for (String component : this.config.getSoftwareComponents().keySet()) {
      for (File sourceFile : this.config.getSoftwareComponents().get(component)) {
        units.add(parse(sourceFile));
      }
    }

    return units;
  }

  @NotNull
  public ScopeManager getScopeManager() {
    return scopeManager;
  }

  public void setScopeManager(@NotNull ScopeManager scopeManager) {
    this.scopeManager = scopeManager;
    this.scopeManager.setLang(this);
  }

  public TranslationUnitDeclaration getCurrentTU() {
    return currentTU;
  }

  public void setCurrentTU(TranslationUnitDeclaration currentTU) {
    this.currentTU = currentTU;
  }

  public abstract TranslationUnitDeclaration parse(File file) throws TranslationException;

  /**
   * Returns the raw code of the ast node, generic for java or c++ ast nodes.
   *
   * @param <T> the raw ast type
   * @param astNode the ast node
   * @return the source code
   */
  @Nullable
  public abstract <T> String getCodeFromRawNode(T astNode);

  /**
   * Returns the {@link Region} of the code with line and column, index starting at 1, generic for
   * java or c++ ast nodes.
   *
   * @param <T> the raw ast type
   * @param astNode the ast node
   * @return the location
   */
  @Nullable
  public abstract <T> PhysicalLocation getLocationFromRawNode(T astNode);

  public <N, S> void setCodeAndLocation(@NotNull N cpgNode, @Nullable S astNode) {
    if (cpgNode instanceof Node && astNode != null) {
      if (config.codeInNodes) {
        // only set code, if its not already set or empty
        String code = getCodeFromRawNode(astNode);

        if (code != null) {
          ((Node) cpgNode).setCode(code);
        } else {
          log.warn("Unexpected: No code for node {}", astNode);
        }
      }
      ((Node) cpgNode).setLocation(getLocationFromRawNode(astNode));
    }
  }

  /**
   * To prevent issues with different newline types and formatting.
   *
   * @param node - The newline type is extracted from the nodes code.
   * @return the String of the newline
   */
  public String getNewLineType(Node node) {
    List<String> nls = Arrays.asList("\n\r", "\r\n", "\n");
    for (String nl : nls) {
      if (node.toString().endsWith(nl)) {
        return nl;
      }
    }
    log.debug("Could not determine newline type. Assuming \\n. {}", node);
    return "\n";
  }

  /**
   * Returns the code represented by the subregion extracted from the parent node and its region.
   *
   * @param node - The parent node of the subregion
   * @param nodeRegion - region needs to be precomputed.
   * @param subRegion - precomputed subregion
   * @return the code of the subregion.
   */
  public String getCodeOfSubregion(
      de.fraunhofer.aisec.cpg.graph.Node node, Region nodeRegion, Region subRegion) {
    String code = node.getCode();
    if (code == null) {
      return "";
    }
    String nlType = getNewLineType(node);
    int start;
    int end;
    if (subRegion.getStartLine() == nodeRegion.getStartLine()) {
      start = subRegion.getStartColumn() - nodeRegion.getStartColumn();
    } else {
      start =
          StringUtils.ordinalIndexOf(
                  code, nlType, subRegion.getStartLine() - nodeRegion.getStartLine())
              + subRegion.getStartColumn();
    }
    if (subRegion.getEndLine() == nodeRegion.getStartLine()) {
      end = subRegion.getEndColumn() - nodeRegion.getStartColumn();
    } else {
      end =
          StringUtils.ordinalIndexOf(
                  code, nlType, subRegion.getEndLine() - nodeRegion.getStartLine())
              + subRegion.getEndColumn();
    }
    return code.substring(start, end);
  }

  /**
   * Merges two regions. The new region contains both and is the minimal region to do so.
   *
   * @param regionOne the first region
   * @param regionTwo the second region
   * @return the merged region
   */
  public Region mergeRegions(Region regionOne, Region regionTwo) {
    Region ret = new Region();
    if (regionOne.getStartLine() < regionTwo.getStartLine()
        || regionOne.getStartLine() == regionTwo.getStartLine()
            && regionOne.getStartColumn() < regionTwo.getStartColumn()) {
      ret.setStartLine(regionOne.getStartLine());
      ret.setStartColumn(regionOne.getStartColumn());
    } else {
      ret.setStartLine(regionTwo.getStartLine());
      ret.setStartColumn(regionTwo.getStartColumn());
    }
    if (regionOne.getEndLine() > regionTwo.getEndLine()
        || regionOne.getEndLine() == regionTwo.getEndLine()
            && regionOne.getEndColumn() > regionTwo.getEndColumn()) {
      ret.setEndLine(regionOne.getEndLine());
      ret.setEndColumn(regionOne.getStartColumn());
    } else {
      ret.setEndLine(regionTwo.getEndLine());
      ret.setEndColumn(regionTwo.getEndColumn());
    }
    return ret;
  }

  public void cleanup() {
    clearProcessed();
  }

  public String getNamespaceDelimiter() {
    return namespaceDelimiter;
  }

  public abstract <S, T> void setComment(S s, T ctx);

  public TranslationConfiguration getConfig() {
    return config;
  }

  @NotNull
  public Language<? extends LanguageFrontend> getLanguage() {
    return this.language;
  }
}
