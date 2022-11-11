plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal() // so that external plugins can be resolved in dependencies section
}

dependencies {
    implementation(libs.kotlin.gradle)
    implementation(libs.dokka.gradle)
    implementation(libs.sonarqube.gradle)
    implementation(libs.spotless.gradle)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))  // this is only there to be able to import 'LibrariesForLibs' in the convention plugins to access the version catalog in buildSrc
}
