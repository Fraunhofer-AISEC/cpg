package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.collections.MutableList

/**
 * Represents an operation that loads a shared library during runtime. A common example would be a call to `dlopen` in C/C++.
 */
public open class LoadLibrary(
  public val entryPoints: MutableList<EntryPoint?>,
  operatingSystemArchitecture: OperatingSystemArchitecture?,
  operatesOn: Memory,
  underlyingNode: Node? = null,
) : DynamicLoadingOperation(operatingSystemArchitecture, operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is LoadLibrary &&
              super.equals(other) &&
              other.entryPoints == this.entryPoints

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              entryPoints,
          )
}
