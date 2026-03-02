package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

public open class Core(
  underlyingNode: Node? = null,
) : Concept(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is Core &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
