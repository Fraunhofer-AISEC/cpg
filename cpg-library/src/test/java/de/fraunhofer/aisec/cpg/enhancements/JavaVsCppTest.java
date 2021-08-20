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
package de.fraunhofer.aisec.cpg.enhancements;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javaparser.utils.Pair;
import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class JavaVsCppTest extends BaseTest {

  @Test
  void cpp() throws Exception {
    analyzeAndSave("src/test/resources/javaVsCpp/simple.cpp");
  }

  @Test
  void java() throws Exception {
    analyzeAndSave("src/test/resources/javaVsCpp/simple.java");
  }

  private void analyzeAndSave(String pathname) throws Exception {
    File toTranslate = new File(pathname);
    Path topLevel = toTranslate.getParentFile().toPath();
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(toTranslate), topLevel, true);
    assertEquals(1, tu.getDeclarations().size());
    Declaration decl = tu.getDeclarations().get(0);
    assertTrue(decl instanceof RecordDeclaration);
    RecordDeclaration rec = (RecordDeclaration) decl;
    assertEquals("Simple", rec.getName());
    assertEquals(2, rec.getMethods().size());
    assertEquals("class", rec.getKind());

    MethodDeclaration methodDeclaration = rec.getMethods().get(0);

    List<Node> worklist = methodDeclaration.getNextEOG();
    HashMap<Node, Integer> nodes = new HashMap<>();
    HashSet<Pair<Integer, Integer>> edges = new HashSet<>();
    int currentId = 0;
    while (worklist.size() > 0) {
      List<Node> next = new ArrayList<>();
      for (Node n : worklist) {
        int nodeID = currentId;
        if (!nodes.containsKey(n)) {
          nodes.put(n, nodeID);
          currentId++;
        }
        assertNotNull(n.getCode());
        for (Node succ : n.getNextEOG()) {
          Integer succID = nodes.get(succ);
          if (succID == null) {
            succID = currentId;
            nodes.put(succ, currentId);
            currentId++;
          }
          edges.add(new Pair<>(nodeID, succID));
          next.add(succ);
        }
      }
      worklist = next;
    }

    TreeMap<Integer, Node> sorted = new TreeMap<>();
    nodes.forEach((n, i) -> sorted.put(i, n));

    final StringBuilder sbNodes = new StringBuilder();
    sorted.forEach(
        (i, n) -> {
          if (n.getClass().getSimpleName().equals("DeclarationStatement")) {
            DeclarationStatement d = (DeclarationStatement) n;
            System.err.println(
                n.getCode()
                    + " type:"
                    + ((VariableDeclaration) d.getSingleDeclaration()).getType().toString());
          }
          sbNodes.append(i).append(" ").append(n.getClass().getSimpleName()).append("\n");
        });
    assertEquals(
        "0 Literal\n"
            + "1 VariableDeclaration\n"
            + "2 DeclarationStatement\n"
            + "3 DeclaredReferenceExpression\n"
            + "4 Literal\n"
            + "5 BinaryOperator\n"
            + "6 IfStatement\n"
            + "7 DeclaredReferenceExpression\n"
            + "8 DeclaredReferenceExpression\n"
            + "9 DeclaredReferenceExpression\n"
            + "10 CallExpression\n"
            + "11 BinaryOperator\n"
            + "12 ReturnStatement\n"
            + "13 CompoundStatement\n",
        sbNodes.toString());

    List<Pair<Integer, Integer>> collect =
        edges.stream()
            .sorted(Comparator.comparing(x -> x.a)) // comparator - how you want to sort it
            .collect(Collectors.toList()); // collector - what you want to collect it to

    final StringBuilder sbEdges = new StringBuilder();
    collect.forEach(
        x -> {
          sbEdges.append(x.a).append(" -> ").append(x.b).append("\n");
        });

    assertEquals(
        "0 -> 1\n"
            + "2 -> 2\n"
            + "3 -> 3\n"
            + "4 -> 4\n"
            + "5 -> 5\n"
            + "6 -> 6\n"
            + "7 -> 7\n"
            + "7 -> 8\n"
            + "9 -> 9\n"
            + "10 -> 10\n"
            + "11 -> 11\n"
            + "12 -> 12\n"
            + "13 -> 13\n"
            + "14 -> 8\n"
            + "14 -> 10\n"
            + "14 -> 12\n",
        sbEdges.toString());
  }
}
