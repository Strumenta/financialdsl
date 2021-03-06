package com.strumenta.financialdsl

import com.strumenta.financialdsl.interpreter.IntValue
import com.strumenta.financialdsl.interpreter.YearlyPeriodValue
import com.strumenta.financialdsl.interpreter.evaluate
import com.strumenta.financialdsl.model.Parser

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

Gabriele is person {
    net_compensation = @{before july 2018} 0 monthly
                       @{since july 2018} 1K monthly -> contributes to income
    gross_compensation = net_compensation -> contributes to personnel_costs of Strumenta
    income <- sum
}

Strumenta is SRL {
    city = Torino
    owners = [Federico at 66%, Gabriele at 34%]
    costs_beside_personnel = 25K
    costs = costs_beside_personnel + personnel_costs
    revenues = 150K
    pre_tax_profit = revenues - costs
    profit = pre_tax_profit - IRES - IRAP
    //gross_profit is amount <- parameter
    personnel_costs is amount <- sum
    net_profit = 500 -> contributes to income of owners

}

tax IRAP on SRL {
	taxable = pre_tax_profit + personnel_costs
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
}

pension contribution InpsTerziario paid by employee {
    considered_salary = (taxable of IRES for employer - amount of IRES for employer - amount of IRAP for employer) by share of employeer of employee
    rate = brackets [to 46,123] -> 22.74%,
                      [to 76,872] -> 23.74%,
					  [above]     -> 0%
    amount = (rate for considered_salary) with minimum 3.535,61
}

pension contribution InpsGLA paid by employer 2/3 and employee 1/3 {
   considered_salary = gross_compensation of employee
    rate = brackets [to 100,323] -> 27.72%,
			        [above]      -> 0%
    amount = rate for considered_salary
}
"""

    val parsingResult = Parser().parse(code)
    if (!parsingResult.correct) {
        println("ERRORS:")
        parsingResult.errors.forEach { e ->
            println("  $e")
        }
        return
    }
    val evaluationResult = parsingResult.ast?.evaluate(
            YearlyPeriodValue(2018),
            mapOf(Pair("Strumenta", "gross_profit") to IntValue(100_000)))
    println("== Evaluation result ==")
    evaluationResult?.let {
        it.persons.forEach { person ->
            println("  == Person ${person.name} ==")
            person.fieldNames.sorted().forEach { fieldName ->
                val fieldValue = person.get(fieldName)
                println("    $fieldName: $fieldValue")
            }
        }
    }
    evaluationResult?.let {
        it.companies.forEach { company ->
            println("  == Company ${company.name} ==")
            company.fieldNames.sorted().forEach { fieldName ->
                val fieldValue = company.get(fieldName)
                println("    $fieldName: $fieldValue")
            }
        }
    }
}