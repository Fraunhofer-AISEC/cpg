import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("cpg.common-conventions")
    id("cpg.publishing-conventions")
}

val libs = the<LibrariesForLibs>()  // necessary to be able to use the version catalog in buildSrc
dependencies {
    api(project(":cpg-core"))

    testImplementation(testFixtures(project(":cpg-core")))

    testImplementation(kotlin("test"))
}