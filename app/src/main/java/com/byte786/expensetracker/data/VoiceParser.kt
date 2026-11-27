package com.byte786.expensetracker.data

import java.math.RoundingMode

/**
 * Turns a spoken sentence into a draft record, matching words against your own
 * category, subcategory and account names. No internet or AI involved.
 *
 * Examples: "500 taka bike mobil from cash", "1000 bills 600 cash 400 bkash",
 * "got 5000 salary in tuition", "transfer 2000 from cash to bkash",
 * "yesterday 230 for lunch food restaurant from cash".
 */
object VoiceParser {
    data class Cat(val id: Long, val name: String, val type: Int, val parentId: Long?)
    data class Acc(val id: Long, val name: String)

    data class Draft(
        val type: Int,
        /** Whole amount in minor units, if heard. */
        val total: Long?,
        /** Accounts in the order spoken, with their part of the amount when one was said. */
        val sources: List<Pair<Long, Long?>>,
        val toAccountId: Long?,
        val categoryId: Long?,
        val subcategoryId: Long?,
        val title: String,
        val heard: String,
        val daysAgo: Int,
    )

    private val accountWords = setOf("from", "by", "with", "using", "via", "in", "through", "paid", "into")
    private val incomeWords = setOf("income", "earned", "earn", "got", "get", "received", "receive", "salary", "sold", "credited", "profit")
    private val transferWords = setOf("transfer", "transferred", "moved", "move")
    private val fillerWords = setOf("taka", "tk", "rupees", "rs", "bdt", "dollars", "dollar")

    private fun norm(s: String): String {
        var t = s.lowercase()
            .replace("৳", " ")
            .replace(Regex("(\\d),(\\d{3})"), "$1$2")
            .replace(Regex("(\\d),(\\d{3})"), "$1$2")
            .replace(Regex("[^a-z0-9.\\s]"), " ")
        // Common ways speech recognition writes "bKash".
        t = t.replace(Regex("\\b(b\\s?kash|bi\\s?kash|be\\s?cash|bee\\s?cash|b\\s?cash|bikash|vkash)\\b"), "bkash")
        // "2k", "2 thousand", "5 hundred"
        t = t.replace(Regex("\\b(\\d+(?:\\.\\d+)?)\\s*(k|thousand)\\b")) { m -> (m.groupValues[1].toBigDecimal() * 1000.toBigDecimal()).toPlainString() }
        t = t.replace(Regex("\\b(\\d+)\\s*hundred\\b")) { m -> (m.groupValues[1].toLong() * 100).toString() }
        return t.replace(Regex("\\s+"), " ").trim()
    }

    private fun words(s: String) = norm(s).split(' ').filter { it.isNotEmpty() }

    private data class Mention(val start: Int, val end: Int, val kind: Char, val id: Long) // kind: 'a' account, 'c' category

