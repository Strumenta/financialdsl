package com.strumenta.financialdsl.interpreter

import com.strumenta.financialdsl.model.Periodicity
import java.time.LocalDate
import java.time.Month

enum class Granularity(val value: Int) {
    CONSTANT_GRANULARITY(4),
    YEARLY_GRANULARITY(3),
    MONTHLY_GRANULARITY(2),
    DAYLY_GRANULARITY(1)
}

fun min(a: Granularity, b: Granularity) = if (a.value <= b.value) a else b

interface Value {
    val type: Type
    val granularity : Granularity
    fun forPeriod(period: PeriodValue) : Value
    fun unlazy(): Value {
        return this
    }
}

abstract class ValueImpl(override val type: Type, override val granularity : Granularity) : Value {
    abstract override fun forPeriod(period: PeriodValue) : Value // this should be constant if the period is granular enough
}

abstract class ComposedValue(override val type: Type, val members: Collection<Value>) : ValueImpl(type, members.toList().minGranularity())

private fun <E : Value> List<E>.minGranularity(): Granularity {
    return this.foldRight(Granularity.CONSTANT_GRANULARITY) { a, b -> min(a.granularity, b) }
}

abstract class ConstantValue(override val type: Type) : ValueImpl(type, Granularity.CONSTANT_GRANULARITY) {
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

data class IntValue(val value: Long) : ConstantValue(IntType) {
    fun toDecimal() = DecimalValue(value.toDouble())
}

data class TimeValue(val alternatives: List<TimeValueEntry>) : ValueImpl(alternatives.map { it.value }.commonSupertypeOfValues(), alternatives
        .map { it.periodValue.granularity() }
        .foldRight(Granularity.CONSTANT_GRANULARITY as Granularity) { a, b -> min(a, b) }) {

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
    abstract val firstDay: LocalDate
}

data class MonthDateValue(override val month: Month, override val year: Int) : DateValue(MonthDateType) {
    override val firstDay: LocalDate
        get() = LocalDate.of(year, month, 1)
}

data class PeriodicValue(val value: Value, val periodicity: Periodicity) : ConstantValue(PeriodicType(value.type, periodicity))

data class SharesMap(val shares: Map<String, PercentageValue>) : ConstantValue(SharesMapType)
