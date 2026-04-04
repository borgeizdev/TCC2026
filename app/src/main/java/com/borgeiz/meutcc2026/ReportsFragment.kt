package com.borgeiz.meutcc2026

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.borgeiz.meutcc2026.model.Transaction
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
        val tvReport = view.findViewById<TextView>(R.id.tvReport)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return view
        val ref = FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("transactions")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalIncome = 0.0
                var totalExpense = 0.0

                for (item in snapshot.children) {
                    val transaction = item.getValue(Transaction::class.java)
                    if (transaction != null) {
                        if (transaction.type == "receita") totalIncome += transaction.amount
                        if (transaction.type == "despesa") totalExpense += transaction.amount
                    }
                }

                val balance = totalIncome - totalExpense
                tvReport.text = """
                    RELATÓRIO GERAL

                    Total de receitas: R$ %.2f
                    Total de despesas: R$ %.2f
                    Saldo final: R$ %.2f
                """.trimIndent().format(totalIncome, totalExpense, balance)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        return view
    }
}