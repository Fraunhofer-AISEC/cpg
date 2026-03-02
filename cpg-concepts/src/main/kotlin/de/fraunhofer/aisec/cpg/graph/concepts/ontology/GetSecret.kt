package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * An operation that retrieves a secret from a (remote) location. This can be a local keystore, a remote key server or a hardware device such as a TPM or HSM.
 */
public open class GetSecret(
  operatesOn: Secret,
  underlyingNode: Node? = null,
) : SecretOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is GetSecret &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
