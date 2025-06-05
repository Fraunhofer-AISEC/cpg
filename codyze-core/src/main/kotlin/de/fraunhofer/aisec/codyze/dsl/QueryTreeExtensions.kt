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
@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package de.fraunhofer.aisec.codyze.dsl

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.query.*

/** Performs a logical and (&&) operation between the values and creates and returns [Decision]s. */
context(TranslationResult)
infix fun QueryTree<Boolean>.and(other: Decision): Decision {
    return this.decide() and other
}

/** Performs a logical and (&&) operation between the values and creates and returns [Decision]s. */
context(RequirementsBuilder, TranslationResult)
infix fun Decision.and(other: QueryTree<Boolean>): Decision {
    return this and other.decide()
}

/** Performs a logical or (||) operation between the values and creates and returns [Decision]s. */
context(TranslationResult)
infix fun QueryTree<Boolean>.or(other: Decision): Decision {
    return this.decide() or other
}

/** Performs a logical or (||) operation between the values and creates and returns [Decision]s. */
context(RequirementsBuilder, TranslationResult)
infix fun Decision.or(other: QueryTree<Boolean>): Decision {
    return this or other.decide()
}

/** Performs a logical xor operation between the values and creates and returns [Decision]s. */
context(TranslationResult)
infix fun QueryTree<Boolean>.xor(other: Decision): Decision {
    return this.decide() xor other
}

/** Performs a logical xor operation between the values and creates and returns [Decision]s. */
context(RequirementsBuilder, TranslationResult)
infix fun Decision.xor(other: QueryTree<Boolean>): Decision {
    return this xor other.decide()
}

/**
 * Performs a logical implication (->) operation between the values and creates and returns
 * [QueryTree]s.
 */
context(TranslationResult)
infix fun QueryTree<Boolean>.implies(other: Decision): Decision {
    return this.decide() implies other
}

/**
 * Performs a logical implication (->) operation between the values and creates and returns
 * [QueryTree]s.
 */
context(RequirementsBuilder, TranslationResult)
infix fun Decision.implies(other: QueryTree<Boolean>): Decision {
    return this implies other.decide()
}
