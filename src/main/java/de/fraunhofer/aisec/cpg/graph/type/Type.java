package de.fraunhofer.aisec.cpg.graph.type;

import de.fraunhofer.aisec.cpg.graph.Node;
import java.util.Objects;
import org.neo4j.ogm.annotation.typeconversion.Convert;

public abstract class Type extends Node {
  public static final String UNKNOWN_TYPE_STRING = "UNKNOWN";

  public enum Storage {
    AUTO,
    EXTERN,
    STATIC,
    REGISTER
  }

  public Storage getStorage() {
    return storage;
  }

  public void setStorage(Storage storage) {
    this.storage = storage;
  }

  public Qualifier getQualifier() {
    return qualifier;
  }

  public void setQualifier(Qualifier qualifier) {
    this.qualifier = qualifier;
  }

  public Origin getTypeOrigin() {
    return origin;
  }

  public void setTypeOrigin(Origin origin) {
    this.origin = origin;
  }

  public enum Origin {
    RESOLVED,
    DATAFLOW,
    GUESSED,
    UNRESOLVED
  }

  public static class Qualifier {
    private boolean isConst; // C, C++, Java
    private boolean isVolatile; // C, C++, Java
    private boolean isRestrict; // C
    private boolean isAtomic; // C

    public Qualifier(boolean isConst, boolean isVolatile, boolean isRestrict, boolean isAtomic) {
      this.isConst = isConst;
      this.isVolatile = isVolatile;
      this.isRestrict = isRestrict;
      this.isAtomic = isAtomic;
    }

    public Qualifier() {
      this.isConst = false;
      this.isVolatile = false;
      this.isRestrict = false;
      this.isAtomic = false;
    }

    public boolean isConst() {
      return isConst;
    }

    public void setConst(boolean aConst) {
      isConst = aConst;
    }

    public boolean isVolatile() {
      return isVolatile;
    }

    public void setVolatile(boolean aVolatile) {
      isVolatile = aVolatile;
    }

    public boolean isRestrict() {
      return isRestrict;
    }

    public void setRestrict(boolean restrict) {
      isRestrict = restrict;
    }

    public boolean isAtomic() {
      return isAtomic;
    }

    public void setAtomic(boolean atomic) {
      isAtomic = atomic;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Qualifier)) return false;
      Qualifier qualifier = (Qualifier) o;
      return isConst == qualifier.isConst
          && isVolatile == qualifier.isVolatile
          && isRestrict == qualifier.isRestrict
          && isAtomic == qualifier.isAtomic;
    }

    @Override
    public int hashCode() {
      return Objects.hash(isConst, isVolatile, isRestrict, isAtomic);
    }

    @Override
    public String toString() {
      return "Qualifier{"
          + "isConst="
          + isConst
          + ", isVolatile="
          + isVolatile
          + ", isRestrict="
          + isRestrict
          + ", isAtomic="
          + isAtomic
          + '}';
    }
  }

  protected Storage
      storage; // auto, extern, static, register --> consider auto as modifier or auto to
  // automatically infer the value

  @Convert(QualifierConverter.class)
  protected Qualifier qualifier;

  protected Origin origin;

  /** */
  public Type() {
    this.name = "";
    this.storage = Storage.AUTO;
    this.qualifier = new Qualifier(false, false, false, false);
  }

  public Type(String typeName) {
    this.name = typeName;
  }

  /** @param type */
  public Type(Type type) {
    this.storage = type.storage;
    this.name = type.name;
    this.qualifier =
        new Qualifier(
            type.qualifier.isConst,
            type.qualifier.isVolatile,
            type.qualifier.isRestrict,
            type.qualifier.isAtomic);
    this.origin = type.origin;
  }

  public Type(String typeName, Storage storage, Qualifier qualifier) {
    this.name = typeName;
    this.storage = storage;
    this.qualifier = qualifier;
    this.origin = Origin.UNRESOLVED;
  }

  public abstract Type reference();

  public abstract Type dereference();

  public abstract Type getFollowingLevel();

  public abstract Type getRoot();

  public abstract Type duplicate();

  public String getTypeName() {
    return name;
  }

  public int getReferenceDepth() {
    return 0;
  }

  public void setTypeModifier(String s) {
    System.out.println(s);
    return;
  }

  public boolean isFirstOrderType(Type t) {
    return t instanceof ObjectType
        || t instanceof UnknownType
        || t instanceof FunctionPointerType
        || t instanceof IncompleteType;
  }

  public boolean isSimilar(Type t) {
    if (this.equals(t)) {
      return true;
    }

    return this.getTypeName().equals(t.getTypeName());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Type)) return false;
    Type type = (Type) o;
    return Objects.equals(name, type.name)
        && storage == type.storage
        && Objects.equals(qualifier, type.qualifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, storage, qualifier);
  }

  @Override
  public String toString() {
    return "Type{"
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
