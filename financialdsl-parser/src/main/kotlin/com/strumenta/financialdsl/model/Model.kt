package com.strumenta.financialdsl.model

import com.strumenta.financialdsl.interpreter.*
import me.tomassetti.kolasu.model.*
import me.tomassetti.kolasu.model.Position
import java.time.Month
import java.util.*

abstract class TopLevelDeclaration(override val position: Position?) : Node(position)

data class EntityRef(override val name: String) : Named
data class EntityFieldRef(val entityName: String, override val name: String) : Named
data class TaxFieldRef(val taxName: String, override val name: String) : Named

interface Scope {
    fun candidatesForValues() : List<Named>
}

data class FinancialDSLFile(val declarations : List<TopLevelDeclaration>,
                            override val position: Position? = null) : Node(position), Scope {

    @Derived val entities : List<Entity>
        get() = declarations.filterIsInstance(Entity::class.java)

    @Derived val companies : List<Entity>
        get() = entities.filter { it.type.isCompany() }

    @Derived val persons : List<Entity>
        get() = entities.filter { it.type.isPerson() }

    @Derived val companyTypes : List<CompanyType>
        get() = declarations.filterIsInstance(CompanyType::class.java)

    @Derived val countriesLists : List<CountriesList>
        get() = declarations.filterIsInstance(CountriesList::class.java)

    @Derived val regionsLists : List<RegionsList>
        get() = declarations.filterIsInstance(RegionsList::class.java)

    @Derived val citiesLists : List<CitiesList>
        get() = declarations.filterIsInstance(CitiesList::class.java)

    @Derived val countries : List<Country>
        get() = countriesLists.foldRight(emptyList()) { el, acc -> el.countries + acc }

    @Derived val regions : List<Region>
        get() = regionsLists.foldRight(emptyList()) { el, acc -> el.regions + acc }

    @Derived val cities : List<City>
        get() = citiesLists.foldRight(emptyList()) { el, acc -> el.cities + acc }

    @Derived val taxes : List<Tax>
        get() = declarations.filterIsInstance(Tax::class.java)

    override fun candidatesForValues(): List<Named> {
        return listOf<List<Named>>(entities, countries, regions, cities).flatten()
    }

    fun entity(name: String): Entity {
        return entities.first { it.name == name }
    }

    fun tax(name: String): Tax {
        return taxes.first { it.name == name }
    }
}

data class CountriesList(val countries: List<Country>,
                         override val position: Position? = null)
    : TopLevelDeclaration(position)

data class Country(override val name : String, val eu: Boolean, override val position: Position? = null) : Node(position), Named {
    @Derived
    val regions: List<Region>
        get() = ancestor(FinancialDSLFile::class.java)!!.regions.filter { it.country == this }
    @Derived
    val cities
        get() = ancestor(FinancialDSLFile::class.java)!!.cities.filter { it.country == this }
}

data class RegionsList(val regions: List<Region>,
                       val country: ReferenceByName<Country>,
                       override val position: Position? = null)
    : TopLevelDeclaration(position)

data class Region(override val name : String, override val position: Position? = null) : Node(position), Named {
    @Derived
    val country
        get() = (this.parent as RegionsList).country.referred!!
    @Derived
    val cities
        get() = this.ancestor(FinancialDSLFile::class.java)!!.cities.filter { it.region == this }
}

data class CitiesList(val cities: List<City>,
                      val region: ReferenceByName<Region>,
                      override val position: Position? = null)
    : TopLevelDeclaration(position)

data class City(override val name : String, override val position: Position? = null) : Node(position), Named {
    @Derived
    val country
            get() = region.country
    @Derived
    val region
            get() = (this.parent as CitiesList).region.referred!!
}

abstract class EntityTypeRef(override val position: Position? = null) : Node(position) {
    abstract fun isCompany(): Boolean
    fun isPerson() = !isCompany()
}

class PersonTypeRef(override val position: Position? = null) : EntityTypeRef(position) {
    override fun isCompany() = false
}

data class CompanyTypeRef(val ref: ReferenceByName<CompanyType>, override val position: Position? = null) : EntityTypeRef(position) {
    override fun isCompany() = true
}

data class CompanyType(override val name: String,
                       override val position: Position? = null) : TopLevelDeclaration(position), Named

data class Entity(override val name: String,
                  val type: EntityTypeRef,
                  val fields: List<EntityField>,
                  override val position: Position? = null) : TopLevelDeclaration(position), Named, Scope {
    @Derived
    val fieldNames: List<String>
        get() = fields.map { it.name }
    @Derived
    val isPerson: Boolean
        get() = type.isPerson()
    @Derived
    val isCompany: Boolean
        get() = type.isCompany()

    fun hasField(name: String) : Boolean = fields.firstOrNull { it.name == name } != null
    fun field(name: String) : EntityField = fields.firstOrNull { it.name == name } ?: throw IllegalArgumentException("Cannot find field $name in entity ${this.name}")

    override fun candidatesForValues(): List<Named> {
        return fields.map { EntityFieldRef(name, it.name) }
    }
}

