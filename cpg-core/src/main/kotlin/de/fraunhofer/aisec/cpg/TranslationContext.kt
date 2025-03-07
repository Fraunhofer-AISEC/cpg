/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.TranslationManager.AdditionalSource
import de.fraunhofer.aisec.cpg.TranslationManager.Companion.log
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import java.io.File

/**
 * The translation context holds all necessary managers and configurations needed during the
 * translation process.
 */
@DoNotPersist
open class TranslationContext(
    /** The configuration for this translation. */
    val config: TranslationConfiguration = TranslationConfiguration.builder().build(),

    /**
     * The scope manager which comprises the complete translation result. In case of sequential
     * parsing, this scope manager is passed to the individual frontends one after another. In case
     * of sequential parsing, individual scope managers will be passed to each language frontend
     * (through individual contexts) and then finally merged into a final one.
     */
    val scopeManager: ScopeManager = ScopeManager(),

    /**
     * The type manager is responsible for managing type information. Currently, we have one
     * instance of a [TypeManager] for the overall [TranslationResult].
     */
    val typeManager: TypeManager = TypeManager(),

    /**
     * Some frontends need access to the current [Component] we are currently processing. Note: for
     * the [TranslationResult.finalCtx] this may either be null or the last component analyzed.
     */
    var currentComponent: Component? = null,

    /**
     * Set of files, that are available for additional analysis. They are not the primary subjects
     * of analysis but are available to the language frontend. The files are obtained by expanding
     * the paths in [TranslationConfiguration.includePaths]. This is done by
     * [TranslationManager.runFrontends].
     *
     * The frontend can decide to add some of the contained files to [importedSources] which will
     * get them translated into the final graph by the [TranslationManager].
     */
    var additionalSources: MutableSet<AdditionalSource> = mutableSetOf(),

    /**
     * The additional sources from the [additionalSources] chosen to be analyzed along with the code
     * under analysis. The language frontends are supposed to fill this list, e.g. by analyzing the
     * import statements of the analyzed code and deciding which sources contain relevant symbols.
     */
    var importedSources: MutableSet<AdditionalSource> = mutableSetOf(),
) {
    /**
     * The set of languages available in this translation context. We store this information here
     * because we want to ensure that we only have one instance of a language per
     * [TranslationResult].
     */
    val availableLanguages: List<Language<*>> = createAvailableLanguages()

    /**
     * Returns the single available language of the given type [T] or null if no such language is
     * available.
     */
    inline fun <reified T : Language<*>> availableLanguage(): T? {
        return availableLanguages.filterIsInstance<T>().singleOrNull()
    }

    /**
     * Creates a list of available [Language] nodes based on the registered languages in the
     * [TranslationConfiguration.languages].
     */
    private fun createAvailableLanguages(): List<Language<*>> {
        // We need to initialize the available languages out of the context
        return config.languages.mapNotNull {
            val language = it.constructors.firstOrNull()?.call(this)
            if (language == null) {
                log.error("Could not create language instance for {}", it.simpleName)
            }
            language
        }
    }

    /**
     * This extension function returns an appropriate [Language] for this [File] based on the
     * [availableLanguages], which in turn are based on the registered file extensions of
     * [TranslationConfiguration.languages]. It will emit a warning if multiple languages are
     * registered for the same extension (and the first one that was registered will be returned).
     */
    internal val File.language: Language<*>?
        get() {
            val languages = availableLanguages.filter { it.handlesFile(this) }
            if (languages.size > 1) {
                TranslationManager.Companion.log.warn(
                    "Multiple languages match for file extension ${this.extension}, the first registered language will be used."
                )
            }
            return languages.firstOrNull()
        }

    /**
     * This is a special [TranslationContext] that is used when no translation context is available.
     * This MUST only be used internally. It primarily serves one use-case: We need to gather
     * certain metadata information about a [Language] inside the [TranslationConfiguration].
     * Therefore, we need to instantiate the language, but we do not have a translation context
     * available at that point. This is why we use this special [EmptyTranslationContext] in the
     * [TranslationConfiguration] to instantiate the language. This language instance is then not
     * used after the config is built.
     */
    internal object EmptyTranslationContext : TranslationContext()
}
