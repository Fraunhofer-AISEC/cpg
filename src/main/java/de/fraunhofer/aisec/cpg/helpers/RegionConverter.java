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

import static java.lang.Math.toIntExact;

import de.fraunhofer.aisec.cpg.graph.Region;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.ogm.typeconversion.CompositeAttributeConverter;

public class RegionConverter implements CompositeAttributeConverter<Region> {

  @Override
  public Map<String, ?> toGraphProperties(Region value) {
    Map<String, Integer> properties = new HashMap<>();
    if (value != null) {
      properties.put("startLine", value.getStartLine());
      properties.put("endLine", value.getEndLine());
      properties.put("startColumn", value.getStartColumn());
      properties.put("endColumn", value.getEndColumn());
    }
    return properties;
  }

  @Override
  public Region toEntityAttribute(Map<String, ?> value) {
    try {
      int startLine = toIntExact((Integer) value.get("startLine"));
      int endLine = toIntExact((Integer) value.get("endLine"));
      int startColumn = toIntExact((Integer) value.get("startColumn"));
      int endColumn = toIntExact((Integer) value.get("endColumn"));
      return new Region(startLine, startColumn, endLine, endColumn);
    } catch (NullPointerException e) {
      return null;
    }
  }
}
