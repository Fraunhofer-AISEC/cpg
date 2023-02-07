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
package de.fraunhofer.aisec.cpg.graph.edge

import de.fraunhofer.aisec.cpg.graph.declarations.TemplateDeclaration.TemplateInitialization
import java.util.function.Function

/**
 * Since Neo4J uses the PropertyEdgeConverter (as it implements the CompositeAttributeConverter
 * interface), we cannot define it as a singleton, as it requires to have a constructor. We want to
 * be able to dynamically define converters for PropertyEdges that have more complex structures such
 * as enums or custom classes, we need a singleton to be able to add the converters. Refer to the
 * documentation of [PropertyEdgeConverter] to see which primitives are supported by default, and
 * which require a custom converter.
 */
class PropertyEdgeConverterManager private constructor() {
    // Maps a class to a function that serialized the object from the given class
    val serializer: MutableMap<String, Function<Any, String>> = HashMap()

    // Maps a string (key of the property) to a function that deserializes the property
    val deserializer: MutableMap<String, Function<Any, Any?>> = HashMap()

    init {
        // Add here converters for PropertyEdges
        addSerializer(TemplateInitialization::class.java.name) { obj: Any -> obj.toString() }
        addDeserializer("INSTANTIATION") { s: Any? ->
            if (s != null) TemplateInitialization.valueOf(s.toString()) else null
        }
    }

    fun addSerializer(clazz: String, func: Function<Any, String>) {
        serializer[clazz] = func
    }

    fun addDeserializer(name: String, func: Function<Any, Any?>) {
        deserializer[name] = func
    }

    companion object {
        val instance = PropertyEdgeConverterManager()
    }
}
