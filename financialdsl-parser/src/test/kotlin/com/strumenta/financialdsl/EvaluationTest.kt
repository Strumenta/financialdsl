package com.strumenta.financialdsl

import com.strumenta.financialdsl.interpreter.IntValue
import com.strumenta.financialdsl.interpreter.YearlyPeriodValue
import com.strumenta.financialdsl.interpreter.evaluate
import kotlin.test.Test
import kotlin.test.assertEquals
import com.strumenta.financialdsl.model.Parser

class EvaluationTest : AbstractTest(){
    @Test
    fun evaluateMonthlyValueInYear() {
        val model = Parser().parse("""
            Federico is person {
            net_compensation = @{before july 2018} 0 monthly
                               @{since july 2018} 2K monthly -> contributes to income
}
        """.trimIndent())
        val res = model.ast!!.evaluate(YearlyPeriodValue(2018), emptyMap())
        assertEquals(IntValue(12_000), res.entity("Federico").get("net_compensation"))
    }
}