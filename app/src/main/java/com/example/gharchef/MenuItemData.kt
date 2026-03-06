package com.example.gharchef

data class MenuItemData(
    val id: String          = "",
    val name: String        = "",
    val description: String = "",
    val price: Double       = 0.0,
    val imageUrl: String    = "",
    val category: String    = "",
    val prepTime: String    = "20 mins",
    val rating: Double      = 4.0,
    val available: Boolean  = true,
    val popular: Boolean    = false
)