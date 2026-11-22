package com.byte786.expensetracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.PathFillType
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
        "smartphone" to Icons.Filled.Smartphone,
        "computer" to Icons.Filled.Computer,
        "headphones" to Icons.Filled.Headphones,
        "shield" to Icons.Filled.Shield,
        "selfimprove" to Icons.Filled.SelfImprovement,
        "mosque" to CustomIcons.Mosque,
        "perfume" to CustomIcons.Perfume,
        "grooming" to Icons.Filled.Face,
        "vape" to Icons.Filled.SmokingRooms,
        "tax" to Icons.Filled.RequestQuote,
        "paid" to Icons.Filled.Paid,
        "teacher" to Icons.Filled.CastForEducation,
        "historyedu" to Icons.Filled.HistoryEdu,
        "language" to Icons.Filled.Language,
        "translate" to Icons.Filled.Translate,
        "document" to Icons.Filled.Description,
        "job" to Icons.Filled.BusinessCenter,
        "subscriptions" to Icons.Filled.Subscriptions,
        "autorenew" to Icons.Filled.Autorenew,
        "ai" to Icons.Filled.SmartToy,
        "theatre" to Icons.Filled.TheaterComedy,
        "casino" to Icons.Filled.Casino,
        "ticket" to Icons.Filled.ConfirmationNumber,
        "sell" to Icons.Filled.Sell,
        "redeem" to Icons.Filled.Redeem,
        // Gen-Z set
        "powerup" to CustomIcons.PowerUp,
        "fire" to Icons.Filled.LocalFireDepartment,
        "sparkles" to Icons.Filled.AutoAwesome,
        "crown" to CustomIcons.Crown,
        "sigma" to CustomIcons.Sigma,
        "skull" to CustomIcons.Skull,
        "ghost" to CustomIcons.Ghost,
        "katana" to CustomIcons.Katana,
        "ramen" to CustomIcons.Ramen,
        "diamond" to CustomIcons.Diamond,
        "trophy" to Icons.Filled.EmojiEvents,
        "brain" to Icons.Filled.Psychology,
        "fight" to Icons.Filled.SportsMma,
        "nightlife" to Icons.Filled.Nightlife,
        "lol" to Icons.Filled.SentimentVerySatisfied,
        "trending" to Icons.Filled.Whatshot,
        "stream" to Icons.Filled.LiveTv,
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

/** Icons Material doesn't have, drawn on the same 24×24 grid. */
object CustomIcons {
    /** Anime-style power-up: spiky hair standing up over a head, with aura sparks. */
    val PowerUp: ImageVector by lazy {
        materialIcon(name = "Custom.PowerUp") {
            materialPath {
                // spiky hair
                moveTo(5f, 13f); lineTo(3f, 6.5f); lineTo(7.2f, 9f); lineTo(7.8f, 2.5f); lineTo(10.8f, 7.5f)
                lineTo(13.2f, 1.5f); lineTo(14.6f, 7.5f); lineTo(18.2f, 3f); lineTo(17.6f, 9f); lineTo(21.5f, 6.8f)
                lineTo(19f, 13f); close()
                // head
                moveTo(7f, 15.5f)
                arcToRelative(5f, 5f, 0f, true, false, 10f, 0f)
                arcToRelative(5f, 5f, 0f, true, false, -10f, 0f); close()
                // aura sparks
                moveTo(1.5f, 15f); lineTo(4f, 13.5f); lineTo(3.5f, 16f); lineTo(5f, 17.5f); lineTo(2f, 18f); close()
                moveTo(22.5f, 15f); lineTo(20f, 13.5f); lineTo(20.5f, 16f); lineTo(19f, 17.5f); lineTo(22f, 18f); close()
            }
        }
    }

    val Crown: ImageVector by lazy {
        materialIcon(name = "Custom.Crown") {
            materialPath {
                moveTo(3f, 7.5f); lineTo(7.5f, 12f); lineTo(12f, 4.5f); lineTo(16.5f, 12f); lineTo(21f, 7.5f)
                lineTo(19f, 18f); horizontalLineTo(5f); close()
                moveTo(5f, 19f); horizontalLineToRelative(14f); verticalLineToRelative(2f); horizontalLineToRelative(-14f); close()
            }
        }
    }

    val Sigma: ImageVector by lazy {
        materialIcon(name = "Custom.Sigma") {
            materialPath {
                moveTo(6f, 3f); horizontalLineTo(18f); verticalLineTo(6.5f); horizontalLineTo(10.6f); lineTo(15f, 12f)
                lineTo(10.6f, 17.5f); horizontalLineTo(18f); verticalLineTo(21f); horizontalLineTo(6f); verticalLineTo(18.6f)
                lineTo(11f, 12f); lineTo(6f, 5.4f); close()
            }
        }
    }

    val Skull: ImageVector by lazy {
        materialIcon(name = "Custom.Skull") {
            materialPath(pathFillType = PathFillType.EvenOdd) {
                moveTo(12f, 2f)
                curveTo(6.5f, 2f, 3f, 5.8f, 3f, 10.5f)
                curveTo(3f, 13.5f, 4.5f, 15.6f, 7f, 16.8f)
                verticalLineTo(21f); horizontalLineTo(17f); verticalLineTo(16.8f)
                curveTo(19.5f, 15.6f, 21f, 13.5f, 21f, 10.5f)
                curveTo(21f, 5.8f, 17.5f, 2f, 12f, 2f); close()
                // eyes
                moveTo(6f, 11f); arcToRelative(2.3f, 2.3f, 0f, true, true, 4.6f, 0f); arcToRelative(2.3f, 2.3f, 0f, true, true, -4.6f, 0f); close()
                moveTo(13.4f, 11f); arcToRelative(2.3f, 2.3f, 0f, true, true, 4.6f, 0f); arcToRelative(2.3f, 2.3f, 0f, true, true, -4.6f, 0f); close()
                // nose
                moveTo(12f, 13.5f); lineTo(13.2f, 15.8f); horizontalLineTo(10.8f); close()
            }
        }
    }

