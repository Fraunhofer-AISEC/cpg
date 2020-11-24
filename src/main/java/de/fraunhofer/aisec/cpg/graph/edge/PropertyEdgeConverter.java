package de.fraunhofer.aisec.cpg.graph.edge;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.neo4j.ogm.typeconversion.CompositeAttributeConverter;

public class PropertyEdgeConverter implements CompositeAttributeConverter<Map<Properties, Object>> {

  /**
   * For every PropertyValue that is not a supported type, a serializer and a deserializer must be
   * provided Supported Types:
   *
   * <p>PRIMITIVES =
   * char,byte,short,int,long,float,double,boolean,char[],byte[],short[],int[],long[],float[],double[],boolean[]
   * AUTOBOXERS = java.lang.Object, java.lang.Character, java.lang.Byte, java.lang.Short,
   * java.lang.Integer, java.lang.Long, java.lang.Float, java.lang.Double, java.lang.Boolean,
   * java.lang.String, java.lang.Object[], java.lang.Character[], java.lang.Byte[],
   * java.lang.Short[], java.lang.Integer[], java.lang.Long[], java.lang.Float[],
   * java.lang.Double[], java.lang.Boolean[], java.lang.String[]
   */

  // Maps a class to a function that serialized the object from the given class
  private Map<Class<?>, Function<Object, String>> serializer = new HashMap<>();

  // Maps a string (key of the property) to a function that deserializes the property
  private Map<String, Function<Object, String>> deserializer = new HashMap<>();

  @Override
  public Map<String, Object> toGraphProperties(Map<Properties, Object> value) {
    Map<String, Object> result = new HashMap<>();
    for (Map.Entry<Properties, Object> entry : value.entrySet()) {
      Object propertyValue = entry.getValue();
      if (serializer.containsKey(propertyValue.getClass())) {
        Object serializedProperty = serializer.get(propertyValue.getClass()).apply(propertyValue);
        result.put(entry.getKey().name(), serializedProperty);
      } else {
        result.put(entry.getKey().name(), propertyValue);
      }
    }

    return result;
  }

  @Override
  public Map<Properties, Object> toEntityAttribute(Map<String, ?> value) {
    Map<Properties, Object> result = new EnumMap<>(Properties.class);
    for (Map.Entry<String, ?> entry : value.entrySet()) {
      Object propertyValue = entry.getValue();
      if (deserializer.containsKey(entry.getKey())) {
        Object deserializedProperty = deserializer.get(entry.getKey()).apply(propertyValue);
        result.put(Properties.valueOf(entry.getKey()), deserializedProperty);
      } else if (!entry.getKey().equals("sub-graph")) {
        result.put(Properties.valueOf(entry.getKey()), propertyValue);
      }
    }

    return result;
  }
}
