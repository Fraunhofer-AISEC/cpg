plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal() // so that external plugins can be resolved in dependencies section
}

dependencies {
    implementation(libs.kotlin.gradle)
    implementation(libs.kotlin.serialization)
    implementation(libs.dokka.gradle)
    implementation(libs.kover.gradle)
    implementation(libs.spotless.gradle)
    implementation(libs.publish.central)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))  // this is only there to be able to import 'LibrariesForLibs' in the convention plugins to access the version catalog in buildSrc
}
