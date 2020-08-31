package de.fraunhofer.aisec.cpg.graph;

import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface HasDefinition<N extends Declaration> extends ReDeclarable<N> {

  /**
   * Return the definition of this declaration. Must be backed by an instance field.
   *
   * @return the definition.
   */
  @Nullable
  N getDefinition();

  /**
   * Sets the definition. Must be backed by an instance field.
   *
   * @param definition the definition
   */
  void setDefinition(HasDefinition<N> definition);

  /**
   * Returns whether this is a definition or not
   *
   * @return true, if this is a definition, false otherwise
   */
  boolean isDefinition();

  /**
   * Rather then dynamically returning the definition, we need it to set to a member variable of the
   * class that implements this interface. This is needed because most OGM mappers only work on
   * fields and not on getter functions.
   */
  @Override
  default void updateFields() {
    HasDefinition<N> definition = null;

    if (isDefinition()) {
      definition = this;
    }

    /*for (var node : this) {
      if (node.isDefinition()) {
        definition = (HasDefinition<N>) node;
      }
    }*/
    for (var it = iterator(); it.hasNext(); ) {
      var node = (HasDefinition<?>) it.next();
      if (node.isDefinition()) {
        definition = (HasDefinition<N>) node;
      }
    }

    // we need to update all nodes in the chain, this is potentially expensive
    // but not sure we can do it any other way
    this.setDefinition(definition);

    for (var it = iterator(); it.hasNext(); ) {
      var node = (HasDefinition<N>) it.next();
      node.setDefinition(definition);
    }
  }
}
