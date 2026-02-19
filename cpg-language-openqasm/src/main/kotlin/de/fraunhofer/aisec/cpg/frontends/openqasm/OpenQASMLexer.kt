/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.frontends.openqasm

import de.fraunhofer.aisec.cpg.frontends.openqasm.tokens.*
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.net.URI

/*
Lexer based on the "lexer grammar qasm3Lexer" published at https://openqasm.com/grammar/index.html
 */
class OpenQASMLexer(private val file: File) {
    var tokens = mutableListOf<Token>()
    private var idx = 0
    private val bytes =
        file
            .inputStream()
            .readBytes() // TODO not efficient as file is read twice (here and when traversing the
    // array)
    private var currentLine = 1 // first line is number 1 (not 0)
    private var currentColumn = 0
    private var currentChar: Char = '_'
    private var nextChar: Char? = bytes.getOrNull(idx)?.toInt()?.toChar()

    fun run() {
        while (nextChar != null) {
            eat()
            when (currentChar) {
                '\n',
                '\r',
                ' ' -> {
                    /* skip */
                }
                '[',
                ']',
                '{',
                '}',
                '(',
                ')',
                ':',
                ';',
                '.',
                ',',
                '=', // =  and ==
                '-', // - and -> and -=
                '+', // + and ++ and +=
                '*', // * and  ** and *= and **=
                '%', // % and %=
                '|', // | and || and |=
                '&', // & and && and &=
                '^', // ^ and ^=
                '@',
                '~', // ~ and ~=
                '!', // ! and !=
                '<', // <<= and < and <= and <<
                '>' // >>= and > and >= and >>
                -> handleSymbol()
                '/' -> {
                    nextChar?.let {
                        when (it) {
                            '/' -> handleLineComment()
                            '*' -> handleBlockComment()
                            else -> handleSymbol() // / and /=
                        }
                    }
                }
                '0',
                '1',
                '2',
                '3',
                '4',
                '5',
                '6',
                '7',
                '8',
                '9' -> handleNumber()
                '$' -> handleHardwareQubit()
                '"',
                '\'' -> handleStringLiteral()
                else -> {
                    handleEverythingElse()
                }
            }
        }
    }

    private fun handleNumber() {
        if (currentChar == '0' && nextChar != null) {
            when (nextChar) {
                'b',
                'B' -> handleBinaryInteger()
                'o' -> handleOctalInteger()
                'x',
                'X' -> handleHexInteger()
                else -> handleDecimalOrFloat()
            }
        } else {
            handleDecimalOrFloat()
        }
    }

    private fun handleDecimalOrFloat() {
        /*
        DecimalIntegerLiteral: ([0-9] '_'?)* [0-9];
        fragment FloatLiteralExponent: [eE] (PLUS | MINUS)? DecimalIntegerLiteral;
        FloatLiteral:
            // 1_123e-3, 123e+4 or 123E5 (needs the exponent or it's just an integer)
            DecimalIntegerLiteral FloatLiteralExponent
            // .1234_5678 or .1e3 (no digits before the dot)
            | DOT DecimalIntegerLiteral FloatLiteralExponent?
            // 123.456, 123. or 145.32e+1_00
            | DecimalIntegerLiteral DOT DecimalIntegerLiteral? FloatLiteralExponent?;
         */
        val startColumn = currentColumn
        var payload = ""
        payload += currentChar
        while (
            nextChar != null &&
                (nextChar!!.isDigit() ||
                    nextChar == '_' ||
                    nextChar == '+' ||
                    nextChar == '-' ||
                    nextChar == '.' ||
                    nextChar == 'e' ||
                    nextChar == 'E')
        ) {
            eat()
            payload += currentChar
        }
        if (payload.matches(Regex("([0-9]_?)*[0-9]"))) {
            tokens +=
                DecimalIntegerLiteralToken(
                    makeLocHelper(currentLine, currentLine, startColumn, currentColumn),
                    payload.toInt(),
                )
        } else {
            TODO()
        }
    }

    private fun handleBinaryInteger() {
        TODO()
    }

    private fun handleHexInteger() {
        TODO()
    }

    private fun handleOctalInteger() {
        TODO()
    }

    private fun handleHardwareQubit() {
        TODO()
    }

