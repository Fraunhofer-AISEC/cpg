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

import de.fraunhofer.aisec.cpg.frontends.openqasm.astnodes.*
import de.fraunhofer.aisec.cpg.frontends.openqasm.tokens.*
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import kotlin.reflect.KClass

/*
Parser based on the "lexer grammar qasm3Lexer" published at https://openqasm.com/grammar/index.html
 */
class OpenQASMParser(private val tokens: List<Token>) {
    private var idx = 0

    fun parse(): ProgramNode {
        // program: version? statement* EOF;

        skipComments()
        var versionNode: VersionNode? = null
        if (tokens[idx] is OpenQASMToken) {
            versionNode = handleVersionNode()
        }
        val stmts = mutableListOf<StatementNode>()
        while (idx < tokens.size) {
            skipComments()
            stmts += handleStatement()
        }
        return ProgramNode(
            locationAdderHelper(tokens[0].location, tokens.last().location),
            versionNode,
            stmts,
        )
    }

    private fun handleStatement(): StatementNode {
        /*
        Note: does not advance `idx` by itself (`idx` is advanced in the handleXXX functions)

        statement:
            pragma
            // All the actual statements of the language.
            | annotation* (
                aliasDeclarationStatement
                | assignmentStatement
                | barrierStatement
                | boxStatement
                | breakStatement
                | calibrationGrammarStatement
                | classicalDeclarationStatement
                | constDeclarationStatement
                | continueStatement
                | defStatement
                | defcalStatement
                | delayStatement
                | endStatement
                | expressionStatement
                | externStatement
                | forStatement
                | gateCallStatement
                | gateStatement
                | ifStatement
                | includeStatement
                | ioDeclarationStatement
                | measureArrowAssignmentStatement
                | oldStyleDeclarationStatement
                | quantumDeclarationStatement
                | resetStatement
                | returnStatement
                | whileStatement
            )
        ;
        */

        return when (tokens[idx]) {
            is LetToken -> handleAliasDeclarationStatement()
            is IdentifierToken -> handleStartsWithIdentifier()
            is BarrierToken -> handleBarrierStatement()
            is BoxToken -> handleBoxStatement()
            is BreakToken -> handleBreakStatement()
            // is CalToken -> handleCalStatement() TODO
            is DefcalgrammarToken -> handleCalibrationGrammarStatement()
            is ScalarTypeToken,
            is ArrayToken -> handleClassicalDeclarationStatement()
            is ConstToken -> handleConstDeclarationStatement()
            is ContinueToken -> handleContinueStatement()
            is DefToken -> handleDefStatement()
            is DefcalToken -> handleDefcalStatement()
            is DelayToken -> handleDelayStatement()
            is EndToken -> handleEndStatement()
            // EXPRESSION STATEMENT
            is ExternToken -> handleExternStatement()
            is ForToken -> handleForStatement()
            is GateToken -> handleGateStmt()
            is IfToken -> handleIfStatement()
            is IncludeToken -> handleIncludeStmt()
            is InputToken,
            is OutputToken -> handleIoDeclarationStatement()
            is MeasureToken -> handleMeasureArrowAssignmentStatement()
            is CregToken,
            is QregToken -> handleOldStyleDeclarationStatement()
            is QubitToken -> handleQuantumDeclarationStatement()
            is ResetToken -> handleResetStatement()
            is ReturnToken -> handleReturnStatement()
            is WhileToken -> handleWhileStatement()
            else -> handleExpressionStatement() // TODO: assuming this is an expression is lazy
        }
    }

    private fun handleExpressionStatement(): StatementNode {
        val result = handleExpression()
        val semicolonToken = eat(SemicolonToken::class)
        return ExpressionStatementNode(
            locationAdderHelper(result.location, semicolonToken.location),
            result,
        )
    }

    private fun handleWhileStatement(): StatementNode {
        TODO()
    }

    private fun handleReturnStatement(): StatementNode {
        TODO()
    }

    private fun handleResetStatement(): StatementNode {
        // resetStatement: RESET gateOperand SEMICOLON;
        val reset = eat(ResetToken::class)
        val gateOperand = handleGateOperand()
        val semicolon = eat(SemicolonToken::class)
        return ResetStatementNode(locationAdderHelper(reset, semicolon), gateOperand)
    }

    private fun handleQuantumDeclarationStatement(): StatementNode {
        // quantumDeclarationStatement: qubitType Identifier SEMICOLON;
        val qubitType = handleQubitType()
        val identifier = handleIdentifier()
        val semicolon = eat(SemicolonToken::class)
        return QuantumDeclarationStatementNode(
            locationAdderHelper(qubitType.location, semicolon.location),
            qubitType,
            identifier,
        )
    }

    private fun handleQubitType(): QubitTypeNode {
        // qubitType: QUBIT designator?;
        val qubit = eat(QubitToken::class)
        if (tokens[idx] is LBracketToken) {
            val designator = handleDesignator()
            return QubitTypeNode(
                locationAdderHelper(qubit.location, designator.location),
                designator,
            )
        }
        return QubitTypeNode(qubit.location, null)
    }

    private fun handleMeasureArrowAssignmentStatement(): MeasureArrowAssignmentStatementNode {
        // measureArrowAssignmentStatement: measureExpression (ARROW indexedIdentifier)? SEMICOLON;
        // measureExpression: MEASURE gateOperand;
        val measureExpression = handleMeasureExpression()

        // optional part: (ARROW indexedIdentifier)?
        val indexedIdentifier =
            when (tokens[idx]) {
                is ArrowToken -> {
                    eat(ArrowToken::class)
                    handleIndexedIdentifier()
                }
                else -> null
            }

        val semicolon = eat(SemicolonToken::class)
        return MeasureArrowAssignmentStatementNode(
            locationAdderHelper(measureExpression, semicolon),
            measureExpression,
            indexedIdentifier,
        )
    }

