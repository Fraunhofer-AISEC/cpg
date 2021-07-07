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
package de.fraunhofer.aisec.cpg.analysis

import org.apache.commons.lang3.builder.ToStringStyle

// ----------------------------------------------------------------------------
class MultiLineToStringStyle internal constructor() : ToStringStyle() {
    private fun readResolve(): Any {
        return this
    }

    companion object {
        private const val serialVersionUID = 1L
    }

    init {
        this.isUseIdentityHashCode = false
        this.isUseShortClassName = true
        contentStart = "{"
        fieldSeparator = System.lineSeparator() + "  "
        this.isFieldSeparatorAtStart = true
        contentEnd = System.lineSeparator() + "}"
    }
}
