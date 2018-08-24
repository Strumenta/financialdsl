package com.strumenta.financialdsl.interpreter

import com.strumenta.financialdsl.model.*
import com.strumenta.financialdsl.model.Date
import java.sql.Ref
import java.time.Month
import java.util.*

abstract class PeriodValue {
    abstract fun granularity(): Granularity
    abstract fun contains(period: PeriodValue): Boolean

    open val isYearly : Boolean
        get() = false
    open val isMonthly : Boolean
        get() = false
    open val year: Int
        get() = throw UnsupportedOperationException()
    open val month: Month
        get() = throw UnsupportedOperationException()
}

class YearlyPeriodValue(override val year: Int) : PeriodValue() {
    override val isYearly: Boolean
        get() = true

    override fun granularity() = GranularityEnum.YEARLY_GRANULARITY

    override fun toString() = "YearlyPeriodValue($year)"

    override fun contains(period: PeriodValue): Boolean {
        if (period.isYearly) {
            return period.year == this.year
        }
        if (period.isMonthly) {
            return period.year == this.year
        }
        return false
    }
}

class MonthlyPeriodValue(override val month: Month, override val year: Int) : PeriodValue() {
    override val isMonthly: Boolean
        get() = true

    override fun granularity() = GranularityEnum.MONTHLY_GRANULARITY

    override fun toString() = "MonthlyPeriodValue($month $year)"

    override fun contains(period: PeriodValue): Boolean {
        if (period.isMonthly) {
            return period.year == this.year && period.month == this.month
        }
        return false
    }
}

data class BeforePeriodValue(val date: DateValue) : PeriodValue() {
    override fun granularity(): Granularity {
        return date.granularity
    }

