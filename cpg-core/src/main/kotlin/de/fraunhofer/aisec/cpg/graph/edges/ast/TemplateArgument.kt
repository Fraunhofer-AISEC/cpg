/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.edges.ast

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ast.AstNode
import de.fraunhofer.aisec.cpg.graph.ast.declarations.TemplateDeclaration.TemplateInitialization
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.CallExpression

/** This edge represents a template argument that is attached to a [CallExpression]. */
class TemplateArgument<NodeType : AstNode>(
    start: Node,
    end: NodeType,
    var instantiation: TemplateInitialization? = TemplateInitialization.EXPLICIT,
) : AstEdge<NodeType>(start as AstNode, end)

/** A container for [TemplateArgument] edges. */
class TemplateArguments<NodeType : AstNode>(thisRef: Node) :
    AstEdges<NodeType, TemplateArgument<NodeType>>(
        thisRef,
        init = { start, end -> TemplateArgument(start, end) },
    )
