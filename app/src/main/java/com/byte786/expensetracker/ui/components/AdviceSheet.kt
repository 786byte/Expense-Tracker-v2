@file:OptIn(ExperimentalMaterial3Api::class)

package com.byte786.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.data.AccountBalance
import com.byte786.expensetracker.data.Advice
import com.byte786.expensetracker.data.CatMood
import com.byte786.expensetracker.data.MoodRules
import com.byte786.expensetracker.data.Txn
import com.byte786.expensetracker.data.TxnType
import com.byte786.expensetracker.data.own
import com.byte786.expensetracker.data.totalBalance
import com.byte786.expensetracker.ui.Dates
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.theme.LocalCurrency
import java.time.YearMonth

/** Everything the status cat looks at. */
data class Status(
    val mood: CatMood,
    val total: Long,
    val monthIncome: Long,
    val monthExpense: Long,
    /** Usual monthly income: average of the last 3 months that had income. */
    val refIncome: Long,
)

@Composable
fun rememberStatus(txns: List<Txn>, accs: List<AccountBalance>): Status {
    val prefs = ExpenseApp.instance.repo.prefs
    val neutral by prefs.moodNeutralPct.collectAsState()
    val very by prefs.moodVeryPct.collectAsState()
    return remember(txns, accs, neutral, very) {
        val now = YearMonth.now()
        val (f, t) = Dates.monthRange(now)
        val tracking = accs.filter { it.account.tracking }.map { it.account.id }.toSet()
        val mine = txns.own(tracking)
        val month = mine.filter { it.date >= f && it.date < t }
        val inc = month.filter { it.type == TxnType.INCOME }.sumOf { it.amount }
        val exp = month.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount }
        val ref = MoodRules.averageIncome(mine, (1..3).map { Dates.monthRange(now.minusMonths(it.toLong())) })
        val total = accs.totalBalance()
        Status(statusMood(total, inc, exp, ref, neutral, very), total, inc, exp, ref)
    }
}

/** Tap the status cat: its mood plus rule-based advice. */
@Composable
fun AdviceSheet(status: Status, onDismiss: () -> Unit) {
    val repo = ExpenseApp.instance.repo
    val txns by repo.txns.collectAsState()
    val cats by repo.categories.collectAsState()
    val accs by repo.accounts.collectAsState()
    val currency = LocalCurrency.current
    val tips = remember(txns, cats, accs, status, currency) {
        val names = cats.associate { it.id to it.name }
        Advice.tips(
            txns.own(accs.filter { it.account.tracking }.map { it.account.id }.toSet()),
            accs.filter { !it.account.tracking }.map { Advice.AccountInfo(it.account.name, it.balance) },
            categoryName = { names[it] }, refIncome = status.refIncome,
            money = { Money.format(it, currency) },
        )
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CatFace(status.mood, Modifier.size(88.dp), description = moodName(status.mood))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(moodName(status.mood), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(moodReason(status, currency), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            tips.forEach { tip ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp),
                ) {
                    Text(tip.emoji, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(tip.title, fontWeight = FontWeight.Bold)
                        Text(tip.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text(
                "The cat compares your total balance with your usual monthly income" +
                    (if (status.refIncome > 0) " (${Money.format(status.refIncome, currency)})" else "") +
                    ". Change how strict it is in Backup & settings → Status cat.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun moodReason(s: Status, currency: String): String = when (s.mood) {
    CatMood.VERY_DEBT -> "Total balance is ${Money.format(s.total, currency)}: deep below zero."
    CatMood.DEBT -> "Total balance is ${Money.format(s.total, currency)}: a little below zero."
    CatMood.ANGRY -> "This month you spent ${Money.format(s.monthExpense - s.monthIncome, currency)} more than you earned."
    CatMood.NEUTRAL -> "Total balance is ${Money.format(s.total, currency)}: about even."
    CatMood.PLUS -> "Total balance is ${Money.format(s.total, currency)}: in plus."
    CatMood.RICH -> "Total balance is ${Money.format(s.total, currency)}: well in plus!"
    CatMood.PLAIN -> ""
}
