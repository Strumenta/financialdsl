package com.strumenta.financialdsl.parser

import com.strumenta.financialdsl.FinancialDSLParser
import com.strumenta.financialdsl.FinancialDSLParser.*
import com.strumenta.financialdsl.model.*
import me.tomassetti.kolasu.mapping.setParentsForSubTree
import me.tomassetti.kolasu.mapping.toPosition
import me.tomassetti.kolasu.model.ReferenceByName

fun FinancialDSLFileContext.toAst() : FinancialDSLFile = FinancialDSLFile(
        this.declarations.map { it.toAst() },
        toPosition()).apply { this.setParentsForSubTree() }

fun TopLevelDeclarationContext.toAst(): TopLevelDeclaration {
    return when (this) {
        is CitiesDeclContext -> CitiesList(
                this.findCitiesDeclaration()!!.cities.map { it.toAst() },
                ReferenceByName(this.findCitiesDeclaration()!!.region!!.text!!),
                toPosition())
        is RegionsDeclContext -> RegionsList(
                this.findRegionsDeclaration()!!.regions.map { it.toAst() },
                ReferenceByName(this.findRegionsDeclaration()!!.country!!.text!!),
                toPosition())
        is CountriesDeclContext -> CountriesList(
                this.findCountriesDeclaration()!!.countries.map { it.toAst() },
                toPosition())
        is CompanyTypeDeclContext -> this.findCompanyTypeDeclaration()!!.let { CompanyType(it.name!!.text!!, toPosition()) }
        is EntityDeclContext -> this.findEntityDeclaration()!!.let { Entity(
                it.name!!.text!!,
                it.findEntityType()!!.toAst(),
                it.stmts.map { it.toAst() },
                toPosition()) }
        is TaxDeclContext -> this.findTaxDeclaration()!!.let { Tax(
                it.name!!.text!!,
                it.findEntityType()!!.toAst(),
                toPosition()) }
        else -> TODO(this.javaClass.canonicalName)
    }
}

private fun EntityDeclarationStmtContext.toAst(): EntityField {
    return EntityField(this.name!!.text!!, this.value?.toAst())
}

private fun ExpressionContext.toAst(): Expression {
    return when (this) {
        else -> TODO(this.javaClass.canonicalName)
    }
}

private fun EntityTypeContext.toAst() = when (this) {
    is PersonEntityContext -> PersonTypeRef(toPosition())
    is CompanyTypeEntityContext -> CompanyTypeRef(ReferenceByName(this.ID().text), toPosition())
    else -> TODO()
}

fun CityDeclarationContext.toAst() = City(this.name!!.text!!)

fun RegionDeclarationContext.toAst() = Region(this.name!!.text!!)

fun CountryDeclarationContext.toAst() = Country(this.name!!.text!!, this.eu != null)


