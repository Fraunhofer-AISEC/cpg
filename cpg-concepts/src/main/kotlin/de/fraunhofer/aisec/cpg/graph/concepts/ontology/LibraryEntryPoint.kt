package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an entry point that is triggered if the code is loaded as a (dynamic) library.
 */
public open class LibraryEntryPoint(
  usedBy: OperatingSystemArchitecture?,
  underlyingNode: Node? = null,
) : LocalEntryPoint(usedBy, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is LibraryEntryPoint &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
