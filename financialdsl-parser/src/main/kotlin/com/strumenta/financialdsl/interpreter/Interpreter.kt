package com.strumenta.financialdsl.interpreter

import com.strumenta.financialdsl.model.*
import java.sql.Ref

abstract class Period

class YearlyPeriod(val year: Int) : Period()

abstract class EntityValues(open val name: String, open val fieldValues: Map<String, Value>)

data class Percentage(val value: Float) : Value

data class SharesMap(val shares: Map<EntityRef, Percentage>) : Value

data class PersonValues(override val name: String, override val fieldValues: Map<String, Value>) : EntityValues(name, fieldValues)

data class CompanyValues(override val name: String, override val fieldValues: Map<String, Value>, val type: CompanyType) : EntityValues(name, fieldValues) {
    val ownership
        get() = fieldValues["owners"] as SharesMap
}

data class EvaluationResult(val companies: List<CompanyValues>, val persons: List<PersonValues>)


class EvaluationContext {
    private val entityRefs = HashMap<String, EntityRef>()
    fun entityRef(name: String) = entityRefs.computeIfAbsent(name) { EntityRef(name, this) }
}

fun FinancialDSLFile.evaluate(period: Period) : EvaluationResult {
    val ctx = EvaluationContext()
    return EvaluationResult(
            this.companies.map { it.evaluateCompany(period, ctx) },
            this.persons.map { it.evaluatePerson(period, ctx) }
    )
}

private fun Entity.evaluatePerson(period: Period, ctx: EvaluationContext): PersonValues {
    return PersonValues(this.name, this.fields.map { it.name to it.value!!.evaluate(period, ctx) }.toMap())
}

private fun Entity.evaluateCompany(period: Period, ctx: EvaluationContext): CompanyValues {
    val ownership = this.field("owners").value!!.evaluate(period, ctx) as SharesMap
    return CompanyValues(
            this.name,
            this.fields.map { it.name to it.value!!.evaluate(period, ctx) }.toMap(),
            (this.type as CompanyTypeRef).ref.referred!!)
}

private fun Expression.evaluate(period: Period, ctx: EvaluationContext): Value {
    return when (this) {
        is SharesMapExpr -> SharesMapValue(this.shares.map {
            it.owner.evaluate(period, ctx) as EntityRef to it.shares.evaluate(period, ctx) as PercentageValue }.toMap())
        is ReferenceExpr -> {
            val target = this.name.referred!!
            TODO("ReferenceExpr to ${target.javaClass.canonicalName}")
        }
        else -> TODO(this.javaClass.canonicalName)
    }
}
