/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg_vis_neo4j

import de.fraunhofer.aisec.cpg.frontends.Language
import kotlin.reflect.full.createInstance
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext

/** This annotation can be used to skip a test if a certain language is not available. */
@ExtendWith(LanguageAvailability::class)
annotation class SkipIfLanguageIsNotAvailable(
    /** The fully qualified name of the language that must be available. */
    val language: String
)

/** This condition checks if a [Language] is available by trying to instantiate it. */
class LanguageAvailability : ExecutionCondition {
    override fun evaluateExecutionCondition(
        context: ExtensionContext?
    ): ConditionEvaluationResult? {
        val annotation =
            context?.element?.get()?.getAnnotation(SkipIfLanguageIsNotAvailable::class.java)

        if (annotation == null) {
            return ConditionEvaluationResult.enabled("No language annotation found")
        }

        try {
            Class.forName(annotation.language).kotlin.createInstance() as? Language<*>
        } catch (_: ClassNotFoundException) {
            return ConditionEvaluationResult.disabled(
                "Language ${annotation.language} is not available"
            )
        }

        return ConditionEvaluationResult.enabled("Language ${annotation.language} is available")
    }
}
