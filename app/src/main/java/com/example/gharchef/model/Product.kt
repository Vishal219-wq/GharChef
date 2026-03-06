package com.example.gharchef.model

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val price: String,          // e.g. "₹249"
    val category: String,       // e.g. "🇮🇳 North Indian"
    val cookTime: String,       // e.g. "30 min"
    val imageUrl: String,       // Firebase Storage URL or drawable name
    val rating: Float,          // e.g. 4.7
    val ratingCount: Int,       // e.g. 120
    val isVeg: Boolean,         // true = veg, false = non-veg
    val isFavourite: Boolean = false
)