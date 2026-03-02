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
 * Represents a configuration option within one [group]. Usually there is one option for each entry in a configuration data structure.
 */
public open class ConfigurationOption(
  public val configurationGroup: ConfigurationGroup?,
  public val key: Key?,
  public val `value`: Value?,
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

  override fun equals(other: Any?): Boolean = other is ConfigurationOption &&
              super.equals(other) &&
              other.configurationGroup == this.configurationGroup &&
              other.key == this.key &&
              other.value == this.value

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              configurationGroup,
              key,
              value,
          )
}
