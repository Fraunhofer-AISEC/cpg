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
package de.fraunhofer.aisec.cpg.frontends.golang.verified

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.LongByReference
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Calls the verified translation in-process through the native library built by
 * `cpg-verified/native/build.sh`.
 */
object LeanTranslator {

    @Suppress("FunctionName")
    private interface CpgVerifiedLibrary : Library {
        fun cpg_verified_init(): Int

        fun cpg_verified_init_thread()

        fun cpg_verified_translate_bytes(
            input: ByteArray,
            len: Long,
            outLen: LongByReference,
        ): Pointer

        fun cpg_verified_free(buffer: Pointer)
    }

    /** The location of the native library, which can be overridden with `cpg.verified.library`. */
    val libraryFile: File by lazy {
        System.getProperty("cpg.verified.library")?.let(::File)
            ?: listOf("dylib", "so")
                .map { File("../cpg-verified/.lake/build/native/libcpgverified.$it") }
                .firstOrNull { it.exists() }
            ?: File("../cpg-verified/.lake/build/native/libcpgverified.so")
    }

    val isAvailable: Boolean
        get() = libraryFile.exists()

    /**
     * The thread that initialized the Lean runtime; every other thread needs to register itself.
     */
    private var initThread: Thread? = null

    private val threadRegistered = ThreadLocal.withInitial { false }

    private val library: CpgVerifiedLibrary by lazy {
        val library = Native.load(libraryFile.absolutePath, CpgVerifiedLibrary::class.java)
        check(library.cpg_verified_init() == 0) { "Could not initialize the Lean runtime" }
        initThread = Thread.currentThread()
        library
    }

    /** Translates all [requests] and returns one result per request. */
    fun translate(requests: List<Sexp>): List<Sexp> {
        val library = library
        if (Thread.currentThread() != initThread && !threadRegistered.get()) {
            library.cpg_verified_init_thread()
            threadRegistered.set(true)
        }

        val input = ByteArrayOutputStream()
        requests.forEach { it.encodeTo(input) }
        val bytes = input.toByteArray()

        val outLen = LongByReference()
        val output = library.cpg_verified_translate_bytes(bytes, bytes.size.toLong(), outLen)
        try {
            return Sexp.parseAll(output.getByteArray(0, outLen.value.toInt()))
        } finally {
            library.cpg_verified_free(output)
        }
    }
}
