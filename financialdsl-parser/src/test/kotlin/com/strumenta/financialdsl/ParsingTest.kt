package com.strumenta.financialdsl

import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import kotlin.test.Test
import kotlin.test.assertEquals

class ParsingTest {

    private fun assertParsedWithoutErrors(exampleName: String) : FinancialDSLParser.FinancialDSLFileContext {
        val input = CharStreams.fromFileName("src/test/resources/$exampleName.fin")
        val lexer = FinancialDSLLexer(input)
        var parser = FinancialDSLParser(CommonTokenStream(lexer))
        return parser.financialDSLFile()
    }

    @Test
    fun parseCompanyTypes() {
        val root = assertParsedWithoutErrors("company_types")
        assertEquals(1, root.declarations.size)
    }
}