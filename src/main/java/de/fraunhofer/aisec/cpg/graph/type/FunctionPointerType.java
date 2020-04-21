package de.fraunhofer.aisec.cpg.graph.type;

import java.util.List;
import java.util.Objects;

public class FunctionPointerType extends Type {
  private List<Type> parameters;
  private Type returnType;

  public void setParameters(List<Type> parameters) {
    this.parameters = parameters;
  }

  public void setReturnType(Type returnType) {
    this.returnType = returnType;
  }

  public FunctionPointerType(
      Type.Qualifier qualifier, Type.Storage storage, List<Type> parameters, Type returnType) {
    super("", storage, qualifier);
    this.parameters = parameters;
    this.returnType = returnType;
  }

  public FunctionPointerType(Type type, List<Type> parameters, Type returnType) {
    super(type);
    this.parameters = parameters;
    this.returnType = returnType;
  }

  public List<Type> getParameters() {
    return parameters;
  }

  public Type getReturnType() {
    return returnType;
  }

  @Override
  public PointerType reference() {
    return new PointerType(this);
  }

  @Override
  public Type dereference() {
    return this;
  }

  @Override
  public Type duplicate() {
    return new FunctionPointerType(this, this.parameters, this.returnType);
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
    if (t instanceof FunctionPointerType) {
      if (returnType == null || ((FunctionPointerType) t).returnType == null) {
        return this.parameters.equals(((FunctionPointerType) t).parameters)
            && (this.returnType == ((FunctionPointerType) t).returnType
                || returnType == ((FunctionPointerType) t).getReturnType());
      }
      return this.parameters.equals(((FunctionPointerType) t).parameters)
          && (this.returnType == ((FunctionPointerType) t).returnType
              || this.returnType.equals(((FunctionPointerType) t).returnType));
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FunctionPointerType)) return false;
    if (!super.equals(o)) return false;
    FunctionPointerType that = (FunctionPointerType) o;
    return Objects.equals(parameters, that.parameters)
        && Objects.equals(returnType, that.returnType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), parameters, returnType);
  }

  @Override
  public String toString() {
    return "FunctionPointerType{"
        + "parameters="
        + parameters
        + ", returnType="
        + returnType
        + ", typeName='"
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
