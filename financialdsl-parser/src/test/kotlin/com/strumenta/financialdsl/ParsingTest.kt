package com.strumenta.financialdsl

import kotlin.test.Test
import kotlin.test.assertEquals

class ParsingTest : AbstractTest() {

    @Test
    fun parseCompanyTypesSimplified() {
        val root = assertExampleParsedWithoutErrors("company_types_1")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCompanyTypes() {
        val root = assertExampleParsedWithoutErrors("company_types")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCompany() {
        val root = assertExampleParsedWithoutErrors("company")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCompanyShare() {
        val root = assertExampleParsedWithoutErrors("company_share")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parsePerson() {
        val root = assertExampleParsedWithoutErrors("person")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parsePersonWithShare() {
        val root = assertExampleParsedWithoutErrors("person2")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseIres() {
        val root = assertExampleParsedWithoutErrors("ires")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseIrap() {
        val root = assertExampleParsedWithoutErrors("irap")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseIrpef() {
        val root = assertExampleParsedWithoutErrors("irpef")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseRegions() {
        val root = assertExampleParsedWithoutErrors("regions")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCities() {
        val root = assertExampleParsedWithoutErrors("cities")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCountries() {
        val root = assertExampleParsedWithoutErrors("countries")
        assertEquals(1, root.declarations.size)
    }

    @Test
    fun parseCompleteExample() {
        val root = assertExampleParsedWithoutErrors("example1")
        assertEquals(9, root.declarations.size)
    }
}