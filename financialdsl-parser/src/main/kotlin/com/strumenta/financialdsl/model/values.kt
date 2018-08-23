package com.strumenta.financialdsl.model

import com.strumenta.financialdsl.interpreter.EvaluationContext

interface Value

data class EntityRef(val name: String, val ctx: EvaluationContext) : Value

data class PercentageValue(val value: Double) : Value

data class DecimalValue(val value: Double) : Value

data class IntValue(val value: Long) : Value

data class SharesMapValue(val entries: Map<EntityRef, PercentageValue>) : Value

object NoValue : Value