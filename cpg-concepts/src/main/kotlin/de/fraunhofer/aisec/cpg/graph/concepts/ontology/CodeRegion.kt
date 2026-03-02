package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class CodeRegion(
  code: String?,
  public val endColumn: Int?,
  public val endLine: Int?,
  public val `file`: String?,
  public val startColumn: Int?,
  public val startLine: Int?,
  underlyingNode: Node? = null,
) : Functionality(underlyingNode) {
  init {
    code?.let { this.code = it }
  }

  override fun equals(other: Any?): Boolean = other is CodeRegion &&
              super.equals(other) &&
              other.code == this.code &&
              other.endColumn == this.endColumn &&
              other.endLine == this.endLine &&
              other.file == this.file &&
              other.startColumn == this.startColumn &&
              other.startLine == this.startLine

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              code,
              endColumn,
              endLine,
              file,
              startColumn,
              startLine,
          )
}
