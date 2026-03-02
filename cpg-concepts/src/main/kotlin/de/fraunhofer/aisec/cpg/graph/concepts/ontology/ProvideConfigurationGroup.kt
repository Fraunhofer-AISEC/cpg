package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

public open class ProvideConfigurationGroup(
  public val configurationGroup: ConfigurationGroup?,
  public val configurationGroupSource: ConfigurationGroupSource?,
  operatesOn: Configuration,
  underlyingNode: Node? = null,
) : ConfigurationOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is ProvideConfigurationGroup &&
              super.equals(other) &&
              other.configurationGroup == this.configurationGroup &&
              other.configurationGroupSource == this.configurationGroupSource

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              configurationGroup,
              configurationGroupSource,
          )
}
