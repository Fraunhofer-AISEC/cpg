import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("cpg.common-conventions")
    id("cpg.publishing-conventions")
    application
}

val libs = the<LibrariesForLibs>()  // necessary to be able to use the version catalog in buildSrc
dependencies {
    api(project(":cpg-core"))

    implementation(libs.bundles.log4j)
    implementation(libs.kotlin.reflect)

    testImplementation(kotlin("test"))
}