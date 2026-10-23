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
package de.fraunhofer.aisec.cpg.helpers

fun <R, T, C : MutableCollection<in R>> Iterable<T>.mapFilteredTo(
    target: C,
    predicate: (T) -> Boolean,
    transform: (T) -> R,
): C {
    for (element in this) {
        if (predicate(element)) {
            target.add(transform(element))
        }
    }
    return target
}

fun <R, T> Iterable<T>.mapFiltered(predicate: (T) -> Boolean, transform: (T) -> R): List<R> =
    mapFilteredTo(mutableListOf(), predicate, transform)

/** Single-pass equivalent of `this.map(transform).filter(predicate)`. */
fun <T, R, C : MutableCollection<in R>> Iterable<T>.filterMappedTo(
    target: C,
    transform: (T) -> R,
    predicate: (R) -> Boolean,
): C {
    for (element in this) {
        val mapped = transform(element)
        if (predicate(mapped)) {
            target.add(mapped)
        }
    }
    return target
}

/** Single-pass equivalent of `this.map(transform).filter(predicate)`. */
fun <T, R> Iterable<T>.filterMapped(transform: (T) -> R, predicate: (R) -> Boolean): List<R> =
    filterMappedTo(mutableListOf(), transform, predicate)

/** Single-pass equivalent of `this.filter(predicate).flatMap(transform)`. */
fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapFilteredTo(
    target: C,
    predicate: (T) -> Boolean,
    transform: (T) -> Iterable<R>,
): C {
    for (element in this) {
        if (predicate(element)) {
            target.addAll(transform(element))
        }
    }
    return target
}

/** Single-pass equivalent of `this.filter(predicate).flatMap(transform)`. */
fun <T, R> Iterable<T>.flatMapFiltered(
    predicate: (T) -> Boolean,
    transform: (T) -> Iterable<R>,
): List<R> = flatMapFilteredTo(mutableListOf(), predicate, transform)

/** Single-pass equivalent of `this.flatMap(transform).filter(predicate)`. */
fun <T, R, C : MutableCollection<in R>> Iterable<T>.filterFlatMappedTo(
    target: C,
    transform: (T) -> Iterable<R>,
    predicate: (R) -> Boolean,
): C {
    for (element in this) {
        for (mapped in transform(element)) {
            if (predicate(mapped)) {
                target.add(mapped)
            }
        }
    }
    return target
}

/** Single-pass equivalent of `this.flatMap(transform).filter(predicate)`. */
fun <T, R> Iterable<T>.filterFlatMapped(
    transform: (T) -> Iterable<R>,
    predicate: (R) -> Boolean,
): List<R> = filterFlatMappedTo(mutableListOf(), transform, predicate)

/** Single-pass equivalent of `this.flatMap(transform).map(mapper)`. */
fun <T, R, U, C : MutableCollection<in U>> Iterable<T>.mapFlatMappedTo(
    target: C,
    transform: (T) -> Iterable<R>,
    mapper: (R) -> U,
): C {
    for (element in this) {
        for (mapped in transform(element)) {
            target.add(mapper(mapped))
        }
    }
    return target
}

/** Single-pass equivalent of `this.flatMap(transform).map(mapper)`. */
fun <T, R, U> Iterable<T>.mapFlatMapped(transform: (T) -> Iterable<R>, mapper: (R) -> U): List<U> =
    mapFlatMappedTo(mutableListOf(), transform, mapper)

/** Single-pass equivalent of `this.filter(predicate).mapNotNull(transform)`. */
fun <T, R, C : MutableCollection<in R>> Iterable<T>.mapNotNullFilteredTo(
    target: C,
    predicate: (T) -> Boolean,
    transform: (T) -> R?,
): C {
    for (element in this) {
        if (predicate(element)) {
            transform(element)?.let { target.add(it) }
        }
    }
    return target
}

/** Single-pass equivalent of `this.filter(predicate).mapNotNull(transform)`. */
fun <T, R> Iterable<T>.mapNotNullFiltered(
    predicate: (T) -> Boolean,
    transform: (T) -> R?,
): List<R> = mapNotNullFilteredTo(mutableListOf(), predicate, transform)

/** Single-pass equivalent of `this.mapNotNull(transform).filter(predicate)`. */
fun <T, R, C : MutableCollection<in R>> Iterable<T>.filterMapNotNulledTo(
    target: C,
    transform: (T) -> R?,
    predicate: (R) -> Boolean,
): C {
    for (element in this) {
        val mapped = transform(element)
        if (mapped != null && predicate(mapped)) {
            target.add(mapped)
        }
    }
    return target
}

/** Single-pass equivalent of `this.mapNotNull(transform).filter(predicate)`. */
fun <T, R> Iterable<T>.filterMapNotNulled(
    transform: (T) -> R?,
    predicate: (R) -> Boolean,
): List<R> = filterMapNotNulledTo(mutableListOf(), transform, predicate)

/** Single-pass equivalent of `this.mapNotNull(transform).map(mapper)`. */
fun <T, R, U, C : MutableCollection<in U>> Iterable<T>.mapMapNotNulledTo(
    target: C,
    transform: (T) -> R?,
    mapper: (R) -> U,
): C {
    for (element in this) {
        transform(element)?.let { target.add(mapper(it)) }
    }
    return target
}

/** Single-pass equivalent of `this.mapNotNull(transform).map(mapper)`. */
fun <T, R, U> Iterable<T>.mapMapNotNulled(transform: (T) -> R?, mapper: (R) -> U): List<U> =
    mapMapNotNulledTo(mutableListOf(), transform, mapper)

/** Single-pass equivalent of `this.filterIsInstance<T>().filter(predicate)`. */
inline fun <reified T, C : MutableCollection<in T>> Iterable<*>.filterIsInstanceAndFilterTo(
    target: C,
    predicate: (T) -> Boolean,
): C {
    for (element in this) {
        if (element is T && predicate(element)) {
            target.add(element)
        }
    }
    return target
}

/** Single-pass equivalent of `this.filterIsInstance<T>().map(transform)`. */
inline fun <reified T, R, C : MutableCollection<in R>> Iterable<*>.filterIsInstanceAndMapTo(
    target: C,
    transform: (T) -> R,
): C {
    for (element in this) {
        if (element is T) {
            target.add(transform(element))
        }
    }
    return target
}

fun <T, R> Iterable<T>.flatMapNotNull(transform: (T) -> Collection<R>?): List<R> {
    val result = ArrayList<R>()
    for (element in this) {
        val newElements = transform(element)
        if (newElements != null) {
            for (newElement in newElements) {
                newElement?.let { result.add(it) }
            }
        }
    }
    return result
}
