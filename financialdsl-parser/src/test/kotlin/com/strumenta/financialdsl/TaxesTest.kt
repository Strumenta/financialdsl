package com.strumenta.financialdsl

import com.strumenta.financialdsl.interpreter.DecimalValue
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

    fun assertIresAmount(grossProfit: String, expectedValue: Double, year: Int) {
        val model = Parser().parse("""
            company type SRL {
                gross_profit is amount
                net_production is amount = gross_profit + personnel_costs
                personnel_costs is amount
            }

            Strumenta is SRL {
                city = Torino
                owners = [Federico at 66%, Gabriele at 34%]
                gross_profit is amount = $grossProfit
            }

            tax IRES on SRL {
                // In maniera grossolana ma abbastanza precisa si puo' calcolare che l'utile tassabile e' circa il 20% piu' grande
                // dell'utile lordo.
                taxable =  120% of gross_profit
                rate = @{before 2017} 27.5%
                       @{since 2017} 24%
            }""".trimIndent())
        assertEquals(true, model.correct, model.errors.toString())
        val res = model.ast!!.evaluate(YearlyPeriodValue(year), emptyMap())
        val tax = res.tax("Strumenta", "IRES")
        assertEquals(expectedValue, tax.amount)
    }

    fun assertIresTaxable(grossProfit: String, expectedValue: Double, year: Int) {
        val model = Parser().parse("""
            company type SRL {
                gross_profit is amount
                net_production is amount = gross_profit + personnel_costs
                personnel_costs is amount
            }

            Strumenta is SRL {
                city = Torino
                owners = [Federico at 66%, Gabriele at 34%]
                gross_profit is amount = $grossProfit
            }

            tax IRES on SRL {
                // In maniera grossolana ma abbastanza precisa si puo' calcolare che l'utile tassabile e' circa il 20% piu' grande
                // dell'utile lordo.
                taxable =  120% of gross_profit
                rate = @{before 2017} 27.5%
                       @{since 2017} 24%
            }""".trimIndent())
        assertEquals(true, model.correct, model.errors.toString())
        val res = model.ast!!.evaluate(YearlyPeriodValue(year), emptyMap())
        val tax = res.tax("Strumenta", "IRES")
        assertEquals(expectedValue, (tax.get("taxable") as DecimalValue).value)
    }

    @Test
    fun irpefWithZeroIncome() {
        assertIrpef("0", 0.0)
    }

    @Test
    fun irpefInFirstBracket() {
        assertIrpef("5,000", 1231.0)
    }

    @Test
    fun irpefInMultipleBrackets() {
        // 31870 + 626.64 + 2425.9
        assertIrpef("90,000", 34922.54)
    }

    @Test
    fun iresAmountWithProfitZeroIn2016() {
        assertIresAmount("0", 0.0, 2016)
    }

    @Test
    fun iresAmountWithProfitZeroIn2017() {
        assertIresAmount("0", 0.0, 2017)
    }

    @Test
    fun iresAmountWithProfitGreaterThanZeroIn2016() {
        assertIresAmount("100,000", 27500.0, 2016)
    }

    @Test
    fun iresAmountWithProfitGreaterThanZeroIn2017() {
        assertIresAmount("100,000", 24000.0, 2017)
    }

    @Test
    fun iresTaxableWithProfitZeroIn2016() {
        assertIresTaxable("0", 0.0, 2016)
    }

    @Test
    fun iresTaxableWithProfitZeroIn2017() {
        assertIresTaxable("0", 0.0, 2017)
    }

    @Test
    fun iresTaxableWithProfitGreaterThanZeroIn2016() {
        assertIresTaxable("100,000", 120000.0, 2016)
    }

    @Test
    fun iresTaxableWithProfitGreaterThanZeroIn2017() {
        assertIresTaxable("100,000", 120000.0, 2017)
    }
}