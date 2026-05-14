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
import androidx.fragment.app.Fragment
import com.borgeiz.meutcc2026.model.Transaction
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
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

        // Spinner de mês para gastos por categoria
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
                    // Porcentagem sem arredondamento para 0 — usa 1 casa decimal
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
                                tvBalance.setTextColor(
                                    if (balance >= 0) Color.parseColor("#22C55E")
                                    else Color.parseColor("#EF4444")
                                )

                                // Gráfico de barras mensal
                                setupBarChart(barChart, txList)

                                // Gráfico de pizza com filtro de mês atual
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
        val colors = listOf(
            Color.parseColor("#6C63FF"), Color.parseColor("#10B981"),
            Color.parseColor("#F59E0B"), Color.parseColor("#EF4444"),
            Color.parseColor("#3B82F6"), Color.parseColor("#EC4899"),
            Color.parseColor("#8B5CF6"), Color.parseColor("#14B8A6"),
            Color.parseColor("#F97316"), Color.parseColor("#84CC16")
        )

        val entries = categoryTotals.entries
            .sortedByDescending { it.value }
            .map { (cat, value) -> PieEntry(value.toFloat(), cat) }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors.take(entries.size).toMutableList()
            valueTextSize = 11f
            valueTextColor = Color.WHITE
            // Formata a porcentagem sem arredondar para 0
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value < 1f && value > 0f) "${"%.1f".format(value)}%" else "${value.toInt()}%"
                }
            }
            sliceSpace = 3f
        }

        pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 42f
            transparentCircleRadius = 47f
            setHoleColor(Color.WHITE)
            setUsePercentValues(true)
            setEntryLabelColor(Color.DKGRAY)
            setEntryLabelTextSize(11f)
            legend.apply {
                isEnabled = true
                textSize = 12f
                textColor = Color.parseColor("#475569")
                formSize = 12f
                xEntrySpace = 10f
            }
            setDrawEntryLabels(false)
            animateY(800)
            invalidate()
        }
    }

    private fun setupBarChart(barChart: BarChart, transactions: List<Transaction>) {
        // Agrupa por mês (yyyy-MM)
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

        val incomeEntries  = mutableListOf<BarEntry>()
        val expenseEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        allMonths.forEachIndexed { idx, month ->
            incomeEntries.add(BarEntry(idx.toFloat() * 2f,       monthlyIncome[month]  ?: 0f))
            expenseEntries.add(BarEntry(idx.toFloat() * 2f + 0.8f, monthlyExpense[month] ?: 0f))
            labels.add(monthLabels.getOrElse(month) { "$month" })
        }

        val incomeSet = BarDataSet(incomeEntries, "Receitas").apply {
            color = Color.parseColor("#10B981")
            valueTextSize = 9f
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float) = if (value == 0f) "" else "R$${value.toInt()}"
            }
        }
        val expenseSet = BarDataSet(expenseEntries, "Despesas").apply {
            color = Color.parseColor("#EF4444")
            valueTextSize = 9f
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float) = if (value == 0f) "" else "R$${value.toInt()}"
            }
        }

        barChart.apply {
            data = BarData(incomeSet, expenseSet).apply { barWidth = 0.75f }
            description.isEnabled = false
            setFitBars(true)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(
                    allMonths.mapIndexed { idx, month ->
                        // Labels posicionados no meio dos dois grupos
                        monthLabels.getOrElse(month) { "$month" }
                    }
                )
                position = XAxis.XAxisPosition.BOTTOM
                setCenterAxisLabels(true)
                granularity = 2f
                setDrawGridLines(false)
                textSize = 11f
                textColor = Color.parseColor("#475569")
            }
            axisLeft.apply {
                axisMinimum = 0f
                textColor = Color.parseColor("#475569")
                gridColor = Color.parseColor("#F1F5F9")
            }
            axisRight.isEnabled = false
            legend.textColor = Color.parseColor("#475569")
            animateY(600)
            invalidate()
        }
    }
}
