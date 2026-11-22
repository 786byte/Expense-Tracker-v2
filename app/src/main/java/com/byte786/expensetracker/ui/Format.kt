package com.byte786.expensetracker.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** How amounts are written. Set from Settings; see [Money.style]. */
data class MoneyStyle(
    val symbolAfter: Boolean = false,
    val decimals: Boolean = true,
    /** 1,00,000 instead of 100,000. */
    val indianGrouping: Boolean = false,
)

object Money {
    @Volatile var style = MoneyStyle()

    /** [minor] is in paisa/cents. */
    fun format(minor: Long, symbol: String, sign: String = "", style: MoneyStyle = this.style): String {
        val value = number(abs(minor), style)
        val s = if (sign.isEmpty() && minor < 0) "-" else sign
        return if (style.symbolAfter) "$s$value${if (symbol.isBlank()) "" else " " + symbol.trim()}" else "$s$symbol$value"
    }

    /** Positive [minor] as text, e.g. 1234567 -> "12,345.67" (or "12,346" without decimals). */
    fun number(minor: Long, style: MoneyStyle = this.style): String {
        val bd = BigDecimal.valueOf(minor, 2)
        val whole: Long
        var frac = ""
        if (style.decimals) {
            whole = minor / 100
            val cents = minor % 100
            if (cents != 0L) frac = "." + (if (cents % 10 == 0L) (cents / 10).toString() else cents.toString().padStart(2, '0'))
        } else {
            whole = bd.setScale(0, RoundingMode.HALF_UP).toLong()
        }
        return group(whole.toString(), style.indianGrouping) + frac
    }

    private fun group(digits: String, indian: Boolean): String {
        if (digits.length <= 3) return digits
        val last3 = digits.takeLast(3)
        val rest = digits.dropLast(3)
        val size = if (indian) 2 else 3
        val parts = rest.reversed().chunked(size).map { it.reversed() }.reversed()
        return parts.joinToString(",") + "," + last3
    }

    fun parse(text: String): Long? = text.trim().replace(",", "").toBigDecimalOrNull()
        ?.movePointRight(2)?.setScale(0, RoundingMode.HALF_UP)?.toLong()

    fun toInput(minor: Long): String =
        if (minor == 0L) "" else BigDecimal.valueOf(minor, 2).stripTrailingZeros().toPlainString()
}

object Dates {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val day = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
    private val shortDay = DateTimeFormatter.ofPattern("d MMM yyyy")
    private val time = DateTimeFormatter.ofPattern("h:mm a")
    private val month = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val shortMonth = DateTimeFormatter.ofPattern("MMM yyyy")

    fun local(ms: Long): LocalDateTime = Instant.ofEpochMilli(ms).atZone(zone).toLocalDateTime()
    fun date(ms: Long): LocalDate = local(ms).toLocalDate()
    fun millis(ldt: LocalDateTime): Long = ldt.atZone(zone).toInstant().toEpochMilli()
    fun start(d: LocalDate): Long = millis(d.atStartOfDay())

    /** Ranges are [start, end) in epoch millis. */
    fun dayRange(d: LocalDate) = start(d) to start(d.plusDays(1))
    fun monthRange(ym: YearMonth) = start(ym.atDay(1)) to start(ym.plusMonths(1).atDay(1))
    fun yearRange(y: Int) = start(LocalDate.of(y, 1, 1)) to start(LocalDate.of(y + 1, 1, 1))

    fun dayLabel(d: LocalDate): String {
        val today = LocalDate.now()
        return when (d) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> day.format(d)
        }
    }
    fun fullDay(d: LocalDate): String = day.format(d)
    fun shortDay(ms: Long): String = shortDay.format(local(ms))
    fun time(ms: Long): String = time.format(local(ms))
    fun month(ym: YearMonth): String = month.format(ym)
    fun shortMonth(ym: YearMonth): String = shortMonth.format(ym)

    // Material date pickers work with UTC midnight.
    fun fromPicker(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
    fun toPicker(d: LocalDate): Long = d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun describeRange(from: Long, to: Long): String {
        if (from < 0 || to < 0) return "All time"
        val a = date(from)
        val bExcl = date(to)
        val b = bExcl.minusDays(1)
        return when {
            a == b -> fullDay(a)
            a.dayOfMonth == 1 && bExcl == a.plusMonths(1) -> month(YearMonth.from(a))
            a.dayOfYear == 1 && bExcl == a.plusYears(1) -> a.year.toString()
            else -> "${shortDay.format(a)} – ${shortDay.format(b)}"
        }
    }
}
