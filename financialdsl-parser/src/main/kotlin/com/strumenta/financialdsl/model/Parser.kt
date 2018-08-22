package com.strumenta.financialdsl.model

import com.strumenta.financialdsl.FinancialDSLLexer
import com.strumenta.financialdsl.FinancialDSLParser
import com.strumenta.kotlinmultiplatform.BitSet
import me.tomassetti.kolasu.model.specificProcess
import me.tomassetti.kolasu.model.tryToResolve
import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.Parser
import org.antlr.v4.kotlinruntime.atn.ATNConfigSet
import org.antlr.v4.kotlinruntime.dfa.DFA
import java.util.*

data class Position(val line: Int, val column: Int)

enum class ErrorType {
    LEXICAL, SYNTACTIC, SEMANTIC
}

data class Error(val type: ErrorType, val message: String, val position: Position)

data class ParsingResult<R>(val ast: R?, val errors: List<Error>)

class Parser {

    fun parse(code: String) : ParsingResult<FinancialDSLFile> {
        val input = CharStreams.fromString(code)
        val lexer = FinancialDSLLexer(input)
        val errors = LinkedList<Error>()
        val lexerErrorListener = object : ANTLRErrorListener {
            override fun reportAmbiguity(recognizer: Parser, dfa: DFA, startIndex: Int, stopIndex: Int, exact: Boolean, ambigAlts: BitSet, configs: ATNConfigSet) {
                // nothing to do
            }

            override fun reportAttemptingFullContext(recognizer: Parser, dfa: DFA, startIndex: Int, stopIndex: Int, conflictingAlts: BitSet, configs: ATNConfigSet) {
                // nothing to do
            }

            override fun reportContextSensitivity(recognizer: Parser, dfa: DFA, startIndex: Int, stopIndex: Int, prediction: Int, configs: ATNConfigSet) {
                // nothing to do
            }

            override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?) {
                errors.add(Error(ErrorType.LEXICAL, msg, Position(line, charPositionInLine)))
            }

        }
        lexer.removeErrorListeners()
        lexer.addErrorListener(lexerErrorListener)
        val parser = FinancialDSLParser(CommonTokenStream(lexer))
        val parserErrorListener = object : ANTLRErrorListener {
            override fun reportAmbiguity(recognizer: Parser, dfa: DFA, startIndex: Int, stopIndex: Int, exact: Boolean, ambigAlts: BitSet, configs: ATNConfigSet) {
                // nothing to do
            }

            override fun reportAttemptingFullContext(recognizer: Parser, dfa: DFA, startIndex: Int, stopIndex: Int, conflictingAlts: BitSet, configs: ATNConfigSet) {
                // nothing to do
            }

            override fun reportContextSensitivity(recognizer: Parser, dfa: DFA, startIndex: Int, stopIndex: Int, prediction: Int, configs: ATNConfigSet) {
                // nothing to do
            }

            override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?) {
                errors.add(Error(ErrorType.SYNTACTIC, msg, Position(line, charPositionInLine)))
            }

        }
        parser.removeErrorListeners()
        parser.addErrorListener(parserErrorListener)
        val ast = parser.financialDSLFile().toAst()
        ast.validate(errors)
        return ParsingResult(ast, errors)
    }
}

private fun FinancialDSLFile.validate(errors: LinkedList<Error>) {
    this.specificProcess(CompanyTypeRef::class.java) { companyTypeRef ->
        companyTypeRef.ref.tryToResolve(this.companyTypes)
    }
}
