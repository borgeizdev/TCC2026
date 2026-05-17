package com.borgeiz.meutcc2026.ui

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color

/**
 * Paleta centralizada para uso programático (gráficos MPAndroidChart, etc.)
 * Sincronizada com colors.xml — Paleta TCC2026
 * Uso: val c = AppColors(resources); c.primary, c.income, c.textSecondary …
 */
class AppColors(resources: Resources) {

    private val isDark = (resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    // Backgrounds
    val bgPage  = if (isDark) Color.parseColor("#0F172A") else Color.parseColor("#F7F9FC")
    val bgCard  = if (isDark) Color.parseColor("#111827") else Color.parseColor("#FFFFFF")
    val bgInput = if (isDark) Color.parseColor("#172033") else Color.parseColor("#F1F5F9")

    // Primary
    val primary   = if (isDark) Color.parseColor("#38BDF8") else Color.parseColor("#2563EB")
    val onPrimary = if (isDark) Color.parseColor("#0F172A") else Color.parseColor("#FFFFFF")

    // Text
    val textPrimary   = if (isDark) Color.parseColor("#E5E7EB") else Color.parseColor("#1E293B")
    val textSecondary = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")

    // Border / grid
    val border = if (isDark) Color.parseColor("#1E293B") else Color.parseColor("#E2E8F0")

    // Semantic
    val income     = if (isDark) Color.parseColor("#22C55E") else Color.parseColor("#16A34A")
    val incomeBg   = if (isDark) Color.parseColor("#052E16") else Color.parseColor("#F0FDF4")
    val incomeText = if (isDark) Color.parseColor("#86EFAC") else Color.parseColor("#14532D")

    val expense     = if (isDark) Color.parseColor("#F87171") else Color.parseColor("#DC2626")
    val expenseBg   = if (isDark) Color.parseColor("#3B0000") else Color.parseColor("#FEF2F2")
    val expenseText = if (isDark) Color.parseColor("#FECACA") else Color.parseColor("#7F1D1D")

    // Gráficos — paleta categórica profissional (10 cores)
    val chartColors = listOf(
        if (isDark) Color.parseColor("#38BDF8") else Color.parseColor("#2563EB"), // azul primário
        if (isDark) Color.parseColor("#22C55E") else Color.parseColor("#16A34A"), // verde
        if (isDark) Color.parseColor("#FB923C") else Color.parseColor("#D97706"), // âmbar
        if (isDark) Color.parseColor("#F87171") else Color.parseColor("#DC2626"), // vermelho
        if (isDark) Color.parseColor("#A78BFA") else Color.parseColor("#7C3AED"), // violeta
        if (isDark) Color.parseColor("#34D399") else Color.parseColor("#059669"), // esmeralda
        if (isDark) Color.parseColor("#60A5FA") else Color.parseColor("#0284C7"), // sky
        if (isDark) Color.parseColor("#FBBF24") else Color.parseColor("#B45309"), // laranja escuro
        if (isDark) Color.parseColor("#818CF8") else Color.parseColor("#4338CA"), // índigo
        if (isDark) Color.parseColor("#F472B6") else Color.parseColor("#BE185D")  // rosa
    )
}