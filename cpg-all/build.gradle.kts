plugins {
    id("cpg.library-conventions")
    id("cpg.frontend-dependency-conventions")
    id("jacoco-report-aggregation")
    id("org.sonarqube")
}

publishing {
    publications {
        named<MavenPublication>("cpg-all") {
            pom {
                artifactId = "cpg" // for legacy reasons (this will be renamed to cpg-core at some point)
                name.set("Code Property Graph")
                description.set("A simple library to extract a code property graph out of source code. It has support for multiple passes that can extend the analysis after the graph is constructed.")
                url.set("https://github.com/Fraunhofer-AISEC/cpg")
            }
        }
    }
}


dependencies {
    // this exposes all of our (published) modules as dependency
    api(projects.cpgCore)
    api(projects.cpgAnalysis)

    jacocoAggregation(projects.cpgCore)
    jacocoAggregation(projects.cpgAnalysis)
}

tasks.sonar {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}

sonarqube {
    properties {
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml")
    }
}
