package com.strumenta.financialdsl

import kotlin.test.Test

class PensionsTest : AbstractTest(){

    private val terziarioExample = """
        pension contribution InpsTerziario paid by employee {
            considered_salary = (taxable of IRES for employer - amount of IRES for employer - amount of IRAP for employer) by share of employeer of employee
            rate = brackets [to 46,123] -> 22.74%,
                              [to 76,872] -> 23.74%,
                              [above]     -> 0%
            amount = (rate for considered_salary) with minimum 3.535,61
        }
    """.trimIndent()

    private val glaExample = """
        pension contribution InpsGLA paid by employer 2/3 and employee 1/3 {
           considered_salary = gross_compensation of employee
            rate = brackets [to 100,323] -> 27.72%,
                            [above]      -> 0%
            amount = rate for considered_salary
        }
    """.trimIndent()

    @Test
    fun parseTerziarioExample() {
        assertCodeParsedWithoutErrors(terziarioExample)
    }

    @Test
    fun parseGlaExample() {
        assertCodeParsedWithoutErrors(glaExample)
    }
}