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
package de.fraunhofer.aisec.cpg.frontends.java

import com.github.javaparser.ParserConfiguration.LanguageLevel
import de.fraunhofer.aisec.cpg.frontends.FrontendConfiguration
import de.fraunhofer.aisec.cpg.graph.FrontendProvider
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import java.nio.file.Path

/**
 * Configuration for the [JavaLanguageFrontend].
 *
 * @param sourceRoots Additional source roots (e.g., the `src/main/java` folders of other modules in
 *   a multi-module project) that the JavaParser type solver uses to resolve symbols, in addition to
 *   the top-level of the current component.
 * @param classpath Jar files (e.g., the dependency classpath of a Maven or Gradle module) that the
 *   JavaParser type solver uses to resolve symbols of external libraries.
 * @param languageLevel The Java language level used to parse the source files.
 */
class JavaFrontendConfiguration(
    val sourceRoots: List<Path> = listOf(),
    val classpath: List<Path> = listOf(),
    val languageLevel: LanguageLevel = LanguageLevel.JAVA_21,
) : FrontendConfiguration<JavaLanguageFrontend>() {
    context(provider: FrontendProvider<JavaLanguageFrontend>)
    override fun doNotParseBody(node: Function): Boolean = false
}
