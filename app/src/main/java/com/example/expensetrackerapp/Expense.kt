package com.example.expensetrackerapp.model


data class Expense(
    val name: String = "",
    val amount: String = "",
    val category: String = "",
    val timestamp: Long = 0L,
    val date: String = ""
)