    private fun handleMeasureExpression(): MeasureExpressionNode {
        // measureExpression: MEASURE gateOperand;
        val measure = eat(MeasureToken::class)
        val gateOperand = handleGateOperand()
        return MeasureExpressionNode(locationAdderHelper(measure, gateOperand), gateOperand)
    }

    private fun handleIoDeclarationStatement(): StatementNode {
        TODO()
    }

    private fun handleIfStatement(): StatementNode {
        // ifStatement: IF LPAREN expression RPAREN if_body=statementOrScope (ELSE
        // else_body=statementOrScope)?;
        val ifToken = eat(IfToken::class)
        eat(LParenToken::class)
        val cond = handleExpression()
        eat(RParenToken::class)
        val thenBody = handleStatementOrScope()
        val elseBody =
            if (tokens[idx] is ElseToken) {
                handleStatementOrScope()
            } else {
                null
            }
        val lastLocation = elseBody?.location ?: thenBody.location
        return IfStatementNode(
            locationAdderHelper(ifToken.location, lastLocation),
            cond,
            thenBody,
            elseBody,
        )
    }

    private fun handleForStatement(): StatementNode {
        // forStatement: FOR scalarType Identifier IN (setExpression | LBRACKET rangeExpression
        // RBRACKET | Identifier) body=statementOrScope;
        val forToken = eat(ForToken::class)
        val scalarType = handleScalarType()
        val identifier = handleIdentifier()
        eat(InToken::class)
        val range =
            when (tokens[idx]) {
                is LBraceToken -> handleSetExpression()
                is LBracketToken -> {
                    eat(LBracketToken::class)
                    val r = handleRangeExpression()
                    eat(RBracketToken::class)
                    r
                }
                else -> handleIdentifier()
            }
        val body = handleStatementOrScope()
        return ForStatementNode(
            locationAdderHelper(forToken.location, body.location),
            scalarType,
            identifier,
            range,
            body,
        )
    }

    private fun handleStatementOrScope(): ASTNode {
        return when (tokens[idx]) {
            is LBraceToken -> handleScope()
            else -> handleStatement()
        }
    }

    private fun handleRangeExpression(
        firstExpression: ExpressionNode? = null
    ): RangeExpressionNode {
        // rangeExpression: expression? COLON expression? (COLON expression)?;

        val firstExpr = // we only need to parse this here, if it's not provided from the outside
            firstExpression
                ?: if (tokens[idx] is ColonToken) {
                    null
                } else {
                    handleExpression()
                }
        val colonToken = eat(ColonToken::class)
        val secondExpr =
            if (tokenStartsExpression(tokens[idx])) {
                handleExpression()
            } else {
                null
            }
        val thirdExpression =
            if (tokens[idx] is ColonToken) {
                eat(ColonToken::class)
                handleExpression()
            } else {
                null
            }

        val startPos = firstExpr?.location ?: colonToken.location
        val lastPos = thirdExpression?.location ?: (secondExpr?.location ?: colonToken.location)
        return RangeExpressionNode(
            locationAdderHelper(startPos, lastPos),
            firstExpr,
            secondExpr,
            thirdExpression,
        )
    }

    private fun tokenStartsExpression(token: Token): Boolean {
        // This checks whether the provided token is a valid first token for an expression.
        return when (token) {
            is LParenToken,
            is TildeToken,
            is ExclamationPointToken,
            is MinusToken,
            is ScalarTypeToken,
            is ArrayToken,
            is DurationofToken,
            is IdentifierToken,
            is LiteralToken,
            is HardwareQubitLiteralToken -> true
            else -> false
        }
    }

    private fun handleSetExpression(): SetExpressionNode {
        TODO()
    }

    private fun handleExternStatement(): StatementNode {
        TODO()
    }

    private fun handleEndStatement(): StatementNode {
        TODO()
    }

    private fun handleDelayStatement(): StatementNode {
        TODO()
    }

    private fun handleDefcalStatement(): StatementNode {
        TODO()
    }

    private fun handleDefStatement(): StatementNode {
        TODO()
    }

    private fun handleContinueStatement(): StatementNode {
        TODO()
    }

    private fun handleConstDeclarationStatement(): StatementNode {
        TODO()
    }

    private fun handleClassicalDeclarationStatement(): StatementNode {
        // classicalDeclarationStatement: (scalarType | arrayType) Identifier (EQUALS
        // declarationExpression)? SEMICOLON;
        val tpe =
            if (tokens[idx] is ArrayToken) {
                handleArrayType()
            } else {
                handleScalarType()
            }
        val identifier = handleIdentifier()
        val declExpression =
            if (tokens[idx] is EqualsToken) {
                eat(EqualsToken::class)
                handleDeclarationExpression()
            } else {
                null
            }
        val semicolon = eat(SemicolonToken::class)
        return ClassicalDeclarationStatementNode(
            locationAdderHelper(tpe, semicolon),
            tpe,
            identifier,
            declExpression,
        )
    }