    val Ghost: ImageVector by lazy {
        materialIcon(name = "Custom.Ghost") {
            materialPath(pathFillType = PathFillType.EvenOdd) {
                moveTo(12f, 2f)
                curveTo(7.6f, 2f, 5f, 5.6f, 5f, 10f)
                verticalLineTo(21f); lineTo(7.3f, 19f); lineTo(9.6f, 21f); lineTo(12f, 19f); lineTo(14.4f, 21f)
                lineTo(16.7f, 19f); lineTo(19f, 21f); verticalLineTo(10f)
                curveTo(19f, 5.6f, 16.4f, 2f, 12f, 2f); close()
                moveTo(8.3f, 10f); arcToRelative(1.6f, 1.6f, 0f, true, true, 3.2f, 0f); arcToRelative(1.6f, 1.6f, 0f, true, true, -3.2f, 0f); close()
                moveTo(12.5f, 10f); arcToRelative(1.6f, 1.6f, 0f, true, true, 3.2f, 0f); arcToRelative(1.6f, 1.6f, 0f, true, true, -3.2f, 0f); close()
            }
        }
    }

    val Katana: ImageVector by lazy {
        materialIcon(name = "Custom.Katana") {
            materialPath {
                // blade
                moveTo(19.5f, 2f); lineTo(22f, 4.5f); lineTo(10.5f, 15.5f); lineTo(8.5f, 13.5f); close()
                // guard
                moveTo(6.3f, 13f); lineTo(7.8f, 11.5f); lineTo(12.5f, 16.2f); lineTo(11f, 17.7f); close()
                // handle
                moveTo(8.6f, 16.8f); lineTo(7.2f, 15.4f); lineTo(2.5f, 20.1f); lineTo(3.9f, 21.5f); close()
            }
        }
    }

    val Ramen: ImageVector by lazy {
        materialIcon(name = "Custom.Ramen") {
            materialPath {
                // chopsticks
                moveTo(8.6f, 11f); lineTo(15.8f, 2.4f); lineTo(16.9f, 3.3f); lineTo(10.4f, 11f); close()
                moveTo(12f, 11f); lineTo(20.1f, 4f); lineTo(21f, 5.1f); lineTo(14.1f, 11f); close()
                // bowl
                moveTo(3f, 12f); horizontalLineTo(21f)
                curveTo(21f, 16.4f, 17.4f, 20f, 13f, 20f); horizontalLineTo(11f)
                curveTo(6.6f, 20f, 3f, 16.4f, 3f, 12f); close()
                moveTo(8f, 20.5f); horizontalLineToRelative(8f); verticalLineToRelative(1.5f); horizontalLineToRelative(-8f); close()
            }
        }
    }

    val Diamond: ImageVector by lazy {
        materialIcon(name = "Custom.Diamond") {
            materialPath(pathFillType = PathFillType.EvenOdd) {
                moveTo(6f, 3f); horizontalLineTo(18f); lineTo(22f, 8.5f); lineTo(12f, 21f); lineTo(2f, 8.5f); close()
                // facet cut-out
                moveTo(5.2f, 9.5f); horizontalLineTo(18.8f); lineTo(12f, 18f); close()
            }
        }
    }

    val Perfume: ImageVector by lazy {
        materialIcon(name = "Custom.Perfume") {
            materialPath {
                // cap
                moveTo(10f, 2f); horizontalLineToRelative(4f); verticalLineToRelative(3f); horizontalLineToRelative(-4f); close()
                // spray nozzle
                moveTo(14.5f, 3f); horizontalLineToRelative(3.5f); verticalLineToRelative(1f); horizontalLineToRelative(-3.5f); close()
                // neck
                moveTo(11f, 5.5f); horizontalLineToRelative(2f); verticalLineToRelative(1.5f); horizontalLineToRelative(-2f); close()
                // bottle
                moveTo(8f, 7.5f); horizontalLineToRelative(8f)
                curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f); verticalLineToRelative(9.5f)
                curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f); horizontalLineTo(8f)
                curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f); verticalLineTo(9.5f)
                curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f); close()
            }
        }
    }

    val Mosque: ImageVector by lazy {
        materialIcon(name = "Custom.Mosque") {
            materialPath {
                // minarets
                moveTo(4f, 3.5f); lineTo(5.2f, 8f); horizontalLineTo(2.8f); close()
                moveTo(3f, 8.5f); horizontalLineToRelative(2f); verticalLineTo(21f); horizontalLineTo(3f); close()
                moveTo(20f, 3.5f); lineTo(21.2f, 8f); horizontalLineTo(18.8f); close()
                moveTo(19f, 8.5f); horizontalLineToRelative(2f); verticalLineTo(21f); horizontalLineToRelative(-2f); close()
                // spire
                moveTo(11.4f, 3f); horizontalLineToRelative(1.2f); verticalLineTo(6.5f); horizontalLineToRelative(-1.2f); close()
                // dome
                moveTo(6.5f, 12.5f)
                curveTo(6.5f, 9.5f, 9.5f, 8f, 12f, 6.5f)
                curveTo(14.5f, 8f, 17.5f, 9.5f, 17.5f, 12.5f); close()
                // hall
                moveTo(6.5f, 13f); horizontalLineTo(17.5f); verticalLineTo(21f); horizontalLineTo(6.5f); close()
            }
        }
    }
}
