package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an operation that loads a symbol during runtime. A common example would be a call to`dlsym` in C/C++.
 */
public open class LoadSymbol(
  operatingSystemArchitecture: OperatingSystemArchitecture?,
  operatesOn: Memory,
  underlyingNode: Node? = null,
) : DynamicLoadingOperation(operatingSystemArchitecture, operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is LoadSymbol &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