    private fun handleDeclarationExpression(): DeclarationExpressionNode {
        // declarationExpression: arrayLiteral | expression | measureExpression;

        val expr =
            when (tokens[idx]) {
                is MeasureToken -> handleMeasureExpression()
                is LBraceToken -> handleArrayLiteral()
                else -> handleExpression()
            }
        return DeclarationExpressionNode(expr.location, expr)
    }

    private fun handleArrayLiteral(): ExpressionNode {
        // arrayLiteral: LBRACE (expression | arrayLiteral) (COMMA (expression | arrayLiteral))*
        // COMMA? RBRACE;
        val lBraceToken = eat(LBraceToken::class)
        var currentToken = tokens[idx]
        TODO()
    }

    private fun handleScalarType(): ScalarTypeNode {
        /*
        scalarType:
            BIT designator?
            | INT designator?
            | UINT designator?
            | FLOAT designator?
            | ANGLE designator?
            | BOOL
            | DURATION
            | STRETCH
            | COMPLEX (LBRACKET scalarType RBRACKET)?
         */
        return when (tokens[idx]) {
            is BitToken -> {
                val bitToken = eat(BitToken::class)
                val designator =
                    if (tokens[idx] is LBracketToken) {
                        handleDesignator()
                    } else {
                        null
                    }
                val loc =
                    if (designator != null) {
                        locationAdderHelper(bitToken.location, designator.location)
                    } else {
                        bitToken.location
                    }
                ScalarTypeBitNode(loc, designator)
            }
            is IntToken -> {
                TODO()
            }
            is UIntToken -> {
                val uintToken = eat(UIntToken::class)
                val designator =
                    if (tokens[idx] is LBracketToken) {
                        handleDesignator()
                    } else {
                        null
                    }
                val loc =
                    if (designator != null) {
                        locationAdderHelper(uintToken.location, designator.location)
                    } else {
                        uintToken.location
                    }
                ScalarTypeUIntNode(loc, designator)
            }
            is FloatToken -> {
                TODO()
            }
            is AngleToken -> {
                TODO()
            }
            is BoolToken -> {
                val boolToken = eat(BoolToken::class)
                ScalarTypeBoolNode(boolToken.location)
            }
            is DurationToken -> {
                TODO()
            }
            is StretchToken -> {
                TODO()
            }
            is ComplexToken -> {
                TODO()
            }
            else -> TODO()
        }
    }

    private fun handleArrayType(): ArrayTypeNode {
        TODO()
    }

    private fun handleCalibrationGrammarStatement(): StatementNode {
        TODO()
    }

    private fun handleBreakStatement(): StatementNode {
        TODO()
    }

    private fun handleBoxStatement(): StatementNode {
        TODO()
    }

    private fun handleBarrierStatement(): StatementNode {
        val barrierStmt = tokens[idx]
        idx++
        return BarrierStatement(barrierStmt.location)
    }

    private fun handleAliasDeclarationStatement(): StatementNode {
        TODO()
    }

    private fun handleOldStyleDeclarationStatement(): StatementNode {
        /*
        oldStyleDeclarationStatement: (CREG | QREG) Identifier designator? SEMICOLON;
         */
        val firstToken = tokens[idx]
        val type =
            when (firstToken) {
                is CregToken -> {
                    "CREG"
                }
                is QregToken -> {
                    "QREG"
                }
                else -> {
                    TODO()
                }
            }
        idx++
        val identifierNode = handleIdentifier()
        var currentToken = tokens[idx]
        var designatorNode: DesignatorNode? = null
        if (currentToken is LBracketToken) {
            // has designator
            designatorNode = handleDesignator()
            currentToken = tokens[idx]
        } else {
            // no designator
        }
        if (currentToken !is SemicolonToken) {
            TODO()
        }
        idx++
        return OldStyleDeclarationStatementNode(
            locationAdderHelper(firstToken, currentToken),
            type,
            identifierNode,
            designatorNode,
        )
    }

    private fun handleDesignator(): DesignatorNode {
        /*
        designator: LBRACKET expression RBRACKET;
         */
        val firstToken = tokens[idx]
        if (firstToken !is LBracketToken) {
            TODO()
        }
        idx++
        val expr = handleExpression()
        val lastToken = tokens[idx]
        if (lastToken !is RBracketToken) {
            TODO()
        }
        idx++ // consume the semicolon
        return DesignatorNode(locationAdderHelper(firstToken, lastToken), expr)
    }

    private fun handleExpression(): ExpressionNode {
        /*
        expression:
            LPAREN expression RPAREN                                  # parenthesisExpression
            | expression indexOperator                                # indexExpression
            | <assoc=right> expression op=DOUBLE_ASTERISK expression  # powerExpression
            | op=(TILDE | EXCLAMATION_POINT | MINUS) expression       # unaryExpression
            | expression op=(ASTERISK | SLASH | PERCENT) expression   # multiplicativeExpression
            | expression op=(PLUS | MINUS) expression                 # additiveExpression
            | expression op=BitshiftOperator expression               # bitshiftExpression
            | expression op=ComparisonOperator expression             # comparisonExpression
            | expression op=EqualityOperator expression               # equalityExpression
            | expression op=AMPERSAND expression                      # bitwiseAndExpression
            | expression op=CARET expression                          # bitwiseXorExpression
            | expression op=PIPE expression                           # bitwiseOrExpression
            | expression op=DOUBLE_AMPERSAND expression               # logicalAndExpression
            | expression op=DOUBLE_PIPE expression                    # logicalOrExpression
            | (scalarType | arrayType) LPAREN expression RPAREN       # castExpression
            | DURATIONOF LPAREN scope RPAREN                          # durationofExpression
            | Identifier LPAREN expressionList? RPAREN                # callExpression
            | (
                Identifier
                | BinaryIntegerLiteral
                | OctalIntegerLiteral
                | DecimalIntegerLiteral
                | HexIntegerLiteral
                | FloatLiteral
                | ImaginaryLiteral
                | BooleanLiteral
                | BitstringLiteral
                | TimingLiteral
                | HardwareQubit
              )                                                       # literalExpression

        */

        return when (tokens[idx]) {
            /* TODO
            is LParenToken -> handleParenthesisExpression()
            is TildeToken,
            is ExclamationPointToken,
            is MinusToken -> handleUnaryExpression()
            is ScalarTypeToken,
            is ArrayToken -> handleCastExpression()
            is DurationofToken -> handleDurationOfExpression()
            // is IdentifierToken -> handleCallOrLiteralExpression()
                         */
            else -> handleOrExpression()
        }
    }

