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
 * DataLocation: Describes the location as local or remote location
 */
public open class Document(
  public val filetype: String?,
  public val cryptographicHashs: MutableList<CryptographicHash?>,
  public val documentSignatures: MutableList<DocumentSignature?>,
  public val securityFeatures: MutableList<SecurityFeature?>,
  public val validatedBy: SchemaValidation?,
  dataLocation: DataLocation?,
  creation_time: ZonedDateTime?,
  description: String?,
  resourceId: String?,
  labels: MutableMap<String, String>?,
  name: String?,
  raw: String?,
  parent: Resource?,
  underlyingNode: Node? = null,
) : Data(dataLocation, creation_time, description, resourceId, labels, name, raw, parent, underlyingNode) {
  init {
    name?.let { this.name = Name(localName = it) }
  }

  override fun equals(other: Any?): Boolean = other is Document &&
              super.equals(other) &&
              other.filetype == this.filetype &&
              other.cryptographicHashs == this.cryptographicHashs &&
              other.documentSignatures == this.documentSignatures &&
              other.securityFeatures == this.securityFeatures &&
              other.validatedBy == this.validatedBy

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              filetype,
              cryptographicHashs,
              documentSignatures,
              securityFeatures,
              validatedBy,
          )
}
