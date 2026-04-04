package com.borgeiz.meutcc2026.model

data class Transaction(
    var id: String = "",
    var type: String = "",
    var title: String = "",
    var amount: Double = 0.0,
    var category: String = "",
    var date: String = "",
    var description: String = ""
)
