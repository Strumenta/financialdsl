package com.strumenta.financialdsl.interpreter

import com.strumenta.financialdsl.model.*

abstract class PeriodValue {
    open val isYearly : Boolean
        get() = false
}

class YearlyPeriodValue(val year: Int) : PeriodValue() {
    override val isYearly: Boolean
        get() = true
}

data class BeforePeriodValue(val date: Value) : PeriodValue()
data class SincePeriodValue(val date: Value) : PeriodValue()
data class AfterPeriodValue(val date: Value) : PeriodValue()

abstract class EntityValues(open val name: String, open val fieldValues: Map<String, Value>)

data class Percentage(val value: Double) : Value

data class SharesMap(val shares: Map<EntityRef, Percentage>) : Value

data class PersonValues(override val name: String, override val fieldValues: Map<String, Value>) : EntityValues(name, fieldValues)

data class CompanyValues(override val name: String, override val fieldValues: Map<String, Value>, val type: CompanyType) : EntityValues(name, fieldValues) {
    val ownership
        get() = fieldValues["owners"] as SharesMap
}

data class EvaluationResult(val companies: List<CompanyValues>, val persons: List<PersonValues>)


class EvaluationContext(val file: FinancialDSLFile, val period: PeriodValue) {
    private val entityRefs = HashMap<String, EntityValue>()
    fun entityRef(name: String) = entityRefs.computeIfAbsent(name) { EntityValue(file.entity(name), this) }
}

fun FinancialDSLFile.evaluate(periodValue: PeriodValue) : EvaluationResult {
    val ctx = EvaluationContext(this, periodValue)
    return EvaluationResult(
            this.companies.map { it.evaluateCompany(ctx) },
            this.persons.map { it.evaluatePerson(ctx) }
    )
}

private fun Entity.evaluatePerson(ctx: EvaluationContext): PersonValues {
    return PersonValues(this.name, this.fields.map {
        it.name to (it.value?.evaluate(ctx) ?: NoValue)
    }.toMap())
}

private fun Entity.evaluateCompany(ctx: EvaluationContext): CompanyValues {
    return CompanyValues(
            this.name,
            this.fields.map { it.name to (it.value?.evaluate(ctx) ?: NoValue) }.toMap(),
            (this.type as CompanyTypeRef).ref.referred!!)
}

fun Expression.evaluate(ctx: EvaluationContext): Value {
    return when (this) {
        is SharesMapExpr -> SharesMapValue(this.shares.map {
            it.owner.evaluate(ctx) as EntityValue to it.shares.evaluate(ctx) as PercentageValue }.toMap())
        is ReferenceExpr -> {
            val target = this.name.referred!!
            when (target) {
                is Entity -> ctx.entityRef(target.name)
                is EntityFieldRef -> ctx.entityRef(target.entityName).get(target.name)
                else -> TODO("ReferenceExpr to ${target.javaClass.canonicalName}")
            }
        }
        is PercentageLiteral -> PercentageValue(this.value)
        is IntLiteral -> IntValue(this.value)
        is TimeExpression -> {
            val type = this.type()
            when (type) {
                is PeriodicType -> {
                    when {
                        ctx.period.isYearly && type.periodicity == Periodicity.MONTHLY -> {
                            if (type.baseType == IntType) {
                                var sum = 0L
                                return IntValue(sum)
                            } else {
                                TODO()
                            }
                        }
                        else -> TODO("Periodic Type for period ${ctx.period} and periodicity ${type.periodicity}")
                    }
                }
                else -> TODO("TimeExpression of type $type")
            }
            //TimeValue(this.clauses.map { it.evaluate(ctx) })
        }
        is PeriodicExpression -> PeriodicValue(this.value.evaluate(ctx), this.periodicity)
        else -> TODO(this.javaClass.canonicalName)
    }
}

private fun TimeClause.evaluate(ctx: EvaluationContext): TimeValueEntry {
    return TimeValueEntry(this.period.evaluate(ctx), this.value.evaluate(ctx))
}

private fun Period.evaluate(ctx: EvaluationContext): PeriodValue {
    return when (this) {
        is BeforePeriod -> BeforePeriodValue(this.date.evaluate(ctx))
        is SincePeriod -> SincePeriodValue(this.date.evaluate(ctx))
        is AfterPeriod -> AfterPeriodValue(this.date.evaluate(ctx))
        else -> TODO(this.javaClass.canonicalName)
    }
}

private fun Date.evaluate(ctx: EvaluationContext): Value {
    return when (this) {
        is MonthDate -> MonthDateValue(this.month, this.year)
        else -> TODO(this.javaClass.canonicalName)
    }
}
