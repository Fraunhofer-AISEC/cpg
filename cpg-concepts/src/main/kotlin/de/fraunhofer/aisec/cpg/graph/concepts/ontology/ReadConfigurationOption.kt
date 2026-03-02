package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an operation to read a specific configuration option. Often this is done with a member access such as `group.option` or a subscript operation such as `group["option"]`.
 */
public open class ReadConfigurationOption(
  public val configurationOption: ConfigurationOption?,
  operatesOn: Configuration,
  underlyingNode: Node? = null,
) : ConfigurationOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is ReadConfigurationOption &&
              super.equals(other) &&
              other.configurationOption == this.configurationOption

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              configurationOption,
          )
}
