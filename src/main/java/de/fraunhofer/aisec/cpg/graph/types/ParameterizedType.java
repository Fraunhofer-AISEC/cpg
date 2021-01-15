package de.fraunhofer.aisec.cpg.graph.types;

public class ParameterizedType extends Type {

  public ParameterizedType(Type type) {
    super(type);
  }

  public ParameterizedType(String typeName) {
    super(typeName);
  }

  @Override
  public Type reference(PointerType.PointerOrigin pointer) {
    return this;
  }

  @Override
  public Type dereference() {
    return this;
  }

  @Override
  public Type duplicate() {
    return new ParameterizedType(this);
  }
}
