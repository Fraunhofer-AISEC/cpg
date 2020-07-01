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

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.mockito.Mockito;

public class TestUtils {

  public static <S extends Node> S findByPredicate(Collection<S> nodes, Predicate<S> predicate) {
    List<S> results = nodes.stream().filter(predicate).collect(Collectors.toList());
    assertEquals(1, results.size());
    return results.get(0);
  }

  public static <S extends Node> S findByUniqueName(Collection<S> nodes, String name) {
    List<S> results = findByName(nodes, name);
    assertEquals(1, results.size());
    return results.get(0);
  }

  public static <S extends Node> List<S> findByName(Collection<S> nodes, String name) {
    return nodes.stream().filter(m -> m.getName().equals(name)).collect(Collectors.toList());
  }

  /**
   * Like {@link #analyze(List, Path, boolean)}, but for all files in a directory tree having a
   * specific file extension
   *
   * @param fileExtension All files found in the directory must end on this String. An empty string
   *     matches all files
   * @param topLevel The directory to traverse while looking for files to parse
   * @param usePasses Whether the analysis should run passes after the initial phase
   * @param cleanupOnCompletion Whether {@link de.fraunhofer.aisec.cpg.graph.TypeManager} etc.
   *     should be cleaned up after the analysis has ended
   * @return A list of {@link TranslationUnitDeclaration} nodes, representing the CPG roots
   * @throws Exception Any exception thrown during the parsing process
   */
  public static List<TranslationUnitDeclaration> analyze(
      String fileExtension, Path topLevel, boolean usePasses, boolean cleanupOnCompletion)
      throws Exception {
    List<File> files =
        Files.walk(topLevel, Integer.MAX_VALUE)
            .map(Path::toFile)
            .filter(File::isFile)
            .filter(f -> f.getName().endsWith(fileExtension))
            .sorted()
            .collect(Collectors.toList());
    return analyze(files, topLevel, usePasses);
  }

  /**
   * Default way of parsing a list of files into a full CPG. All default passes are applied
   *
   * @param topLevel The directory to traverse while looking for files to parse
   * @param usePasses Whether the analysis should run passes after the initial phase
   * @return A list of {@link TranslationUnitDeclaration} nodes, representing the CPG roots
   * @throws Exception Any exception thrown during the parsing process
   */
  public static List<TranslationUnitDeclaration> analyze(
      List<File> files, Path topLevel, boolean usePasses) throws Exception {
    TranslationConfiguration.Builder builder =
        TranslationConfiguration.builder()
            .sourceLocations(files)
            .topLevel(topLevel.toFile())
            .loadIncludes(true)
            .debugParser(true)
            .failOnError(true);
    if (usePasses) {
      builder.defaultPasses();
    }
    TranslationConfiguration config = builder.build();

    TranslationManager analyzer = TranslationManager.builder().config(config).build();

    return analyzer.analyze().get().getTranslationUnits();
  }

  public static TranslationUnitDeclaration analyzeAndGetFirstTU(
      List<File> files, Path topLevel, boolean usePasses) throws Exception {
    List<TranslationUnitDeclaration> translationUnits = analyze(files, topLevel, usePasses);
    return translationUnits.stream()
        .filter(t -> !t.getName().equals("unknown declarations"))
        .findFirst()
        .orElseThrow();
  }

  /**
   * Returns the (first) statement at source line nr.
   *
   * <p>If a line contains several statements, only the first one is returned.
   *
   * @param body
   * @param line
   * @return Statement at source line or null if not present.
   */
  public static Node getByLineNr(CompoundStatement body, int line) {
    List<Node> nodes = SubgraphWalker.flattenAST(body);
    for (Node n : nodes) {
      PhysicalLocation location = n.getLocation();
      assertNotNull(location);

      if (location.getRegion().getStartLine() == line) {
        return n;
      }
    }
    return null;
  }

  static void disableTypeManagerCleanup() throws IllegalAccessException {
    TypeManager spy = Mockito.spy(TypeManager.getInstance());
    Mockito.doNothing().when(spy).cleanup();
    FieldUtils.writeStaticField(TypeManager.class, "INSTANCE", spy, true);
  }
}