    private fun handleStringLiteral() {
        // String or Bitstring
        val delimiter = currentChar
        eat()
        val startLine = currentLine
        val startColumn = currentColumn
        var payload = ""
        while (nextChar != null && nextChar != delimiter) {
            payload += currentChar
            eat()
        }
        payload += currentChar
        eat() // delimiter
        // TODO bitstring literal
        tokens +=
            StringLiteralToken(
                makeLocHelper(startLine, currentLine, startColumn, currentColumn),
                payload,
            )
    }

    private fun handleEverythingElse() {
        /*
        The catchall function. It handles:
        - language keywords
        - types
        - builtin identifiers and operations
        - identifiers
         */
        if (!isFirstIdCharacter(currentChar)) {
            TODO() // warn
        }
        val startLine = currentLine
        val startColumn = currentColumn
        var payload = ""
        payload += currentChar
        while (nextChar != null && isGeneralIdCharacter(nextChar!!)) {
            eat()
            payload += currentChar
        }
        val endColumn = currentColumn
        tokens +=
            when (payload) {
                "OPENQASM" -> {
                    /*
                    OPENQASM: 'OPENQASM' -> pushMode(VERSION_IDENTIFIER);
                    mode VERSION_IDENTIFIER;
                        VERSION_IDENTIFIER_WHITESPACE: [ \t\r\n]+ -> skip;
                         VersionSpecifier: [0-9]+ ('.' [0-9]+)? -> popMode;
                     */
                    eat() // the 'M' in "OPENQASM" // TODO
                    skipWhitespacesTabsNewlines()
                    var versionString = ""
                    versionString += currentChar
                    while (nextChar != null && nextChar != ';') {
                        eat()
                        versionString += currentChar
                    }

                    // TODO check format

                    OpenQASMToken(
                        makeLocHelper(startLine, startLine, startColumn, endColumn),
                        versionString,
                    )
                }
                "include" ->
                    IncludeToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "defcalgrammar" ->
                    DefcalgrammarToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "def" -> DefToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "defcal" -> DefcalToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "gate" -> GateToken((makeLocHelper(startLine, startLine, startColumn, endColumn)))
                "extern" ->
                    ExternToken((makeLocHelper(startLine, startLine, startColumn, endColumn)))
                "box" -> BoxToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "let" -> LetToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "break" -> BreakToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "continue" ->
                    ContinueToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "if" -> IfToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "else" -> ElseToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "end" -> EndToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "return" -> ReturnToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "for" -> ForToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "while" -> WhileToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "in" -> InToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "#",
                "#pragma" -> TODO()
                // TODO AnnotationKeyword
                "input" -> InputToken((makeLocHelper(startLine, startLine, startColumn, endColumn)))
                "output" -> OutputToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "const" -> ConstToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "mutable" ->
                    MutableToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "qreg" -> QregToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "qubit" -> QubitToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "creg" -> CregToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "bool" -> BoolToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "bit" -> BitToken((makeLocHelper(startLine, startLine, startColumn, endColumn)))
                "int" -> IntToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "uint" -> UIntToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "float" -> FloatToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "angle" -> AngleToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "complex" ->
                    ComplexToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "array" -> ArrayToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "duration" ->
                    DurationToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "stretch" ->
                    StretchToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "gphase" -> GphaseToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "inv" -> InvToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "pow" -> PowToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "ctrl" -> CtrlToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "negctrl" ->
                    NegctrlToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "#dim" -> DimToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "durationof" ->
                    DurationofToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "delay" -> DelayToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "reset" -> ResetToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "measure" ->
                    MeasureToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "barrier" ->
                    BarrierToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "true" -> TrueToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "false" -> FalseToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                "im" -> ImagToken(makeLocHelper(startLine, startLine, startColumn, endColumn))
                else ->
                    IdentifierToken(
                        makeLocHelper(startLine, startLine, startColumn, endColumn),
                        payload,
                    )
            }
    }

    // Eat one character (if possible) and advance column and line counters.
    private fun eat() {
        nextChar?.let {
            currentChar = nextChar as Char
            when (currentChar) {
                '\n' -> {
                    currentLine++
                    currentColumn = 0
                }
                else -> currentColumn++
            }
            idx++
            nextChar = bytes.getOrNull(idx)?.toInt()?.toChar()
        }
    }

