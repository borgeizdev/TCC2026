package com.borgeiz.meutcc2026

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.borgeiz.meutcc2026.model.Transaction
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ReportsFragment : Fragment() {

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

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return view
        val db  = FirebaseDatabase.getInstance().reference.child("users").child(uid)

        db.child("balanceAdjustment")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(adjSnapshot: DataSnapshot) {
                    val adjustment = adjSnapshot.getValue(Double::class.java) ?: 0.0

                    db.child("transactions")
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                var totalIncome  = 0.0
                                var totalExpense = 0.0
                                val categoryTotals = mutableMapOf<String, Double>()

                                for (item in snapshot.children) {
                                    val t = item.getValue(Transaction::class.java) ?: continue
                                    if (t.type == "receita") totalIncome += t.amount
                                    if (t.type == "despesa") {
                                        totalExpense += t.amount
                                        val cat = t.category.ifBlank { "Outros" }
                                        categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + t.amount
                                    }
                                }

                                val balance = totalIncome - totalExpense + adjustment

                                tvIncome.text  = "R$ %.2f".format(totalIncome)
                                tvExpense.text = "R$ %.2f".format(totalExpense)
                                tvBalance.text = "R$ %.2f".format(balance)

                                tvBalance.setTextColor(
                                    if (balance >= 0) Color.parseColor("#22C55E")
                                    else Color.parseColor("#EF4444")
                                )

                                // Gráfico de pizza
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
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        return view
    }

    private fun setupPieChart(pieChart: PieChart, categoryTotals: Map<String, Double>) {
        // Cores vibrantes para cada fatia
        val colors = listOf(
            Color.parseColor("#6C63FF"),
            Color.parseColor("#10B981"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#EF4444"),
            Color.parseColor("#3B82F6"),
            Color.parseColor("#EC4899"),
            Color.parseColor("#8B5CF6"),
            Color.parseColor("#14B8A6")
        )

        val entries = categoryTotals.entries
            .sortedByDescending { it.value }
            .map { (cat, value) -> PieEntry(value.toFloat(), cat) }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors.take(entries.size).toMutableList()
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(pieChart)
            sliceSpace = 3f
        }

        val pieData = PieData(dataSet)

        pieChart.apply {
            data = pieData
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
            setDrawEntryLabels(false) // Deixa só a legenda, fica mais limpo
            animateY(800)
            invalidate()
        }
    }
}