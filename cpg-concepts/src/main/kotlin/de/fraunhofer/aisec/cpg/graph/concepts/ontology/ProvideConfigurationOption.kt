package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an operation to provide a [ConfigurationOption]. It connects a [ConfigurationOptionSource] with a [ConfigurationOption].
 */
public open class ProvideConfigurationOption(
  public val configurationOption: ConfigurationOption?,
  public val configurationOptionSource: ConfigurationOptionSource?,
  operatesOn: Configuration,
  underlyingNode: Node? = null,
) : ConfigurationOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is ProvideConfigurationOption &&
              super.equals(other) &&
              other.configurationOption == this.configurationOption &&
              other.configurationOptionSource == this.configurationOptionSource

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              configurationOption,
              configurationOptionSource,
          )
}
