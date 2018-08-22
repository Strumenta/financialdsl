package com.strumenta.financialdsl.model

import me.tomassetti.kolasu.model.Node
import me.tomassetti.kolasu.model.Position
import me.tomassetti.kolasu.model.pos

abstract class TopLevelDeclaration(override val position: Position? = null) : Node(position)

data class FinancialDSLFile(val declarations : List<TopLevelDeclaration>,
                        override val position: Position? = null) : Node(position)