    private fun skipWhitespacesTabsNewlines() {
        while (
            currentChar == ' ' || currentChar == '\t' || currentChar == '\r' || currentChar == '\n'
        ) eat()
    }

    private fun handleLineComment() {
        // LineComment : '//' ~[\r\n]* -> skip;
        val startLine = currentLine
        val startColumn = currentColumn

        eat() // currentChar is now the second '/' in "// some comment"
        eat() // currentChar is now after the second '/'

        var done = false
        var commentString = ""

        // start of the comment string
        while (!done) {
            if (nextChar == '\n' || nextChar == '\r') {
                done = true
            } else {
                commentString += currentChar
                eat() // advance the cursor
            }
        }
        tokens +=
            LineCommentToken(
                makeLocHelper(startLine, startLine, startColumn, currentColumn),
                commentString,
            )
    }

    private fun handleBlockComment() {
        // BlockComment : '/*' .*? '*/' -> skip;
        val startLine = currentLine
        val startColumn = currentColumn

        eat() // currentChar is now the '/' in "/* some comment */"
        eat() // currentChar is now at the '*'

        var commentString = ""

        var done = false

        // start of the comment string
        while (!done) {
            // TODO handle unexpected end of file
            if (currentChar == '*' && (nextChar != null && nextChar == '/')) {
                done = true
            } else {
                commentString += currentChar
            }
            eat() // advance the cursor
        }
        val endColumn = currentColumn
        val endLine = currentLine
        tokens +=
            BlockCommentToken(
                makeLocHelper(startLine, endLine, startColumn, endColumn),
                commentString,
            )
    }

