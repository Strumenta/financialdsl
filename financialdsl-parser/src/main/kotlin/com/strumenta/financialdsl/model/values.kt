package com.strumenta.financialdsl.model

import com.strumenta.financialdsl.interpreter.EvaluationContext
import com.strumenta.financialdsl.interpreter.PeriodValue
import com.strumenta.financialdsl.interpreter.evaluate
import java.time.Month

enum class Granularity(val value: Int) {
    CONSTANT_GRANULARITY(4),
    YEARLY_GRANULARITY(3),
    MONTHLY_GRANULARITY(2),
    DAYLY_GRANULARITY(1)
}

fun min(a: Granularity, b: Granularity) = if (a.value <= b.value) a else b

abstract class Value(open val type: Type, val granularity : Granularity) {
    abstract fun forPeriod(period: PeriodValue) : Value // this should be constant if the period is granular enough
}

abstract class ComposedValue(val members: Collection<Value>) : Value(members.commonSupertypeOfValues(), members.toList().minGranularity())

private fun <E : Value> List<E>.minGranularity(): Granularity {
    return this.foldRight(Granularity.CONSTANT_GRANULARITY) { a, b -> min(a.granularity, b)}
}

abstract class ConstantValue(override val type: Type) : Value(type, Granularity.CONSTANT_GRANULARITY) {
    override fun forPeriod(period: PeriodValue) = this
}

// Values could be:
// * constant  : they never change
// * functions : they change over time, with a given granularity (daily, monthly, yearly)

data class PercentageValue(val value: Double) : ConstantValue(PercentageType)

data class DecimalValue(val value: Double) : ConstantValue(DecimalType)

data class IntValue(val value: Long) : ConstantValue(IntType)

data class SharesMapValue(val entries: Map<EntityValue, PercentageValue>) : ConstantValue(SharesMapType)

data class TimeValue(val alternatives: List<TimeValueEntry>) : Value(alternatives.map { it.value }.commonSupertypeOfValues(), alternatives
        .map { it.periodValue.granularity() }
        .foldRight(Granularity.CONSTANT_GRANULARITY) { a, b -> min(a,b)}) {

    override fun forPeriod(period: PeriodValue): Value {
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
        TODO()
    }
}


data class TimeValueEntry(val periodValue: PeriodValue, val value: Value)

object NoValue : ConstantValue(NoType) {
    override fun toString() = "NoValue"
}

data class MonthDateValue(val month: Month, val year: Year) : ConstantValue(MonthDateType)

data class PeriodicValue(val value: Value, val periodicity: Periodicity) : ConstantValue(PeriodicType(value.type, periodicity))

data class EntityValue(val entityDecl: Entity, val evaluationContext: EvaluationContext) : ConstantValue(EntityType) {
    private val fieldValues = HashMap<String, Value>()
    val name
        get() = entityDecl.name
    fun get(fieldName: String): Value {
        return fieldValues.computeIfAbsent(fieldName) { entityDecl.field(fieldName).value?.evaluate(evaluationContext) ?: NoValue }
    }
}