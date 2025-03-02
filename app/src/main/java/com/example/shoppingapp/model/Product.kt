package com.example.shoppingapp.model

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val category: String = "", // Added for filtering
    var quantity: Int = 0
)