    private fun handleSymbol() {
        tokens +=
            when (currentChar) {
                '[' ->
                    LBracketToken(
                        makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                    )
                ']' ->
                    RBracketToken(
                        makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                    )
                '{' ->
                    LBraceToken(
                        makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                    )
                '}' ->
                    RBraceToken(
                        makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                    )
                '(' ->
                    LParenToken(
                        makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                    )
                ')' ->
                    RParenToken(
                        makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                    )
                ':' ->
                    ColonToken(
                        makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                    )
                ';' ->
                    SemicolonToken(
                        makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                    )
                '.' -> // TODO check Float
                DotToken(makeLocHelper(currentLine, currentLine, currentColumn, currentColumn))
                ',' ->
                    CommaToken(
                        makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                    )
                '=' -> // = and ==
                {
                    if (nextChar != null && nextChar == '=') {
                        eat()
                        EqualityOperatorEqualToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else {
                        EqualsToken(
                            makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                        )
                    }
                }
                '-' -> // - and -> and -=
                {
                    if (nextChar != null && nextChar == '=') {
                        eat()
                        CompoundAssignmentOperatorMinusToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else if (nextChar != null && nextChar == '>') {
                        eat()
                        ArrowToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else {
                        MinusToken(
                            makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                        )
                    }
                }
                '+' -> // + and ++ and +=
                {
                    if (nextChar != null && nextChar == '=') {
                        eat()
                        CompoundAssignmentOperatorPlusToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else if (nextChar != null && nextChar == '+') {
                        eat()
                        DoublePlusToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else {
                        PlusToken(
                            makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                        )
                    }
                }
                '*' -> // * and  ** and *= and **=
                {
                    if (nextChar != null && nextChar == '*') {
                        // ** or **=
                        eat()
                        if (nextChar != null && nextChar == '=') { // **=
                            eat()
                            CompoundAssignmentOperatorDoubleAsteriskToken(
                                makeLocHelper(
                                    currentLine,
                                    currentLine,
                                    currentColumn - 2,
                                    currentColumn,
                                )
                            )
                        } else { // **
                            DoubleAsteriskToken(
                                makeLocHelper(
                                    currentLine,
                                    currentLine,
                                    currentColumn - 1,
                                    currentColumn,
                                )
                            )
                        }
                    } else if (nextChar != null && nextChar == '=') {
                        eat()
                        CompoundAssignmentOperatorAsteriskToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else {
                        AsteriskToken(
                            makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                        )
                    }
                }
                '/' -> {
                    SlashToken(
                        makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                    )
                }
                '%' -> // % and %=
                {
                    if (nextChar != null && nextChar == '=') {
                        eat()
                        CompoundAssignmentOperatorPercentToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else {
                        PercentToken(
                            makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                        )
                    }
                }
                '|' -> // | and || and |=
                {
                    if (nextChar != null && nextChar == '=') {
                        eat()
                        CompoundAssignmentOperatorPipeToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else if (nextChar != null && nextChar == '|') {
                        eat()
                        DoublePipeToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else {
                        PipeToken(
                            makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                        )
                    }
                }
                '&' -> // & and && and &=
                {
                    if (nextChar != null && nextChar == '=') {
                        eat()
                        CompoundAssignmentOperatorAmpersandToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else if (nextChar != null && nextChar == '&') {
                        eat()
                        DoubleAmpersandToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else {
                        AmpersandToken(
                            makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                        )
                    }
                }
                '^' -> // ^ and ^=
                {
                    if (nextChar != null && nextChar == '=') {
                        eat()
                        CompoundAssignmentOperatorCaretToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else {
                        CaretToken(
                            makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                        )
                    }
                }
                '@' -> {
                    AsteriskToken(
                        makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                    )
                }
                '~' -> // ~ and ~=
                {
                    if (nextChar != null && nextChar == '=') {
                        eat()
                        CompoundAssignmentOperatorTildeToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else {
                        TildeToken(
                            makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                        )
                    }
                }
                '!' -> // ! and !=
                {
                    if (nextChar != null && nextChar == '=') {
                        eat()
                        EqualityOperatorNotEqualToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else {
                        ExclamationPointToken(
                            makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                        )
                    }
                }
                '<' -> // <<= and < and <= and <<
                {
                    if (nextChar != null && nextChar == '<') {
                        // << or <<=
                        eat()
                        if (nextChar != null && nextChar == '=') { // <<=
                            eat()
                            CompoundAssignmentOperatorLShiftToken(
                                makeLocHelper(
                                    currentLine,
                                    currentLine,
                                    currentColumn - 2,
                                    currentColumn,
                                )
                            )
                        } else { // <<
                            BitshiftOperatorLeftToken(
                                makeLocHelper(
                                    currentLine,
                                    currentLine,
                                    currentColumn - 1,
                                    currentColumn,
                                )
                            )
                        }
                    } else if (nextChar != null && nextChar == '=') { // <=
                        eat()
                        ComparisonOperatorLessEqToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else { // <
                        ComparisonOperatorLessToken(
                            makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                        )
                    }
                }
                '>' -> // >>= and > and >= and >>
                {
                    if (nextChar != null && nextChar == '>') {
                        // >> or >>=
                        eat()
                        if (nextChar != null && nextChar == '=') { // >>=
                            eat()
                            CompoundAssignmentOperatorRShiftToken(
                                makeLocHelper(
                                    currentLine,
                                    currentLine,
                                    currentColumn - 2,
                                    currentColumn,
                                )
                            )
                        } else { // <<
                            BitshiftOperatorRightToken(
                                makeLocHelper(
                                    currentLine,
                                    currentLine,
                                    currentColumn - 1,
                                    currentColumn,
                                )
                            )
                        }
                    } else if (nextChar != null && nextChar == '=') { // >=
                        eat()
                        ComparisonOperatorGreaterEqToken(
                            makeLocHelper(
                                currentLine,
                                currentLine,
                                currentColumn - 1,
                                currentColumn,
                            )
                        )
                    } else { // >
                        ComparisonOperatorGreaterToken(
                            makeLocHelper(currentLine, currentLine, currentColumn, currentColumn)
                        )
                    }
                }
                else -> TODO()
            }
    }

    private fun isValidUnicode(c: Char): Boolean {
        return false // TODO implement me
    }

    private fun isFirstIdCharacter(c: Char): Boolean {
        // fragment FirstIdCharacter: '_' | ValidUnicode | Letter;
        return c == '_' || isValidUnicode(c) || c.isLetter()
    }

    private fun isGeneralIdCharacter(c: Char): Boolean {
        // fragment GeneralIdCharacter: FirstIdCharacter | [0-9];
        return isFirstIdCharacter(c) || c.isDigit()
    }

    private fun makeLocHelper(
        startLine: Int,
        endLine: Int,
        startColumn: Int,
        endColumn: Int,
    ): PhysicalLocation {
        return PhysicalLocation(
            URI("file://" + file.absolutePath),
            Region(startLine, startColumn, endLine, endColumn),
        )
    }
}