    private fun handleCallOrLiteralExpression(): ExpressionNode {
        return when (tokens[idx + 1]) {
            is LParenToken -> handleCallExpression()
            else -> {
                val identifier = eat(IdentifierToken::class)
                if (identifier !is IdentifierToken) {
                    TODO()
                }
                return IdentifierNode(identifier.location, identifier.payload)
            }
        }
    }

    private fun handleCallExpression(): CallExpressionNode {
        val currentToken = tokens[idx]
        val idtoken = currentToken as? IdentifierToken ?: TODO()
        val identifierNode = IdentifierNode(idtoken.location, idtoken.payload)
        idx++
        eat(LParenToken::class)
        val args = mutableListOf<ExpressionNode>()
        var endLine = idtoken.location.region.endLine
        var endColumn = idtoken.location.region.endColumn
        while (tokens[idx] !is RParenToken) {
            if (tokens[idx] is CommaToken) {
                // Consume COMMA
                idx++
            } else {
                val argument = handleExpression()
                args.add(argument)
                endLine = argument.location.region.endLine
                endColumn = argument.location.region.endColumn
            }
        }
        eat(RParenToken::class)
        // TODO: This is not 100% accurate but should be good enough for now.
        val location =
            PhysicalLocation(
                idtoken.location.artifactLocation.uri,
                Region(
                    idtoken.location.region.startLine,
                    idtoken.location.region.startColumn,
                    endLine,
                    endColumn,
                ),
            )
        return CallExpressionNode(location, identifierNode, args)
    }

    private fun handleDurationOfExpression(): ExpressionNode {
        TODO()
    }

    private fun handleParenthesisExpression(): ExpressionNode {
        TODO()
    }

    private fun handleOrExpression(): ExpressionNode {
        var lhs = handleAndExpression()
        while (tokens[idx] is DoublePipeToken) {
            val startPosition = lhs
            eat(DoublePipeToken::class)
            val rhs = handleAndExpression()
            lhs =
                LogicalOrExpressionNode(
                    locationAdderHelper(startPosition.location, rhs.location),
                    lhs,
                    rhs,
                )
        }
        return lhs
    }

    private fun handleAndExpression(): ExpressionNode {
        var lhs = handleBitOrExpression()
        while (tokens[idx] is DoubleAmpersandToken) {
            val startPosition = lhs
            eat(DoubleAmpersandToken::class)
            val rhs = handleBitOrExpression()
            lhs =
                LogicalAndExpressionNode(
                    locationAdderHelper(startPosition.location, rhs.location),
                    lhs,
                    rhs,
                )
        }
        return lhs
    }

    private fun handleBitOrExpression(): ExpressionNode {
        var lhs = handleBitXorExpression()
        while (tokens[idx] is BitToken) {
            val startPosition = lhs
            eat(DoubleAmpersandToken::class)
            val rhs = handleBitXorExpression()
            lhs =
                BitwiseOrExpressionNode(
                    locationAdderHelper(startPosition.location, rhs.location),
                    lhs,
                    rhs,
                )
        }
        return lhs
    }

    private fun handleBitXorExpression(): ExpressionNode {
        var lhs = handleBitAndExpression()
        while (tokens[idx] is CaretToken) {
            val startPosition = lhs
            eat(CaretToken::class)
            val rhs = handleBitAndExpression()
            lhs =
                BitwiseXorExpressionNode(
                    locationAdderHelper(startPosition.location, rhs.location),
                    lhs,
                    rhs,
                )
        }
        return lhs
    }

    private fun handleBitAndExpression(): ExpressionNode {
        var lhs = handleEqualityExpression()
        while (tokens[idx] is AmpersandToken) {
            val startPosition = lhs
            eat(AmpersandToken::class)
            val rhs = handleEqualityExpression()
            lhs =
                BitwiseAndExpressionNode(
                    locationAdderHelper(startPosition.location, rhs.location),
                    lhs,
                    rhs,
                )
        }
        return lhs
    }

    private fun handleEqualityExpression(): ExpressionNode {
        var lhs = handleComparisonExpression()
        while (tokens[idx] is EqualityOperatorToken) {

            lhs =
                if (tokens[idx] is EqualityOperatorEqualToken) {
                    eat(EqualityOperatorEqualToken::class)
                    val rhs = handleComparisonExpression()
                    EqualityEqualsExpressionNode(locationAdderHelper(lhs, rhs), lhs, rhs)
                } else if (tokens[idx] is EqualityOperatorNotEqualToken) {
                    eat(EqualityOperatorNotEqualToken::class)
                    val rhs = handleComparisonExpression()
                    EqualityNotEqualsExpressionNode(locationAdderHelper(lhs, rhs), lhs, rhs)
                } else {
                    TODO()
                }
        }
        return lhs
    }

