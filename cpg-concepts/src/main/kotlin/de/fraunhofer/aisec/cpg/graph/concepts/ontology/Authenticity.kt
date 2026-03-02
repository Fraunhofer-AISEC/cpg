package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

public open class Authenticity(
  public val contextIsChecked: Boolean?,
  public val rotationInterval: Int?,
  underlyingNode: Node? = null,
) : SecurityFeature(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is Authenticity &&
              super.equals(other) &&
              other.contextIsChecked == this.contextIsChecked &&
              other.rotationInterval == this.rotationInterval

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              contextIsChecked,
              rotationInterval,
          )
}
