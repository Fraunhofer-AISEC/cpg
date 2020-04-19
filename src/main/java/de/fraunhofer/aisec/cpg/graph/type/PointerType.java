package de.fraunhofer.aisec.cpg.graph.type;

import java.util.Objects;

public class PointerType extends Type {
  private Type elementType;

  public PointerType(Type elementType) {
    super();
    this.name = elementType.getName() + "*";
    this.elementType = elementType;
  }

  public PointerType(Type type, Type elementType) {
    super(type);
    this.name = elementType.getName() + "*";
    this.elementType = elementType;
  }

  @Override
  public PointerType reference() {
    return new PointerType(this);
  }

  @Override
  public Type dereference() {
    return elementType;
  }

  @Override
  public Type duplicate() {
    return new PointerType(this, this.elementType);
  }

  @Override
  public boolean isSimilar(Type t) {
    if (!(t instanceof PointerType)) {
      return false;
    }

    PointerType pointerType = (PointerType) t;

    return this.getReferenceDepth() == pointerType.getReferenceDepth()
        && this.getElementType().isSimilar(pointerType.getRoot())
        && super.isSimilar(t);
  }

  @Override
  public Type getRoot() {
    return this.elementType.getRoot();
  }

  @Override
  public Type getFollowingLevel() {
    return elementType;
  }

  public Type getElementType() {
    return elementType;
  }

  public void setElementType(Type elementType) {
    this.elementType = elementType;
  }

  @Override
  public int getReferenceDepth() {
    int depth = 0;
    Type containedType = this.elementType;
    while (containedType instanceof PointerType) {
      depth++;
      containedType = ((PointerType) containedType).getElementType();
    }
    return depth;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PointerType)) return false;
    if (!super.equals(o)) return false;
    PointerType that = (PointerType) o;
    return Objects.equals(elementType, that.elementType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), elementType);
  }

  @Override
  public String toString() {
    return "ReferenceType{"
        + "elementType="
        + elementType
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