    private fun handleComparisonExpression(): ExpressionNode {
        var lhs = handleBitshiftExpression()
        while (tokens[idx] is ComparisonOperatorToken) {
            if (tokens[idx] is ComparisonOperatorLessToken) {
                eat(ComparisonOperatorLessToken::class)
                val rhs = handleBitshiftExpression()
                lhs = ComparisonLessExpressionNode(locationAdderHelper(lhs, rhs), lhs, rhs)
            } else if (tokens[idx] is ComparisonOperatorLessEqToken) {
                eat(ComparisonOperatorLessEqToken::class)
                val rhs = handleBitshiftExpression()
                lhs = ComparisonLessEqExpressionNode(locationAdderHelper(lhs, rhs), lhs, rhs)
            } else if (tokens[idx] is ComparisonOperatorGreaterToken) {
                eat(ComparisonOperatorGreaterToken::class)
                val rhs = handleBitshiftExpression()
                lhs = ComparisonGreaterExpressionNode(locationAdderHelper(lhs, rhs), lhs, rhs)
            } else if (tokens[idx] is ComparisonOperatorGreaterEqToken) {
                eat(ComparisonOperatorGreaterEqToken::class)
                val rhs = handleBitshiftExpression()
                lhs = ComparisonGreaterEqExpressionNode(locationAdderHelper(lhs, rhs), lhs, rhs)
            } else {
                TODO()
            }
        }
        return lhs
    }

    private fun handleBitshiftExpression(): ExpressionNode {
        var lhs = handleAdditiveExpression()
        while (tokens[idx] is BitshiftToken) {
            lhs =
                if (tokens[idx] is BitshiftOperatorLeftToken) {
                    eat(BitshiftOperatorLeftToken::class)
                    val rhs = handleAdditiveExpression()

                    BitshiftLeftExpressionNode(locationAdderHelper(lhs, rhs), lhs, rhs)
                } else if (tokens[idx] is BitshiftOperatorRightToken) {
                    eat(BitshiftOperatorRightToken::class)
                    val rhs = handleAdditiveExpression()

                    BitshiftRightExpressionNode(locationAdderHelper(lhs, rhs), lhs, rhs)
                } else {
                    TODO()
                }
        }
        return lhs
    }

    private fun handleAdditiveExpression(): ExpressionNode {
        var lhs = handleMultiplicativeExpression()
        while (tokens[idx] is PlusToken || tokens[idx] is MinusToken) {
            lhs =
                if (tokens[idx] is PlusToken) {
                    eat(PlusToken::class)
                    val rhs = handleMultiplicativeExpression()

                    AdditivePlusExpressionNode(locationAdderHelper(lhs, rhs), lhs, rhs)
                } else if (tokens[idx] is MinusToken) {
                    eat(MinusToken::class)
                    val rhs = handleMultiplicativeExpression()

                    AdditiveMinusExpressionNode(locationAdderHelper(lhs, rhs), lhs, rhs)
                } else {
                    TODO()
                }
        }
        return lhs
    }

    private fun handleMultiplicativeExpression(): ExpressionNode {
        var lhs = handleUnaryExpression()
        while (
            tokens[idx] is AsteriskToken || tokens[idx] is SlashToken || tokens[idx] is PercentToken
        ) {
            if (tokens[idx] is AsteriskToken) {
                eat(AsteriskToken::class)
                val rhs = handleUnaryExpression()

                lhs = MultiplicativeAsteriskExpressionNode(locationAdderHelper(lhs, rhs), lhs, rhs)
            } else if (tokens[idx] is SlashToken) {
                eat(SlashToken::class)
                val rhs = handleUnaryExpression()

                lhs = MultiplicativeSlashExpressionNode(locationAdderHelper(lhs, rhs), lhs, rhs)
            } else if (tokens[idx] is PercentToken) {
                eat(PercentToken::class)
                val rhs = handleUnaryExpression()

                lhs = MultiplicativePercentExpressionNode(locationAdderHelper(lhs, rhs), lhs, rhs)
            } else {
                TODO()
            }
        }
        return lhs
    }

    private fun handleUnaryExpression(): ExpressionNode {
        // op=(TILDE | EXCLAMATION_POINT | MINUS) expression       # unaryExpression
        // TODO check order
        val stack = mutableListOf<Token>()
        while (
            tokens[idx] is TildeToken ||
                tokens[idx] is ExclamationPointToken ||
                tokens[idx] is MinusToken
        ) {
            val currentToken = eat(Token::class)
            stack.add(currentToken)
        }
        var expr = handlePowerExpression()
        while (stack.isNotEmpty()) {
            when (stack.first()) {
                is AsteriskToken ->
                    expr =
                        UnaryExpressionAsteriskNode(
                            locationAdderHelper(stack.first().location, expr.location),
                            expr,
                        )
                is ExclamationPointToken ->
                    expr =
                        UnaryExpressionExclamationPointNode(
                            locationAdderHelper(stack.first().location, expr.location),
                            expr,
                        )
                is MinusToken ->
                    expr =
                        UnaryExpressionMinusNode(
                            locationAdderHelper(stack.first().location, expr.location),
                            expr,
                        )
            }
            stack.removeFirst()
        }
        return expr
    }

    private fun handlePowerExpression(): ExpressionNode {
        // TODO

        return handleCallIndexCastExpression()
    }

