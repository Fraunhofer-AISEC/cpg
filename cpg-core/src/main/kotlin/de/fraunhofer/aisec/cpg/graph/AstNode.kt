/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.graph

import com.fasterxml.jackson.annotation.JsonIgnore
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.neo4j.ogm.annotation.Relationship

/**
 * This is the base class for all AST nodes in the CPG. It is used to represent any node in the
 * abstract syntax tree (AST) of a program. It serves as a base class for more specific node types
 * such as [Statement]s, [Expression]s, [Declaration]s, etc.
 */
abstract class AstNode : Node() {

    /**
     * Virtual property to return a list of the node's children. Uses the [SubgraphWalker] to
     * retrieve the appropriate nodes.
     *
     * Note: This only returns the *direct* children of this node. If you want to have *all*
     * children, e.g., a flattened AST, you need to call [AstNode.allChildren].
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

    val idAst: String by lazy {
        /*
         * proposed structure:
         *     {parent}/{simple class name}/{name | signature | value}
         */
        var item: String =
            when (this) {
                is FunctionDeclaration -> signature
                is Literal<*> -> value.toString()
                is Block -> astParent.blocks.indexOf(this).toString()
                is ParameterDeclaration ->
                    if (name.isEmpty()) astParent.parameters.indexOf(this).toString()
                    else name.toString()
                else -> name.toString()
            }

        val parentId = if (astParent != null) astParent?.idAst + "/" else ""

        parentId +
            this::class.simpleName +
            if (item.isEmpty()) "" else "/" + URLEncoder.encode(item, StandardCharsets.UTF_8)
    }
}
