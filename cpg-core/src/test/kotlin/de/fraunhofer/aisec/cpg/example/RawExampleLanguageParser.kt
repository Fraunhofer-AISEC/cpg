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
package de.fraunhofer.aisec.cpg.example

class RawFileNode(name: String, val children: List<RawDeclarationNode>) : RawDeclarationNode(name)

class RawFunctionNode(name: String, val params: List<RawParameterNode>) : RawDeclarationNode(name)

class RawParameterNode(name: String, val type: RawTypeNode) : RawDeclarationNode(name)

open class RawDeclarationNode(val name: String) : RawNode()

/** This node represents some sort of type in the example language. */
open class RawTypeNode(val name: String)

/**
 * This node represents an in-memory object of a raw node of our example language that a parser
 * would create. It is the base node for all other classes.
 */
open class RawNode(val code: String? = null)
