package de.fraunhofer.aisec.cpg.graph.ast

import com.fasterxml.jackson.annotation.JsonIgnore
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import org.neo4j.ogm.annotation.Relationship

/**
 * This is the base class for all AST nodes in the CPG. It is used to represent any node in the
 * abstract syntax tree (AST) of a program. It serves as a base class for more specific node types
 * such as [de.fraunhofer.aisec.cpg.graph.ast.statements.Statement]s, [de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Expression]s, [de.fraunhofer.aisec.cpg.graph.ast.declarations.Declaration]s, etc.
 */
abstract class AstNode : Node() {

    /**
     * Virtual property to return a list of the node's children. Uses the [de.fraunhofer.aisec.cpg.helpers.SubgraphWalker] to
     * retrieve the appropriate nodes.
     *
     * Note: This only returns the *direct* children of this node. If you want to have *all*
     * children, e.g., a flattened AST, you need to call [de.fraunhofer.aisec.cpg.graph.allChildren].
     *
     * For Neo4J OGM, this relationship will be automatically filled by a pre-save event before OGM
     * persistence. Therefore, this property is a `var` and not a `val`.
     */
    @Relationship("AST")
    @JsonIgnore
    var astChildren: List<AstNode> = listOf()
        get() = SubgraphWalker.getAstChildren(this)

    /** List of [Annotation]s associated with that node. */
    @Relationship("ANNOTATIONS") var annotationEdges = astEdgesOf<Annotation>()
    var annotations by unwrapping(AstNode::annotationEdges)

    override fun disconnectFromGraph() {
        super.disconnectFromGraph()

        // Disconnect all AST children first
        astChildren.forEach { it.disconnectFromGraph() }
    }
}