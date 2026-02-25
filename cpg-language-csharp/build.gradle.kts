plugins { id("cpg.frontend-conventions") }

dependencies { implementation("net.java.dev.jna:jna:5.18.1") }

val publishNativeParser by
    tasks.registering(Exec::class) {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        val rid =
            when {
                os.contains("mac") && arch.contains("aarch64") -> "osx-arm64"
                os.contains("linux") && arch.contains("aarch64") -> "linux-arm64"
                else -> error("Unsupported OS/arch: $os / $arch")
            }
        workingDir = file("src/main/csharp/NativeParser")
        commandLine("dotnet", "publish", "-c", "Release", "-r", rid)
    }

tasks.named("compileKotlin") { dependsOn(publishNativeParser) }

mavenPublishing {
    pom {
        name.set("Code Property Graph - C# Frontend")
        description.set("A C# language frontend for the CPG")
    }
}
