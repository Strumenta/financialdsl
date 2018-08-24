package com.strumenta.financialdsl.model

interface Type

object IntType : Type

data class PeriodicType(val baseType: Type, val periodicity: Periodicity) : Type

fun Expression.type() : Type {
    return when (this) {
        is TimeExpression -> commonSupertype(this.clauses.map { it.value.type() })
        is PeriodicExpression -> PeriodicType(this.value.type(), this.periodicity)
        is IntLiteral -> IntType
        else -> TODO(this.javaClass.canonicalName)
    }
}

fun commonSupertype(types: List<Type>) : Type {
    if (types.isEmpty()) {
        throw IllegalArgumentException("At least one expected")
    }
    if (types.size == 1) {
        return types[0]
    }
    return commonSupertype(types[0], commonSupertype(types.drop(1)))
}

fun commonSupertype(typeA: Type, typeB: Type) : Type {
    if (typeA == typeB) {
        return typeA
    }
    TODO("Between $typeA and $typeB")
}