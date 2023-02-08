/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.SupportsParallelParsing
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.io.FileOutputStream

@SupportsParallelParsing(false)
class GoLanguageFrontend(
    language: Language<GoLanguageFrontend>,
    config: TranslationConfiguration,
    scopeManager: ScopeManager,
) : LanguageFrontend(language, config, scopeManager) {
    companion object {

        init {
            try {
                val arch =
                    System.getProperty("os.arch")
                        .replace("aarch64", "arm64")
                        .replace("x86_64", "amd64")
                val ext: String =
                    if (System.getProperty("os.name").startsWith("Mac")) {
                        ".dylib"
                    } else {
                        ".so"
                    }

                val stream =
                    GoLanguageFrontend::class.java.getResourceAsStream("/libcpgo-$arch$ext")

                val tmp = File.createTempFile("libcpgo", ext)
                tmp.deleteOnExit()
                val fos = FileOutputStream(tmp)
                stream.copyTo(FileOutputStream(tmp))

                fos.close()
                stream.close()

                log.info("Loading cpgo library from ${tmp.absoluteFile}")

                System.load(tmp.absolutePath)
            } catch (ex: Exception) {
                log.error(
                    "Error while loading cpgo library. Go frontend will not work correctly",
                    ex
                )
            }
        }
    }

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        return parseInternal(
            file.readText(Charsets.UTF_8),
            file.path,
            config.topLevel?.absolutePath ?: file.parent
        )
    }

    override fun <T> getCodeFromRawNode(astNode: T): String? {
        // this is handled by native code
        return null
    }

    override fun <T> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        // this is handled by native code
        return null
    }

    override fun <S, T> setComment(s: S, ctx: T) {}

    private external fun parseInternal(
        s: String?,
        path: String,
        topLevel: String
    ): TranslationUnitDeclaration
}
