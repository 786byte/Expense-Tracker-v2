package com.byte786.expensetracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class CsvParserTest {
    private val zone = ZoneId.of("Asia/Dhaka")

    // Same shape as a real export: trailing spaces after each line, multi-line notes,
    // an unescaped quoted note, and "From->To" transfers.
    private val sample = "\"TIME\",\"TYPE\",\"AMOUNT\",\"CATEGORY\",\"ACCOUNT\",\"NOTES\" \n" +
        "\"Aug 01, 2026 10:39 AM\",\"(+) Income\",\"5500.00\",\"Gift\",\"Bkash\",\"for repair\" \n" +
        "\"Aug 02, 2026 10:06 AM\",\"(-) Expense\",\"1,750.50\",\"Subscription\",\"Bkash\",\"\"Premium plan\"\" \n" +
        "\"Aug 03, 2026 9:02 PM\",\"(-) Expense\",\"1680.00\",\"Theatre\",\"Cash\",\"Tickets = 1000\nPopcorn = 320\" \n" +
        "\"Aug 15, 2026 9:23 PM\",\"(*) Transfer\",\"4600.00\",\"  -  \",\"Savings Box->Loan\",\"\" \n" +
        "\"not a date\",\"(-) Expense\",\"10\",\"Food\",\"Cash\",\"\" \n"

    @Test fun parsesAllValidRows() {
        val r = CsvParser.parse(sample, zone)
        assertEquals(4, r.rows.size)
        assertEquals(1, r.errors.size)
    }

    @Test fun readsFields() {
        val rows = CsvParser.parse(sample, zone).rows
        val income = rows[0]
        assertEquals(TxnType.INCOME, income.type)
        assertEquals(550000L, income.amount)
        assertEquals("Gift", income.category)
        assertEquals("Bkash", income.account)
        assertEquals(LocalDateTime.of(2026, 8, 1, 10, 39).atZone(zone).toInstant().toEpochMilli(), income.date)

        assertEquals(175050L, rows[1].amount)
        assertEquals("\"Premium plan\"", rows[1].memo)
        assertEquals("Tickets = 1000\nPopcorn = 320", rows[2].memo)
        assertEquals(LocalDateTime.of(2026, 8, 3, 21, 2).atZone(zone).toInstant().toEpochMilli(), rows[2].date)
    }

    @Test fun splitsTransfers() {
        val t = CsvParser.parse(sample, zone).rows[3]
        assertEquals(TxnType.TRANSFER, t.type)
        assertEquals("Savings Box", t.account)
        assertEquals("Loan", t.toAccount)
        assertNull(t.category)
    }

    @Test fun standardQuotingStillWorks() {
        val recs = CsvParser.split("a,\"say \"\"hi\"\", ok\",\"x,y\"\n1,,3\n")
        assertEquals(listOf("a", "say \"hi\", ok", "x,y"), recs[0])
        assertEquals(listOf("1", "", "3"), recs[1])
    }

    @Test fun readsOwnExportFormat() {
        val csv = "Date,Type,Amount,Account,To account,Category,Subcategory,Memo\n" +
            "\"2026-11-01 17:23\",\"Expense\",\"3450.00\",\"Cash\",\"\",\"Bike\",\"Parts\",\"brake pads\"\n"
        val row = CsvParser.parse(csv, zone).rows.single()
        assertEquals("Bike", row.category)
        assertEquals("Parts", row.subcategory)
        assertEquals(345000L, row.amount)
        assertTrue(row.toAccount == null)
    }
}
