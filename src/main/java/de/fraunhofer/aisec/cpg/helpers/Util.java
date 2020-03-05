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

package de.fraunhofer.aisec.cpg.helpers;

import static java.nio.charset.StandardCharsets.UTF_8;

import de.fraunhofer.aisec.cpg.graph.Node;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Util {

  /**
   * Filters a list of elements with common type T for all elements of instance S, returning a list
   * of type {@link List}.
   *
   * @param genericList List with elements fo type T.
   * @param specificClass Class type to filter for.
   * @param <T> Generic List type.
   * @param <S> Class type to filter for.
   * @return a specific List as all elements are cast to the specified class type.
   */
  public static <T, S extends T> List<S> filterCast(List<T> genericList, Class<S> specificClass) {
    return genericList.stream()
        .filter(g -> specificClass.isAssignableFrom(g.getClass()))
        .map(specificClass::cast)
        .collect(Collectors.toList());
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
    return filterCast(SubgraphWalker.flattenAST(node), specificClass).stream()
        .filter(distinctByIdentity())
        .collect(Collectors.toList());
  }

  public static <S extends Node> List<S> subnodesOfType(
      Collection<? extends Node> roots, Class<S> specificClass) {
    return roots.stream()
        .map(n -> subnodesOfType(n, specificClass))
        .flatMap(Collection::stream)
        .filter(distinctByIdentity())
        .collect(Collectors.toList());
  }

  /**
   * Filters the nodes in the AST subtree at root <code>node</code> for Nodes with the specified
   * code.
   *
   * @param node root of the subtree that is searched.
   * @param searchCode exact code that a node needs to have.
   * @return a list of nodes with the specified String.
   */
  public static List<Node> subnodesOfCode(Node node, String searchCode) {
    return SubgraphWalker.flattenAST(node).stream()
        .filter(n -> n.getCode() != null && n.getCode().equals(searchCode))
        .collect(Collectors.toList());
  }

  public static boolean eogConnect(Connect cn, Edge en, Node n, Connect cr, Node... refs) {
    return eogConnect(Quantifier.ALL, cn, en, n, cr, refs);
  }

  public static boolean eogConnect(Quantifier q, Connect cn, Edge en, Node n, Node... refs) {
    return eogConnect(q, cn, en, n, Connect.SUBTREE, refs);
  }

  public static boolean eogConnect(Quantifier q, Edge en, Node n, Connect cr, Node... refs) {
    return eogConnect(q, Connect.SUBTREE, en, n, cr, refs);
  }

  public static boolean eogConnect(Connect cn, Edge en, Node n, Node... refs) {
    return eogConnect(Quantifier.ALL, cn, en, n, Connect.SUBTREE, refs);
  }

  public static boolean eogConnect(Quantifier q, Edge en, Node n, Node... refs) {
    return eogConnect(q, Connect.SUBTREE, en, n, Connect.SUBTREE, refs);
  }

  public static boolean eogConnect(Edge en, Node n, Connect cr, Node... refs) {
    return eogConnect(Quantifier.ALL, Connect.SUBTREE, en, n, cr, refs);
  }

  public static boolean eogConnect(Edge en, Node n, Node... refs) {
    return eogConnect(Quantifier.ALL, Connect.SUBTREE, en, n, Connect.SUBTREE, refs);
  }

  /**
   * Checks if the Node <code>n</code> connects to the nodes in <code>refs</code> over the CPGS EOG
   * graph edges that depict the evaluation order. The parameter q defines if all edges of interest
   * to node must connect to an edge in refs or one is enough, cn and cr define whether the passed
   * AST nodes themselves are used to search the connections or the EOG Border nodes in the AST
   * subnode. Finally, en defines whether the EOG edges go * from n to r in refs or the inverse.
   *
   * @param q - The quantifier, ALL or ANY node of n must connect to refs, defaults to ALL.
   * @param cn - NODE if n itself is the NODE to connect or SUBTREE if the EOG borders are of
   *     interest. Defaults to SUBTREE
   * @param en - The Edge direction and therefore the borders of n to connect to refs (ENTRIES,
   *     EXITS)
   * @param n - Node of interest
   * @param cr - NODE if refs nodes itself are the NODE to connect or SUBTREE if the EOG borders are
   *     of interest
   * @param refs - Multiple reference nodes that can be passed as varargs
   * @return true if all/any of the connections from node connect to n.
   */
  public static boolean eogConnect(
      Quantifier q, Connect cn, Edge en, Node n, Connect cr, Node... refs) {
    List<Node> nodeSide = List.of(n);
    Edge er = en == Edge.ENTRIES ? Edge.EXITS : Edge.ENTRIES;
    List<Node> refSide = Arrays.asList(refs);
    if (cn == Connect.SUBTREE) {
      SubgraphWalker.Border border = SubgraphWalker.getEOGPathEdges(n);
      nodeSide =
          en == Edge.ENTRIES
              ? border.getEntries().stream()
                  .flatMap(r -> r.getPrevEOG().stream())
                  .collect(Collectors.toList())
              : border.getExits();
    } else {
      nodeSide =
          nodeSide.stream()
              .flatMap(node -> (en == Edge.ENTRIES ? node.getPrevEOG() : List.of(node)).stream())
              .collect(Collectors.toList());
    }
    if (cr == Connect.SUBTREE) {
      List<SubgraphWalker.Border> borders =
          Arrays.stream(refs).map(SubgraphWalker::getEOGPathEdges).collect(Collectors.toList());
      refSide =
          borders.stream()
              .flatMap(
                  border ->
                      (Edge.ENTRIES == er
                          ? border.getEntries().stream().flatMap(r -> r.getPrevEOG().stream())
                          : border.getExits().stream()))
              .collect(Collectors.toList());
    } else {
      refSide =
          refSide.stream()
              .flatMap(node -> (er == Edge.ENTRIES ? node.getPrevEOG() : List.of(node)).stream())
              .collect(Collectors.toList());
    }
    final List<Node> refNodes = refSide;
    return Quantifier.ANY == q
        ? nodeSide.stream().anyMatch(refNodes::contains)
        : refNodes.containsAll(nodeSide);
  }

  public static String inputStreamToString(InputStream inputStream) throws IOException {
    try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[1024];
      int length;
      while ((length = inputStream.read(buffer)) != -1) {
        result.write(buffer, 0, length);
      }

      return result.toString(UTF_8);
    }
  }

  public static <T> Predicate<T> distinctByIdentity() {
    Map<Object, Boolean> seen = new IdentityHashMap<>();
    return t -> {
      if (seen.containsKey(t)) {
        return false;
      } else {
        seen.put(t, true);
        return true;
      }
    };
  }

  public static <T> Predicate<T> distinctBy(Function<? super T, ?> by) {
    Set<Object> seen = new HashSet<>();
    return t -> seen.add(by.apply(t));
  }

  public static String getExtension(@NonNull File file) {
    int pos = file.getName().lastIndexOf('.');
    if (pos > 0) {
      return file.getName().substring(pos).toLowerCase();
    } else {
      return "";
    }
  }

  public enum Connect {
    NODE,
    SUBTREE;
  }

  public enum Quantifier {
    ANY,
    ALL;
  }

  public enum Edge {
    ENTRIES,
    EXITS;
  }
}
