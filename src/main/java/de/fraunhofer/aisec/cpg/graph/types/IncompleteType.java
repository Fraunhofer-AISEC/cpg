package de.fraunhofer.aisec.cpg.graph.types;

import java.util.Objects;

/**
 * IncompleteTypes are defined as object with unknown size. For instance: void, arrays of unknown
 * length, forward declarated classes in C++
 *
 * <p>Right now we are only dealing with void for objects with unknown size, therefore the name is
 * fixed to void. However, this can be changed in future, in order to support other objects with
 * unknown size apart from void. Therefore this Type is not called VoidType
 */
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
}
