package com.strumenta.financialdsl

import com.strumenta.kotlinmultiplatform.BitSet
import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.atn.ATNConfigSet
import org.antlr.v4.kotlinruntime.dfa.DFA
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ParsingTest : AbstractTest() {

    @Test
    fun parseCompanyTypesSimplified() {
        val root = assertParsedWithoutErrors("company_types_1")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCompanyTypes() {
        val root = assertParsedWithoutErrors("company_types")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCompany() {
        val root = assertParsedWithoutErrors("company")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCompanyShare() {
        val root = assertParsedWithoutErrors("company_share")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parsePerson() {
        val root = assertParsedWithoutErrors("person")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parsePersonWithShare() {
        val root = assertParsedWithoutErrors("person2")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseIres() {
        val root = assertParsedWithoutErrors("ires")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseIrap() {
        val root = assertParsedWithoutErrors("irap")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseIrpef() {
        val root = assertParsedWithoutErrors("irpef")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseRegions() {
        val root = assertParsedWithoutErrors("regions")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCities() {
        val root = assertParsedWithoutErrors("cities")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCountries() {
        val root = assertParsedWithoutErrors("countries")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCompleteExample() {
        val root = assertParsedWithoutErrors("example1")
        assertEquals(9, root.declarations.size)
    }
}