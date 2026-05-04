package com.borgeiz.meutcc2026.model

data class SalaryEntry(
    var dayOfMonth: Int = 1,
    var amount: Double = 0.0
)

data class SalaryConfig(
    // Compatibilidade com versão antiga (campo único)
    var amount: Double = 0.0,
    var dayOfMonth: Int = 1,
    // Nova lista de entradas de salário (múltiplos dias)
    var entries: List<SalaryEntry> = emptyList()
) {
    /** Retorna todas as entradas válidas, mesclando legado e nova lista */
    fun resolvedEntries(): List<SalaryEntry> {
        return if (entries.isNotEmpty()) {
            entries
        } else if (amount > 0 && dayOfMonth in 1..31) {
            listOf(SalaryEntry(dayOfMonth = dayOfMonth, amount = amount))
        } else {
            emptyList()
        }
    }
}