package com.strumenta.financialdsl.interpreter

import com.strumenta.financialdsl.model.*
import com.strumenta.financialdsl.model.Date
import java.util.*
import kotlin.collections.HashMap

abstract class EntityValues(
        open val ctx : EvaluationContext,
        open val name: String, open val fieldEvaluator: (String) -> Value) : Value {

    private val fieldValues = HashMap<String, Value>()

    override val type: Type
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val granularity: Granularity
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    val fieldNames: List<String>
        get() = ctx.file.entity(name).fieldNames

    fun get(name: String): Value {
        return fieldValues.computeIfAbsent(name, fieldEvaluator)
    }

    abstract fun share(entityName: String): PercentageValue
}

data class PersonValues(override val ctx : EvaluationContext,
                        override val name: String, override val fieldEvaluator: (String) -> Value) : EntityValues(ctx, name, fieldEvaluator) {
    override fun forPeriod(period: PeriodValue): Value {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun share(entityName: String): PercentageValue {
        return if (entityName == name) {
            PercentageValue.ALL
        } else {
            PercentageValue.NOTHING
        }
    }
}

data class CompanyValues(override val ctx : EvaluationContext,
                         override val name: String,
                         override val fieldEvaluator: (String) -> Value, val companyType: CompanyType) : EntityValues(ctx, name, fieldEvaluator) {
    val ownership
        get() = get("owners") as SharesMap
    override fun forPeriod(period: PeriodValue): CompanyValues {
        TODO()
    }

    override fun share(entityName: String): PercentageValue {
        return ownership.shares[entityName] ?: PercentageValue.NOTHING
    }
}

data class EvaluationResult(val companies: List<CompanyValues>, val persons: List<PersonValues>)

//class LazyGranularity(val lambda: () -> Granularity) : Granularity {
//
//    private var calculated : Granularity? = null
//
//    override val value: Int
//        get() {
//            if (calculated == null) {
//                calculated = lambda.invoke()
//            }
//            return calculated!!.value
//        }
//}

class LazyEntityFieldValue(val entityName: String, val fieldName: String, val ctx: EvaluationContext) : Value {

    private var calculatedValue : Value? = null

    override fun unlazy() = calculatedValue()

    private fun calculatedValue() : Value {
        TODO()
//        if (calculatedValue == null) {
//            val field = ctx.file.entity(entityName).field(fieldName)
//            val explicitValue = field.value
//            if (explicitValue != null) {
//                calculatedValue = explicitValue.evaluate(ctx)
//            } else if (field.isSum) {
//
//                // Iterate through all entities to find all possible contributions
//                val contributions = LinkedList<Value>()
//
//                ctx.file.entities.forEach { entity ->
//                    entity.fields.filter { it.contribution != null }.forEach { field ->
//                        when (field.contribution!!.target) {
//                            is ReferenceExpr -> {
//                                if (entity.name == entityName && (field.contribution.target as ReferenceExpr).name.name == fieldName) {
//                                    contributions.add(ctx.entityRef(entity.name).get(field.name))
//                                }
//                            }
//                            is FieldAccessExpr -> {
//                                val fieldAccessExpr = field.contribution.target as FieldAccessExpr
//                                if (fieldAccessExpr.scope is ReferenceExpr) {
//                                    if (fieldAccessExpr.scope.name.name == entityName && fieldAccessExpr.fieldName == fieldName) {
//                                        contributions.add(ctx.entityRef(entity.name).get(field.name))
//                                    }
//                                    if (fieldAccessExpr.scope.name.name == "owners" && fieldAccessExpr.fieldName == fieldName && field.contribution.byShare) {
//                                        val percentageValue = ctx.entityRef(entityName).share(entityName)
//                                        contributions.add(multiplyValues(percentageValue, ctx.entityRef(entity.name).get(field.name)))
//                                    }
//                                } else {
//                                    TODO()
//                                }
//                            }
//                            else -> TODO(field.contribution.toString())
//                        }
//
//                    }
//                }
//
//                return sumValues(contributions)
//            } else if (field.isParameter) {
//                return ctx.parameterValue(entityName, fieldName)
//            } else {
//                TODO("Field $entityName.$fieldName")
//            }
//        }
//        return calculatedValue!!
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
                    TimeValueEntry(period, sumValues(valueA.alternatives[index].value, valueB.alternatives[index].value))
                })
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
        valueA is DecimalValue && valueB is IntValue -> {
            return sumValues(valueA, valueB.toDecimal())
        }
        valueA is IntValue && valueB is DecimalValue -> {
            return sumValues(valueA.toDecimal(), valueB)
        }
        valueA is DecimalValue && valueB is DecimalValue -> {
            return DecimalValue(valueA.value + valueB.value)
        }
        else -> TODO("Sum ${valueA.type} and ${valueB.type} ${valueA.javaClass} ${valueB.javaClass}")
    }
}

fun multiplyValues(valueA: Value, valueB: Value) : Value {
    return when {
        valueA is PercentageValue && valueB is IntValue -> {
            DecimalValue(valueA.value.div(100.0) * valueB.value)
        }
        valueA is IntValue && valueB is IntValue -> {
            IntValue(valueA.value * valueB.value)
        }
        valueA is LazyEntityFieldValue || valueB is LazyEntityFieldValue -> {
            multiplyValues(valueA.unlazy(), valueB.unlazy())
        }
        else -> TODO("Multiply ${valueA.type} and ${valueB.type} ${valueA.javaClass} ${valueB.javaClass}")
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
//    private val entityRefs = HashMap<String, EntityValues>()
//    init {
//        file.companies.forEach { entityRefs[it.name] = it.evaluateCompany(this) }
//        file.persons.forEach { entityRefs[it.name] = it.evaluatePerson(this) }
//    }
//
//    fun entityRef(name: String) = entityRefs[name] ?: throw IllegalArgumentException("No entity named $name found")
//    fun parameterValue(entityName: String, fieldName: String): Value {
//        return parameters[Pair(entityName, fieldName)]!!
//    }

    fun entityValues(name: String, period: PeriodValue): EntityValues {
        return file.entities.find { it.name == name }?.evaluate(this, period) ?: throw IllegalArgumentException("Unknown entity $name")
    }

    fun parameterValue(entityName: String, fieldName: String, period: PeriodValue): Value {
        // The parameters are assumed to be constant
        return parameters[Pair(entityName, fieldName)]!!
    }
}

private fun TimeClause.evaluate(ctx: EvaluationContext, period: PeriodValue): TimeValueEntry {
    return TimeValueEntry(this.period.evaluate(ctx, period), this.value.evaluate(ctx, period))
}

fun Period.evaluate(ctx: EvaluationContext, period: PeriodValue): PeriodValue {
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
