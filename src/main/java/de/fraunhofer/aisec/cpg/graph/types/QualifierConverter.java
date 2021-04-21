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
package de.fraunhofer.aisec.cpg.graph.types;

import java.util.HashMap;
import java.util.Map;
import org.neo4j.ogm.typeconversion.CompositeAttributeConverter;

public class QualifierConverter implements CompositeAttributeConverter<Type.Qualifier> {
  @Override
  public Map<String, ?> toGraphProperties(Type.Qualifier value) {
    Map<String, Boolean> properties = new HashMap<>();
    properties.put("isConst", value.isConst());
    properties.put("isVolatile", value.isVolatile());
    properties.put("isRestrict", value.isRestrict());
    properties.put("isAtomic", value.isAtomic());
    return properties;
  }

  @Override
  public Type.Qualifier toEntityAttribute(Map<String, ?> value) {
    Map<String, Boolean> val = (Map<String, Boolean>) value;
    return new Type.Qualifier(
        val.get("isConst"), val.get("isVolatile"), val.get("isRestrict"), val.get("isAtomic"));
  }
}
