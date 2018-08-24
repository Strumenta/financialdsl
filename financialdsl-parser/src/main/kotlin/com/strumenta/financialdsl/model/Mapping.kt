package com.strumenta.financialdsl.model

import com.strumenta.financialdsl.FinancialDSLParser.*
import me.tomassetti.kolasu.mapping.setParentsForSubTree
import me.tomassetti.kolasu.mapping.toPosition
import me.tomassetti.kolasu.model.ReferenceByName
import java.time.Month

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
        is EntityDeclContext -> this.findEntityDeclaration()!!.let {
            Entity(
                    it.name!!.text!!,
                    it.findEntityType()!!.toAst(),
                    it.stmts.map { it.toAst() },
                    toPosition())
        }
        is TaxDeclContext -> this.findTaxDeclaration()!!.let {
            Tax(
                    it.name!!.text!!,
                    it.findEntityType()!!.toAst(),
                    toPosition())
        }
        else -> TODO(this.javaClass.canonicalName)
    }
}

private fun EntityDeclarationStmtContext.toAst(): EntityField {
    return EntityField(this.name!!.text!!, this.value?.toAst(),
            this.SUM() != null,
            this.PARAMETER() != null,
            if (this.contributed == null) null else Contribution(this.contributed!!.toAst(), this.SHARE() != null),
            toPosition())
}

private fun ExpressionContext.toAst(): Expression {
    return when (this) {
        is TimeExprContext -> TimeExpression(this.findValueInTime().map { it.toAst() }, toPosition())
        is PeriodicExprContext -> PeriodicExpression(
                this.findExpression()!!.toAst(),
                Periodicity.valueOf(this.PERIODICITY()!!.text.toUpperCase()),
                toPosition())
        is IntLiteralContext -> {
            val s = this.INTLIT()!!.text
            val value = if (s.endsWith("K")) {
                s.removeSuffix("K").toLong() * 1000
            } else {
                s.toLong()
            }
            IntLiteral(value, toPosition())
        }
        is DecimalLiteralContext -> {
            val s = this.DECLIT()!!.text
            val value = if (s.endsWith("K")) {
                s.removeSuffix("K").toDouble() * 1000
            } else {
                s.toDouble()
            }
            DecimalLiteral(value, toPosition())
        }
        is PercentageLiteralContext -> {
            val s = this.PERCLIT()!!.text.removeSuffix("%")
            val value = if (s.endsWith("K")) {
                s.removeSuffix("K").toDouble() * 1000
            } else {
                s.toDouble()
            }
            PercentageLiteral(value, toPosition())
        }
        is ReferenceExprContext -> ReferenceExpr(ReferenceByName(this.name!!.text!!), toPosition())
        is SharesMapExprContext -> SharesMapExpr(this.entries.map { it.toAst() }, toPosition())
        is FieldAccessExprContext -> FieldAccessExpr(this.findExpression()!!.toAst(), this.fieldName!!.text!!)
        else -> TODO(this.javaClass.canonicalName)
    }
}

private fun ShareEntryContext.toAst() = Share(this.owner!!.toAst(), this.value!!.toAst(), toPosition())

private fun ValueInTimeContext.toAst() = TimeClause(this.findTimeClause()!!.toAst(), this.findExpression()!!.toAst(), toPosition())

private fun TimeClauseContext.toAst(): Period {
    return when {
        this.BEFORE() != null -> BeforePeriod(this.findDate()!!.toAst(), toPosition())
        this.SINCE() != null -> SincePeriod(this.findDate()!!.toAst(), toPosition())
        this.AFTER() != null -> AfterPeriod(this.findDate()!!.toAst(), toPosition())
        else -> TODO(this.javaClass.canonicalName)
    }
}

private fun DateContext.toAst(): Date {
    return when (this) {
        is MonthDateContext -> MonthDate(Month.valueOf(this.MONTH()!!.text.toUpperCase()), Year(this.year!!.text!!.toInt(), toPosition()))
        else -> TODO(this.javaClass.canonicalName)
    }
}

private fun EntityTypeContext.toAst() = when (this) {
    is PersonEntityContext -> PersonTypeRef(toPosition())
    is CompanyTypeEntityContext -> CompanyTypeRef(ReferenceByName(this.ID()!!.text), toPosition())
    else -> TODO()
}

fun CityDeclarationContext.toAst() = City(this.name!!.text!!)

fun RegionDeclarationContext.toAst() = Region(this.name!!.text!!)

fun CountryDeclarationContext.toAst() = Country(this.name!!.text!!, this.eu != null)

