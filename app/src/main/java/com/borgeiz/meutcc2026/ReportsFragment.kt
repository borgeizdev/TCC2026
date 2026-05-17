package com.borgeiz.meutcc2026

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.borgeiz.meutcc2026.model.Transaction
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class ReportsFragment : Fragment() {

    private val monthLabels = listOf(
        "Todos", "Jan", "Fev", "Mar", "Abr",
        "Mai", "Jun", "Jul", "Ago",
        "Set", "Out", "Nov", "Dez"
    )

    private val monthLabelsFull = listOf(
        "Todos os meses",
        "Janeiro", "Fevereiro", "Março", "Abril",
        "Maio", "Junho", "Julho", "Agosto",
        "Setembro", "Outubro", "Novembro", "Dezembro"
    )

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
        val spCatMonth     = view.findViewById<Spinner>(R.id.spCatMonth)

        spCatMonth.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            monthLabelsFull
        )
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        spCatMonth.setSelection(currentMonth)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return view
        val db  = FirebaseDatabase.getInstance().reference.child("users").child(uid)

        var allTransactions = listOf<Transaction>()
        var adjustment = 0.0

        fun refreshCategoryChart(monthFilter: Int) {
            val expenses = allTransactions.filter { t ->
                t.type == "despesa" && (monthFilter == 0 || run {
                    val parts = t.date.split("-")
                    parts.size >= 2 && parts[1].toIntOrNull() == monthFilter
                })
            }

            val categoryTotals = mutableMapOf<String, Double>()
            var filteredExpenseTotal = 0.0
            for (t in expenses) {
                filteredExpenseTotal += t.amount
                val cat = t.category.ifBlank { "Outros" }
                categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + t.amount
            }

            if (categoryTotals.isEmpty()) {
                pieChart.visibility = View.GONE
                tvCatBreakdown.text = "Nenhuma despesa registrada para este período."
            } else {
                pieChart.visibility = View.VISIBLE
                setupPieChart(pieChart, categoryTotals)

                val sb = StringBuilder()
                for ((cat, value) in categoryTotals.entries.sortedByDescending { it.value }) {
                    val pct = if (filteredExpenseTotal > 0) (value / filteredExpenseTotal * 100) else 0.0
                    val pctStr = if (pct < 1.0 && pct > 0.0) "%.1f".format(pct) else pct.toInt().toString()
                    sb.appendLine("$cat — R$ ${"%.2f".format(value)} ($pctStr%)")
                }
                tvCatBreakdown.text = sb.toString().trimEnd()
            }
        }

        spCatMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                refreshCategoryChart(pos)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        db.child("balanceAdjustment")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(adjSnapshot: DataSnapshot) {
                    adjustment = adjSnapshot.getValue(Double::class.java) ?: 0.0

                    db.child("transactions")
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                var totalIncome  = 0.0
                                var totalExpense = 0.0
                                val txList = mutableListOf<Transaction>()

                                for (item in snapshot.children) {
                                    val t = item.getValue(Transaction::class.java) ?: continue
                                    txList.add(t)
                                    if (t.type == "receita") totalIncome  += t.amount
                                    if (t.type == "despesa") totalExpense += t.amount
                                }
                                allTransactions = txList

                                val balance = totalIncome - totalExpense + adjustment
                                tvIncome.text  = "R$ %.2f".format(totalIncome)
                                tvExpense.text = "R$ %.2f".format(totalExpense)
                                tvBalance.text = "R$ %.2f".format(balance)

                                setupBarChart(barChart, txList)
                                refreshCategoryChart(spCatMonth.selectedItemPosition)
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        return view
    }

    private fun setupPieChart(pieChart: PieChart, categoryTotals: Map<String, Double>) {
        // Paleta profissional, sem cores berrantes
        val colors = listOf(
            Color.parseColor("#2563EB"), // azul primário
            Color.parseColor("#16A34A"), // verde
            Color.parseColor("#D97706"), // âmbar
            Color.parseColor("#DC2626"), // vermelho
            Color.parseColor("#7C3AED"), // violeta
            Color.parseColor("#0891B2"), // ciano
            Color.parseColor("#059669"), // esmeralda
            Color.parseColor("#B45309"), // marrom
            Color.parseColor("#4F46E5"), // índigo
            Color.parseColor("#BE185D")  // rosa escuro
        )

        val entries = categoryTotals.entries
            .sortedByDescending { it.value }
            .map { (cat, value) -> PieEntry(value.toFloat(), cat) }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors.take(entries.size).toMutableList()
            valueTextSize = 11f
            valueTextColor = Color.WHITE
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value < 1f && value > 0f) "${"%.1f".format(value)}%" else "${value.toInt()}%"
                }
            }
            sliceSpace = 2f
        }

        // Detecta modo escuro pelo contexto
        val isDark = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val holeColor    = if (isDark) Color.parseColor("#1E293B") else Color.WHITE
        val legendColor  = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#374151")

        pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 44f
            transparentCircleRadius = 48f
            setHoleColor(holeColor)
            setUsePercentValues(true)
            setEntryLabelColor(Color.TRANSPARENT)
            setDrawEntryLabels(false)
            legend.apply {
                isEnabled = true
                textSize = 12f
                textColor = legendColor
                formSize = 12f
                xEntrySpace = 10f
            }
            animateY(800)
            invalidate()
        }
    }

    private fun setupBarChart(barChart: BarChart, transactions: List<Transaction>) {
        // Agrupa por mês numérico
        val monthlyIncome  = mutableMapOf<Int, Float>()
        val monthlyExpense = mutableMapOf<Int, Float>()

        for (t in transactions) {
            val parts = t.date.split("-")
            if (parts.size < 2) continue
            val month = parts[1].toIntOrNull() ?: continue
            if (t.type == "receita") monthlyIncome[month]  = (monthlyIncome[month]  ?: 0f) + t.amount.toFloat()
            if (t.type == "despesa") monthlyExpense[month] = (monthlyExpense[month] ?: 0f) + t.amount.toFloat()
        }

        val allMonths = (monthlyIncome.keys + monthlyExpense.keys).toSortedSet()
        if (allMonths.isEmpty()) {
            barChart.visibility = View.GONE
            return
        }
        barChart.visibility = View.VISIBLE

        // ── CORREÇÃO DO BUG ──
        // Cada grupo ocupa 2.5f de espaço. As duas barras ficam em 0f e 1f dentro
        // do grupo. groupBars() reposiciona tudo automaticamente.
        val groupSpace = 0.4f
        val barSpace   = 0.05f
        val barWidth   = 0.25f  // 2 * barWidth + 2 * barSpace + groupSpace = 1.0f ✓
        // 2*0.25 + 2*0.05 + 0.4 = 1.0 ✓

        val incomeEntries  = mutableListOf<BarEntry>()
        val expenseEntries = mutableListOf<BarEntry>()
        val labels         = mutableListOf<String>()

        allMonths.forEachIndexed { idx, month ->
            incomeEntries.add(BarEntry(idx.toFloat(), monthlyIncome[month]  ?: 0f))
            expenseEntries.add(BarEntry(idx.toFloat(), monthlyExpense[month] ?: 0f))
            labels.add(monthLabels.getOrElse(month) { "$month" })
        }

        val isDark = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val axisTextColor  = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#6B7280")
        val axisGridColor  = if (isDark) Color.parseColor("#2D3748") else Color.parseColor("#E5E7EB")

        val incomeSet = BarDataSet(incomeEntries, "Receitas").apply {
            color = Color.parseColor("#16A34A")
            valueTextSize = 9f
            valueTextColor = axisTextColor
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float) = if (value == 0f) "" else "R$${value.toInt()}"
            }
        }
        val expenseSet = BarDataSet(expenseEntries, "Despesas").apply {
            color = Color.parseColor("#DC2626")
            valueTextSize = 9f
            valueTextColor = axisTextColor
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float) = if (value == 0f) "" else "R$${value.toInt()}"
            }
        }

        val barData = BarData(incomeSet, expenseSet).apply {
            this.barWidth = barWidth
        }

        // axisMaximum calculado manualmente: (barWidth * 2 + barSpace * 2 + groupSpace) * nGrupos
        val groupWidthVal = barWidth * 2 + barSpace * 2 + groupSpace

        barChart.apply {
            data = barData

            // groupBars precisa ser chamado DEPOIS de setar os dados
            // fromX = 0f, groupSpace, barSpace — agrupa as barras corretamente
            barData.groupBars(0f, groupSpace, barSpace)

            description.isEnabled = false
            setFitBars(true)

            xAxis.apply {
                // O centro de cada grupo fica em idx + 0.5f (metade do espaço de 1.0)
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                setCenterAxisLabels(true)
                granularity = 1f
                axisMinimum = 0f
                axisMaximum = groupWidthVal * allMonths.size
                setDrawGridLines(false)
                textSize = 11f
                textColor = axisTextColor
            }
            axisLeft.apply {
                axisMinimum = 0f
                textColor = axisTextColor
                gridColor = axisGridColor
            }
            axisRight.isEnabled = false
            legend.textColor = axisTextColor
            setBackgroundColor(Color.TRANSPARENT)
            animateY(600)
            invalidate()
        }
    }
}