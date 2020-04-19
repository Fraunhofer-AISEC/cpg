package de.fraunhofer.aisec.cpg.graph.type;

import java.util.Objects;

public class ReferenceType extends Type {

  private Type reference;

  public ReferenceType(Type reference) {
    super();
    this.name = reference.getName() + "&";
    this.reference = reference;
  }

  public ReferenceType(Type type, Type reference) {
    super(type);
    this.name = reference.getName() + "&";
    this.reference = reference;
  }

  @Override
  public Type reference() {
    return new PointerType(this);
  }

  @Override
  public Type dereference() {
    return reference.dereference();
  }

  @Override
  public Type duplicate() {
    return new ReferenceType(this, this.reference);
  }

  @Override
  public Type getFollowingLevel() {
    return reference;
  }

  @Override
  public Type getRoot() {
    return reference.getRoot();
  }

  public Type getReference() {
    return reference;
  }

  @Override
  public boolean isSimilar(Type t) {
    return t instanceof ReferenceType
        && ((ReferenceType) t).getReference().equals(this)
        && super.isSimilar(t);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ReferenceType)) return false;
    if (!super.equals(o)) return false;
    ReferenceType that = (ReferenceType) o;
    return Objects.equals(reference, that.reference);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), reference);
  }

  @Override
  public String toString() {
    return "ImplicitReferenceType{"
        + "reference="
        + reference
        + ", typeName='"
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
