package com.strumenta.financialdsl

import com.strumenta.financialdsl.interpreter.YearlyPeriodValue
import com.strumenta.financialdsl.interpreter.evaluate
import com.strumenta.financialdsl.model.Parser
import kotlin.test.Test
import kotlin.test.assertEquals

class TaxesTest : AbstractTest(){

    fun assertIrpef(income: String, expectedValue: Double) {
        val model = Parser().parse("""
            Federico is person {
                city = Torino
                income = $income
            }

            countries {
                Italy EU
                Germany EU
                France EU
                Switzerland
                Japan
            }

            regions of Italy {
                Piedmont
                Lombardy
            }

            cities of Piedmont {
                Torino
            }

            tax IRPEF on person {
                amount = (national_rate for taxable) + (regional_rate for taxable) + (town_rate for taxable)
                taxable = income
                national_rate = brackets [to 15K] -> 23%,
                                         [to 28K] -> 27%,
                                         [to 55K] -> 38%,
                                         [to 75K] -> 41%,
                                         [above]  -> 43%
                town_rate = when town=Torino brackets [to 11,670] -> 0%,
                                                      [above] -> 0.8%
                regional_rate = when region=Piedmont brackets [to 15K] -> 1.62%,
                                                              [to 28K] -> 2.13%,
                                                              [to 55K] -> 2.75%,
                                                              [to 75K] -> 3.32%,
                                                              [above]  -> 3.33%
            }
        """.trimIndent())
        assertEquals(true, model.correct, model.errors.toString())
        val res = model.ast!!.evaluate(YearlyPeriodValue(2018), emptyMap())
        val tax = res.tax("Federico", "IRPEF")
        assertEquals(expectedValue, tax.amount)
    }

    @Test
    fun irpefWithZeroIncome() {
        assertIrpef("0", 0.0)
    }

    @Test
    fun irpefWithInFirstBracket() {
        assertIrpef("5,000", 1231.0)
    }

}