    fun parse(text: String, cats: List<Cat>, accs: List<Acc>): Draft {
        val w = words(text).filter { it !in fillerWords }

        // Numbers (position -> minor units)
        val numbers = w.mapIndexedNotNull { i, x ->
            x.trimEnd('.').toBigDecimalOrNull()?.let { i to it.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong() }
        }.toMutableList()

        // Name mentions
        fun find(nameWords: List<String>, kind: Char, id: Long) = (0..w.size - nameWords.size).filter { i ->
            nameWords.isNotEmpty() && nameWords.indices.all { k -> w[i + k] == nameWords[k] }
        }.map { Mention(it, it + nameWords.size, kind, id) }

        val raw = accs.flatMap { find(words(it.name), 'a', it.id) } + cats.flatMap { find(words(it.name), 'c', it.id) }
        // Same words matching both an account and a category: an account only after "from/by/in…".
        val resolved = raw.filter { m ->
            val clash = raw.any { o -> o !== m && o.kind != m.kind && o.start == m.start && o.end == m.end }
            if (!clash) true else {
                val prev = w.getOrNull(m.start - 1)
                val afterPrep = prev in accountWords || (prev?.toBigDecimalOrNull() != null && w.getOrNull(m.start - 2) in accountWords)
                if (m.kind == 'a') afterPrep else !afterPrep
            }
        }
        // Prefer longer matches where they overlap.
        val mentions = resolved.sortedWith(compareByDescending<Mention> { it.end - it.start }.thenBy { it.start })
            .fold(mutableListOf<Mention>()) { acc, m ->
                if (acc.none { it.start < m.end && m.start < it.end }) acc.add(m); acc
            }.sortedBy { it.start }

        val catBy = cats.associateBy { it.id }
        val catMentions = mentions.filter { it.kind == 'c' }.mapNotNull { catBy[it.id] }
        val accMentions = mentions.filter { it.kind == 'a' }

        // Type
        val type = when {
            w.any { it in transferWords } -> TxnType.TRANSFER
            w.any { it in incomeWords } -> TxnType.INCOME
            catMentions.isNotEmpty() -> catMentions.first().type
            else -> TxnType.EXPENSE
        }

        // Category / subcategory
        var catId: Long? = null
        var subId: Long? = null
        if (type != TxnType.TRANSFER) {
            val ofType = catMentions.filter { it.type == type }
            val parentIds = ofType.filter { it.parentId == null }.map { it.id }.toSet()
            val sub = ofType.filter { it.parentId != null }.sortedByDescending { it.parentId in parentIds }.firstOrNull()
            if (sub != null) { subId = sub.id; catId = sub.parentId } else catId = ofType.firstOrNull { it.parentId == null }?.id
        }

        // Amounts per account: the number right before the account (or right after it).
        val used = mutableSetOf<Int>()
        fun numberNear(m: Mention): Long? {
            var i = m.start - 1
            while (i >= 0 && i >= m.start - 3 && (w[i] in accountWords)) i--
            numbers.firstOrNull { it.first == i && it.first !in used }?.let { used.add(it.first); return it.second }
            var j = m.end
            while (j < w.size && j <= m.end + 2 && w[j] in accountWords) j++
            numbers.firstOrNull { it.first == j && it.first !in used }?.let { used.add(it.first); return it.second }
            return null
        }

        var toAccount: Long? = null
        val sources = mutableListOf<Pair<Long, Long?>>()
        if (type == TxnType.TRANSFER) {
            accMentions.forEach { m ->
                if (w.getOrNull(m.start - 1) == "to" && toAccount == null) toAccount = m.id
                else if (sources.isEmpty()) sources.add(m.id to null)
                else if (toAccount == null) toAccount = m.id
            }
        } else if (accMentions.size > 1) {
            accMentions.distinctBy { it.id }.forEach { m -> sources.add(m.id to numberNear(m)) }
        } else {
            accMentions.firstOrNull()?.let { sources.add(it.id to null) }
        }
        val free = numbers.filter { it.first !in used }.map { it.second }
        val parts = sources.mapNotNull { it.second }.sum()
        val total = when {
            free.isNotEmpty() -> maxOf(free.max(), parts)
            parts > 0 -> parts
            else -> null
        }

        // Title: the words after "for"/"on" until an account/category word or a stop word.
        val mentionStarts = mentions.map { it.start }.toSet()
        val forIdx = w.indexOfLast { it == "for" || it == "on" }
        val title = if (forIdx < 0) "" else w.drop(forIdx + 1)
            .takeWhile { it !in accountWords && it.toBigDecimalOrNull() == null }
            .let { list ->
                val stop = list.indices.firstOrNull { (forIdx + 1 + it) in mentionStarts } ?: list.size
                list.take(stop)
            }
            .joinToString(" ").replaceFirstChar { it.uppercase() }

        val daysAgo = when {
            "day before yesterday" in w.joinToString(" ") -> 2
            "yesterday" in w -> 1
            else -> 0
        }
        return Draft(type, total, sources, toAccount, catId, subId, title, text.trim(), daysAgo)
    }
}
