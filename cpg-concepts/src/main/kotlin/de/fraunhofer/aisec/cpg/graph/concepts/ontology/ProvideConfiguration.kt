package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an operation to provide a [Configuration], e.g., in the form of a configuration file (through a [ConfigurationSource]). When the configuration file is loaded, a [LoadConfiguration] operation would be found in the code component (matching the configuration file's name in [LoadConfiguration.fileExpression]) and the [ProvideConfiguration] operation would be found in the configuration component. But also other sources of configuration could be represented by a [ProvideConfiguration] operation, such as environment variables or command-line arguments. Note: The [ProvideConfiguration] operation is part of the [ConfigurationSource.ops] and not of the [Configuration.ops] as it's an operation of the source, not the target.
 */
public open class ProvideConfiguration(
  public val configurationSource: ConfigurationSource?,
  operatesOn: Configuration,
  underlyingNode: Node? = null,
) : ConfigurationOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is ProvideConfiguration &&
              super.equals(other) &&
              other.configurationSource == this.configurationSource

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              configurationSource,
          )
}
