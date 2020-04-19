package de.fraunhofer.aisec.cpg.graph.type;

public class IncompleteType extends Type {

  public IncompleteType() {
    super("void", Storage.AUTO, new Qualifier(false, false, false, false));
  }

  public IncompleteType(Type type) {
    super(type);
  }

  @Override
  public Type reference() {
    return new PointerType(this);
  }

  @Override
  public Type dereference() {
    return this;
  }

  @Override
  public Type duplicate() {
    return new IncompleteType(this);
  }

  @Override
  public Type getRoot() {
    return this;
  }

  @Override
  public Type getFollowingLevel() {
    return this;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof IncompleteType;
  }

  @Override
  public String toString() {
    return "IncompleteType{"
        + "typeName='"
        + name
        + '\''
        + ", storage="
        + storage
        + ", qualifier="
        + qualifier
        + ", origin="
        + origin
        + '}';
  }
}
