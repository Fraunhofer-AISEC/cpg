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

import java.util.*
import java.util.function.Function
import org.neo4j.ogm.typeconversion.CompositeAttributeConverter

class PropertyEdgeConverter : CompositeAttributeConverter<Map<Properties, Any?>> {
    /**
     * For every PropertyValue that is not a supported type, a serializer and a deserializer must be
     * provided Supported Types:
     *
     * PRIMITIVES =
     * char,byte,short,int,long,float,double,boolean,char[],byte[],short[],int[],long[],float[],double[],boolean[]
     * AUTOBOXERS = java.lang.Object, java.lang.Character, java.lang.Byte, java.lang.Short,
     * java.lang.Integer, java.lang.Long, java.lang.Float, java.lang.Double, java.lang.Boolean,
     * java.lang.String, java.lang.Object[], java.lang.Character[], java.lang.Byte[],
     * java.lang.Short[], java.lang.Integer[], java.lang.Long[], java.lang.Float[],
     * java.lang.Double[], java.lang.Boolean[], java.lang.String[]
     */
    // Maps a class to a function that serialized the object from the given class
    private val serializer: Map<String, Function<Any, String>>

    // Maps a string (key of the property) to a function that deserializes the property
    private val deserializer: Map<String, Function<Any, Any?>>

    init {
        serializer = PropertyEdgeConverterManager.instance.serializer
        deserializer = PropertyEdgeConverterManager.instance.deserializer
    }

    override fun toGraphProperties(value: Map<Properties, Any?>): Map<String, Any?> {
        val result: MutableMap<String, Any?> = HashMap()
        for ((key, propertyValue) in value) {
            if (serializer.containsKey(propertyValue!!.javaClass.name)) {
                val serializedProperty: Any =
                    serializer[propertyValue.javaClass.name]!!.apply(propertyValue)
                result[key.name] = serializedProperty
            } else {
                result[key.name] = propertyValue
            }
        }
        return result
    }

    override fun toEntityAttribute(value: Map<String?, *>): Map<Properties, Any?> {
        val result: MutableMap<Properties, Any?> = EnumMap(Properties::class.java)
        for (prop in Properties.values()) {
            if (deserializer.containsKey(prop.name)) {
                val deserializedProperty = deserializer[prop.name]!!.apply(value[prop.name]!!)
                result[prop] = deserializedProperty
            } else {
                result[prop] = value[prop.name]
            }
        }
        return result
    }
}
