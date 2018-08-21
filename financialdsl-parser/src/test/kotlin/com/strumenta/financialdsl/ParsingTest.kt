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
        val inputStream = LexingTest::class.java.getResourceAsStream("/$exampleName.fin")
        val input = CharStreams.fromStream(inputStream)
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

    @Test
    fun parseCompanyTypesSimplified() {
        val root = assertParsedWithoutErrors("company_types_1")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCompanyTypes() {
        val root = assertParsedWithoutErrors("company_types")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCompany() {
        val root = assertParsedWithoutErrors("company")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parsePerson() {
        val root = assertParsedWithoutErrors("person")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parsePersonWithShare() {
        val root = assertParsedWithoutErrors("person2")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseIres() {
        val root = assertParsedWithoutErrors("ires")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseIrap() {
        val root = assertParsedWithoutErrors("irap")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseRegions() {
        val root = assertParsedWithoutErrors("regions")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCities() {
        val root = assertParsedWithoutErrors("cities")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCountries() {
        val root = assertParsedWithoutErrors("countries")
        assertEquals(1, root.declarations.size)
    }
}