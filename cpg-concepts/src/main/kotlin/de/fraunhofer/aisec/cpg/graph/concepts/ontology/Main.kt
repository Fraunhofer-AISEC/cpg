package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * The main function of a program.
 */
public open class Main(
  usedBy: OperatingSystemArchitecture?,
  underlyingNode: Node? = null,
) : LocalEntryPoint(usedBy, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is Main &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
