package com.strumenta.financialdsl.model

import me.tomassetti.kolasu.model.*

abstract class TopLevelDeclaration(override val position: Position?) : Node(position)

data class FinancialDSLFile(val declarations : List<TopLevelDeclaration>,
                        override val position: Position? = null) : Node(position) {

    @Derived val entities : List<Entity>
        get() = declarations.filterIsInstance(Entity::class.java)

    @Derived val companies : List<Entity>
        get() = entities.filter { it.type.isCompany() }

    @Derived val persons : List<Entity>
        get() = entities.filter { it.type.isPerson() }

    @Derived val companyTypes : List<CompanyType>
        get() = declarations.filterIsInstance(CompanyType::class.java)
}

data class CountriesList(val countries: List<Country>,
                         override val position: Position? = null)
    : TopLevelDeclaration(position)
data class Country(override val name : String, val eu: Boolean, override val position: Position? = null) : Node(position), Named

data class RegionsList(val regions: List<Region>,
                       val country: ReferenceByName<Country>,
                         override val position: Position? = null)
    : TopLevelDeclaration(position)
data class Region(override val name : String, override val position: Position? = null) : Node(position), Named {
    fun country() = (this.parent as RegionsList).country.referred!!
}

data class CitiesList(val cities: List<City>,
                       val region: ReferenceByName<Region>,
                       override val position: Position? = null)
    : TopLevelDeclaration(position)
data class City(val name : String, override val position: Position? = null) : Node(position) {
    fun country() = region().country()
    fun region() = (this.parent as CitiesList).region.referred!!
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
                  override val position: Position? = null) : TopLevelDeclaration(position), Named {
    fun field(name: String) : EntityField = fields.first { it.name == name }
}

abstract class Expression(override val position: Position? = null) : Node(position)

data class EntityField(override val name: String,
                       val value: Expression?, override val position: Position? = null) : Node(position), Named

data class Tax(override val name: String,
                  val target: EntityTypeRef,
                  override val position: Position? = null) : TopLevelDeclaration(position), Named