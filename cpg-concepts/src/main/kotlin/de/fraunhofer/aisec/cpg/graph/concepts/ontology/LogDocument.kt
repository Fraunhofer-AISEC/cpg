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

public open class LogDocument(
  filetype: String?,
  cryptographicHashs: MutableList<CryptographicHash?>,
  documentSignatures: MutableList<DocumentSignature?>,
  securityFeatures: MutableList<SecurityFeature?>,
  validatedBy: SchemaValidation?,
  dataLocation: DataLocation?,
  creation_time: ZonedDateTime?,
  description: String?,
  resourceId: String?,
  labels: MutableMap<String, String>?,
  name: String?,
  raw: String?,
  parent: Resource?,
  underlyingNode: Node? = null,
) : Document(filetype, cryptographicHashs, documentSignatures, securityFeatures, validatedBy, dataLocation, creation_time, description, resourceId, labels, name, raw, parent, underlyingNode) {
  init {
    name?.let { this.name = Name(localName = it) }
  }

  override fun equals(other: Any?): Boolean = other is LogDocument &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
