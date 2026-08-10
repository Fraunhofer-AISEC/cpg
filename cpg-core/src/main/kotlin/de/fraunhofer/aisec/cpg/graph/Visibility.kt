/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.passes.SymbolResolver

/**
 * The canonical, language-independent visibility of a [Declaration].
 *
 * Different languages spell visibility very differently (and sometimes overload a single keyword
 * for several unrelated meanings — e.g. C's `static`, which denotes internal linkage at file scope
 * but merely static storage duration inside a function). Language frontends are responsible for
 * mapping their surface syntax onto this canonical enum, keeping the raw, lossless spelling in
 * [HasModifiers.modifiers]. Passes such as the [SymbolResolver] can then reason about visibility
 * uniformly, without having to know any language's concrete keywords.
 *
 * Note that this enum deliberately conflates three conceptually distinct axes that never apply to
 * the same declaration at once, so that a single value is always sufficient:
 * - **Access control** ([PUBLIC], [PROTECTED], [PRIVATE]) restricts access relative to the
 *   *declaring [Record]* and only applies to record members.
 * - **Module visibility** ([PACKAGE]) restricts access relative to the *enclosing module/package*
 *   (e.g. Java's or Go's package), independently of any record.
 * - **Linkage** ([INTERNAL]) restricts access relative to the *translation unit* and only applies
 *   to non-member (file- or namespace-scope) declarations.
 *
 * Static-vs-instance "membership" is an orthogonal axis and is intentionally *not* modeled here; it
 * is captured by [de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration.isStatic].
 *
 * Only the **C/C++** frontend currently populates this property. The remaining entries document the
 * *intended* canonical mapping for the other languages, to be wired up as each frontend starts
 * interpreting its modifiers (see [de.fraunhofer.aisec.cpg.frontends.Language.applyModifiers]);
 * until then those languages leave every declaration at [UNKNOWN].
 * - **C** (implemented): external linkage → left [UNKNOWN] (C has no access control); file-scope
 *   `static` → [INTERNAL].
 * - **C++** (implemented): `public`/`protected`/`private` members; file-scope `static` and
 *   anonymous namespaces → [INTERNAL].
 * - **Java** (intended): `public`/`protected`/`private` members; the default (no modifier) →
 *   [PACKAGE].
 * - **Go** (intended): exported (upper-case) identifiers → [PUBLIC]; unexported (lower-case) →
 *   [PACKAGE].
 * - **TypeScript** (intended): `public`/`protected`/`private` members.
 * - **Ruby** (intended): `public`/`protected`/`private` methods.
 */
enum class Visibility {
    /** Visible everywhere the declaring scope is reachable. This is the most permissive value. */
    PUBLIC,

    /** A record member that is visible only within the declaring [Record] and its subclasses. */
    PROTECTED,

    /** A record member that is visible only within the declaring [Record]. */
    PRIVATE,

    /**
     * Module/package visibility: a declaration that is visible only within its own enclosing module
     * or package, but (unlike [PRIVATE]/[PROTECTED]) not tied to a [Record]. Java's package-private
     * default and Go's unexported (lower-case) identifiers are each intended to map to this
     * visibility once those frontends are wired up; no frontend populates it yet. Note this differs
     * from [INTERNAL] linkage, which is confined to a single translation unit rather than to a
     * whole module/package.
     */
    PACKAGE,

    /**
     * Internal linkage: a non-member declaration that is visible only within its own translation
     * unit and must therefore not be resolved from another one. The C/C++ `static` storage-class
     * specifier at file scope and C++ anonymous namespaces produce this visibility.
     */
    INTERNAL,

    /**
     * The visibility is unknown, not applicable, or not (yet) modeled by the language frontend.
     * This is the default and must be treated as "no visibility restriction" by consumers, so that
     * languages which do not model visibility are unaffected.
     */
    UNKNOWN,
}

/**
 * Whether this declaration has [Visibility.INTERNAL] linkage, i.e. it is confined to its own
 * translation unit. Such declarations still live in the shared [GlobalScope], but the
 * [SymbolResolver] drops them as resolution candidates for references originating in a different
 * translation unit, so that they cannot be resolved from another one.
 */
val Declaration.hasInternalLinkage: Boolean
    get() = visibility == Visibility.INTERNAL

/**
 * Whether access to this declaration is restricted by access control, i.e. it is
 * [Visibility.PRIVATE] or [Visibility.PROTECTED] and therefore not reachable from everywhere its
 * declaring [Record] is. Used by the [SymbolResolver] to decide whether a member is a viable
 * resolution candidate for a given access.
 */
val Declaration.hasRestrictedVisibility: Boolean
    get() = visibility == Visibility.PRIVATE || visibility == Visibility.PROTECTED
