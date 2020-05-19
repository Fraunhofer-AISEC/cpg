package de.fraunhofer.aisec.cpg.graph.type;

import java.util.Objects;

public class IncompleteType extends Type {

  public IncompleteType() {
    super("void", Storage.AUTO, new Qualifier(false, false, false, false));
  }

  public IncompleteType(Type type) {
    super(type);
  }

  /** @return PointerType to a IncompleteType, e.g. void* */
  @Override
  public Type reference(PointerType.PointerOrigin pointerOrigin) {
    return new PointerType(this, pointerOrigin);
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

  @Override
  public boolean equals(Object o) {
    return o instanceof IncompleteType;
  }

  @Override
  public int hashCode() {

    return Objects.hash(super.hashCode());
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
