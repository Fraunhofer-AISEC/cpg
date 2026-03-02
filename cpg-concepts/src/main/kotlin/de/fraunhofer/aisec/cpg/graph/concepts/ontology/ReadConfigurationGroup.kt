package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an operation to read a specific configuration group. Often this is done with a member access or a subscript operation on the configuration object, such as`conf.GROUP` or`conf["GROUP"]`.
 */
public open class ReadConfigurationGroup(
  public val configurationGroup: ConfigurationGroup?,
  operatesOn: Configuration,
  underlyingNode: Node? = null,
) : ConfigurationOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is ReadConfigurationGroup &&
              super.equals(other) &&
              other.configurationGroup == this.configurationGroup

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              configurationGroup,
          )
}
