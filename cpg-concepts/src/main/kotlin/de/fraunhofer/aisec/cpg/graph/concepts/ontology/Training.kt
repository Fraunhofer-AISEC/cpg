package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import java.time.ZonedDateTime
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.MutableMap

public open class Training(
  public val annualUpdateCompleted: Boolean?,
  public val successfullyCompletedPercentage: Boolean?,
  creation_time: ZonedDateTime?,
  description: String?,
  resourceId: String?,
  labels: MutableMap<String, String>?,
  name: String?,
  raw: String?,
  parent: Resource?,
  underlyingNode: Node? = null,
) : HumanResources(creation_time, description, resourceId, labels, name, raw, parent, underlyingNode) {
  init {
    name?.let { this.name = Name(localName = it) }
  }

  override fun equals(other: Any?): Boolean = other is Training &&
              super.equals(other) &&
              other.annualUpdateCompleted == this.annualUpdateCompleted &&
              other.successfullyCompletedPercentage == this.successfullyCompletedPercentage

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              annualUpdateCompleted,
              successfullyCompletedPercentage,
          )
}
