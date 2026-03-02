package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.String
import kotlin.collections.MutableList

/**
 * enabled means the resource _can_ be reached via https, while enforced means it _can only_ be reached via https (or http traffic is redirected)
 */
public open class TransportEncryption(
  public val enabled: Boolean?,
  public val enforced: Boolean?,
  public val protocol: String?,
  public val protocolVersion: Float?,
  public val tlsSignatureAlgorithm: String?,
  public val cipherSuites: MutableList<CipherSuite?>,
  basedOn: Cipher?,
  secret: Secret?,
  underlyingNode: Node? = null,
) : Encryption(basedOn, secret, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is TransportEncryption &&
              super.equals(other) &&
              other.enabled == this.enabled &&
              other.enforced == this.enforced &&
              other.protocol == this.protocol &&
              other.protocolVersion == this.protocolVersion &&
              other.tlsSignatureAlgorithm == this.tlsSignatureAlgorithm &&
              other.cipherSuites == this.cipherSuites

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              enabled,
              enforced,
              protocol,
              protocolVersion,
              tlsSignatureAlgorithm,
              cipherSuites,
          )
}
