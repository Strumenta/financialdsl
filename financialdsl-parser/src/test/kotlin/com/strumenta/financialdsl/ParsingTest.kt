package com.strumenta.financialdsl

import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.runtime.CharStreams
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test

class ParsingTest {

    @Test
    fun parseCompanyTypes() {
        val inputStream = ParsingTest::class.java.getResourceAsStream("/company_types.fin")
        val input = CharStreams.fromStream(inputStream)
        val lexer = Financi(input)
//        var parser = MiniCalcParser(CommonTokenStream(lexer))
    }
}