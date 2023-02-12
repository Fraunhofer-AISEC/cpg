/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.llvm

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.types.FloatingPointType
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.NumericType
import kotlin.reflect.KClass

/** The LLVM IR language. */
class LLVMIRLanguage : Language<LLVMIRLanguageFrontend>() {
    override val fileExtensions = listOf("ll")
    override val namespaceDelimiter = "::"
    override val frontend: KClass<out LLVMIRLanguageFrontend> = LLVMIRLanguageFrontend::class

    // TODO: In theory, the integers can have any bitwidth from 1 to 1^32 bits. It's not known if
    // they are interpreted as signed or unsigned.
    @Transient
    override val simpleTypes =
        mapOf(
            "i1" to IntegerType("i1", 1, this, NumericType.Modifier.NOT_APPLICABLE),
            "i8" to IntegerType("i8", 8, this, NumericType.Modifier.NOT_APPLICABLE),
            "i32" to IntegerType("i32", 32, this, NumericType.Modifier.NOT_APPLICABLE),
            "i64" to IntegerType("i64", 64, this, NumericType.Modifier.NOT_APPLICABLE),
            "i128" to IntegerType("i128", 128, this, NumericType.Modifier.NOT_APPLICABLE),
            "half" to FloatingPointType("half", 16, this, NumericType.Modifier.SIGNED),
            "bfloat" to FloatingPointType("bfloat", 16, this, NumericType.Modifier.SIGNED),
            "float" to FloatingPointType("float", 32, this, NumericType.Modifier.SIGNED),
            "double" to FloatingPointType("double", 64, this, NumericType.Modifier.SIGNED),
            "fp128" to FloatingPointType("fp128", 128, this, NumericType.Modifier.SIGNED),
            "x86_fp80" to FloatingPointType("x86_fp80", 80, this, NumericType.Modifier.SIGNED),
            "ppc_fp128" to FloatingPointType("ppc_fp128", 128, this, NumericType.Modifier.SIGNED),
        )

    override fun newFrontend(
        config: TranslationConfiguration,
        scopeManager: ScopeManager,
    ): LLVMIRLanguageFrontend {
        return LLVMIRLanguageFrontend(this, config, scopeManager)
    }
}
