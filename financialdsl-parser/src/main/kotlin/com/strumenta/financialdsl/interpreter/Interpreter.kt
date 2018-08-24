package com.strumenta.financialdsl.interpreter

import com.strumenta.financialdsl.model.*
import com.strumenta.financialdsl.model.Date
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
    fun get(name: String): Value {
        return fieldValues[name]!!
    }
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
class LazyGranularity(val lambda: () -> Granularity) : Granularity {

    private var calculated : Granularity? = null

    override val value: Int
        get() {
            if (calculated == null) {
                calculated = lambda.invoke()
            }
            return calculated!!.value
        }
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

                ctx.file.entities.forEach { entity ->
                    entity.fields.filter { it.contribution != null }.forEach { field ->
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
                                    if (fieldAccessExpr.scope.name.name == "owners" && fieldAccessExpr.fieldName == fieldName && field.contribution.byShare) {
                                        val percentageValue = ctx.entityRef(entityName)
                                        //contributions.add(ctx.entityRef(entity.name).get(field.name))
                                    }
                                } else {
                                    TODO()
                                }
                            }
                            else -> TODO(field.contribution.toString())
                        }

                    }
                }

                return sumValues(contributions)
            } else if (field.isParameter) {
                return ctx.parameterValue(entityName, fieldName)
            } else {
                TODO("Field $entityName.$fieldName")
            }
        }
        return calculatedValue!!
    }

    override val type: Type
        get() = calculatedValue().type
    override val granularity: Granularity
        get() = calculatedValue().granularity

    override fun forPeriod(period: PeriodValue): Value {
        return calculatedValue().forPeriod(period)
    }
}

fun sumValues(valueA: Value, valueB: Value) : Value {
    when {
        valueA is TimeValue && valueB is TimeValue -> {
            val periodsA = valueA.alternatives.map { it.periodValue }
            val periodsB = valueB.alternatives.map { it.periodValue }
            if (periodsA == periodsB) {
                return TimeValue(periodsA.mapIndexed { index, period ->
                    TimeValueEntry(period, sumValues(valueA.alternatives[index].value, valueB.alternatives[index].value)) })
            } else {
                TODO("Entries: $periodsA and $periodsB")
            }
        }
        valueA is PeriodicValue && valueB is PeriodicValue -> {
            if (valueA.periodicity == valueB.periodicity) {
                return PeriodicValue(sumValues(valueA.value, valueB.value), valueA.periodicity)
            } else {
                TODO()
            }
        }
        valueA is IntValue && valueB is IntValue -> {
            return IntValue(valueA.value + valueB.value)
        }
        valueA is LazyEntityFieldValue || valueB is LazyEntityFieldValue -> {
            return sumValues(valueA.unlazy(), valueB.unlazy())
        }
        else -> TODO("Sum ${valueA.type} and ${valueB.type} ${valueA.javaClass} ${valueB.javaClass}")
    }
}

fun sumValues(elements: List<Value>) : Value {
    return when {
        elements.isEmpty() -> NoValue
        elements.size == 1 -> elements[0]
        else -> sumValues(elements[0], sumValues(elements.drop(1)))
    }

//                    val fieldType = contributions.commonSupertypeOfValues()
//                    when (fieldType) {
//                        is PeriodicType -> {

//                            if (fieldType.baseType is IntType) {
//                                var result = 0L
//                                contributions.forEach {
//                                    val v = it.unlazy()
//                                    if (v is PeriodicValue) {
//                                        if (v.periodicity != fieldType.periodicity) {
//                                            TODO()
//                                        }
//                                        if (v.value is IntValue) {
//                                            result += v.value.value
//                                        } else {
//                                            TODO()
//                                        }
//                                    } else if (v is TimeValue) {
//                                        TODO(v.toString()+ " "+ v.type)
//                                    } else {
//                                        TODO("Not periodic value but $it")
//                                    }
//                                }
//                                return PeriodicValue(IntValue(result), fieldType.periodicity)
//                            } else {
//                                TODO()
//                            }
//                        }
//                        else -> TODO(fieldType.toString())
  //  }
//}
}

class EvaluationContext(val file: FinancialDSLFile, val parameters: Map<Pair<String, String>, Value>) {
    private val entityRefs = HashMap<String, EntityValues>()
    init {
        file.companies.forEach { entityRefs[it.name] = it.evaluateCompany(this) }
        file.persons.forEach { entityRefs[it.name] = it.evaluatePerson(this) }
    }

    fun entityRef(name: String) = entityRefs[name] ?: throw IllegalArgumentException("No entity named $name found")
    fun parameterValue(entityName: String, fieldName: String): Value {
        return parameters[Pair(entityName, fieldName)]!!
    }
}

fun FinancialDSLFile.evaluate(period: PeriodValue, parameters: Map<Pair<String, String>, Value>) : EvaluationResult {
    val ctx = EvaluationContext(this, parameters)
    return EvaluationResult(
            this.companies.map { ctx.entityRef(it.name).forPeriod(period) as CompanyValues },
            this.persons.map { ctx.entityRef(it.name).forPeriod(period) as PersonValues }
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
            it.owner.evaluate(ctx) as EntityValues to it.shares.evaluate(ctx) as PercentageValue }.toMap())
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
