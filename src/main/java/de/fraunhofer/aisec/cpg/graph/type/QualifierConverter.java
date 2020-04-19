package de.fraunhofer.aisec.cpg.graph.type;

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
