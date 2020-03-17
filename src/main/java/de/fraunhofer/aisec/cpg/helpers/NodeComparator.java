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

import de.fraunhofer.aisec.cpg.graph.Node;
import java.util.Comparator;

public class NodeComparator implements Comparator<Node> {

  @Override
  public int compare(Node n1, Node n2) {
    if (n1 == null && n2 == null) return 0;
    if (n2 == null) return -1;
    if (n1 == null) return 1;

    if (n1.getLocation() == null && n2.getLocation() == null) return 0;
    if (n2.getLocation() == null) return -1;
    if (n1.getLocation() == null) return 1;

    int comparisonValue;
    if ((comparisonValue =
            Integer.compare(
                n1.getLocation().getRegion().getStartLine(),
                n2.getLocation().getRegion().getStartLine()))
        != 0) return comparisonValue;
    if ((comparisonValue =
            Integer.compare(
                n1.getLocation().getRegion().getStartColumn(),
                n2.getLocation().getRegion().getStartColumn()))
        != 0) return comparisonValue;

    if ((comparisonValue =
            Integer.compare(
                n1.getLocation().getRegion().getEndLine(),
                n2.getLocation().getRegion().getEndLine()))
        != 0) return -comparisonValue;
    if ((comparisonValue =
            Integer.compare(
                n1.getLocation().getRegion().getEndColumn(),
                n2.getLocation().getRegion().getEndColumn()))
        != 0) return -comparisonValue;

    if (n1.getCode() == null && n2.getCode() == null) return 0;
    if (n2.getCode() == null) return -1;
    if (n1.getCode() == null) return 1;

    return Integer.compare(n2.getCode().length(), n1.getCode().length());
  }
}
