package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.collections.MutableList

public open class SecurityAdvisoryFeed(
  public val securityAdvisoryDocuments: MutableList<SecurityAdvisoryDocument?>,
  underlyingNode: Node? = null,
) : Functionality(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is SecurityAdvisoryFeed &&
              super.equals(other) &&
              other.securityAdvisoryDocuments == this.securityAdvisoryDocuments

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              securityAdvisoryDocuments,
          )
}
