package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.List

public open class DatabaseQuery(
  public val modify: Boolean?,
  calls: List<String>?,
  databaseService: DatabaseService?,
  operatesOn: DatabaseStorage,
  underlyingNode: Node? = null,
) : DatabaseOperation(calls, databaseService, operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is DatabaseQuery &&
              super.equals(other) &&
              other.modify == this.modify

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              modify,
          )
}
