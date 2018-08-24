package com.strumenta.financialdsl.model

import com.strumenta.financialdsl.interpreter.EvaluationContext
import com.strumenta.financialdsl.interpreter.PeriodValue
import com.strumenta.financialdsl.interpreter.evaluate
import java.time.Month

interface Value

data class PercentageValue(val value: Double) : Value

data class DecimalValue(val value: Double) : Value

data class IntValue(val value: Long) : Value

data class SharesMapValue(val entries: Map<EntityValue, PercentageValue>) : Value

data class TimeValue(val alternatives: List<TimeValueEntry>) : Value

data class TimeValueEntry(val periodValue: PeriodValue, val value: Value) : Value

object NoValue : Value {
    override fun toString() = "NoValue"
}

data class MonthDateValue(val month: Month, val year: Year) : Value

data class PeriodicValue(val value: Value, val periodicity: Periodicity) : Value

data class EntityValue(val entityDecl: Entity, val evaluationContext: EvaluationContext) : Value {
    private val fieldValues = HashMap<String, Value>()
    val name
        get() = entityDecl.name
    fun get(fieldName: String): Value {
        return fieldValues.computeIfAbsent(fieldName) { entityDecl.field(fieldName).value?.evaluate(evaluationContext) ?: NoValue }
    }
}