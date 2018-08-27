package com.strumenta.financialdsl.interpreter

import com.strumenta.financialdsl.model.*
import com.strumenta.financialdsl.model.Date
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
    val city : City
        get() = (get("city") as CityValue).city
    val region : Region
        get() = city.region
    val country : Country
        get() = city.country

    fun get(name: String): Value {
        return fieldValues.computeIfAbsent(name, fieldEvaluator)
    }

    abstract fun share(entityName: String): PercentageValue
}

data class TaxValues (
        open val ctx : EvaluationContext,
        open val name: String,
        val entityName: String,
        open val fieldEvaluator: (String) -> Value) : Value {
    override fun forPeriod(period: PeriodValue): Value {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val type: Type
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val granularity: Granularity
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    private val fieldValues = HashMap<String, Value>()

    val fieldNames: List<String>
        get() = ctx.file.entity(name).fieldNames

    fun get(name: String): Value {
        return fieldValues.computeIfAbsent(name, fieldEvaluator)
    }
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

data class TaxPayment(val entity: Entity, val tax: Tax, val amount: Double)

data class EvaluationResult(
        val countries: List<Country>,
        val regions: List<Region>,
        val cities: List<City>,
        val companies: List<CompanyValues>,
        val persons: List<PersonValues>,
        val taxes: List<Tax>,
        val taxPayments: List<TaxPayment>) {

    fun entity(name: String): EntityValues {
        return (companies + persons).find { it.name == name }!!
    }

    fun country(name: String) = countries.find { it.name == name } ?: throw IllegalArgumentException("No country named $name found")
    fun regionsOf(countryName: String) = regions.filter { it.country.name == countryName }
    fun region(name: String) : Region = regions.find { it.name == name } ?: throw IllegalArgumentException("No region named $name found")
    fun city(name: String) = cities.find { it.name == name } ?: throw IllegalArgumentException("No city named $name found")
    fun person(name: String) = persons.find { it.name == name}!!
    fun company(name: String) = companies.find { it.name == name}!!
    fun tax(name: String) = taxes.find { it.name == name}!!
    fun tax(entityName: String, taxName: String) = taxPayments.find { it.tax.name == taxName && it.entity.name == entityName }!!
}

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
        valueA is BracketsValue && valueB is BracketsValue -> {
            if (valueA.brackets.map { it.limit }  == valueB.brackets.map { it.limit }) {
                //return BracketsValue(BracketsExpr(valueA.expr.entries.mapIndexed { index, entryA -> BracketEntry(entryA.range, SumExpr(entryA.value, valueB.expr.entries[index].value)) }))
                TODO()
            } else {
                TODO("Brackets A: ${valueA.brackets.map { it.limit }} Brackets B: ${valueB.brackets.map { it.limit }}")
            }
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
        valueA is DecimalValue && valueB is PercentageValue -> {
            DecimalValue(valueA.value * valueB.value.div(100.0))
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
}

data class EvaluationContext(val file: FinancialDSLFile, val parameters: Map<Pair<String, String>, Value>, val currentEntity: Entity? = null) {

    fun entityValues(name: String, period: PeriodValue): EntityValues {
        return file.entities.find { it.name == name }?.evaluate(this, period) ?: throw IllegalArgumentException("Unknown entity $name")
    }

    fun parameterValue(entityName: String, fieldName: String, period: PeriodValue): Value {
        // The parameters are assumed to be constant
        return parameters[Pair(entityName, fieldName)]!!
    }

    fun taxPayments(tax: Tax, period: PeriodValue): List<TaxPayment> {
        return file.entities.filter { tax.isApplicableTo(it) }.map { entity ->
            TaxPayment(entity, tax, tax.amountToPay(entity, this.inEntity(entity), period))
        }
    }

    fun taxValues(taxName: String, entityName: String, period: PeriodValue): TaxValues {
        return file.taxes.find { it.name == taxName }?.evaluate(entityName, this, period) ?: throw IllegalArgumentException("Unknown tax $taxName")
    }

    fun valueOfieldInThisContext(fieldName: String, period: PeriodValue): Value? {
        if (currentEntity == null) {
            return null
        }
        return entityValues(currentEntity!!.name, period).get(fieldName)
    }

    fun inEntity(entity: Entity) : EvaluationContext {
        return this.copy(currentEntity = entity)
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
