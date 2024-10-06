package de.fraunhofer.aisec.cpg.rules

import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression

class Util {
    companion object {
        fun isPathLike(arg: Expression): Boolean {
            return arg.type.name.localName.contains(Regex("str|char", RegexOption.IGNORE_CASE)) &&
                    (arg.name.localName.contains(
                        Regex("path|file|dir|directory|url|uri", RegexOption.IGNORE_CASE)
                    ) ||
                            arg.code?.contains(
                                Regex("path|file|dir|directory|url|uri", RegexOption.IGNORE_CASE)
                            ) ?: false ||
                            arg.code?.contains('/') ?: false)
        }

        fun isUserInput(arg: CallExpression): Boolean {
            val userInputRegex = Regex("user|input|param|arg|argument|request|query", RegexOption.IGNORE_CASE)
            return arg.name.localName.contains(userInputRegex) || arg.code?.contains(userInputRegex) ?: false
        }

        fun isValidationFunction(arg: CallExpression): Boolean {
            val validationFunctionRegex = Regex("val|validate|check|san|sanitize|clean", RegexOption.IGNORE_CASE)
            return arg.name.localName.contains(validationFunctionRegex) || arg.code?.contains(validationFunctionRegex) ?: false
        }
    }
}
