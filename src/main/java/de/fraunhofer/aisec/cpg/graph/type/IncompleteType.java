package de.fraunhofer.aisec.cpg.graph.type;

public class IncompleteType extends Type {

  public IncompleteType() {
    super("void", Storage.AUTO, new Qualifier(false, false, false, false));
  }

  public IncompleteType(Type type) {
    super(type);
  }

  /** @return PointerType to a IncompleteType, e.g. void* */
  @Override
  public Type reference() {
    return new PointerType(this);
  }

  /** @return dereferencing void results in void therefore the same type is returned */
  @Override
  public Type dereference() {
    return this;
  }

  @Override
  public Type duplicate() {
    return new IncompleteType(this);
  }

  /** @return this, as IncompleteType is the root of the Type chain */
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
        + this.getStorage()
        + ", qualifier="
        + this.getQualifier()
        + ", origin="
        + this.getTypeOrigin()
        + '}';
  }
}
