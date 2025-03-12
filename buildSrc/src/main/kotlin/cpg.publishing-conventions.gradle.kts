import com.vanniktech.maven.publish.*

plugins {
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
    id("signing")
}

publishing {
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/your-org/your-project")
            // username and password (a personal Github access token) should be specified as
            // `githubPackagesUsername` and `githubPackagesPassword` Gradle properties or alternatively
            // as `ORG_GRADLE_PROJECT_githubPackagesUsername` and `ORG_GRADLE_PROJECT_githubPackagesPassword`
            // environment variables
            credentials(PasswordCredentials::class)
        }
    }
}

// Only include javadoc if the includeJavadoc property is set to true (required for maven central)
val includeJavadoc: String? by project
val javadocJar = if(includeJavadoc.toBoolean()) {
    JavadocJar.Dokka("dokkaHtml")
} else {
    JavadocJar.Empty()
}

// Publication settings for maven central
mavenPublishing {
    configure(KotlinJvm(
        javadocJar = javadocJar,
        sourcesJar = true,
    ))
    coordinates(project.group.toString(), project.name, version.toString())

    pom {
        url.set("https://github.com/Fraunhofer-AISEC/cpg")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                organization.set("Fraunhofer AISEC")
                organizationUrl.set("https://www.aisec.fraunhofer.de")
            }
        }
        scm {
            connection.set("scm:git:git://github.com:Fraunhofer-AISEC/cpg.git")
            developerConnection.set("scm:git:ssh://github.com:Fraunhofer-AISEC/cpg.git")
            url.set("https://github.com/Fraunhofer-AISEC/cpg")
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
}


signing {
    setRequired({
        gradle.taskGraph.hasTask("publishAllPublicationsToMavenCentralRepository")
    })

    val signingInMemoryKey: String? by project
    val signingInMemoryKeyPassword: String? by project
    useInMemoryPgpKeys(signingInMemoryKey, signingInMemoryKeyPassword)

    sign(publishing.publications["maven"])
}
