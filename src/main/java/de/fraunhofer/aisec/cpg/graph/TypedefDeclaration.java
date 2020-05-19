package de.fraunhofer.aisec.cpg.graph;

import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.graph.type.UnknownType;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents a type alias definition as found in C/C++: <code>typedef unsigned long ulong;</code>
 */
public class TypedefDeclaration extends Declaration {

  /** The already existing type that is to be aliased */
  private Type type = UnknownType.getUnknownType();

  /** The newly created alias to be defined */
  private Type alias = UnknownType.getUnknownType();

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public Type getAlias() {
    return alias;
  }

  public void setAlias(Type alias) {
    this.alias = alias;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TypedefDeclaration)) {
      return false;
    }
    TypedefDeclaration that = (TypedefDeclaration) o;
    return Objects.equals(type, that.type) && Objects.equals(alias, that.alias);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, alias);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("type", type).append("alias", alias).toString();
  }
}
