package com.strumenta.financialdsl

import com.strumenta.financialdsl.interpreter.DecimalValue
import com.strumenta.financialdsl.interpreter.YearlyPeriodValue
import com.strumenta.financialdsl.interpreter.evaluate
import com.strumenta.financialdsl.model.Parser
import kotlin.test.Test
import kotlin.test.assertEquals

class TaxesTest : AbstractTest(){

    @Test
    fun irpefWithZeroIncome() {
        val model = Parser().parse("""
            Federico is person {
                city = Torino
                income = 0
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
                taxable = income
                rate = national_rate + regional_rate + town_rate
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
        val res = model.ast!!.evaluate(YearlyPeriodValue(2018), emptyMap())
        val tax = res.tax("Federico", "IRPEF")
        assertEquals(0.0, tax.amount)
    }

}