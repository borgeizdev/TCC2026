package com.borgeiz.meutcc2026.model

data class SalaryEntry(
    var dayOfMonth: Int = 1,
    var amount: Double = 0.0
)

data class SalaryConfig(
    var amount: Double? = null,
    var dayOfMonth: Int? = null,
    var entries: List<SalaryEntry> = emptyList()
) {
    fun resolvedEntries(): List<SalaryEntry> {
        return if (entries.isNotEmpty()) {
            entries
        } else if ((amount ?: 0.0) > 0 && (dayOfMonth ?: 0) in 1..31) {
            listOf(SalaryEntry(dayOfMonth = dayOfMonth!!, amount = amount!!))
        } else {
            emptyList()
        }
    }
}