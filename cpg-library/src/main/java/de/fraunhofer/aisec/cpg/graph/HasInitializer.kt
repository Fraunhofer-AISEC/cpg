package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression

/** Specifies that a certain node has an initializer. */
interface HasInitializer {

    val initializer: Expression?

}