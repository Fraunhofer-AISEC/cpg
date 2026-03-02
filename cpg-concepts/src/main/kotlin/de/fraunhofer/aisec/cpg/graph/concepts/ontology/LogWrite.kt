package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

public enum class LogLevel {
  FATAL,
  CRITICAL,
  ERROR,
  WARN,
  INFO,
  DEBUG,
  TRACE,
  UNKNOWN,
}

/**
 * A log write operation e.g. `loggint.warn("...")`.
 */
public open class LogWrite(
  public val logLevel: LogLevel?,
  operatesOn: Logging,
  underlyingNode: Node? = null,
) : LogOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is LogWrite &&
              super.equals(other) &&
              other.logLevel == this.logLevel

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              logLevel,
          )
}
