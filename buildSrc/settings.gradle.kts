rootProject.name = "cpg"

// this is needed to access the version catalog in buildSrc
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}