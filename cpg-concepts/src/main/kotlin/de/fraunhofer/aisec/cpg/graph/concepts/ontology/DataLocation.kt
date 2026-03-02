package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

/**
 * path: Describes either local path or path in URL format
 */
public open class DataLocation(
  public val path: String?,
  public val storage: Storage?,
  underlyingNode: Node? = null,
) : Functionality(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is DataLocation &&
              super.equals(other) &&
              other.path == this.path &&
              other.storage == this.storage

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              path,
              storage,
          )
}
