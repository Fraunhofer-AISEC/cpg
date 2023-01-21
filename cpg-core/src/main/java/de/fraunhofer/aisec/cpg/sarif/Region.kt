package de.fraunhofer.aisec.cpg.sarif

import java.util.*

/** Code source location, in a SASP/SARIF-compliant "Region" format. */
class Region(val startLine: Int, val startColumn: Int, val endLine: Int, val endColumn: Int) : Comparable<Region> {

  override fun toString(): String {
    return "$startLine:$startColumn-$endLine:$endColumn"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is Region) {
      return false
    }

    return (this.startLine == other.startLine
            && this.startColumn == other.startColumn
            && this.endLine == other.endLine
            && this.endColumn == other.endColumn)
  }

  override fun compareTo(other: Region): Int {
    val compStartLine = this.startLine.compareTo(other.startLine)
    if (compStartLine != 0) {
      return compStartLine
    }

    val compStartColumn = this.startColumn.compareTo(other.startColumn)
    if (compStartColumn != 0) {
      return compStartColumn
    }

    val compEndLine = this.endLine.compareTo(other.endLine)
    if (compEndLine != 0) {
      return -compEndLine
    }

    return this.endColumn.compareTo(other.endColumn)
  }

  @Override
  override fun hashCode(): Int {
    return Objects.hash(this.startColumn, this.startLine, this.endColumn, this.endLine)
  }
}
