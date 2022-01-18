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
package de.fraunhofer.aisec.cpg.helpers;

import static de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMethodParameterIn;
import static de.fraunhofer.aisec.cpg.sarif.PhysicalLocation.locationLink;
import static java.nio.charset.StandardCharsets.UTF_8;

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.ParamVariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType;
import de.fraunhofer.aisec.cpg.graph.types.PointerType;
import de.fraunhofer.aisec.cpg.graph.types.ReferenceType;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;

public class Util {

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
   * @param q - The quantifier, all or any node of n must connect to refs, defaults to ALL.
   * @param cn - NODE if n itself is the node to connect or SUBTREE if the EOG borders are of
   *     interest. Defaults to SUBTREE
   * @param en - The Edge direction and therefore the borders of n to connect to refs
   * @param n - Node of interest
   * @param cr - NODE if refs nodes itself are the nodes to connect or SUBTREE if the EOG borders
   *     are of interest
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

  public static <S> void warnWithFileLocation(
      @NonNull LanguageFrontend lang, S astNode, Logger log, String format, Object... arguments) {
    log.warn(
        String.format("%s: %s", locationLink(lang.getLocationFromRawNode(astNode)), format),
        arguments);
  }

  public static <S> void errorWithFileLocation(
      @NonNull LanguageFrontend lang, S astNode, Logger log, String format, Object... arguments) {
    log.error(
        String.format("%s: %s", locationLink(lang.getLocationFromRawNode(astNode)), format),
        arguments);
  }

  public static void warnWithFileLocation(
      @NonNull Node node, Logger log, String format, Object... arguments) {
    log.warn(String.format("%s: %s", locationLink(node.getLocation()), format), arguments);
  }

  public static void errorWithFileLocation(
      @NonNull Node node, Logger log, String format, Object... arguments) {
    log.error(String.format("%s: %s", locationLink(node.getLocation()), format), arguments);
  }

  /**
   * Split a String into multiple parts by using one or more delimiter characters. Any delimiters
   * that are surrounded by matching opening and closing brackets are skipped. E.g. "a,(b,c)" will
   * result in a list containing "a" and "(b,c)" when splitting on commas. Empty parts are ignored,
   * so when splitting "a,,,,(b,c)", the same result is returned as in the previous example.
   *
   * @param toSplit The input String
   * @param delimiters A String containing all characters that should be treated as delimiters
   * @return A list of all parts of the input, as divided by any delimiter
   */
  public static List<String> splitLeavingParenthesisContents(String toSplit, String delimiters) {
    List<String> result = new ArrayList<>();
    int openParentheses = 0;
    StringBuilder currPart = new StringBuilder();
    for (char c : toSplit.toCharArray()) {
      if (c == '(') {
        openParentheses++;
        currPart.append(c);
      } else if (c == ')') {
        if (openParentheses > 0) {
          openParentheses--;
        }
        currPart.append(c);
      } else if (delimiters.contains("" + c)) {
        if (openParentheses == 0) {
          String toAdd = currPart.toString().strip();
          if (!toAdd.isEmpty()) {
            result.add(currPart.toString().strip());
          }
          currPart = new StringBuilder();
        } else {
          currPart.append(c);
        }
      } else {
        currPart.append(c);
      }
    }

    if (currPart.length() > 0) {
      result.add(currPart.toString().strip());
    }
    return result;
  }

  /**
   * Removes pairs of parentheses that do not provide any further separation. E.g. "(foo)" results
   * in "foo" and "(((foo))((bar)))" in "(foo)(bar)", whereas "(foo)(bar)" stays the same.
   *
   * @param original The String to clean
   * @return The modified version without excess parentheses
   */
  public static String removeRedundantParentheses(String original) {
    char[] result = original.toCharArray();
    char marker = '\uffff';
    Deque<Integer> openingParentheses = new ArrayDeque<>();
    for (int i = 0; i < original.length(); i++) {
      char c = original.charAt(i);
      switch (c) {
        case '(':
          openingParentheses.push(i);
          break;
        case ')':
          int matching = openingParentheses.pop();
          if (matching == 0 && i == original.length() - 1) {
            result[matching] = result[i] = marker;
          } else if (matching > 0 && result[matching - 1] == '(' && result[i + 1] == ')') {
            result[matching] = result[i] = marker;
          }
      }
    }
    return new String(result).replace("" + marker, "");
  }

