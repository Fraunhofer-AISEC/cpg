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

import de.fraunhofer.aisec.cpg.graph.Type;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.ogm.typeconversion.CompositeAttributeConverter;

/**
 * The {@link TypeConverter} is a helper converter that takes the Object-based {@link Type} and
 * serializes into a flatten structured in the database graph. It stores the type as a string
 * representation, using {@link Type#toString} as well as additional information such as modifiers.
 */
public class TypeConverter implements CompositeAttributeConverter<Type> {

  @Override
  public Map<String, ?> toGraphProperties(Type value) {
    Map<String, String> properties = new HashMap<>();
    if (value != null) {
      // the type as string representation
      properties.put("type", value.toString());
      properties.put("typeName", value.getTypeName());
      properties.put("typeModifier", value.getTypeModifier());
      properties.put("typeAdjustment", value.getTypeAdjustment());
    }

    return properties;
  }

  @Override
  public Type toEntityAttribute(Map<String, ?> value) {
    try {
      return Type.createFrom((String) value.get("type"));
    } catch (NullPointerException e) {
      return Type.UNKNOWN;
    }
  }
}
