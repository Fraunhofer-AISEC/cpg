package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import java.time.ZonedDateTime
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.MutableMap

public open class Resource(
  public val creation_time: ZonedDateTime?,
  public val description: String?,
  public val resourceId: String?,
  public val labels: MutableMap<String, String>?,
  name: String?,
  public val raw: String?,
  public val parent: Resource?,
  underlyingNode: Node? = null,
) : Concept(underlyingNode) {
  init {
    name?.let { this.name = Name(localName = it) }
  }

  override fun equals(other: Any?): Boolean = other is Resource &&
              super.equals(other) &&
              other.creation_time == this.creation_time &&
              other.description == this.description &&
              other.resourceId == this.resourceId &&
              other.labels == this.labels &&
              other.name == this.name &&
              other.raw == this.raw &&
              other.parent == this.parent

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              creation_time,
              description,
              resourceId,
              labels,
              name,
              raw,
              parent,
          )
}
