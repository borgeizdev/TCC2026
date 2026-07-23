package com.borgeiz.meutcc2026.util

/**
 * Converte texto digitado em formato monetário pt-BR para Double.
 * Se houver vírgula, ela é tratada como separador decimal e os pontos
 * (se houver) como separador de milhar (ex: "1.234,56" -> 1234.56).
 * Sem vírgula, o texto é interpretado como já está (ex: "150.00" -> 150.0).
 */
fun parseAmountPtBr(raw: String?): Double? {
    val trimmed = raw?.trim() ?: return null
    if (trimmed.isEmpty()) return null
    val normalized = if (trimmed.contains(',')) {
        trimmed.replace(".", "").replace(",", ".")
    } else {
        trimmed
    }
    return normalized.toDoubleOrNull()
}
