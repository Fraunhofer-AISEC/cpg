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
package de.fraunhofer.aisec.cpg.meta;

import de.fraunhofer.aisec.cpg.graph.Node;
import java.util.Map;
import java.util.Objects;

public class Edge {

  private final String label;
  private final Map<String, Object> properties;
  private final Node from, to;

  public Edge(Node from, String label, Map<String, Object> properties, Node to) {
    this.label = label;
    this.properties = properties;
    this.from = from;
    this.to = to;
  }

  public String getLabel() {
    return label;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public Node getFrom() {
    return from;
  }

  public Node getTo() {
    return to;
  }

  @Override
  public String toString() {
    return "{" + from + "} --" + label + "-> {" + to + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Edge edge = (Edge) o;
    return Objects.equals(label, edge.label)
        && Objects.equals(properties, edge.properties)
        && from == edge.from
        && to == edge.to;
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, properties, from, to);
  }
}
