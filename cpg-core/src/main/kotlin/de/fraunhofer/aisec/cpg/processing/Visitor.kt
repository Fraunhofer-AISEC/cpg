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
package de.fraunhofer.aisec.cpg.processing

import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import java.lang.reflect.InvocationTargetException

/**
 * Reflective visitor that visits the most specific implementation of visit() methods.
 *
 * @param <V> V must implement `IVisitable`. </V>
 */
abstract class Visitor<V : Visitable> {
    @JvmField val visited = IdentitySet<V>()

    open fun visit(t: V) {
        try {
            val mostSpecificVisit = this.javaClass.getMethod("visit", t::class.java)
            mostSpecificVisit.isAccessible = true
            mostSpecificVisit.invoke(this, t)
        } catch (e: NoSuchMethodException) {
            // Nothing to do here
        } catch (e: InvocationTargetException) {
            // Nothing to do here
        } catch (e: IllegalAccessException) {
            // Nothing to do here
        }
    }
}
