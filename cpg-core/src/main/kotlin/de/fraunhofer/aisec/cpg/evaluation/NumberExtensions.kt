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
package de.fraunhofer.aisec.cpg.evaluation

internal operator fun Number.plus(other: Number): Number =
    when (this) {
        is Int -> this + other
        is Byte -> this + other
        is Long -> this + other
        is Short -> this + other
        is Float -> this + other
        is Double -> this + other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Int.plus(other: Number): Number =
    when (other) {
        is Int -> this + other
        is Byte -> this + other
        is Long -> this + other
        is Short -> this + other
        is Float -> this + other
        is Double -> this + other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Byte.plus(other: Number): Number =
    when (other) {
        is Int -> this + other
        is Byte -> this + other
        is Long -> this + other
        is Short -> this + other
        is Float -> this + other
        is Double -> this + other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Long.plus(other: Number): Number =
    when (other) {
        is Int -> this + other
        is Byte -> this + other
        is Long -> this + other
        is Short -> this + other
        is Float -> this + other
        is Double -> this + other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Short.plus(other: Number): Number =
    when (other) {
        is Int -> this + other
        is Byte -> this + other
        is Long -> this + other
        is Short -> this + other
        is Float -> this + other
        is Double -> this + other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Float.plus(other: Number): Number =
    when (other) {
        is Int -> this + other
        is Byte -> this + other
        is Long -> this + other
        is Short -> this + other
        is Float -> this + other
        is Double -> this + other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Double.plus(other: Number): Number =
    when (other) {
        is Int -> this + other
        is Byte -> this + other
        is Long -> this + other
        is Short -> this + other
        is Float -> this + other
        is Double -> this + other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Number.minus(other: Number): Number =
    when (this) {
        is Int -> this + -other
        is Byte -> this + -other
        is Long -> this + -other
        is Short -> this + -other
        is Float -> this + -other
        is Double -> this + -other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Number.times(other: Number): Number =
    when (this) {
        is Int -> this * other
        is Byte -> this * other
        is Long -> this * other
        is Short -> this * other
        is Float -> this * other
        is Double -> this * other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Int.times(other: Number): Number =
    when (other) {
        is Int -> this * other
        is Byte -> this * other
        is Long -> this * other
        is Short -> this * other
        is Float -> this * other
        is Double -> this * other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Byte.times(other: Number): Number =
    when (other) {
        is Int -> this * other
        is Byte -> this * other
        is Long -> this * other
        is Short -> this * other
        is Float -> this * other
        is Double -> this * other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Long.times(other: Number): Number =
    when (other) {
        is Int -> this * other
        is Byte -> this * other
        is Long -> this * other
        is Short -> this * other
        is Float -> this * other
        is Double -> this * other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Short.times(other: Number): Number =
    when (other) {
        is Int -> this * other
        is Byte -> this * other
        is Long -> this * other
        is Short -> this * other
        is Float -> this * other
        is Double -> this * other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Float.times(other: Number): Number =
    when (other) {
        is Int -> this * other
        is Byte -> this * other
        is Long -> this * other
        is Short -> this * other
        is Float -> this * other
        is Double -> this * other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Double.times(other: Number): Number =
    when (other) {
        is Int -> this * other
        is Byte -> this * other
        is Long -> this * other
        is Short -> this * other
        is Float -> this * other
        is Double -> this * other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Number.div(other: Number): Number =
    when (this) {
        is Int -> this / other
        is Byte -> this / other
        is Long -> this / other
        is Short -> this / other
        is Float -> this / other
        is Double -> this / other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Int.div(other: Number): Number =
    when (other) {
        is Int -> this / other
        is Byte -> this / other
        is Long -> this / other
        is Short -> this / other
        is Float -> this / other
        is Double -> this / other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Byte.div(other: Number): Number =
    when (other) {
        is Int -> this / other
        is Byte -> this / other
        is Long -> this / other
        is Short -> this / other
        is Float -> this / other
        is Double -> this / other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Long.div(other: Number): Number =
    when (other) {
        is Int -> this / other
        is Byte -> this / other
        is Long -> this / other
        is Short -> this / other
        is Float -> this / other
        is Double -> this / other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Short.div(other: Number): Number =
    when (other) {
        is Int -> this / other
        is Byte -> this / other
        is Long -> this / other
        is Short -> this / other
        is Float -> this / other
        is Double -> this / other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Float.div(other: Number): Number =
    when (other) {
        is Int -> this / other
        is Byte -> this / other
        is Long -> this / other
        is Short -> this / other
        is Float -> this / other
        is Double -> this / other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Double.div(other: Number): Number =
    when (other) {
        is Int -> this / other
        is Byte -> this / other
        is Long -> this / other
        is Short -> this / other
        is Float -> this / other
        is Double -> this / other
        else -> throw UnsupportedOperationException()
    }

internal operator fun Number.unaryMinus(): Number =
    when (this) {
        is Int -> -this
        is Byte -> -this
        is Long -> -this
        is Short -> -this
        is Float -> -this
        is Double -> -this
        else -> throw UnsupportedOperationException()
    }

internal operator fun Number.inc(): Number =
    when (this) {
        is Int -> this + 1
        is Byte -> this + 1
        is Long -> this + 1
        is Short -> this + 1
        is Float -> this + 1
        is Double -> this + 1
        else -> throw UnsupportedOperationException()
    }

internal operator fun Number.dec(): Number =
    when (this) {
        is Int -> this - 1
        is Byte -> this - 1
        is Long -> this - 1
        is Short -> this - 1
        is Float -> this - 1
        is Double -> this - 1
        else -> throw UnsupportedOperationException()
    }
