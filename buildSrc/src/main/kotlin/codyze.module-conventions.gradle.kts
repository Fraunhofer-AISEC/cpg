import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("cpg.common-conventions")
    id("cpg.frontend-conventions")
    id("cpg.publishing-conventions")
}

val libs = the<LibrariesForLibs>()  // necessary to be able to use the version catalog in buildSrc
dependencies {
    api(project(":codyze-core"))
    api(libs.clikt)
}
