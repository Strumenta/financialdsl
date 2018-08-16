package com.strumenta.financialdsl

import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test

class ParsingTest {

    @Test
    fun parseCompanyTypes() {
        val inputStream = ParsingTest::class.java.getResourceAsStream("/company_types.fin")
        println("IS $inputStream")
        val input = CharStreams.fromStream(inputStream)
        val lexer = FinancialDSLLexer(input)
        var parser = FinancialDSLParser(CommonTokenStream(lexer))
        val root = parser.financialDSLFile()
    }
}