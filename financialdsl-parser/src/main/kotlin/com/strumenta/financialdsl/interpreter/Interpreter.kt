package com.strumenta.financialdsl.interpreter

import com.strumenta.financialdsl.model.*

abstract class PeriodValue {
    abstract fun granularity(): Granularity

    open val isYearly : Boolean
        get() = false
}

class YearlyPeriodValue(val year: Int) : PeriodValue() {
    override val isYearly: Boolean
        get() = true

    override fun granularity() = Granularity.YEARLY_GRANULARITY
}

data class BeforePeriodValue(val date: Value) : PeriodValue() {
    override fun granularity(): Granularity {
        return date.granularity
    }
}
data class SincePeriodValue(val date: Value) : PeriodValue(){
    override fun granularity(): Granularity {
        return date.granularity
    }
}
data class AfterPeriodValue(val date: Value) : PeriodValue(){
    override fun granularity(): Granularity {
        return date.granularity
    }
}

abstract class EntityValues(open val name: String, open val fieldValues: Map<String, Value>) : ComposedValue(fieldValues.values) {

}

data class Percentage(val value: Double) : ConstantValue(PercentageType)

data class SharesMap(val shares: Map<EntityRef, Percentage>) : ConstantValue(SharesMapType)

data class PersonValues(override val name: String, override val fieldValues: Map<String, Value>) : EntityValues(name, fieldValues) {
    override fun forPeriod(period: PeriodValue): PersonValues {
        return PersonValues(name, fieldValues.mapValues { it.value.forPeriod(period) })
    }
}

data class CompanyValues(override val name: String, override val fieldValues: Map<String, Value>, val companyType: CompanyType) : EntityValues(name, fieldValues) {
    val ownership
        get() = fieldValues["owners"] as SharesMap
    override fun forPeriod(period: PeriodValue): CompanyValues {
        return CompanyValues(name, fieldValues.mapValues { it.value.forPeriod(period) }, companyType)
    }
}

data class EvaluationResult(val companies: List<CompanyValues>, val persons: List<PersonValues>)


class EvaluationContext(val file: FinancialDSLFile) {
    private val entityRefs = HashMap<String, EntityValue>()
    fun entityRef(name: String) = entityRefs.computeIfAbsent(name) { EntityValue(file.entity(name), this) }
}

fun FinancialDSLFile.evaluate(period: PeriodValue) : EvaluationResult {
    val ctx = EvaluationContext(this)
    return EvaluationResult(
            this.companies.map { it.evaluateCompany(ctx).forPeriod(period) },
            this.persons.map { it.evaluatePerson(ctx).forPeriod(period) }
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
//            val type = this.type()
//            when (type) {
//                is PeriodicType -> {
//                    when {
//                        ctx.period.isYearly && type.periodicity == Periodicity.MONTHLY -> {
//                            if (type.baseType == IntType) {
//                                var sum = 0L
//                                return IntValue(sum)
//                            } else {
//                                TODO()
//                            }
//                        }
//                        else -> TODO("Periodic Type for period ${ctx.period} and periodicity ${type.periodicity}")
//                    }
//                }
//                else -> TODO("TimeExpression of type $type")
//            }
            TimeValue(this.clauses.map { it.evaluate(ctx) })
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