    private fun handleCallIndexCastExpression(): ExpressionNode {
        val currentToken = tokens[idx]
        if (currentToken is ScalarTypeToken || currentToken is ArrayToken) {
            return handleCastExpression()
        }
        // TODO
        return handleOperatorExpression()
    }

    private fun handleCastExpression(): CastExpressionNode {
        // (scalarType | arrayType) LPAREN expression RPAREN       # castExpression
        val tpe =
            if (tokens[idx] is ScalarTypeToken) {
                handleScalarType()
            } else {
                handleArrayType()
            }
        eat(LParenToken::class)
        val expr = handleExpression()
        val lastToken = eat(RParenToken::class)
        return CastExpressionNode(locationAdderHelper(tpe.location, lastToken.location), tpe, expr)
    }

    private fun handleOperatorExpression(): ExpressionNode {
        return when (val currentToken = tokens[idx]) {
            is DecimalIntegerLiteralToken -> {
                eat(DecimalIntegerLiteralToken::class)
                DecimalIntegerLiteralExpressionNode(currentToken.location, currentToken.payload)
            }
            is IdentifierToken -> {
                val identifier = eat(IdentifierToken::class)
                if (identifier == null || identifier !is IdentifierToken) {
                    TODO()
                }
                when (tokens[idx]) {
                    is LBracketToken -> {
                        // expression indexOperator                                # indexExpression
                        val indexOp = handleIndexOperator()
                        IndexExpressionNode(
                            locationAdderHelper(identifier, indexOp),
                            identifier.payload,
                            indexOp,
                        )
                    }
                    is LParenToken -> {
                        // Identifier LPAREN expressionList? RPAREN                # callExpression
                        TODO()
                    }
                    else -> {
                        // Only an identifier. Nothing else.
                        IdentifierNode(identifier.location, identifier.payload)
                    }
                }
            }
            else -> TODO("token not implemented: $currentToken")
        }
    }

    private fun handleStartsWithIdentifier(): StatementNode {
        /*
        This function handles all possible statements starting with an identifier.
        - assignmentStatement
        - gateCallStatement
        - callExpression
        - literalExpression

        This makes this a tricky task to implement.
        We first check whether we currently have an assignmentStatement.

        assignmentStatement: indexedIdentifier op=(EQUALS | CompoundAssignmentOperator) (expression | measureExpression) SEMICOLON;
        indexedIdentifier: Identifier indexOperator*;
        indexOperator:
            LBRACKET
            (
                setExpression
                | (expression | rangeExpression) (COMMA (expression | rangeExpression))* COMMA?
            )
            RBRACKET;

        gateCallStatement:
            gateModifier* Identifier (LPAREN expressionList? RPAREN)? designator? gateOperandList SEMICOLON
            | gateModifier* GPHASE (LPAREN expressionList? RPAREN)? designator? gateOperandList? SEMICOLON
        gateModifier: (
            INV
            | POW LPAREN expression RPAREN
            | (CTRL | NEGCTRL) (LPAREN expression RPAREN)?
        ) AT;

        callExpression: Identifier LPAREN expressionList? RPAREN SEMICOLON

        literalExpression: (
            Identifier
            | BinaryIntegerLiteral
            | OctalIntegerLiteral
            | DecimalIntegerLiteral
            | HexIntegerLiteral
            | FloatLiteral
            | ImaginaryLiteral
            | BooleanLiteral
            | BitstringLiteral
            | TimingLiteral
            | HardwareQubit
          )
         */
        val firstToken = tokens[idx]
        val identifier = handleIdentifier()
        return when (tokens[idx]) {
            is SemicolonToken -> {
                // The one simple case: this is a literalExpression wrapped in a statement.
                val semicolon = tokens[idx]
                idx++
                ExpressionStatementNode(
                    locationAdderHelper(firstToken, semicolon),
                    LiteralExpressionIdentifierNode(firstToken.location, identifier),
                )
            }
            is LBracketToken -> {
                // looks like an indexOperator
                // -> we are parsing an assignmentStatement
                handleAssignmentStatement(identifier)
            }
            is EqualsToken,
            is CompoundAssignmentOperatorToken -> {
                // looks like an assignmentStatement without an indexOperator
                handleAssignmentStatement(identifier)
            }

            // now we can still have a gateCallStatement (first case) like this:
            // Identifier (LPAREN expressionList? RPAREN)? designator? gateOperandList
            // or we have a callExpression (wrapped in a statement)
            else -> handleGateCallStmtOrCallExprStmt(identifier)
        }
    }

    private fun handleGateCallStmtOrCallExprStmt(identifier: IdentifierNode): StatementNode {
        // This is a helper function  which decides between a gateCallStatement and a
        // callExpression. This function only applies in the limited case:
        // Identifier (LPAREN expressionList? RPAREN)? designator? gateOperandList SEMICOLON
        // or
        // callExpression: Identifier LPAREN expressionList? RPAREN SEMICOLON
        var exprList: List<ExpressionNode>? = null
        var rParenToken: Token? = null // needed for location of callExpression
        if (tokens[idx] is LParenToken) {
            idx++
            exprList = exprListHelper()
            rParenToken = tokens[idx]
            if (rParenToken !is RParenToken) {
                TODO()
            }
            idx++
        }
        // we have a callExpression iff the next token is a SEMICOLON
        val currentToken = tokens[idx]
        if (currentToken is SemicolonToken) {
            if (rParenToken == null || exprList == null) {
                TODO()
            }
            val result =
                ExpressionStatementNode(
                    locationAdderHelper(identifier.location, currentToken.location),
                    CallExpressionNode(
                        locationAdderHelper(identifier.location, rParenToken.location),
                        identifier,
                        exprList,
                    ),
                )
            idx++
            return result
        } else {
            // this is a gateCallStatement
            var designator: DesignatorNode? = null
            if (currentToken is LBracketToken) {
                designator = handleDesignator()
            }
            val gateOpList = handleGateOperandList()
            val semicolonToken = eat(SemicolonToken::class)
            return GateCallStatementNode(
                locationAdderHelper(identifier.location, semicolonToken.location),
                null,
                identifier,
                exprList,
                designator,
                gateOpList,
            )
        }
    }

