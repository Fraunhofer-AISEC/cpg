package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

public open class PasswordBasedAuthentication(
  public val activated: Boolean?,
  contextIsChecked: Boolean?,
  rotationInterval: Int?,
  underlyingNode: Node? = null,
) : Authenticity(contextIsChecked, rotationInterval, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is PasswordBasedAuthentication &&
              super.equals(other) &&
              other.activated == this.activated

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              activated,
          )
}
