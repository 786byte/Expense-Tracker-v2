package com.byte786.expensetracker.data

/** Default accounts and categories created on first launch. Everything can be edited later. */
object Seed {
    private val expense = listOf(
        Triple("Food", "food", 0xFFFB8C00) to listOf(
            "Groceries" to "grocery", "Restaurant" to "food", "Snacks" to "fastfood", "Tea & Coffee" to "cafe",
        ),
        Triple("Bike", "bike", 0xFF3949AB) to listOf(
            "Mobil" to "oil", "Modification" to "modify", "Parts" to "parts", "Service" to "service",
            "Fuel" to "fuel", "Parking" to "parking",
        ),
        Triple("Transport", "bus", 0xFF00ACC1) to listOf(
            "Bus" to "bus", "Rickshaw / CNG" to "taxi", "Ride share" to "car", "Train" to "train",
        ),
        Triple("Bills", "bills", 0xFF8E24AA) to listOf(
            "Electricity" to "electricity", "Internet" to "wifi", "Mobile recharge" to "phone",
            "Gas" to "gas", "Water" to "water",
        ),
        Triple("Shopping", "shopping", 0xFFD81B60) to listOf(
            "Clothes" to "clothes", "Electronics" to "electronics", "Household" to "home",
        ),
        Triple("Health", "health", 0xFFE53935) to listOf("Medicine" to "medicine", "Doctor" to "health"),
        Triple("Education", "education", 0xFF5E35B1) to listOf("Books" to "book", "Courses" to "education"),
        Triple("Entertainment", "movie", 0xFFF4511E) to listOf("Movies" to "movie", "Games" to "games", "Outing" to "travel"),
        Triple("Family", "family", 0xFF6D4C41) to emptyList(),
        Triple("Rent", "rent", 0xFF546E7A) to emptyList(),
        Triple("Gifts & Charity", "gift", 0xFFC0CA33) to emptyList(),
        Triple("Other", "other", 0xFF757575) to emptyList(),
    )

    private val income = listOf(
        Triple("Salary", "salary", 0xFF43A047) to emptyList<Pair<String, String>>(),
        Triple("Business", "business", 0xFF00897B) to emptyList(),
        Triple("Freelance", "laptop", 0xFF1E88E5) to emptyList(),
        Triple("Gift", "gift", 0xFFC0CA33) to emptyList(),
        Triple("Interest / Profit", "interest", 0xFF7CB342) to emptyList(),
        Triple("Other", "other", 0xFF757575) to emptyList(),
    )

    suspend fun run(dao: AppDao) {
        dao.putAccount(Account(name = "Cash", icon = "cash", color = 0xFF43A047, sortOrder = 0))
        dao.putAccount(Account(name = "Bank", icon = "bank", color = 0xFF1E88E5, sortOrder = 1))
        dao.putAccount(Account(name = "bKash", icon = "phone", color = 0xFFD81B60, sortOrder = 2))

        suspend fun insert(type: Int, list: List<Pair<Triple<String, String, Long>, List<Pair<String, String>>>>) {
            list.forEachIndexed { i, (parent, subs) ->
                val (name, icon, color) = parent
                val id = dao.putCategory(Category(name = name, type = type, icon = icon, color = color, sortOrder = i))
                subs.forEachIndexed { j, (subName, subIcon) ->
                    dao.putCategory(
                        Category(name = subName, type = type, parentId = id, icon = subIcon, color = color, sortOrder = j)
                    )
                }
            }
        }
        insert(TxnType.EXPENSE, expense)
        insert(TxnType.INCOME, income)
    }
}
