package com.byte786.expensetracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/** Icons you can choose for categories and accounts. Stored by key in the database. */
object AppIcons {
    val all: LinkedHashMap<String, ImageVector> = linkedMapOf(
        "food" to Icons.Filled.Restaurant,
        "grocery" to Icons.Filled.LocalGroceryStore,
        "fastfood" to Icons.Filled.Fastfood,
        "cafe" to Icons.Filled.LocalCafe,
        "bike" to Icons.Filled.TwoWheeler,
        "oil" to Icons.Filled.WaterDrop,
        "parts" to Icons.Filled.Settings,
        "service" to Icons.Filled.Build,
        "modify" to Icons.Filled.Construction,
        "fuel" to Icons.Filled.LocalGasStation,
        "parking" to Icons.Filled.LocalParking,
        "car" to Icons.Filled.DirectionsCar,
        "bus" to Icons.Filled.DirectionsBus,
        "taxi" to Icons.Filled.LocalTaxi,
        "train" to Icons.Filled.Train,
        "flight" to Icons.Filled.Flight,
        "bills" to Icons.Filled.Receipt,
        "electricity" to Icons.Filled.Bolt,
        "wifi" to Icons.Filled.Wifi,
        "phone" to Icons.Filled.PhoneAndroid,
        "gas" to Icons.Filled.Whatshot,
        "water" to Icons.Filled.Opacity,
        "shopping" to Icons.Filled.ShoppingBag,
        "cart" to Icons.Filled.ShoppingCart,
        "clothes" to Icons.Filled.Checkroom,
        "electronics" to Icons.Filled.Devices,
        "laptop" to Icons.Filled.Laptop,
        "tv" to Icons.Filled.Tv,
        "health" to Icons.Filled.LocalHospital,
        "medicine" to Icons.Filled.LocalPharmacy,
        "fitness" to Icons.Filled.FitnessCenter,
        "education" to Icons.Filled.School,
        "book" to Icons.Filled.MenuBook,
        "movie" to Icons.Filled.Movie,
        "games" to Icons.Filled.SportsEsports,
        "music" to Icons.Filled.MusicNote,
        "sports" to Icons.Filled.SportsSoccer,
        "travel" to Icons.Filled.FlightTakeoff,
        "hotel" to Icons.Filled.Hotel,
        "family" to Icons.Filled.FamilyRestroom,
        "child" to Icons.Filled.ChildCare,
        "pets" to Icons.Filled.Pets,
        "home" to Icons.Filled.Home,
        "rent" to Icons.Filled.Apartment,
        "tools" to Icons.Filled.Handyman,
        "beauty" to Icons.Filled.Spa,
        "haircut" to Icons.Filled.ContentCut,
        "laundry" to Icons.Filled.LocalLaundryService,
        "gift" to Icons.Filled.CardGiftcard,
        "party" to Icons.Filled.Celebration,
        "charity" to Icons.Filled.VolunteerActivism,
        "salary" to Icons.Filled.Work,
        "business" to Icons.Filled.Business,
        "interest" to Icons.Filled.Insights,
        "savings" to Icons.Filled.Savings,
        "bank" to Icons.Filled.AccountBalance,
        "wallet" to Icons.Filled.AccountBalanceWallet,
        "cash" to Icons.Filled.Payments,
        "card" to Icons.Filled.CreditCard,
        "money" to Icons.Filled.AttachMoney,
        "security" to Icons.Filled.Security,
        "star" to Icons.Filled.Star,
        "heart" to Icons.Filled.Favorite,
        "category" to Icons.Filled.Category,
        "other" to Icons.Filled.MoreHoriz,
    )

    fun get(key: String): ImageVector = all[key] ?: Icons.Filled.Category

    val palette: List<Long> = listOf(
        0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB, 0xFF1E88E5,
        0xFF039BE5, 0xFF00ACC1, 0xFF00897B, 0xFF43A047, 0xFF7CB342, 0xFFC0CA33,
        0xFFFDD835, 0xFFFFB300, 0xFFFB8C00, 0xFFF4511E, 0xFF6D4C41, 0xFF757575, 0xFF546E7A, 0xFF263238,
    )
}
