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

/**
 * The node that represents the "value" of this option. For example, in an INI file, this would be the [FieldDeclaration.initializer] node that represents the value.
 */
public open class Value(
  dataLocation: DataLocation?,
  creation_time: ZonedDateTime?,
  description: String?,
  resourceId: String?,
  labels: MutableMap<String, String>?,
  name: String?,
  raw: String?,
  parent: Resource?,
  underlyingNode: Node? = null,
) : Data(dataLocation, creation_time, description, resourceId, labels, name, raw, parent, underlyingNode) {
  init {
    name?.let { this.name = Name(localName = it) }
  }

  override fun equals(other: Any?): Boolean = other is Value &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
