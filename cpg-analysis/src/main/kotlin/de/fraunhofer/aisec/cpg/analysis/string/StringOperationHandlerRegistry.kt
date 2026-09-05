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
package de.fraunhofer.aisec.cpg.analysis.string

import de.fraunhofer.aisec.cpg.analysis.string.jvm.JvmStringOperationHandler
import de.fraunhofer.aisec.cpg.analysis.string.python.PythonStringOperationHandler
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.Node
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A per-language registry of [StringOperationHandler]s, so that [Node.evaluateString] can
 * automatically dispatch to the right handler(s) without every caller having to construct a
 * [StringEvaluator] with an explicit `operationHandlers` list.
 *
 * **Why keyed by [Language]'s simple class name, and not by [Language] itself.**
 * [StringOperationHandler] (and everything else in `cpg-analysis`) cannot reference concrete
 * frontend `Language` subclasses such as `PythonLanguage`/`JavaLanguage` directly: those live in
 * their own frontend modules (`cpg-language-python`, `cpg-language-java`, ...), which are
 * individually optional at build time (see `gradle.properties`'s `enable*Frontend` flags), and
 * `cpg-analysis` must build successfully regardless of which frontends are enabled. At the same
 * time, `cpg-core` (where [Language] lives) cannot depend on `cpg-analysis` (where
 * [StringOperationHandler]/[StringPattern] live) either, since that would invert the module
 * dependency direction. [Language.name] is derived automatically from the concrete subclass's
 * simple name (`Name(this::class.simpleName ?: EMPTY_NAME)`, see `Language`'s `init` block), e.g.
 * `"PythonLanguage"`/`"JavaLanguage"`, giving us a stable string identifier for a language without
 * needing to import its class. This mirrors the codebase's existing idiom of explicit,
 * string/class-based registration (e.g. `TranslationConfiguration.Builder.registerLanguage`) rather
 * than classpath scanning/`ServiceLoader`, which this project does not use anywhere.
 *
 * Registration is thread-safe: while in practice the built-in handlers are registered once, at
 * class-init time, below, [register] itself makes no such assumption (evaluators, per
 * [StringEvaluator]'s own KDoc, are expected to run concurrently).
 */
object StringOperationHandlerRegistry {
    private val handlers = ConcurrentHashMap<String, MutableList<StringOperationHandler>>()

    init {
        register("PythonLanguage", PythonStringOperationHandler())
        register("JavaLanguage", JvmStringOperationHandler())
    }

    /**
     * Registers [handler] for the language whose [Language]-subclass simple name is
     * [languageSimpleName] (e.g. `"PythonLanguage"`). Multiple handlers may be registered for the
     * same language; they are consulted in registration order, alongside any handler explicitly
     * passed to [StringEvaluator].
     */
    fun register(languageSimpleName: String, handler: StringOperationHandler) {
        handlers.computeIfAbsent(languageSimpleName) { CopyOnWriteArrayList() }.add(handler)
    }

    /**
     * The handlers registered for [language], in registration order, or an empty list if no handler
     * has been registered for it - this is a normal, expected case (e.g. a language for which no
     * [StringOperationHandler] exists yet), not an error.
     */
    fun forLanguage(language: Language<*>): List<StringOperationHandler> =
        handlers[language::class.simpleName]?.toList() ?: emptyList()

    /** Convenience overload of [forLanguage] taking a [Node], delegating to [Node.language]. */
    fun forLanguage(node: Node): List<StringOperationHandler> = forLanguage(node.language)
}
