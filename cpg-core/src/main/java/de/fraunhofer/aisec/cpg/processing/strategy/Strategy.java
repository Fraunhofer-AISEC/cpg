/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.processing.strategy;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import java.util.*;
import org.jetbrains.annotations.NotNull;

/** Strategies (iterators) for traversing graphs to be used by visitors. */
public class Strategy {

  private Strategy() {
    // Do not call.
  }

  /**
   * Do not traverse any nodes.
   *
   * @param x
   * @return
   */
  @NotNull
  public static Iterator<Node> NO_STRATEGY(@NotNull Node x) {
    return Collections.emptyIterator();
  }

  /**
   * Traverse Evaluation Order Graph in forward direction.
   *
   * @param x Current node in EOG.
   * @return Iterator over successors.
   */
  @NotNull
  public static Iterator<Node> EOG_FORWARD(@NotNull Node x) {
    return x.getNextEOG().iterator();
  }

  /**
   * Traverse Evaluation Order Graph in backward direction.
   *
   * @param x Current node in EOG.
   * @return Iterator over successors.
   */
  @NotNull
  public static Iterator<Node> EOG_BACKWARD(@NotNull Node x) {
    return x.getPrevEOG().iterator();
  }

  /**
   * Traverse Data Flow Graph in forward direction.
   *
   * @param x Current node in DFG.
   * @return Iterator over successors.
   */
  @NotNull
  public static Iterator<Node> DFG_FORWARD(@NotNull Node x) {
    return x.getNextDFG().iterator();
  }

  /**
   * Traverse Data Flow Graph in backward direction.
   *
   * @param x Current node in DFG.
   * @return Iterator over successors.
   */
  @NotNull
  public static Iterator<Node> DFG_BACKWARD(@NotNull Node x) {
    return x.getPrevDFG().iterator();
  }

  /**
   * Traverse AST in forward direction.
   *
   * @param x
   * @return
   */
  @NotNull
  public static Iterator<Node> AST_FORWARD(@NotNull Node x) {
    return SubgraphWalker.getAstChildren(x).iterator();
  }
}
