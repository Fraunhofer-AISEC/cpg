import de.undercouch.gradle.tasks.download.Download

plugins {
    id("cpg.frontend-conventions")
    alias(libs.plugins.download)
}

dependencies { implementation(libs.jna) }

tasks {
    val downloadLibCSharpAST by
        registering(Download::class) {
            val version = "v0.0.11"

            src(
                listOf(
                    "https://github.com/Fraunhofer-AISEC/libast/releases/download/${version}/libcsharpast-arm64.dylib",
                    "https://github.com/Fraunhofer-AISEC/libast/releases/download/${version}/libcsharpast-amd64.dylib",
                    "https://github.com/Fraunhofer-AISEC/libast/releases/download/${version}/libcsharpast-arm64.so",
                    "https://github.com/Fraunhofer-AISEC/libast/releases/download/${version}/libcsharpast-amd64.so",
                    "https://github.com/Fraunhofer-AISEC/libast/releases/download/${version}/libcsharpast-amd64.dll",
                )
            )
            dest(projectDir.resolve("build/downloads/csharpast"))
            onlyIfModified(true)
        }

    // JNA finds the library by its name in the resource folder of the current platform
    val prepareCSharpAstResources by
        registering(Copy::class) {
            dependsOn(downloadLibCSharpAST)

            into(projectDir.resolve("src/main/resources"))

            from("build/downloads/csharpast/libcsharpast-amd64.so") {
                into("linux-x86-64")
                rename { "libcsharpast.so" }
            }

            from("build/downloads/csharpast/libcsharpast-arm64.so") {
                into("linux-aarch64")
                rename { "libcsharpast.so" }
            }

            from("build/downloads/csharpast/libcsharpast-amd64.dylib") {
                into("darwin-x86-64")
                rename { "libcsharpast.dylib" }
            }

            from("build/downloads/csharpast/libcsharpast-arm64.dylib") {
                into("darwin-aarch64")
                rename { "libcsharpast.dylib" }
            }

            from("build/downloads/csharpast/libcsharpast-amd64.dll") {
                into("win32-x86-64")
                rename { "csharpast.dll" }
            }
        }

    processResources { dependsOn(prepareCSharpAstResources) }

    sourcesJar { dependsOn(prepareCSharpAstResources) }
}

mavenPublishing {
    pom {
        name.set("Code Property Graph - C# Frontend")
        description.set("A C# language frontend for the CPG")
    }
}
