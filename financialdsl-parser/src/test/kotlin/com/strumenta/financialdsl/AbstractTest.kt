package com.strumenta.financialdsl

import com.strumenta.financialdsl.model.Parser
import com.strumenta.kotlinmultiplatform.BitSet
import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.atn.ATNConfigSet
import org.antlr.v4.kotlinruntime.dfa.DFA
import kotlin.test.fail

abstract class AbstractTest {
    protected fun assertParsedWithoutErrors(exampleName: String) : FinancialDSLParser.FinancialDSLFileContext {
        val inputStream = LexingTest::class.java.getResourceAsStream("/$exampleName.fin")
        val input = CharStreams.fromStream(inputStream)
        val lexer = FinancialDSLLexer(input)
        val errorListener = object : ANTLRErrorListener {
            override fun reportAmbiguity(recognizer: org.antlr.v4.kotlinruntime.Parser, dfa: DFA, startIndex: Int, stopIndex: Int, exact: Boolean, ambigAlts: BitSet, configs: ATNConfigSet) {
                // nothing to do
            }

            override fun reportAttemptingFullContext(recognizer: org.antlr.v4.kotlinruntime.Parser, dfa: DFA, startIndex: Int, stopIndex: Int, conflictingAlts: BitSet, configs: ATNConfigSet) {
                // nothing to do
            }

            override fun reportContextSensitivity(recognizer: org.antlr.v4.kotlinruntime.Parser, dfa: DFA, startIndex: Int, stopIndex: Int, prediction: Int, configs: ATNConfigSet) {
                // nothing to do
            }

            override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?) {
                e?.printStackTrace()
                fail("Syntax error: $msg at $line:$charPositionInLine, $offendingSymbol, $e")
            }

        }
        lexer.removeErrorListeners()
        lexer.addErrorListener(errorListener)
        val parser = FinancialDSLParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(errorListener)
        return parser.financialDSLFile()
    }
}