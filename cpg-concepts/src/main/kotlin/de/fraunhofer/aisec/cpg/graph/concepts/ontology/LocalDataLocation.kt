package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class LocalDataLocation(
  public val atRestEncryption: AtRestEncryption?,
  path: String?,
  storage: Storage?,
  underlyingNode: Node? = null,
) : DataLocation(path, storage, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is LocalDataLocation &&
              super.equals(other) &&
              other.atRestEncryption == this.atRestEncryption

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              atRestEncryption,
          )
}
