package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import java.time.ZonedDateTime
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.MutableList
import kotlin.collections.MutableMap

/**
 * Represents a Darwin architecture, commonly found on macOS systems. macOS is a certified. [UNIX](https://www.opengroup.org/openbrand/register/apple.htm) and is (mostly) POSIX compatible.
 */
public open class Darwin(
  codeModules: MutableList<CodeModule?>,
  codeRepository: CodeRepository?,
  functionalities: MutableList<Functionality?>,
  creation_time: ZonedDateTime?,
  description: String?,
  resourceId: String?,
  labels: MutableMap<String, String>?,
  name: String?,
  raw: String?,
  parent: Resource?,
  underlyingNode: Node? = null,
) : OperatingSystemArchitecture(codeModules, codeRepository, functionalities, creation_time, description, resourceId, labels, name, raw, parent, underlyingNode) {
  init {
    name?.let { this.name = Name(localName = it) }
  }

  override fun equals(other: Any?): Boolean = other is Darwin &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
