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

import de.fraunhofer.aisec.cpg.frontends.DeclarationContext
import de.fraunhofer.aisec.cpg.frontends.HasKeywordSemantics
import de.fraunhofer.aisec.cpg.frontends.KeywordSemantics
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.Visibility
import de.fraunhofer.aisec.cpg.graph.types.FloatingPointType
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.NumericType
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import kotlin.reflect.KClass

/**
 * The LLVM IR [linkage type](https://llvm.org/docs/LangRef.html#linkage-types) keywords that
 * confine a global value or function to its own module (translation unit): `private` symbols are
 * not even exposed by name to the rest of the module, and `internal` behaves like C's file-scope
 * `static`. The `linker_private` variants are historical spellings of `private`. All of them map
 * onto [Visibility.INTERNAL].
 */
val LLVM_INTERNAL_LINKAGES = setOf("private", "internal", "linker_private", "linker_private_weak")

/** The LLVM IR language. */
open class LLVMIRLanguage : Language<LLVMIRLanguageFrontend>(), HasKeywordSemantics {
    override val fileExtensions = listOf("ll")
    override val namespaceDelimiter = "::"
    @DoNotPersist
    override val frontend: KClass<out LLVMIRLanguageFrontend> = LLVMIRLanguageFrontend::class
    override val compoundAssignmentOperators = setOf<String>()

    /**
     * Interprets an LLVM IR linkage keyword into its canonical [KeywordSemantics].
     *
     * LLVM IR expresses visibility through the
     * [linkage type](https://llvm.org/docs/LangRef.html#linkage-types) of a global value or
     * function rather than through record access control (LLVM has no such concept, so this
     * language deliberately does not implement
     * [de.fraunhofer.aisec.cpg.frontends.HasAccessControl]). The only axis this canonical model
     * captures is whether a symbol is confined to its own module:
     * - `private` / `internal` (and the historical `linker_private` spellings) grant *internal
     *   linkage*, i.e. the symbol must not be resolved from another translation unit
     *   ([Visibility.INTERNAL]);
     * - every other linkage type — including the default `external`, as well as `weak`, `linkonce`,
     *   `common`, ... — does *not* restrict resolution in a way this model captures, so it yields
     *   empty [KeywordSemantics] and leaves the declaration's visibility [Visibility.UNKNOWN].
     *
     * Leaving `external` at [Visibility.UNKNOWN] (rather than mapping it onto the access-control
     * value [Visibility.PUBLIC]) matches how the analogous, linkage-based C frontend treats
     * external linkage: as LLVM/C have no access control, external linkage carries no visibility
     * restriction (see the [Visibility] documentation). The [context] is ignored because LLVM
     * linkage always applies to module-level globals and functions.
     */
    override fun interpretKeyword(keyword: String, context: DeclarationContext): KeywordSemantics {
        return when (keyword) {
            in LLVM_INTERNAL_LINKAGES -> KeywordSemantics(visibility = Visibility.INTERNAL)
            else -> KeywordSemantics()
        }
    }

    // TODO: In theory, the integers can have any bit-width from 1 to 1^32 bits. It's not known if
    //  they are interpreted as signed or unsigned.
    @DoNotPersist
    override val builtInTypes =
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

            // these are not real LLVM-IR types, but we use them to differentiate unsigned types
            "ui1" to IntegerType("ui1", 1, this, NumericType.Modifier.UNSIGNED),
            "ui8" to IntegerType("ui8", 8, this, NumericType.Modifier.UNSIGNED),
            "ui32" to IntegerType("ui32", 32, this, NumericType.Modifier.UNSIGNED),
            "ui64" to IntegerType("ui64", 64, this, NumericType.Modifier.UNSIGNED),
            "ui128" to IntegerType("ui128", 128, this, NumericType.Modifier.UNSIGNED),
        )
}
