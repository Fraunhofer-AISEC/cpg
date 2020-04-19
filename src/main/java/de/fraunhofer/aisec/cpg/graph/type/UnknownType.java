package de.fraunhofer.aisec.cpg.graph.type;

public class UnknownType extends Type {

  private static final UnknownType unknownType = new UnknownType();

  private UnknownType() {
    super();
    this.name = "UNKNOWN";
  }

  public static UnknownType getUnknownType() {
    return unknownType;
  }

  public UnknownType(String typeName) {
    super(typeName);
  }

  @Override
  public Type reference() {
    return this;
  }

  @Override
  public Type dereference() {
    return this;
  }

  @Override
  public Type getRoot() {
    return unknownType;
  }

  @Override
  public Type getFollowingLevel() {
    return unknownType;
  }

  @Override
  public Type duplicate() {
    return unknownType;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof UnknownType;
  }

  @Override
  public String toString() {
    return "UNKNOWN";
  }
}
