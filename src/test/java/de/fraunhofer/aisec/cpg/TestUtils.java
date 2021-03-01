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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.statements.Statement;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.helpers.Util;
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

  public static <S extends Node> S findByUniquePredicate(
      Collection<S> nodes, Predicate<S> predicate) {
    List<S> results = findByPredicate(nodes, predicate);
    assertEquals(1, results.size(), "Expected exactly one node matching the predicate");
    return results.get(0);
  }

  public static <S extends Node> List<S> findByPredicate(
      Collection<S> nodes, Predicate<S> predicate) {
    return nodes.stream().filter(predicate).collect(Collectors.toList());
  }

  public static <S extends Node> S findByUniqueName(Collection<S> nodes, String name) {
    return findByUniquePredicate(nodes, m -> m.getName().equals(name));
  }

  public static <S extends Node> List<S> findByName(Collection<S> nodes, String name) {
    return findByPredicate(nodes, m -> m.getName().equals(name));
  }

  /**
   * Like {@link #analyze(List, Path, boolean)}, but for all files in a directory tree having a
   * specific file extension
   *
   * @param fileExtension All files found in the directory must end on this String. An empty string
   *     matches all files
   * @param topLevel The directory to traverse while looking for files to parse
   * @param usePasses Whether the analysis should run passes after the initial phase
   * @return A list of {@link TranslationUnitDeclaration} nodes, representing the CPG roots
   * @throws Exception Any exception thrown during the parsing process
   */
  public static List<TranslationUnitDeclaration> analyze(
      String fileExtension, Path topLevel, boolean usePasses) throws Exception {
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
            .disableCleanup()
            .debugParser(true)
            .failOnError(true);
    if (usePasses) {
      builder.defaultPasses();
    }
    TranslationConfiguration config = builder.build();

    TranslationManager analyzer = TranslationManager.builder().config(config).build();

    return analyzer.analyze().get().getTranslationUnits();
  }

  /**
   * Default way of parsing a list of files into a full CPG. All default passes are applied
   *
   * @param builder A {@link TranslationConfiguration.Builder} which contains the configuration
   * @return A list of {@link TranslationUnitDeclaration} nodes, representing the CPG roots
   * @throws Exception Any exception thrown during the parsing process
   */
  public static List<TranslationUnitDeclaration> analyzeWithBuilder(
      TranslationConfiguration.Builder builder) throws Exception {
    TranslationConfiguration config = builder.build();

    TranslationManager analyzer = TranslationManager.builder().config(config).build();

    return analyzer.analyze().get().getTranslationUnits();
  }

  public static TranslationUnitDeclaration analyzeAndGetFirstTU(
      List<File> files, Path topLevel, boolean usePasses) throws Exception {
    List<TranslationUnitDeclaration> translationUnits = analyze(files, topLevel, usePasses);
    return translationUnits.stream().findFirst().orElseThrow();
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
    List<Statement> nodes = subnodesOfType(body, Statement.class);
    for (Statement n : nodes) {
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

  /**
   * Returns the first element of the specified Class-type {@code specificClass} that has the name
   * {@code name} in the list {@code listOfNodes}.
   *
   * @param <S> Some class that extends {@link Node}.
   */
  public static <S extends Node> S getOfTypeWithName(
      List<Node> listOfNodes, Class<S> specificClass, String name) {
    List<S> listOfNodesWithName =
        Util.filterCast(listOfNodes, specificClass).stream()
            .filter(s -> s.getName().equals(name))
            .collect(Collectors.toList());
    if (listOfNodesWithName.isEmpty()) {
      return null;
    }
    // Here we return the first node, if there are more nodes
    return listOfNodesWithName.get(0);
  }

  /**
   * Returns the first element of the specified Class-type {code specifiedClass} that has the name
   * {@code name} in the list of nodes that are subnodes of the AST-root node {@code root}.
   *
   * @param <S> Some class that extends {@link Node}.
   */
  public static <S extends Node> S getSubnodeOfTypeWithName(
      Node root, Class<S> specificClass, String name) {
    return getOfTypeWithName(SubgraphWalker.flattenAST(root), specificClass, name);
  }

  /**
   * Given a root node in the AST graph, all AST children of the node are filtered for a specific
   * CPG Node type and returned.
   *
   * @param node root of the searched AST subtree
   * @param specificClass Class type to be searched for
   * @param <S> Type variable that allows returning a list of specific type
   * @return a List of searched types
   */
  public static <S extends Node> List<S> subnodesOfType(Node node, Class<S> specificClass) {
    return Util.filterCast(SubgraphWalker.flattenAST(node), specificClass).stream()
        .filter(Util.distinctByIdentity())
        .collect(Collectors.toList());
  }

  /**
   * Similar to {@link TestUtils#subnodesOfType(Node, Class)} but used when working with a list of
   * nodes that is already flat. No walking to get childnodes necessary.
   */
  public static <S extends Node> List<S> subnodesOfType(
      Collection<? extends Node> roots, Class<S> specificClass) {
    return roots.stream()
        .map(n -> subnodesOfType(n, specificClass))
        .flatMap(Collection::stream)
        .filter(Util.distinctByIdentity())
        .collect(Collectors.toList());
  }

  /**
   * Compare the given parameter {@code toCompare} to the start- or end-line of the given node. If
   * the node has no location {@code false} is returned. {@code startLine} is used to specify if the
   * start-line or end-line of a location is supposed to be used.
   *
   * @param n
   * @param startLine
   * @param toCompare
   * @return
   */
  public static boolean compareLineFromLocationIfExists(Node n, boolean startLine, int toCompare) {
    PhysicalLocation loc = n.getLocation();
    if (loc == null) {
      return false;
    }
    if (startLine) {
      return loc.getRegion().getStartLine() == toCompare;
    } else {
      return loc.getRegion().getEndLine() == toCompare;
    }
  }
}
