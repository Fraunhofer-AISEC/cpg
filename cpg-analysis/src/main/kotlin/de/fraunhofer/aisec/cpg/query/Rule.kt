/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.TranslationResult

interface Rule {
    /** the query result, one of the members has to be `null` */
    var queryResult: Pair<QueryTree<*>?, Pair<Boolean, List<*>>?>?

    // consider
    // https://github.com/microsoft/sarif-tutorials/blob/main/docs/Authoring-rule-metadata-and-result-messages.md
    // TODO: descriptive or "correct" names for the fields?
    // TODO: consider metadatea structure wrt output formats (SARIF but potentially others)
    //  rn the fields are quite specific which might not be ideal with multiple output formats
    /** stable and opaque identifier for the query */
    val id: String

    /** human readable name of the query */
    val name: String
    val shortDescription: String
    val mdShortDescription: String?
        get() = null

    val level: Level
    val message: String?
        get() = null

    val mdMessage: String?
        get() = null

    val messageArguments: List<String>?
        get() = null

    /**
     * executes the query on the given result. Stores the result in the [queryResult] field of the
     * respective rule. Should populate the [queryResult] field.
     *
     * @param result the result of a translation
     */
    fun run(result: TranslationResult)

    enum class Level {
        Error,
        Warning,
        Note,
        None
    }
}
