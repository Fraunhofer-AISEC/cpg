/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationResult.Companion.DEFAULT_APPLICATION_NAME
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import java.io.File
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@DoNotPersist
class BenchmarkResults(val entries: List<List<Any>>) {

    val json: String
        get() {
            val mapper = jacksonObjectMapper()
            mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false)

            return mapper.writeValueAsString(entries.associate { it[0] to it[1] })
        }

    /** Pretty-prints benchmark results for easy copying to GitHub issues. */
    fun print() {
        println("# Benchmark run ${UUID.randomUUID()}")
        printMarkdown(entries, listOf("Metric", "Value"))
    }
}

/** Interface definition to hold different statistics about the translation process. */
interface StatisticsHolder {
    val translatedFiles: List<String>
    val benchmarks: Set<MeasurementHolder>
    val config: TranslationConfiguration

    fun addBenchmark(b: MeasurementHolder)

    val benchmarkResults: BenchmarkResults
        get() {
            val results =
                mutableListOf(
                    listOf("Translation config", config),
                    listOf("Number of files translated", translatedFiles.size),
                    listOf(
                        "Translated file(s)",
                        translatedFiles.map {
                            relativeOrAbsolute(
                                Path.of(it),
                                config.topLevels[DEFAULT_APPLICATION_NAME],
                            )
                        },
                    ),
                )

            benchmarks.forEach {
                it.measurements.forEach { measurement ->
                    results += listOf(measurement.key, measurement.value)
                }
            }

            return BenchmarkResults(results)
        }
}

/**
 * Prints a table of values and headers in Markdown format. Table columns are automatically adjusted
 * to the longest column.
 */
fun printMarkdown(table: List<List<Any>>, headers: List<String>) {
    val lengths = IntArray(headers.size)

    // first, we need to calculate the longest column per line
    for (row in table) {
        for (i in row.indices) {
            val value = row[i].toString()
            if (value.length > lengths[i]) {
                lengths[i] = value.length
            }
        }
    }

    // table header
    val dash = lengths.joinToString(" | ", "| ", " |") { ("-".repeat(it)) }
    var i = 0
    val header = headers.joinToString(" | ", "| ", " |") { it.padEnd(lengths[i++]) }

    println()
    println(header)
    println(dash)

    for (row in table) {
        var rowIndex = 0
        // TODO: Add pretty printing for objects (e.g. List, Map)
        val line = row.joinToString(" | ", "| ", " |") { it.toString().padEnd(lengths[rowIndex++]) }
        println(line)
    }

    println()
}

/**
 * This function will shorten / relativize the [path], if it is relative to [topLevel]. Otherwise,
 * the full path will be returned.
 */
fun relativeOrAbsolute(path: Path, topLevel: File?): Path {
    return if (topLevel != null) {
        try {
            topLevel.toPath().toAbsolutePath().relativize(path)
        } catch (ex: IllegalArgumentException) {
            path
        }
    } else {
        path
    }
}

/** Measures the time between creating the object to calling its stop() method. */
open class Benchmark
@JvmOverloads
constructor(
    c: Class<*>,
    message: String,
    debug: Boolean = false,
    holder: StatisticsHolder? = null,
) : MeasurementHolder(c, message, debug, holder) {

    private val start: Instant

    /** Stops this benchmark and adds its measurement to the its [StatisticsHolder]. */
    fun stop() {
        addMeasurement()
    }

    /** Stops the time and computes the difference between */
    override fun addMeasurement(measurementKey: String?, measurementValue: String?): Any? {
        val duration = Duration.between(start, Instant.now()).toMillis()
        measurements["${caller}: $message"] = "$duration ms"

        logDebugMsg("$caller: $message done in $duration ms")

        // update our holder, if we have any
        holder?.addBenchmark(this)

        return duration
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(MeasurementHolder::class.java)
    }

    init {
        measurements["${caller}: $message"] = "No value available yet."
        start = Instant.now()
    }
}

@DoNotPersist
/** Represents some kind of measurements, e.g., on the performance or problems. */
open class MeasurementHolder
@JvmOverloads
constructor(
    /** The class which called this benchmark. */
    c: Class<*>,
    /** A string indicating what this benchmark should measure. */
    val message: String,
    /** Changes the level used for log output. */
    protected var debug: Boolean = false,
    /** The class which should be updated if the value measured by this benchmark changed. */
    protected var holder: StatisticsHolder? = null,
) {

    val caller: String

    /** Stores the values measured by the benchmark */
    var measurements: MutableMap<String, String> = mutableMapOf()

    /**
     * Returns a list of strings which summarize the insights gained by the benchmark. The first
     * item of the list is the key, the second one is the value.
     */
    val benchmarkedValues: List<String>
        get() {
            return measurements.flatMap { listOf(it.key, it.value) }
        }

    fun logDebugMsg(msg: String) {
        if (debug) {
            log.debug(msg)
        } else {
            log.info(msg)
        }
    }

    /** Adds a measurement for the respective benchmark and saves it to the map. */
    @JvmOverloads
    open fun addMeasurement(
        measurementKey: String? = null,
        measurementValue: String? = null,
    ): Any? {
        if (measurementKey == null || measurementValue == null) return null

        measurements["Measurement: $measurementKey"] = measurementValue
        logDebugMsg("$caller $measurementKey: result is $measurementValue")

        // update our holder, if we have any
        holder?.addBenchmark(this)
        return null
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(MeasurementHolder::class.java)
    }

    init {
        caller = c.simpleName
        logDebugMsg("$caller: $message")
    }
}
