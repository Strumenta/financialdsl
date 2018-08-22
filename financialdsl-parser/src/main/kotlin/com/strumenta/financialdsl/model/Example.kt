package com.strumenta.financialdsl.model

import com.strumenta.financialdsl.interpreter.YearlyPeriod
import com.strumenta.financialdsl.interpreter.evaluate

fun main(args: Array<String>) {
    val code = """countries {
    Italy EU
    Germany EU
    France EU
    Switzerland
    Japan
}

regions of Italy {
    Piedmont
}

cities of Piedmont {
    Torino
}

company type SRL {
    gross_profit is amount
    net_production is amount = gross_profit + personnel_costs
    personnel_costs is amount
}

Federico is person {
    net_compensation = @{before july 2018} 0 monthly
                       @{since july 2018} 2K monthly -> contributes to income
    gross_compensation = net_compensation -> contributes to personnel_costs of Strumenta
    income <- sum
}

Strumenta is SRL {
    owners = [Federico at 66%, Gabriele at 34%]
    gross_profit is amount <- parameter
    personnel_costs is amount <- sum
    net_profit = 0 -> contributes to income of owners by share
}

tax IRAP on SRL {
	taxable = net_production
	rate = when region=Piedmont 3.9%
}

tax IRES on SRL {
    // In maniera grossolana ma abbastanza precisa si puo' calcolare che l'utile tassabile e' circa il 20% piu' grande
    // dell'utile lordo.
	taxable =  120% of gross_profit
	rate = @{before 2017} 27.5%
	      @{since 2017} 24%
}

tax IRPEF on person {
    taxable = income
	rate = national_rate + regional_rate + town_rate
	national_rate = brackets [to 15K] -> 23%,
							 [to 28K] -> 27%,
							 [to 55K] -> 38%,
							 [to 75K] -> 41%,
							 [above]  -> 43%
	town_rate = when town=Torino brackets [to 11,670] -> 0%,
                                          [above] -> 0.8%
	regional_rate = when region=Piedmont brackets [to 15K] -> 1.62%,
											      [to 28K] -> 2.13%,
											      [to 55K] -> 2.75%,
											      [to 75K] -> 3.32%,
											      [above]  -> 3.33%
}"""

    val parsingResult = Parser().parse(code)
    val evaluationResult = parsingResult.ast?.evaluate(YearlyPeriod(2018))
    println("Evaluation result: $evaluationResult")
}