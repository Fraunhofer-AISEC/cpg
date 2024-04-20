package de.fraunhofer.aisec.cpg.analysis

import de.fraunhofer.aisec.cpg.analysis.Reporter.Companion.defaultTimeFormatter
import io.github.detekt.sarif4k.*
import java.nio.file.Path
import java.time.LocalDateTime

class SarifReporter : Reporter {
    override fun report(rule: Rule, minify: Boolean): String {
        if (rule.queryResult == null) {
            throw IllegalStateException(
                "Query result of rule ${rule.id}: ${rule.name} is null. Please run the rule before generating a report."
            )
        }
        // TODO: consider validation of rule fields
        val sarifObj = SarifSchema210(
            schema = "https://docs.oasis-open.org/sarif/sarif/v2.1.0/errata01/os/schemas/sarif-schema-2.1.0.json",
            version = Version.The210,
            runs = listOf(
                Run(
                    tool = Tool(
                        driver = ToolComponent(
                            name = "AISEC cpg-console", // TODO: mby dont hardcode, at least not here
                            informationURI = "https://github.com/Fraunhofer-AISEC/cpg/",
                            rules = listOf(
                                ReportingDescriptor(
                                    id = rule.id,
                                    name = rule.name,
                                    shortDescription = MultiformatMessageString(
                                        text = rule.shortDescription,
                                        markdown = rule.mdShortDescription
                                    ),
                                    defaultConfiguration = ReportingConfiguration(
                                        level = rule.level
                                    )
                                    // TODO: consider default message
                                )
                            )
                        )
                    ),
                    // TODO: automationDetails, invocation?
                    results = listOf(
                        Result(
                            ruleID = rule.id,
                            ruleIndex = 0, // currently one report per run per rule
                            message = Message(
                                text = rule.message,
                                markdown = rule.mdMessage,
                                arguments = rule.messageArguments
                            ),
                            locations = listOf(
                                Location(
                                    physicalLocation = PhysicalLocation(
                                        // TODO: QueryTree does not have a file location or any other Node info
                                        //  de.fraunhofer.aisec.cpg.sarif.(PhysicalLocation|Region) aren't accessible
                                        artifactLocation = null,
                                        region = null
                                    )
                                )
                                // TODO: codeFlows
                            )
                        )
                    )
                )
            )
        )
        return if (minify) SarifSerializer.toMinifiedJson(sarifObj) else SarifSerializer.toJson(sarifObj)
    }

    override fun toFile(report: String, path: Path) {
        path.toFile().writeText(report)
    }

    override fun getDefaultPath(): Path {
        // TODO: duplicates technically possible if multiple reports are generated in the same second
        val currentTime = LocalDateTime.now()
        // eg reports/sarif/report-2021-09-29-15-00-00.sarif
        return Path.of("reports", "report-${currentTime.format(defaultTimeFormatter)}.sarif")
    }

}