    private fun handleGateOperandList(): GateOperandListNode {
        // gateOperandList: gateOperand (COMMA gateOperand)* COMMA?;
        // gateOperand: indexedIdentifier | HardwareQubit;
        // Note: all possible occurrences of gateOperandList are followed by a SEMICOLON -> this
        // make it easy for us to decide whether we have parsed all gateOperands
        val firstToken = tokens[idx]
        var currentToken: Token
        val list = mutableListOf<GateOperandNode>()
        do {
            currentToken = tokens[idx]
            if (currentToken is CommaToken) {
                eat(CommaToken::class)
            }
            currentToken = tokens[idx]
            if (currentToken !is SemicolonToken) {
                list += handleGateOperand()
            }
        } while (tokens[idx] !is SemicolonToken)

        return GateOperandListNode(locationAdderHelper(firstToken, currentToken), list)
    }

    private fun handleGateOperand(): GateOperandNode {
        // gateOperand: indexedIdentifier | HardwareQubit;
        // TODO HardwareQubit
        val indexedIdentifier = handleIndexedIdentifier()
        return GateOperandNode(indexedIdentifier.location, indexedIdentifier)
    }

    private fun handleIndexedIdentifier(): IndexedIdentifierNode {
        // indexedIdentifier: Identifier indexOperator*;
        // indexOperator:
        //    LBRACKET
        //    (
        //        setExpression
        //        | (expression | rangeExpression) (COMMA (expression | rangeExpression))* COMMA?
        //    )
        //    RBRACKET;

        val identifier = handleIdentifier()
        val idxOperators = mutableListOf<IndexOperatorNode>()
        while (tokens[idx] is LBracketToken) {
            idxOperators.add(handleIndexOperator())
        }
        val lastLocation =
            if (idxOperators.isEmpty()) {
                identifier.location
            } else {
                idxOperators.last().location
            }
        return IndexedIdentifierNode(
            locationAdderHelper(identifier.location, lastLocation),
            identifier,
            idxOperators,
        )
    }

    private fun handleIndexOperator(): IndexOperatorNode {
        // indexOperator:
        //    LBRACKET
        //    (
        //        setExpression
        //        | (expression | rangeExpression) (COMMA (expression | rangeExpression))* COMMA?
        //    )
        //    RBRACKET;
        val firstToken = eat(LBracketToken::class)
        val exprs = mutableListOf<ExpressionNode>()

        if (tokens[idx] is LBraceToken) {
            // this is a setExpression
            exprs.add(handleSetExpression())
        } else {
            // (expression | rangeExpression) (COMMA (expression | rangeExpression))* COMMA?
            exprs.add(handleRangeOrNormalExpression())
            while (tokens[idx] !is RBracketToken) {
                eat(CommaToken::class)
                if (tokens[idx] !is RBracketToken) {
                    exprs.add(handleRangeOrNormalExpression())
                }
            }
        }
        val lastToken = eat(RBracketToken::class)

        return IndexOperatorNode(locationAdderHelper(firstToken, lastToken), exprs)
    }

    /*
    This is a small helper function. It starts by parsing an expression and then decides (depending on the now current token being a COLON) whether it sees a normal expression or a rangeExpression.
    Thus, this parses: (expression | rangeExpression)
     */
    private fun handleRangeOrNormalExpression(): ExpressionNode {
        val firstExpression = handleExpression()
        return if (tokens[idx] is ColonToken) {
            handleRangeExpression(firstExpression)
        } else {
            firstExpression
        }
    }

    private fun exprListHelper(): List<ExpressionNode> {
        // could be an empty list
        val exprList = mutableListOf<ExpressionNode>()
        while (tokens[idx] !is RParenToken) {
            if (tokens[idx] is CommaToken) {
                // Consume COMMA
                idx++
            } else {
                val argument = handleExpression()
                exprList.add(argument)
            }
        }
        return exprList
    }

    private fun handleAssignmentStatement(identifier: IdentifierNode): StatementNode {
        TODO()
    }

    private fun handleIdentifier(): IdentifierNode {
        // either callExpression or literalExpression
        val currentToken = tokens[idx]
        val nextToken: Token = tokens[idx + 1]
        /*if (nextToken is LParenToken) {
            // callExpression

            return handleCallExpression()
        } else {*/
        val idtoken = currentToken as? IdentifierToken
        idx++
        if (idtoken == null) {
            TODO()
        }
        return IdentifierNode(idtoken.location, idtoken.payload)
        // }
    }

    private fun handleIncludeStmt(): IncludeStatementNode {
        /*
        includeStatement: INCLUDE StringLiteral SEMICOLON;
         */
        val includeToken = tokens[idx] as? IncludeToken
        idx++
        val stringToken = tokens[idx] as? StringLiteralToken
        idx++
        val semicolonToken = tokens[idx] as? SemicolonToken
        idx++
        if (includeToken == null || stringToken == null || semicolonToken == null) TODO()
        return IncludeStatementNode(
            locationAdderHelper(includeToken.location, semicolonToken.location),
            stringToken.payload,
        )
    }