data class EntityField(override val name: String,
                       val value: Expression?,
                       val isSum: Boolean,
                       val isParameter: Boolean,
                       val contribution: Contribution?,
                       override val position: Position? = null) : Node(position), Named

abstract class Contribution(override val position: Position? = null)
    : Node(position)

data class SameEntityContribution(val fieldName: String, override val position: Position? = null) : Contribution(position)
data class OtherEntityContribution(val fieldName: String, val entityName: String, override val position: Position? = null) : Contribution(position)
data class OwnersContribution(val fieldName: String, override val position: Position? = null) : Contribution(position)


data class Tax(override val name: String,
               val target: EntityTypeRef,
               val fields: List<EntityField>,
               override val position: Position? = null) : TopLevelDeclaration(position), Named, Scope {

    fun isApplicableTo(entity: Entity): Boolean {
        return when {
            this.target.isPerson() -> entity.isPerson
            this.target.isCompany() -> entity.isCompany && entity.name == (this.target as CompanyTypeRef).ref.name
            else -> TODO()
        }
    }

    fun amountToPay(entity: Entity, ctx: EvaluationContext, period: PeriodValue): Double {
        val taxValues = ctx.taxValues(name, entity.name, period)
        if (hasField("amount")) {
            return (taxValues.get("amount") as DecimalValue).value
        }
        val taxable = taxValues.get("taxable").toDecimal()
        val rate = taxValues.get("rate") as PercentageValue
        return (multiplyValues(taxable, rate) as DecimalValue).value
    }

    fun hasField(name: String) : Boolean = fields.firstOrNull { it.name == name } != null
    fun field(name: String) : EntityField = fields.firstOrNull { it.name == name } ?: throw IllegalArgumentException("Cannot find field $name in entity ${this.name}")

    override fun candidatesForValues(): List<Named> {
        return fields.map { TaxFieldRef(name, it.name) }
    }
}

abstract class Period(override val position: Position? = null) : Node(position)
class BeforePeriod(val date: Date, override val position: Position? = null) : Period(position)
class AfterPeriod(val date: Date, override val position: Position? = null) : Period(position)
class SincePeriod(val date: Date, override val position: Position? = null) : Period(position)

abstract class Date(override val position: Position? = null) : Node(position)
data class Year(val value: Int, override val position: Position? = null) : Node(position)
class MonthDate(val month: Month, val year: Year, override val position: Position? = null) : Date(position)
class YearDate(val year: Year, override val position: Position? = null) : Date(position)

///
/// Expressions
///

abstract class Expression(override val position: Position? = null) : Node(position)

abstract class BinaryExpression(open val left: Expression, open val right: Expression, override val position: Position? = null) : Expression(position)

data class TimeExpression(val clauses: List<TimeClause>, override val position: Position? = null) : Expression(position)
data class TimeClause(val period: Period, val value: Expression, override val position: Position? = null) : Node(position)

enum class Periodicity {
    MONTHLY,
    YEARLY
}

data class PeriodicExpression(val value: Expression, val periodicity: Periodicity, override val position: Position? = null) : Expression(position)

data class IntLiteral(val value: Long, override val position: Position? = null) : Expression(position)
data class DecimalLiteral(val value: Double, override val position: Position? = null) : Expression(position)
data class PercentageLiteral(val value: Double, override val position: Position? = null) : Expression(position)

data class ReferenceExpr(val name: ReferenceByName<Named>, override val position: Position? = null) : Expression(position)

data class SharesMapExpr(val shares: List<Share>, override val position: Position? = null) : Expression(position)

class Share(val owner: String, val shares: Expression, override val position: Position? = null) : Expression(position)

data class FieldAccessExpr(val scope: Expression, val fieldName: String, override val position: Position? = null) : Expression(position)

data class SumExpr(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression(left, right, position)

data class BracketsExpr(val entries: List<BracketEntry>, override val position: Position? = null) : Expression(position)
data class BracketEntry(val range: Range, val value: Expression, override val position: Position? = null) : Expression(position)

abstract class Range(override val position: Position? = null) : Node(position) {
    abstract fun withoutPosition() : Range
}
data class ToRange(val upperLimit: Expression, override val position: Position? = null) : Range(position) {
    override fun withoutPosition() = ToRange(upperLimit)
}
data class AboveRange(override val position: Position? = null) : Range(position) {
    override fun withoutPosition() = AboveRange()
}

data class WhenExpr(val clauses: List<WhenExprClause>, override val position: Position? = null) : Expression(position)
data class WhenExprClause(val condition: Expression, val value: Expression, override val position: Position? = null) : Node(position)

data class EqualityExpr(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression(left, right, position)

data class BracketsApplicationExpr(val brackets: Expression, val value: Expression, override val position: Position? = null) : Expression(position)