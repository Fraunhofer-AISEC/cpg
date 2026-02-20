plugins { id("cpg.frontend-conventions") }

dependencies { implementation("net.java.dev.jna:jna:5.18.1") }

val publishNativeParser by
    tasks.registering(Exec::class) {
        workingDir = file("src/main/csharp/NativeParser")
        val dotnet =
            listOf(
                    "/opt/homebrew/opt/dotnet@8/bin/dotnet",
                    "/usr/local/bin/dotnet",
                    "/usr/bin/dotnet",
                )
                .map { file(it) }
                .firstOrNull { it.exists() }
                ?.absolutePath ?: "dotnet"
        commandLine(dotnet, "publish", "-c", "Release", "-r", "osx-arm64")
    }

tasks.named("compileKotlin") { dependsOn(publishNativeParser) }

mavenPublishing {
    pom {
        name.set("Code Property Graph - C# Frontend")
        description.set("A C# language frontend for the CPG")
    }
}
