package com.strumenta.financialdsl

import com.strumenta.kotlinmultiplatform.BitSet
import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.atn.ATNConfigSet
import org.antlr.v4.kotlinruntime.dfa.DFA
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

private val Token.typeName
    get() = if (this.type == -1) "EOF" else FinancialDSLLexer.VOCABULARY.getSymbolicName(this.type)

class LexingTest {

    private fun assertLexedWithoutErrors(exampleName: String) : List<Token> {
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
        val tokens = LinkedList<Token>()
        do {
            tokens.add(lexer.nextToken())
        } while (tokens.last.type != -1)
        return tokens
    }

    @Test
    fun parseCompanyTypesSimplified() {
        var tokens = assertLexedWithoutErrors("company_types_1")
        tokens = tokens.filter { it.channel == 0 }
        println(tokens.map { it.typeName })
        assertEquals(listOf("COMPANY", "TYPE", "ID", "LBRACE", "RBRACE", "EOF"), tokens.map { it.typeName })
    }

    @Test
    fun parseCompanyTypes() {
        var tokens = assertLexedWithoutErrors("company_types")
        tokens = tokens.filter { it.channel == 0 }
        println(tokens.map { it.typeName })
        assertEquals(listOf("COMPANY", "TYPE", "ID", "LBRACE", "ID", "IS", "AMOUNT", "ID", "IS", "AMOUNT", "EQUAL",
                "ID", "PLUS", "ID", "ID", "IS", "AMOUNT", "RBRACE", "EOF"), tokens.map { it.typeName })
    }

    @Test
    fun parseCompany() {
        val tokens = assertLexedWithoutErrors("company")
    }

    @Test
    fun parsePerson() {
        val tokens = assertLexedWithoutErrors("person")
    }
}