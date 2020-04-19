package de.fraunhofer.aisec.cpg.graph.type;

import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ObjectType extends Type {

  public enum Modifier {
    SIGNED,
    UNSIGNED,
    NOT_APPLICABLE,
  }

  private final Modifier modifier;
  private final boolean primitive;
  private RecordDeclaration recordDeclaration = null;
  private List<Type> generics;

  public ObjectType(
      String typeName,
      Storage storage,
      Qualifier qualifier,
      List<Type> generics,
      Modifier modifier,
      boolean primitive) {
    super(typeName, storage, qualifier);
    this.generics = generics;
    this.modifier = modifier;
    this.primitive = primitive;
  }

  public ObjectType(Type type, List<Type> generics, Modifier modifier, boolean primitive) {
    super(type);
    this.generics = generics;
    this.modifier = modifier;
    this.primitive = primitive;
  }

  public ObjectType() {
    super();
    this.generics = new ArrayList<>();
    this.modifier = Modifier.NOT_APPLICABLE;
    this.primitive = false;
  }

  public List<Type> getGenerics() {
    return generics;
  }

  public void setRecordDeclaration(RecordDeclaration recordDeclaration) {
    this.recordDeclaration = recordDeclaration;
  }

  @Override
  public PointerType reference() {
    return new PointerType(this);
  }

  @Override
  public Type dereference() {
    return UnknownType.getUnknownType();
  }

  @Override
  public Type duplicate() {
    return new ObjectType(this, this.generics, this.modifier, this.primitive);
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
  public boolean isSimilar(Type t) {
    return t instanceof ObjectType
        && this.getGenerics().equals(((ObjectType) t).getGenerics())
        && super.isSimilar(t);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ObjectType)) return false;
    if (!super.equals(o)) return false;
    ObjectType that = (ObjectType) o;
    return Objects.equals(generics, that.generics)
        && this.primitive == that.primitive
        && this.modifier.equals(that.modifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), generics, modifier, primitive);
  }

  @Override
  public String toString() {
    return "ObjectType{"
        + "generics="
        + generics
        + ", typeName='"
        + name
        + '\''
        + ", storage="
        + storage
        + ", qualifier="
        + qualifier
        + ", modifier="
        + modifier
        + ", primitive="
        + primitive
        + ", origin="
        + origin
        + '}';
  }
}