    override fun contains(period: PeriodValue): Boolean {
        if (period.isYearly) {
            TODO()
        }
        if (period.isMonthly) {
            if (date.year > period.year) {
                return true
            }
            if (date.year == period.year && date.month > period.month) {
                return true
            }
            return false
        }
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

data class SincePeriodValue(val date: DateValue) : PeriodValue(){
    override fun granularity(): Granularity {
        return date.granularity
    }
    override fun contains(period: PeriodValue): Boolean {
        if (period.isYearly) {
            TODO()
        }
        if (period.isMonthly) {
            if (date.year < period.year) {
                return true
            }
            if (date.year == period.year && date.month <= period.month) {
                return true
            }
            return false
        }
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
data class AfterPeriodValue(val date: DateValue) : PeriodValue(){
    override fun granularity(): Granularity {
        return date.granularity
    }
    override fun contains(period: PeriodValue): Boolean {
        if (period.isYearly) {
            TODO()
        }
        if (period.isMonthly) {
            if (date.year < period.year) {
                return true
            }
            if (date.year == period.year && date.month < period.month) {
                return true
            }
            return false
        }
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

abstract class EntityValues(
        open val ctx : EvaluationContext,
        open val name: String, open val fieldValues: Map<String, Value>) : ComposedValue(EntityType, fieldValues.values) {
}

data class Percentage(val value: Double) : ConstantValue(PercentageType)

data class SharesMap(val shares: Map<EntityRef, Percentage>) : ConstantValue(SharesMapType)

data class PersonValues(override val ctx : EvaluationContext,
                        override val name: String, override val fieldValues: Map<String, Value>) : EntityValues(ctx, name, fieldValues) {
    override fun forPeriod(period: PeriodValue): PersonValues {
        return PersonValues(ctx, name, fieldValues.mapValues { it.value.forPeriod(period) })
    }
}

data class CompanyValues(override val ctx : EvaluationContext,
                         override val name: String,
                         override val fieldValues: Map<String, Value>, val companyType: CompanyType) : EntityValues(ctx, name, fieldValues) {
    val ownership
        get() = fieldValues["owners"] as SharesMap
    override fun forPeriod(period: PeriodValue): CompanyValues {
        return CompanyValues(ctx, name, fieldValues.mapValues { it.value.forPeriod(period) }, companyType)
    }
}

data class EvaluationResult(val companies: List<CompanyValues>, val persons: List<PersonValues>)

object LazyType : Type
object LazyGranularity : Granularity {
    override val value: Int
        get() = throw UnsupportedOperationException()
}

class LazyEntityFieldValue(val entityName: String, val fieldName: String, val ctx: EvaluationContext) : Value {

    private var calculatedValue : Value? = null

    override fun unlazy() = calculatedValue()

    private fun calculatedValue() : Value {
        if (calculatedValue == null) {
            val field = ctx.file.entity(entityName).field(fieldName)
            val explicitValue = field.value
            if (explicitValue != null) {
                calculatedValue = explicitValue.evaluate(ctx)
            } else if (field.isSum) {
                // Iterate through all entities to find all possible contributions
                val contributions = LinkedList<Value>()

                ctx.file.entities.forEach {entity ->
                    entity.fields.filter { it.contribution != null }.forEach {field ->
                        when (field.contribution!!.target) {
                            is ReferenceExpr -> {
                                if (entity.name == entityName && (field.contribution.target as ReferenceExpr).name.name == fieldName) {
                                    contributions.add(ctx.entityRef(entity.name).get(field.name))
                                }
                            }
                            is FieldAccessExpr -> {
                                val fieldAccessExpr = field.contribution.target as FieldAccessExpr
                                if (fieldAccessExpr.scope is ReferenceExpr) {
                                    if (fieldAccessExpr.scope.name.name == entityName && fieldAccessExpr.fieldName == fieldName) {
                                        contributions.add(ctx.entityRef(entity.name).get(field.name))
                                    }
                                } else {
                                    TODO()
                                }
                            }
                            else -> TODO(field.contribution.toString())
                        }

                    }
                }

                if (contributions.isEmpty()) {
                    return NoValue
                } else {
                    val type = contributions.commonSupertypeOfValues()
                    when (type) {
                        is PeriodicType -> {
                            if (type.baseType is IntType) {
                                var result = 0L
                                contributions.forEach {
                                    var v = it.unlazy()
                                    if (v is PeriodicValue) {
                                        if (v.periodicity != type.periodicity) {
                                            TODO()
                                        }
                                        if (v.value is IntValue) {
                                            result += (v.value as IntValue).value
                                        } else {
                                            TODO()
                                        }
                                    } else {
                                        TODO("Not periodic value but $it")
                                    }
                                }
                                return PeriodicValue(IntValue(result), type.periodicity)
                            } else {
                                TODO()
                            }
                        }
                        else -> TODO(type.toString())
                    }
                }
            } else {
                TODO()
            }
        }
        return calculatedValue!!
    }

    override val type: Type
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val granularity: Granularity
        get() = calculatedValue().granularity

    override fun forPeriod(period: PeriodValue): Value {
        return calculatedValue().forPeriod(period)
    }
}


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
    return PersonValues(ctx, this.name, this.fields.map {
        it.name to LazyEntityFieldValue(this.name, it.name, ctx)
    }.toMap())
}

private fun Entity.evaluateCompany(ctx: EvaluationContext): CompanyValues {
    return CompanyValues(
            ctx,
            this.name,
            this.fields.map { it.name to LazyEntityFieldValue(this.name, it.name, ctx) }.toMap(),
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
        is BeforePeriod -> BeforePeriodValue(this.date.evaluate(ctx) as DateValue)
        is SincePeriod -> SincePeriodValue(this.date.evaluate(ctx) as DateValue)
        is AfterPeriod -> AfterPeriodValue(this.date.evaluate(ctx) as DateValue)
        else -> TODO(this.javaClass.canonicalName)
    }
}

private fun Date.evaluate(ctx: EvaluationContext): Value {
    return when (this) {
        is MonthDate -> MonthDateValue(this.month, this.year.value)
        else -> TODO(this.javaClass.canonicalName)
    }
}
