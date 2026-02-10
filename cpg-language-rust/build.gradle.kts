plugins { id("cpg.frontend-conventions") }

mavenPublishing {
    pom {
        name.set("Code Property Graph - Rust Frontend")
        description.set("A Rust language frontend for the CPG")
    }
}

dependencies {
    implementation(libs.treesitter)
    implementation(libs.treesitter.rust)
    testImplementation(project(":cpg-analysis"))
}
