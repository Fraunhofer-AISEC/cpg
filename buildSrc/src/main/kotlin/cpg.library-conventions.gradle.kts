import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("cpg.common-conventions")
    id("cpg.publishing-conventions")
}

val libs = the<LibrariesForLibs>()  // necessary to be able to use the version catalog in buildSrc
dependencies {
    // Unit tests
    testImplementation(kotlin("test"))
}
