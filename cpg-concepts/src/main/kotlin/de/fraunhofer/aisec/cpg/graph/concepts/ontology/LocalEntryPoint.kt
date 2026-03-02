package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents a local entry point into the execution of the program, such as a main function.
 */
public open class LocalEntryPoint(
  public val usedBy: OperatingSystemArchitecture?,
  underlyingNode: Node? = null,
) : EntryPoint(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is LocalEntryPoint &&
              super.equals(other) &&
              other.usedBy == this.usedBy

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              usedBy,
          )
}
