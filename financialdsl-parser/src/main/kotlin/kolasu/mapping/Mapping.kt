package me.tomassetti.kolasu.mapping

import me.tomassetti.kolasu.model.Node
import me.tomassetti.kolasu.model.Point
import me.tomassetti.kolasu.model.Position
import me.tomassetti.kolasu.model.processConsideringParent
import org.antlr.v4.kotlinruntime.ParserRuleContext
import org.antlr.v4.kotlinruntime.Token

interface ParseTreeToAstMapper<in PTN : ParserRuleContext, out ASTN : Node> {
    fun map(parseTreeNode: PTN) : ASTN
}

fun Token.startPointKLS() = Point(line, charPositionInLine)

fun Token.endPointKLS() = Point(line, charPositionInLine + text!!.length)

fun ParserRuleContext.toPosition() : Position {
    return Position(start!!.startPointKLS(), stop!!.endPointKLS())
}

fun Node.setParentsForSubTree() {
    this.processConsideringParent( { node, parent ->
        node.parent = parent
    }, null)
}