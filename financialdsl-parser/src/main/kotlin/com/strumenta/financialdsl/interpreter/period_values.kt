package com.strumenta.financialdsl.interpreter

import java.time.LocalDate
import java.time.Month

abstract class PeriodValue {
    abstract fun granularity(): Granularity
    abstract fun contains(period: PeriodValue): Boolean
    abstract fun endsBefore(limit: LocalDate): Boolean
    abstract fun startsOn(firstDay: LocalDate): Boolean
    abstract fun startsAfter(firstDay: LocalDate): Boolean

    open val isYearly : Boolean
        get() = false
    open val isMonthly : Boolean
        get() = false
    open val year: Int
        get() = throw UnsupportedOperationException(this.javaClass.canonicalName)
    open val month: Month
        get() = throw UnsupportedOperationException(this.javaClass.canonicalName)
}

class YearlyPeriodValue(override val year: Int) : PeriodValue() {
    override fun startsOn(limit: LocalDate): Boolean {
        return limit.year == year && limit.month == Month.JANUARY && limit.dayOfMonth == 1
    }

    override fun startsAfter(limit: LocalDate): Boolean {
        return year > limit.year
    }

    override fun endsBefore(limit: LocalDate): Boolean {
        return year < limit.year
    }

    override val isYearly: Boolean
        get() = true

    override fun granularity() = Granularity.YEARLY_GRANULARITY

    override fun toString() = "YearlyPeriodValue($year)"

    override fun contains(period: PeriodValue): Boolean {
        if (period.isYearly) {
            return period.year == this.year
        }
        if (period.isMonthly) {
            return period.year == this.year
        }
        return false
    }
}

class MonthlyPeriodValue(override val month: Month, override val year: Int) : PeriodValue() {
    override fun endsBefore(limit: LocalDate): Boolean {
        return year < limit.year || (year == limit.year && month < limit.month)
    }
    override fun startsOn(firstDay: LocalDate): Boolean {
        return year == firstDay.year && month == firstDay.month && firstDay.dayOfMonth == 1
    }

    override fun startsAfter(firstDay: LocalDate): Boolean {
        if (year > firstDay.year) {
            return true
        }
        if (year == firstDay.year && month > firstDay.month) {
            return true
        }
        return false
    }

    override val isMonthly: Boolean
        get() = true

    override fun granularity() = Granularity.MONTHLY_GRANULARITY

    override fun toString() = "MonthlyPeriodValue($month $year)"

    override fun contains(period: PeriodValue): Boolean {
        if (period.isMonthly) {
            return period.year == this.year && period.month == this.month
        }
        return false
    }
}

data class BeforePeriodValue(val date: DateValue) : PeriodValue() {
    override fun endsBefore(limit: LocalDate): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun startsOn(firstDay: LocalDate): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun startsAfter(firstDay: LocalDate): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun granularity(): Granularity {
        return date.granularity
    }

    override fun contains(period: PeriodValue): Boolean {
        val firstDay : LocalDate = date.firstDay

        return period.endsBefore(firstDay)
    }
}

data class SincePeriodValue(val date: DateValue) : PeriodValue(){
    override fun endsBefore(limit: LocalDate): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun startsOn(firstDay: LocalDate): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun startsAfter(firstDay: LocalDate): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun granularity(): Granularity {
        return date.granularity
    }
    override fun contains(period: PeriodValue): Boolean {
        val firstDay : LocalDate = date.firstDay
        return period.startsOn(firstDay) || period.startsAfter(firstDay)
    }
}
data class AfterPeriodValue(val date: DateValue) : PeriodValue(){
    override fun endsBefore(limit: LocalDate): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun startsOn(firstDay: LocalDate): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun startsAfter(firstDay: LocalDate): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun granularity(): Granularity {
        return date.granularity
    }
    override fun contains(period: PeriodValue): Boolean {
        if (period.isYearly) {
            TODO()
        }
        if (period.isMonthly) {
            if (date.year < period.year) {
                return true
            }
            if (date.year == period.year && date.month < period.month) {
                return true
            }
            return false
        }
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}