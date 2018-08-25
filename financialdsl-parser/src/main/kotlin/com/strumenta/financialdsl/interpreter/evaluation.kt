package com.strumenta.financialdsl.interpreter

import com.strumenta.financialdsl.model.*
import com.strumenta.financialdsl.model.Date
import com.strumenta.financialdsl.model.Periodicity.MONTHLY
import java.time.Month
import java.util.*

///
/// File level
///

fun FinancialDSLFile.evaluate(period: PeriodValue, parameters: Map<Pair<String, String>, Value>) : EvaluationResult {
    val ctx = EvaluationContext(this, parameters)
    return EvaluationResult(
            this.companies.map { ctx.entityValues(it.name, period) as CompanyValues },
            this.persons.map { ctx.entityValues(it.name, period) as PersonValues }
    )
}

///
/// Entity level
///

fun Entity.evaluate(ctx: EvaluationContext, period: PeriodValue) : EntityValues {
    return when  {
        this.type.isPerson() -> evaluatePerson(ctx, period)
        this.type.isCompany() -> evaluateCompany(ctx, period)
        else -> throw UnsupportedOperationException()
    }
}

private fun Entity.evaluatePerson(ctx: EvaluationContext, period: PeriodValue): PersonValues {
    val fieldEvaluator : (String) -> Value = { fieldName ->
        val field = ctx.file.entity(this.name).field(fieldName)
        field.evaluate(this.name, ctx, period)
    }
    return PersonValues(ctx, this.name, fieldEvaluator)
}

private fun Entity.evaluateCompany(ctx: EvaluationContext, period: PeriodValue): CompanyValues {
    val fieldEvaluator : (String) -> Value = { fieldName ->
        val field = ctx.file.entity(this.name).field(fieldName)
        field.evaluate(this.name, ctx, period)
    }
    return CompanyValues(
            ctx,
            this.name,
            fieldEvaluator,
            (this.type as CompanyTypeRef).ref.referred!!)
}

private fun EntityField.evaluate(entityName: String, ctx: EvaluationContext, period: PeriodValue): Value {
    val explicitValue = this.value
    return when {
        explicitValue != null -> explicitValue.evaluate(ctx, period)
        this.isSum -> evaluateAsSum(entityName, ctx, period)
        this.isParameter -> ctx.parameterValue(entityName, this.name, period)
        else -> TODO("Field ${this.name}")
    }
}

private fun EntityField.evaluateAsSum(entityName: String, ctx: EvaluationContext, period: PeriodValue) : Value {

    // Iterate through all entities to find all possible contributions
    val contributions = LinkedList<Value>()

    ctx.file.entities.forEach { entity ->
        entity.fields.filter { it.contribution != null }.forEach { field ->
//            field.contribution!!.target
//            when (field.contribution!!.target) {
//                is ReferenceExpr -> {
//                    if (entity.name == entityName && (field.contribution.target as ReferenceExpr).name.name == fieldName) {
//                        contributions.add(ctx.entityRef(entity.name).get(field.name))
//                    }
//                }
//                is FieldAccessExpr -> {
//                    val fieldAccessExpr = field.contribution.target as FieldAccessExpr
//                    if (fieldAccessExpr.scope is ReferenceExpr) {
//                        if (fieldAccessExpr.scope.name.name == entityName && fieldAccessExpr.fieldName == fieldName) {
//                            contributions.add(ctx.entityRef(entity.name).get(field.name))
//                        }
//                        if (fieldAccessExpr.scope.name.name == "owners" && fieldAccessExpr.fieldName == fieldName && field.contribution.byShare) {
//                            val percentageValue = ctx.entityRef(entityName).share(entityName)
//                            contributions.add(multiplyValues(percentageValue, ctx.entityRef(entity.name).get(field.name)))
//                        }
//                    } else {
//                        TODO()
//                    }
//                }
//                else -> TODO(field.contribution.toString())
            TODO()
            //}

        }
    }
    TODO()

    return sumValues(contributions)
}

///
/// Expression level
///

fun Expression.evaluate(ctx: EvaluationContext, period: PeriodValue): Value {
    return when (this) {
        is SharesMapExpr -> SharesMapValue(this.shares.map {
            it.owner.evaluate(ctx, period) as EntityValues to it.shares.evaluate(ctx, period) as PercentageValue
        }.toMap())
        is ReferenceExpr -> {
            val target = this.name.referred!!
            when (target) {
                is Entity -> ctx.entityValues(target.name, period)
                is EntityFieldRef -> ctx.entityValues(target.entityName, period).get(target.name)
                else -> TODO("ReferenceExpr to ${target.javaClass.canonicalName}")
            }
        }
        is PercentageLiteral -> PercentageValue(this.value)
        is IntLiteral -> IntValue(this.value)
        is TimeExpression -> {
            val onlyRelevantClause = this.clauses.find { it.period.evaluate(ctx, period).contains(period) }
            if (onlyRelevantClause != null) {
                return onlyRelevantClause.value.evaluate(ctx, period)
            }

            val type = this.type()
            if (type is PeriodicType && type.periodicity == MONTHLY && period.isYearly) {
                val contributions = LinkedList<Value>()
                Month.values().forEach { month ->
                    val monthlyValue = this.evaluate(ctx, MonthlyPeriodValue(month, period.year))
                    require(monthlyValue is PeriodicValue && monthlyValue.periodicity == MONTHLY)
                    contributions.add((monthlyValue as PeriodicValue).value)
                }
                return sumValues(contributions)
            }
            TODO()
        }
        is PeriodicExpression -> PeriodicValue(this.value.evaluate(ctx, period), this.periodicity)
        else -> TODO(this.javaClass.canonicalName)
    }
}

fun TimeExpression.granularity(ctx: EvaluationContext, period: PeriodValue) : Granularity {
    return this.clauses.foldRight(Granularity.CONSTANT_GRANULARITY) { a, b -> min(a.period.granularity(ctx, period), b)}
}

private fun Period.granularity(ctx: EvaluationContext, period: PeriodValue): Granularity {
    return when (this) {
        is SincePeriod -> this.date.granularity(ctx, period)
        is BeforePeriod -> this.date.granularity(ctx, period)
        is AfterPeriod -> this.date.granularity(ctx, period)
        else -> TODO(this.javaClass.canonicalName)
    }
}

private fun Date.granularity(ctx: EvaluationContext, period: PeriodValue): Granularity {
    return when (this) {
        is MonthDate -> Granularity.MONTHLY_GRANULARITY
        else -> TODO(this.javaClass.canonicalName)
    }
}
