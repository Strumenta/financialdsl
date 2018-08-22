package com.strumenta.financialdsl.model

import com.strumenta.financialdsl.interpreter.EvaluationContext

interface Value

data class EntityRef(val name: String, val ctx: EvaluationContext) : Value

data class PercentageValue(val value: Double) : Value

data class SharesMapValue(val entries: Map<EntityRef, PercentageValue>) : Value