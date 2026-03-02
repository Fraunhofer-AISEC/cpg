package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class RemoteDataLocation(
  public val authenticity: Authenticity?,
  public val transportEncryption: TransportEncryption?,
  path: String?,
  storage: Storage?,
  underlyingNode: Node? = null,
) : DataLocation(path, storage, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is RemoteDataLocation &&
              super.equals(other) &&
              other.authenticity == this.authenticity &&
              other.transportEncryption == this.transportEncryption

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              authenticity,
              transportEncryption,
          )
}
