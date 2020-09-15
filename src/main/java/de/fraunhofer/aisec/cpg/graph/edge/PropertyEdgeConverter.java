package de.fraunhofer.aisec.cpg.graph.edge;

import org.neo4j.ogm.typeconversion.CompositeAttributeConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PropertyEdgeConverter implements CompositeAttributeConverter<Map<String, Object>> {

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
  public Map<String, Object> toGraphProperties(Map<String, Object> value) {
    Map<String, Object> result = new HashMap<>();
    for (String key : value.keySet()) {
      Object propertyValue = value.get(key);
      if (serializer.containsKey(propertyValue.getClass())) {
        Object serializedProperty = serializer.get(propertyValue.getClass()).apply(propertyValue);
        result.put(key, serializedProperty);
      } else {
        result.put(key, propertyValue);
      }
    }

    return result;
  }

  @Override
  public Map<String, Object> toEntityAttribute(Map<String, ?> value) {
    Map<String, Object> result = new HashMap<>();
    for (String key : value.keySet()) {
      Object propertyValue = value.get(key);
      if (deserializer.containsKey(key)) {
        Object deserializedProperty = deserializer.get(key).apply(propertyValue);
        result.put(key, deserializedProperty);
      } else {
        result.put(key, propertyValue);
      }
    }

    return result;
  }
}
