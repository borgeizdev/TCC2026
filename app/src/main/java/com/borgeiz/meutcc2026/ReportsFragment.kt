package com.borgeiz.meutcc2026

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.borgeiz.meutcc2026.model.Transaction
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ReportsFragment : Fragment() {

    // Guarda as transações por índice de mês no gráfico
    private val monthlyTransactions = mutableMapOf<Int, List<Transaction>>()
    // Guarda os labels para lookup no clique
    private val monthLabels = mutableListOf<String>()
    // Índices 0 = despesas, 1 = receitas no BarData
    private val DATASET_EXPENSE = 0
    private val DATASET_INCOME  = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_reports, container, false)

        val tvIncome       = view.findViewById<TextView>(R.id.tvIncome)
        val tvExpense      = view.findViewById<TextView>(R.id.tvExpense)
        val tvBalance      = view.findViewById<TextView>(R.id.tvBalance)
        val tvCatBreakdown = view.findViewById<TextView>(R.id.tvCatBreakdown)
        val pieChart       = view.findViewById<PieChart>(R.id.pieChart)
        val barChart       = view.findViewById<BarChart>(R.id.barChart)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return view
        val db  = FirebaseDatabase.getInstance().reference.child("users").child(uid)

        db.child("balanceAdjustment").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(adjSnapshot: DataSnapshot) {
                val adjustment = adjSnapshot.getValue(Double::class.java) ?: 0.0
                db.child("transactions").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var totalIncome  = 0.0
                        var totalExpense = 0.0
                        val categoryTotals = mutableMapOf<String, Double>()
                        val byYearMonth    = mutableMapOf<String, MutableList<Transaction>>()

                        for (item in snapshot.children) {
                            val t = item.getValue(Transaction::class.java) ?: continue
                            if (t.type == "receita") totalIncome += t.amount
                            if (t.type == "despesa") {
                                totalExpense += t.amount
                                val cat = t.category.ifBlank { "Outros" }
                                categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + t.amount
                            }
                            val ym = if (t.date.length >= 7) t.date.substring(0, 7) else "sem data"
                            byYearMonth.getOrPut(ym) { mutableListOf() }.add(t)
                        }

                        val balance = totalIncome - totalExpense + adjustment
                        tvIncome.text  = "R$ ${"%.2f".format(totalIncome)}"
                        tvExpense.text = "R$ ${"%.2f".format(totalExpense)}"
                        tvBalance.text = "R$ ${"%.2f".format(balance)}"
                        tvBalance.setTextColor(
                            if (balance >= 0) Color.parseColor("#22C55E") else Color.parseColor("#EF4444")
                        )

                        if (categoryTotals.isEmpty()) {
                            pieChart.visibility = View.GONE
                            tvCatBreakdown.text = "Nenhuma despesa registrada."
                        } else {
                            pieChart.visibility = View.VISIBLE
                            setupPieChart(pieChart, categoryTotals)
                            val sb = StringBuilder()
                            for ((cat, value) in categoryTotals.entries.sortedByDescending { it.value }) {
                                val pct = if (totalExpense > 0) (value / totalExpense * 100).toInt() else 0
                                sb.appendLine("$cat — R$ ${"%.2f".format(value)} ($pct%)")
                            }
                            tvCatBreakdown.text = sb.toString().trimEnd()
                        }

                        setupBarChart(barChart, byYearMonth)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        return view
    }

    // ───────────── BAR CHART ─────────────

    private fun setupBarChart(chart: BarChart, byYearMonth: Map<String, List<Transaction>>) {
        // Filtra apenas meses que têm pelo menos uma transação com valor
        val sortedKeys = byYearMonth.keys
            .filter { it != "sem data" }
            .filter { ym ->
                val txs = byYearMonth[ym] ?: return@filter false
                txs.any { it.amount > 0 }
            }
            .sorted()

        if (sortedKeys.isEmpty()) { chart.visibility = View.GONE; return }
        chart.visibility = View.VISIBLE
        monthlyTransactions.clear()
        monthLabels.clear()

        val monthNames = listOf("Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez")

        sortedKeys.forEachIndexed { idx, ym ->
            monthlyTransactions[idx] = byYearMonth[ym] ?: emptyList()
            val parts = ym.split("-")
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
            val y = parts.getOrNull(0)?.takeLast(2) ?: ""
            monthLabels.add("${monthNames.getOrElse(m - 1) { ym }}/$y")
        }

        val barWidth   = 0.35f
        val groupSpace = 0.1f
        val barSpace   = 0.025f

        val expenseEntries = mutableListOf<BarEntry>()
        val incomeEntries  = mutableListOf<BarEntry>()

        sortedKeys.forEachIndexed { idx, ym ->
            val txs = byYearMonth[ym] ?: emptyList()
            val exp = txs.filter { it.type == "despesa" }.sumOf { it.amount }.toFloat()
            val inc = txs.filter { it.type == "receita" }.sumOf { it.amount }.toFloat()
            expenseEntries.add(BarEntry(idx.toFloat(), exp))
            incomeEntries.add(BarEntry(idx.toFloat(), inc))
        }

        val expenseSet = BarDataSet(expenseEntries, "Despesas").apply {
            color = Color.parseColor("#EF4444")
            valueTextColor = Color.TRANSPARENT
            valueTextSize = 0f
        }
        val incomeSet = BarDataSet(incomeEntries, "Receitas").apply {
            color = Color.parseColor("#22C55E")
            valueTextColor = Color.TRANSPARENT
            valueTextSize = 0f
        }

        val barData = BarData(expenseSet, incomeSet).apply { this.barWidth = barWidth }

        chart.apply {
            data = barData
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setFitBars(true)
            legend.apply { isEnabled = true; textSize = 12f; textColor = Color.parseColor("#475569") }

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setCenterAxisLabels(true)
                valueFormatter = IndexAxisValueFormatter(monthLabels)
                textColor = Color.parseColor("#475569")
                textSize = 10f
                setDrawGridLines(false)
                axisMinimum = 0f
                axisMaximum = sortedKeys.size.toFloat()
                labelRotationAngle = -30f
            }
            axisLeft.apply {
                axisMinimum = 0f
                textColor = Color.parseColor("#64748B")
                textSize = 10f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E2E8F0")
                val maxVal = (expenseEntries + incomeEntries).maxOfOrNull { it.y } ?: 0f
                granularity = when {
                    maxVal <= 500   -> 100f
                    maxVal <= 2000  -> 200f
                    maxVal <= 5000  -> 500f
                    maxVal <= 10000 -> 1000f
                    else            -> 2000f
                }
                isGranularityEnabled = true
            }
            axisRight.isEnabled = false

            // Clique na barra vermelha (despesas) ou verde (receitas)
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val idx        = e?.x?.toInt() ?: return
                    val datasetIdx = h?.dataSetIndex ?: return
                    val txs        = monthlyTransactions[idx] ?: return
                    val label      = monthLabels.getOrElse(idx) { "Mês" }
                    if (datasetIdx == DATASET_EXPENSE) {
                        showDetailDialog(label, txs.filter { it.type == "despesa" }, "despesa")
                    } else {
                        showDetailDialog(label, txs.filter { it.type == "receita" }, "receita")
                    }
                }
                override fun onNothingSelected() {}
            })

            groupBars(0f, groupSpace, barSpace)
            notifyDataSetChanged()
            invalidate()
            animateY(600)
        }
    }

    /**
     * Mostra detalhe somente do tipo clicado (receitas ou despesas).
     */
    private fun showDetailDialog(monthLabel: String, txs: List<Transaction>, type: String) {
        val isIncome = type == "receita"
        val icon     = if (isIncome) "📥" else "📤"
        val typeLabel = if (isIncome) "Receitas" else "Despesas"
        val total    = txs.sumOf { it.amount }

        val sb = StringBuilder()
        sb.appendLine("$icon $typeLabel em $monthLabel")
        sb.appendLine("Total: R$ ${"%.2f".format(total)}")
        sb.appendLine()

        if (txs.isEmpty()) {
            sb.appendLine("Nenhuma transação neste mês.")
        } else {
            sb.appendLine("── Transações ──")
            for (t in txs.sortedByDescending { it.date }) {
                val bullet = if (isIncome) "🟢" else "🔴"
                val cat    = if (t.category.isNotBlank()) " [${t.category}]" else ""
                sb.appendLine("$bullet ${t.date}  ${t.title}$cat")
                sb.appendLine("     R$ ${"%.2f".format(t.amount)}")
                if (t.description.isNotBlank()) sb.appendLine("     ${t.description}")
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("$icon $typeLabel — $monthLabel")
            .setMessage(sb.toString().trimEnd())
            .setPositiveButton("Fechar", null)
            .show()
    }

    // ───────────── PIE CHART ─────────────

    private fun setupPieChart(pieChart: PieChart, categoryTotals: Map<String, Double>) {
        val colors = listOf(
            Color.parseColor("#6C63FF"), Color.parseColor("#10B981"),
            Color.parseColor("#F59E0B"), Color.parseColor("#EF4444"),
            Color.parseColor("#3B82F6"), Color.parseColor("#EC4899"),
            Color.parseColor("#8B5CF6"), Color.parseColor("#14B8A6")
        )

        val entries = categoryTotals.entries
            .sortedByDescending { it.value }
            .map { (cat, value) -> PieEntry(value.toFloat(), cat) }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors.take(entries.size).toMutableList()
            valueTextSize = 12f; valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(pieChart); sliceSpace = 3f
        }

        pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true; holeRadius = 42f; transparentCircleRadius = 47f
            setHoleColor(Color.WHITE); setUsePercentValues(true)
            setEntryLabelColor(Color.DKGRAY); setEntryLabelTextSize(11f)
            setDrawEntryLabels(false)
            legend.apply { isEnabled = true; textSize = 12f; textColor = Color.parseColor("#475569"); formSize = 12f; xEntrySpace = 10f }
            animateY(800); invalidate()
        }
    }
}
