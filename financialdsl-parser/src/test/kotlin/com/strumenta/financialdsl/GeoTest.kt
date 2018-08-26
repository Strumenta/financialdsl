package com.strumenta.financialdsl

import com.strumenta.financialdsl.interpreter.YearlyPeriodValue
import com.strumenta.financialdsl.interpreter.evaluate
import com.strumenta.financialdsl.model.Parser
import kotlin.test.Test
import kotlin.test.assertEquals

class GeoTest : AbstractTest(){

    @Test
    fun countriesAreListed() {
        val model = Parser().parse("""
            countries {
                Italy EU
                Germany EU
                France EU
                Switzerland
                Japan
            }
        """.trimIndent())
        val res = model.ast!!.evaluate(YearlyPeriodValue(2018), emptyMap())
        assertEquals(5, res.countries.size)
        assertEquals(true, res.country("Italy").eu)
        assertEquals(false, res.country("Japan").eu)
    }

    @Test
    fun regionsAreListed() {
        val model = Parser().parse("""
            countries {
                Italy EU
                Germany EU
                France EU
                Switzerland
                Japan
            }

            regions of Italy {
                Piedmont
            }
        """.trimIndent())
        val res = model.ast!!.evaluate(YearlyPeriodValue(2018), emptyMap())
        assertEquals(0, res.regionsOf("Germany").size)
        assertEquals(1, res.regionsOf("Italy").size)
        assertEquals("Italy", res.region("Piedmont").country.name)
        assertEquals(1, res.country("Italy").regions.size)
    }

    @Test
    fun citiesAreListed() {
        val model = Parser().parse("""
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
        """.trimIndent())
        val res = model.ast!!.evaluate(YearlyPeriodValue(2018), emptyMap())
        assertEquals("Italy", res.city("Torino").country.name)
        assertEquals("Piedmont", res.city("Torino").region.name)
        assertEquals(1, res.region("Piedmont").cities.size)
        assertEquals(0, res.region("Lombardy").cities.size)
        assertEquals(1, res.country("Italy").cities.size)
        assertEquals(0, res.country("France").cities.size)
    }
}