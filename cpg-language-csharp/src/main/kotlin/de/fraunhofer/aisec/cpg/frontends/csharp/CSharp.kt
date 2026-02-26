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
package de.fraunhofer.aisec.cpg.frontends.csharp

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.PointerType
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException

interface Csharp : Library {
    open class CsharpObject(p: Pointer? = Pointer.NULL) : PointerType(p) {
        val csharpType: String
            get() {
                return "INSTANCE.GetType(this.pointer)"
            }
    }

    fun parseCsharp(source: String): Pointer

    fun freeString(ptr: Pointer)

    companion object {
        val INSTANCE: Csharp by lazy {
            try {
                val osName = System.getProperty("os.name")
                val os = if (osName.startsWith("Mac")) "osx" else "linux"
                val arch =
                    System.getProperty("os.arch")
                        .replace("aarch64", "arm64")
                        .replace("amd64", "amd64")
                val rid = "$os-$arch"
                val ext = if (osName.startsWith("Mac")) ".dylib" else ".so"

                val dylib =
                    java.io.File(
                        "src/main/csharp/NativeParser/bin/Release/net8.0/$rid/publish/NativeParser$ext"
                    )

                LanguageFrontend.log.info("Loading NativeParser from ${dylib.absolutePath}")

                Native.load(dylib.absolutePath, Csharp::class.java)
            } catch (ex: Exception) {
                throw TranslationException("Error loading C# native library: $ex")
            }
        }
    }
}
