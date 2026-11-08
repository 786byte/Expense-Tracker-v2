package com.byte786.expensetracker.data

import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

/**
 * Reads CSV exports from other expense apps (and this app's own export).
 * Pure Kotlin, no Android types, so it can be unit tested.
 *
 * Understood columns (header names, any case):
 *  TIME / DATE, TYPE, AMOUNT, CATEGORY, SUBCATEGORY, ACCOUNT, TO ACCOUNT, NOTES / NOTE / MEMO
 * A transfer can also be written as ACCOUNT = "From->To".
 */
object CsvParser {

    data class Row(
        val line: Int,
        val date: Long,
        val type: Int,
        /** Minor units (paisa/cents), always positive. */
        val amount: Long,
        val category: String?,
        val subcategory: String?,
        val account: String,
        val toAccount: String?,
        /** Title (first line of the note). */
        val memo: String,
        /** The rest of a multi-line note, or the DESCRIPTION column. */
        val description: String = "",
    )

    data class Result(val rows: List<Row>, val errors: List<String>)

    /** Splits CSV text into records. Handles quotes, "" escapes, line breaks inside quotes, and stray spaces. */
    fun split(text: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var fields = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var wasQuoted = false
        var i = if (text.startsWith("﻿")) 1 else 0

        fun endField() {
            fields.add(if (wasQuoted) cur.toString() else cur.toString().trim())
            cur.setLength(0)
            wasQuoted = false
        }
        fun endRecord() {
            endField()
            if (!(fields.size == 1 && fields[0].isEmpty())) records.add(fields)
            fields = mutableListOf()
        }

        // True if position j is the end of a field (optionally after spaces).
        fun atDelimiter(j: Int): Boolean {
            var k = j
            while (k < text.length && (text[k] == ' ' || text[k] == '\t')) k++
            return k >= text.length || text[k] == ',' || text[k] == '\n' || text[k] == '\r'
        }
        // Some apps write a note that itself contains quotes without escaping them, e.g. ""Hi there"".
        // Once we've seen such a stray quote in a field, `""` right before the field end also closes it.
        var lenient = false

        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    when {
                        i + 1 < text.length && text[i + 1] == '"' -> {
                            cur.append('"'); i++
                            if (lenient && atDelimiter(i + 1)) inQuotes = false
                        }
                        atDelimiter(i + 1) -> inQuotes = false
                        else -> { cur.append('"'); lenient = true }
                    }
                } else cur.append(c)
            } else when (c) {
                '"' -> {
                    // A quote starts a quoted field only at the field's beginning (ignoring spaces).
                    if (cur.isBlank()) { cur.setLength(0); inQuotes = true; wasQuoted = true; lenient = false } else cur.append(c)
                }
                ',' -> endField()
                '\r' -> {}
                '\n' -> endRecord()
                else -> if (!(wasQuoted && c.isWhitespace())) cur.append(c) // ignore spaces after a closing quote
            }
            i++
        }
        if (cur.isNotEmpty() || fields.isNotEmpty() || wasQuoted) endRecord()
        return records
    }

    private val dateFormats: List<DateTimeFormatter> = listOf(
        "MMM d, yyyy h:mm a", "MMM d, yyyy H:mm", "yyyy-MM-dd HH:mm", "yyyy-MM-dd H:mm", "yyyy-MM-dd HH:mm:ss",
        "dd/MM/yyyy HH:mm", "d/M/yyyy h:mm a", "yyyy-MM-dd", "MMM d, yyyy",
    ).map { DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(it).toFormatter(Locale.US) }

    fun parseDate(s: String, zone: ZoneId): Long? {
        val t = s.trim().replace(Regex("\\s+"), " ")
        for (f in dateFormats) {
            val ldt = runCatching { LocalDateTime.parse(t, f) }.getOrNull()
                ?: runCatching { java.time.LocalDate.parse(t, f).atTime(12, 0) }.getOrNull()
            if (ldt != null) return ldt.atZone(zone).toInstant().toEpochMilli()
        }
        return null
    }

    fun parseAmount(s: String): Long? = s.trim().replace(",", "").replace(Regex("[^0-9.\\-]"), "")
        .toBigDecimalOrNull()?.abs()?.movePointRight(2)?.setScale(0, RoundingMode.HALF_UP)?.toLong()

    fun parseType(s: String): Int {
        val t = s.lowercase()
        return when {
            "transfer" in t || t.startsWith("(*)") -> TxnType.TRANSFER
            "income" in t || t.startsWith("(+)") -> TxnType.INCOME
            else -> TxnType.EXPENSE
        }
    }

    /** Blank or placeholder ("-") category names become null. */
    private fun cleanName(s: String?): String? = s?.trim()?.takeIf { it.isNotEmpty() && it.any(Char::isLetterOrDigit) }

    fun parse(text: String, zone: ZoneId = ZoneId.systemDefault()): Result {
        val records = split(text)
        if (records.isEmpty()) return Result(emptyList(), listOf("The file is empty"))
        val header = records.first().map { it.trim().uppercase() }
        fun col(vararg names: String) = header.indexOfFirst { it in names }
        val cDate = col("TIME", "DATE", "DATE TIME", "DATETIME")
        val cType = col("TYPE")
        val cAmount = col("AMOUNT")
        val cCat = col("CATEGORY")
        val cSub = col("SUBCATEGORY", "SUB CATEGORY", "SUB-CATEGORY")
        val cAcc = col("ACCOUNT", "FROM ACCOUNT")
        val cTo = col("TO ACCOUNT", "TO")
        val cMemo = col("NOTES", "NOTE", "MEMO", "TITLE")
        val cDesc = col("DESCRIPTION")
        if (cDate < 0 || cAmount < 0 || cAcc < 0) {
            return Result(emptyList(), listOf("Couldn't find the TIME/DATE, AMOUNT and ACCOUNT columns in the first line"))
        }

        val rows = mutableListOf<Row>()
        val errors = mutableListOf<String>()
        records.drop(1).forEachIndexed { idx, r ->
            val n = idx + 2
            fun get(c: Int) = if (c >= 0 && c < r.size) r[c] else ""
            val date = parseDate(get(cDate), zone)
            val amount = parseAmount(get(cAmount))
            if (date == null) { errors.add("Row $n: can't read date \"${get(cDate)}\""); return@forEachIndexed }
            if (amount == null || amount == 0L) { errors.add("Row $n: can't read amount \"${get(cAmount)}\""); return@forEachIndexed }
            val type = if (cType >= 0) parseType(get(cType)) else TxnType.EXPENSE
            var from = get(cAcc).trim()
            var to = cleanName(get(cTo))
            if (type == TxnType.TRANSFER && to == null && "->" in from) {
                val parts = from.split("->", limit = 2)
                from = parts[0].trim(); to = cleanName(parts[1])
            }
            if (from.isEmpty()) { errors.add("Row $n: no account"); return@forEachIndexed }
            if (type == TxnType.TRANSFER && to == null) { errors.add("Row $n: transfer without a destination account"); return@forEachIndexed }
            val note = get(cMemo).trim()
            val title: String
            val description: String
            if (cDesc >= 0) {
                title = note; description = get(cDesc).trim()
            } else {
                title = note.substringBefore('\n').trim()
                description = if ('\n' in note) note.substringAfter('\n').trim() else ""
            }
            rows.add(
                Row(
                    line = n, date = date, type = type, amount = amount,
                    category = if (type == TxnType.TRANSFER) null else cleanName(get(cCat)),
                    subcategory = if (type == TxnType.TRANSFER) null else cleanName(get(cSub)),
                    account = from, toAccount = if (type == TxnType.TRANSFER) to else null,
                    memo = title,
                    description = description,
                )
            )
        }
        return Result(rows, errors)
    }
}
