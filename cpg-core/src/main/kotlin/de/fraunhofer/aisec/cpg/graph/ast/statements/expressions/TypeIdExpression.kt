/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.ast.statements.expressions

import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.Objects

/**
 * Models C++ operations that inspect types. These are `typeof`, `sizeof`, `typeid`, `alignof`and
 * are stored as string in their operator code.
 *
 * TODO: Is such a class really necessary??
 */
class TypeIdExpression : Expression() {
    var referencedType: Type? = null
    var operatorCode: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeIdExpression) return false
        return super.equals(other) &&
            operatorCode == other.operatorCode &&
            referencedType == other.referencedType
    }

    override fun hashCode() = Objects.hash(super.hashCode(), operatorCode, referencedType)
}
