package com.strumenta.financialdsl.parser

import com.strumenta.financialdsl.FinancialDSLParser
import com.strumenta.financialdsl.FinancialDSLParser.*
import com.strumenta.financialdsl.model.FinancialDSLFile
import com.strumenta.financialdsl.model.TopLevelDeclaration
import me.tomassetti.kolasu.mapping.toPosition

fun FinancialDSLFileContext.toAst() : FinancialDSLFile = FinancialDSLFile(
        this.declarations.map { it.toAst() },
        toPosition())

fun TopLevelDeclarationContext.toAst(): TopLevelDeclaration {
    when (this) {
        is CitiesDeclContext -> TODO()
        else -> TODO()
    }
}