    private fun handleGateStmt(): GateStatementNode {
        /*
        gateStatement: GATE Identifier (LPAREN params=identifierList? RPAREN)? qubits=identifierList scope;
         */
        val gateToken = eat(GateToken::class)

        val identifierToken = handleIdentifier()

        val args = mutableListOf<ExpressionNode>()
        if (tokens[idx] is LParenToken) {
            eat(LParenToken::class)
            var endLine = identifierToken.location.region.endLine
            var endColumn = identifierToken.location.region.endColumn
            while (tokens[idx] !is RParenToken) {
                if (tokens[idx] is CommaToken) {
                    // Consume COMMA
                    idx++
                } else {
                    val argument = handleExpression()
                    args.add(argument)
                    endLine = argument.location.region.endLine
                    endColumn = argument.location.region.endColumn
                }
            }
            eat(RParenToken::class)
        }

        val identifierList = handleIdentifierList()

        val scopeToken = tokens[idx] // for the location
        val scope = handleScope()

        if (gateToken == null || identifierToken == null || identifierList == null || scope == null)
            TODO()

        return GateStatementNode(
            locationAdderHelper(gateToken, scopeToken),
            identifierToken.payload,
            identifierList,
            scope,
        )
    }

    private fun handleIdentifierList(): IdentifierListNode {
        /*
        identifierList: Identifier (COMMA Identifier)* COMMA?;
         */
        val firstIdentifier = tokens[idx] as? IdentifierToken
        idx++
        if (firstIdentifier == null) TODO()
        val identiferList = mutableListOf(firstIdentifier.payload)
        var currentToken = tokens[idx]
        while (currentToken is IdentifierToken || currentToken is CommaToken) {
            currentToken = tokens[idx] // redundant on first iteration
            when (currentToken) {
                is CommaToken -> idx++ // consume the comma
                is IdentifierToken -> {
                    identiferList += currentToken.payload
                    idx++
                }
                else -> {
                    // we are done
                }
            }
        }
        return IdentifierListNode(locationAdderHelper(firstIdentifier, currentToken), identiferList)
    }

    private fun handleScope(): ScopeNode {
        /*
        scope: LBRACE statement* RBRACE;
         */
        val firstToken = eat(LBraceToken::class)
        val stmts = mutableListOf<StatementNode>()
        while (tokens[idx] !is RBraceToken) {
            stmts += handleStatement()
        }
        val lastToken = eat(RBraceToken::class)
        return ScopeNode(locationAdderHelper(firstToken, lastToken), stmts)
    }

    private fun handleVersionNode(): VersionNode {
        val openQASMToken = tokens[idx] as? OpenQASMToken
        idx++
        val semicolonToken = tokens[idx] as? SemicolonToken
        idx++
        if (openQASMToken == null || semicolonToken == null) TODO()
        return VersionNode(
            locationAdderHelper(openQASMToken, semicolonToken),
            openQASMToken.versionString,
        )
    }

    private fun skipComments() {
        while (
            idx < tokens.size && tokens[idx] is BlockCommentToken || tokens[idx] is LineCommentToken
        ) idx++
    }

    /*
    Computes the compounding region given two regions with the same URI
     */
    private fun locationAdderHelper(
        loc1: PhysicalLocation,
        loc2: PhysicalLocation,
    ): PhysicalLocation {
        if (loc1.artifactLocation != loc2.artifactLocation) TODO()
        val region1 = loc1.region
        val region2 = loc2.region
        val startLine: Int
        val endLine: Int
        val startColumn: Int
        val endColumn: Int
        if (region1.startLine < region2.startLine) {
            startLine = region1.startLine
            startColumn = region1.startColumn
        } else if (region1.startLine == region2.startLine) {
            startLine = region1.startLine
            startColumn =
                if (region1.startColumn < region2.startColumn) region1.startColumn
                else region2.startColumn
        } else {
            startLine = region2.startLine
            startColumn = region2.startColumn
        }

        if (region1.endLine > region2.endLine) {
            endLine = region1.endLine
            endColumn = region1.endColumn
        } else if (region1.endLine == region2.endLine) {
            endLine = region1.endLine
            endColumn =
                if (region1.endColumn > region2.endColumn) region1.endColumn else region2.endColumn
        } else {
            endLine = region2.endLine
            endColumn = region2.endColumn
        }
        return PhysicalLocation(
            loc1.artifactLocation.uri,
            Region(startLine, startColumn, endLine, endColumn),
        )
    }

    private fun locationAdderHelper(t1: Token, t2: Token): PhysicalLocation {
        return locationAdderHelper(t1.location, t2.location)
    }

    private fun locationAdderHelper(n1: ASTNode, n2: ASTNode): PhysicalLocation {
        return locationAdderHelper(n1.location, n2.location)
    }

    private fun locationAdderHelper(n: ASTNode, t: Token): PhysicalLocation {
        return locationAdderHelper(n.location, t.location)
    }

    private fun locationAdderHelper(t: Token, n: ASTNode): PhysicalLocation {
        return locationAdderHelper(t.location, n.location)
    }

    private fun eat(expectedType: KClass<out Token>): Token {
        val currentToken = tokens[idx]
        if (!expectedType.isInstance(currentToken)) {

            TODO()
        }
        idx++
        return currentToken
    }
}