  public static boolean containsOnOuterLevel(String input, char marker) {
    int openParentheses = 0;
    for (char c : input.toCharArray()) {
      if (c == '(') {
        openParentheses++;
      } else if (c == ')') {
        openParentheses--;
      } else if (c == marker && openParentheses == 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Establish dataflow from call arguments to the target {@link FunctionDeclaration} parameters
   *
   * @param target The call's target {@link FunctionDeclaration}
   * @param arguments The call's arguments to be connected to the target's parameters
   */
  public static void attachCallParameters(FunctionDeclaration target, List<Expression> arguments) {
    target
        .getParametersPropertyEdge()
        .sort(Comparator.comparing(pe -> pe.getEnd().getArgumentIndex()));

    for (int j = 0; j < arguments.size(); j++) {
      var parameters = target.getParameters();

      if (j < parameters.size()) {
        var param = parameters.get(j);
        if (param.isVariadic()) {
          for (; j < arguments.size(); j++) {
            // map all the following arguments to this variadic param
            param.addPrevDFG(arguments.get(j));
          }
          break;
        } else {
          param.addPrevDFG(arguments.get(j));
        }
      }
    }
  }

  public static String getSimpleName(String delimiter, String name) {
    if (name.contains(delimiter)) {
      name = name.substring(name.lastIndexOf(delimiter) + delimiter.length());
    }
    return name;
  }

  /**
   * Inverse operation of {@link #attachCallParameters}
   *
   * @param target
   * @param arguments
   */
  public static void detachCallParameters(FunctionDeclaration target, List<Expression> arguments) {
    for (ParamVariableDeclaration param : target.getParameters()) {
      // A param could be variadic, so multiple arguments could be set as incoming DFG
      param.getPrevDFG().stream().filter(arguments::contains).forEach(param::removeNextDFG);
    }
  }

  public static List<ParamVariableDeclaration> createInferredParameters(List<Type> signature) {
    List<ParamVariableDeclaration> params = new ArrayList<>();
    for (int i = 0; i < signature.size(); i++) {
      Type targetType = signature.get(i);
      String paramName = generateParamName(i, targetType);
      ParamVariableDeclaration param = newMethodParameterIn(paramName, targetType, false, "");
      param.setInferred(true);
      param.setArgumentIndex(i);
      params.add(param);
    }
    return params;
  }

  private static String generateParamName(int i, @NonNull Type targetType) {
    Deque<String> hierarchy = new ArrayDeque<>();
    Type currLevel = targetType;
    while (currLevel != null) {
      if (currLevel instanceof FunctionPointerType) {
        hierarchy.push("Fptr");
        currLevel = null;
      } else if (currLevel instanceof PointerType) {
        hierarchy.push("Ptr");
        currLevel = ((PointerType) currLevel).getElementType();
      } else if (currLevel instanceof ReferenceType) {
        hierarchy.push("Ref");
        currLevel = ((ReferenceType) currLevel).getElementType();
      } else {
        hierarchy.push(currLevel.getTypeName());
        currLevel = null;
      }
    }

    StringBuilder paramName = new StringBuilder();
    while (!hierarchy.isEmpty()) {
      String part = hierarchy.pop();
      if (part.isEmpty()) {
        continue;
      }
      if (paramName.length() > 0) {
        paramName.append(part.substring(0, 1).toUpperCase());
        if (part.length() >= 2) {
          paramName.append(part.substring(1));
        }
      } else {
        paramName.append(part.toLowerCase());
      }
    }

    paramName.append(i);
    return paramName.toString();
  }

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

  static <T> Stream<T> reverse(Stream<T> input) {
    Object[] temp = input.toArray();
    return (Stream<T>) IntStream.range(0, temp.length).mapToObj(i -> temp[temp.length - i - 1]);
  }

  public enum Connect {
    NODE,
    SUBTREE
  }

  public enum Quantifier {
    ANY,
    ALL
  }

  public enum Edge {
    ENTRIES,
    EXITS
  }

  /**
   * This function returns the set of adjacent DFG nodes that is contained in the nodes subgraph.
   *
   * @param n Node of interest
   * @param incoming whether the node connected by an incoming or, if false, outgoing DFG edge
   * @return
   */
  public static List<Node> getAdjacentDFGNodes(final Node n, boolean incoming) {

    var subnodes = SubgraphWalker.getAstChildren(n);
    List<Node> adjacentNodes;
    if (incoming) {
      adjacentNodes =
          subnodes.stream()
              .filter(prevCandidate -> prevCandidate.getNextDFG().contains(n))
              .collect(Collectors.toList());
    } else {
      adjacentNodes =
          subnodes.stream()
              .filter(nextCandidate -> nextCandidate.getPrevDFG().contains(n))
              .collect(Collectors.toList());
    }
    return adjacentNodes;
  }

  /**
   * Connects the node <code>n</code> with the node <code>branchingExp</code> if present or with the
   * node <code>branchingDecl</code>. The assumption is that only <code>branchingExp</code> or
   * <code>branchingDecl</code> are present, e.g. C++.
   *
   * @param n
   * @param branchingExp
   * @param branchingDecl
   */
  public static void addDFGEdgesForMutuallyExclusiveBranchingExpression(
      Node n, Node branchingExp, Node branchingDecl) {
    List<Node> conditionNodes = new ArrayList<>();

    if (branchingExp != null) {
      conditionNodes = new ArrayList<>();
      conditionNodes.add(branchingExp);
    } else if (branchingDecl != null) {
      conditionNodes = getAdjacentDFGNodes(branchingDecl, true);
    }
    conditionNodes.forEach(c -> n.addPrevDFG(c));
  }
}
