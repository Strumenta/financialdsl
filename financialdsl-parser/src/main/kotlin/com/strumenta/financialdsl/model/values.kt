package com.strumenta.financialdsl.model

import com.strumenta.financialdsl.interpreter.*
import java.time.Month

interface Granularity {
    val value : Int
}

enum class GranularityEnum(override val value: Int) : Granularity {
    CONSTANT_GRANULARITY(4),
    YEARLY_GRANULARITY(3),
    MONTHLY_GRANULARITY(2),
    DAYLY_GRANULARITY(1)
}

fun min(a: Granularity, b: Granularity) = if (a.value <= b.value) a else b

interface Value {
    val type: Type
    val granularity : Granularity
    abstract fun forPeriod(period: PeriodValue) : Value
    fun unlazy(): Value {
        return this
    }
}

abstract class ValueImpl(override val type: Type, override val granularity : Granularity) : Value {
    abstract override fun forPeriod(period: PeriodValue) : Value // this should be constant if the period is granular enough
}

abstract class ComposedValue(override val type: Type, val members: Collection<Value>) : ValueImpl(type, LazyGranularity { members.toList().minGranularity() })

private fun <E : Value> List<E>.minGranularity(): Granularity {
    return this.foldRight(GranularityEnum.CONSTANT_GRANULARITY as Granularity) { a, b -> min(a.granularity, b)}
}

abstract class ConstantValue(override val type: Type) : ValueImpl(type, GranularityEnum.CONSTANT_GRANULARITY) {
    override fun forPeriod(period: PeriodValue) = this
}

// Values could be:
// * constant  : they never change
// * functions : they change over time, with a given granularity (daily, monthly, yearly)

data class PercentageValue(val value: Double) : ConstantValue(PercentageType) {
    companion object {
        val ALL = PercentageValue(100.0)
        val NOTHING = PercentageValue(0.0)
    }
}

data class DecimalValue(val value: Double) : ConstantValue(DecimalType)

data class IntValue(val value: Long) : ConstantValue(IntType)

data class SharesMapValue(val entries: Map<EntityValues, PercentageValue>) : ConstantValue(SharesMapType)

data class TimeValue(val alternatives: List<TimeValueEntry>) : ValueImpl(alternatives.map { it.value }.commonSupertypeOfValues(), alternatives
        .map { it.periodValue.granularity() }
        .foldRight(GranularityEnum.CONSTANT_GRANULARITY as Granularity) { a, b -> min(a,b)}) {

    override fun forPeriod(period: PeriodValue): Value {
            val type = this.type
            when (type) {
                is PeriodicType -> {
                    when {
                        period.isYearly && type.periodicity == Periodicity.MONTHLY -> {
                            if (type.baseType == IntType) {
                                var sum = 0L
                                for (month in Month.values()) {
                                    sum += ((forPeriod(MonthlyPeriodValue(month, period.year)) as PeriodicValue).value as IntValue).value
                                }
                                return IntValue(sum)
                            } else {
                                TODO()
                            }
                        }
                        period.isMonthly && type.periodicity == Periodicity.MONTHLY -> {
                            // get the first alternative that is true
                            return alternatives.find { it.periodValue.contains(period) }?.value?.forPeriod(period) ?: throw RuntimeException("No alternative found")
                        }
                        else -> TODO("Periodic Type for period $period and periodicity ${type.periodicity}")
                    }
                }
                else -> TODO("TimeValue of type $type")
            }
    }
}


data class TimeValueEntry(val periodValue: PeriodValue, val value: Value)

object NoValue : ConstantValue(NoType) {
    override fun toString() = "NoValue"
}

abstract class DateValue(override val type: Type) : ConstantValue(type) {
    open val month : Month
        get() = throw UnsupportedOperationException()
    open val year : Int
        get() = throw UnsupportedOperationException()
}

data class MonthDateValue(override val month: Month, override val year: Int) : DateValue(MonthDateType)

data class PeriodicValue(val value: Value, val periodicity: Periodicity) : ConstantValue(PeriodicType(value.type, periodicity))

//data class EntityValue(val entityDecl: Entity, val evaluationContext: EvaluationContext) : ConstantValue(EntityType) {
//    private val fieldValues = HashMap<String, Value>()
//    val name
//        get() = entityDecl.name
//    fun get(fieldName: String): Value {
//        return fieldValues.computeIfAbsent(fieldName) { entityDecl.field(fieldName).value?.evaluate(evaluationContext) ?: NoValue }
//    }
//}