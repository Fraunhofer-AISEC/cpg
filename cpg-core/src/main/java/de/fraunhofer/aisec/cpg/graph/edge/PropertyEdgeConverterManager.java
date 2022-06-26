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
package de.fraunhofer.aisec.cpg.graph.edge;

import de.fraunhofer.aisec.cpg.graph.declarations.TemplateDeclaration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

/**
 * Since Neo4J uses the PropertyEdgeConverter (as it implements the CompositeAttributeConverter
 * interface), we cannot define it as a singleton, as it requires to have a constructor. We want to
 * be able to dynamically define converters for PropertyEdges that have more complex structures such
 * as enums or custom classes, we need a singleton to be able to add the converters. Refer to the
 * documentation of {@link PropertyEdgeConverter} to see which primitives are supported by default,
 * and which require a custom converter.
 */
public class PropertyEdgeConverterManager {

  @NotNull
  private static final PropertyEdgeConverterManager INSTANCE = new PropertyEdgeConverterManager();

  private PropertyEdgeConverterManager() {
    // Add here converters for PropertyEdges
    this.addSerializer(
        TemplateDeclaration.TemplateInitialization.class.getName(), Object::toString);

    this.addDeserializer(
        "INSTANTIATION",
        (s -> s != null ? TemplateDeclaration.TemplateInitialization.valueOf(s.toString()) : null));
  }

  public static PropertyEdgeConverterManager getInstance() {
    return INSTANCE;
  }

  // Maps a class to a function that serialized the object from the given class
  private final Map<String, Function<Object, String>> serializer = new HashMap<>();

  // Maps a string (key of the property) to a function that deserializes the property
  private final Map<String, Function<Object, Object>> deserializer = new HashMap<>();

  public void addSerializer(String clazz, Function<Object, String> func) {
    serializer.put(clazz, func);
  }

  public void addDeserializer(String name, Function<Object, Object> func) {
    deserializer.put(name, func);
  }

  public Map<String, Function<Object, String>> getSerializer() {
    return serializer;
  }

  public Map<String, Function<Object, Object>> getDeserializer() {
    return deserializer;
  }
}
