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

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.Visibility
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
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

/**
 * The LLVM IR [linkage type](https://llvm.org/docs/LangRef.html#linkage-types) keyword for the
 * default `external` linkage, i.e. a symbol that participates in normal cross-module linking and is
 * therefore visible everywhere ([Visibility.PUBLIC]). This is LLVM's default when no linkage is
 * spelled out.
 */
const val LLVM_EXTERNAL_LINKAGE = "external"

/** The LLVM IR language. */
open class LLVMIRLanguage : Language<LLVMIRLanguageFrontend>() {
    override val fileExtensions = listOf("ll")
    override val namespaceDelimiter = "::"
    @DoNotPersist
    override val frontend: KClass<out LLVMIRLanguageFrontend> = LLVMIRLanguageFrontend::class
    override val compoundAssignmentOperators = setOf<String>()

    /**
     * Projects the LLVM IR linkage keyword recorded in [Declaration.modifiers] onto the canonical
     * [Visibility] model.
     *
     * LLVM IR expresses visibility through the
     * [linkage type](https://llvm.org/docs/LangRef.html#linkage-types) of a global value or
     * function rather than through record access control (LLVM has no such concept, so this
     * language deliberately does not implement
     * [de.fraunhofer.aisec.cpg.frontends.HasVisibilityModifiers]). The relevant axis is whether a
     * symbol is confined to its own module:
     * - `private` / `internal` (and the historical `linker_private` spellings) grant *internal
     *   linkage*, i.e. the symbol must not be resolved from another translation unit
     *   ([Visibility.INTERNAL]);
     * - the default `external` linkage participates in normal cross-module linking and is therefore
     *   visible everywhere ([Visibility.PUBLIC]).
     *
     * Every other linkage type (`weak`, `linkonce`, `common`, ...) does *not* restrict resolution
     * in a way this canonical model captures, so the declaration's visibility is left untouched at
     * [Visibility.UNKNOWN]. The [scope] is ignored because LLVM linkage always applies to
     * module-level globals and functions.
     */
    override fun applyModifiers(declaration: Declaration, scope: Scope?) {
        when {
            declaration.modifiers.any { it in LLVM_INTERNAL_LINKAGES } ->
                declaration.visibility = Visibility.INTERNAL
            LLVM_EXTERNAL_LINKAGE in declaration.modifiers ->
                declaration.visibility = Visibility.PUBLIC
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
