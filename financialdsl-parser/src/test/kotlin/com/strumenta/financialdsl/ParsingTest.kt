package com.strumenta.financialdsl

import com.strumenta.kotlinmultiplatform.BitSet
import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.atn.ATNConfigSet
import org.antlr.v4.kotlinruntime.dfa.DFA
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ParsingTest {

    private fun assertParsedWithoutErrors(exampleName: String) : FinancialDSLParser.FinancialDSLFileContext {
        val input = CharStreams.fromFileName("src/test/resources/$exampleName.fin")
        val lexer = FinancialDSLLexer(input)
        val errorListener = object : ANTLRErrorListener {
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
                fail("Syntax error: $msg at $line:$charPositionInLine")
            }

        }
        lexer.removeErrorListeners()
        lexer.addErrorListener(errorListener)
        var parser = FinancialDSLParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(errorListener)
        return parser.financialDSLFile()
    }

    @Test
    fun parseCompanyTypes() {
        val root = assertParsedWithoutErrors("company_types")
        assertEquals(1, root.declarations.size)
    }
}