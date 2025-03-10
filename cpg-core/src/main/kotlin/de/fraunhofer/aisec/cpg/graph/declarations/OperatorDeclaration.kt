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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.HasOperatorOverloading
import de.fraunhofer.aisec.cpg.graph.HasOperatorCode
import de.fraunhofer.aisec.cpg.graph.types.ObjectType

/**
 * Some languages allow to either overload operators or to add custom operators to classes (see
 * [HasOperatorOverloading]). In both cases, this special function class denotes that this handles
 * this particular operator call.
 *
 * We need to derive from [MethodDeclaration] because all operators have a base class on which they
 * operate on. Therefore, we need to associate them with an [ObjectType] and/or [RecordDeclaration].
 * There are some very special cases for C++, where we can have a global operator for a particular
 * class. In this case we just pretend like it is a method operator.
 */
class OperatorDeclaration internal constructor(ctx: TranslationContext) :
    MethodDeclaration(ctx), HasOperatorCode {
    /** The operator code which this operator declares. */
    override var operatorCode: String? = null

    val isPrefix: Boolean = false
    val isPostfix: Boolean = false
}
