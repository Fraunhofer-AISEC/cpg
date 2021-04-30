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

import static java.lang.Math.toIntExact;

import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import de.fraunhofer.aisec.cpg.sarif.Region;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.ogm.typeconversion.CompositeAttributeConverter;

public class LocationConverter implements CompositeAttributeConverter<PhysicalLocation> {

  public static final String START_LINE = "startLine";
  public static final String END_LINE = "endLine";
  public static final String START_COLUMN = "startColumn";
  public static final String END_COLUMN = "endColumn";
  public static final String ARTIFACT = "artifact";

  @Override
  public Map<String, ?> toGraphProperties(PhysicalLocation value) {
    Map<String, Object> properties = new HashMap<>();
    if (value != null) {
      properties.put(ARTIFACT, value.getArtifactLocation().getUri().toString());
      properties.put(START_LINE, value.getRegion().getStartLine());
      properties.put(END_LINE, value.getRegion().getEndLine());
      properties.put(START_COLUMN, value.getRegion().getStartColumn());
      properties.put(END_COLUMN, value.getRegion().getEndColumn());
    }
    return properties;
  }

  @Override
  public PhysicalLocation toEntityAttribute(Map<String, ?> value) {
    try {
      final int startLine = toInt(value.get(START_LINE));
      final int endLine = toInt(value.get(END_LINE));
      final int startColumn = toInt(value.get(START_COLUMN));
      final int endColumn = toInt(value.get(END_COLUMN));
      final URI uri = URI.create((String) value.get(ARTIFACT));

      return new PhysicalLocation(uri, new Region(startLine, startColumn, endLine, endColumn));
    } catch (NullPointerException e) {
      return null;
    }
  }

  private int toInt(final Object objectToMap) {
    final long value = Long.parseLong(objectToMap.toString());
    return toIntExact(value);
  }
}
