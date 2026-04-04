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

class DashboardFragment : Fragment() {

    private lateinit var tvBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        tvBalance = view.findViewById(R.id.tvBalance)
        tvIncome = view.findViewById(R.id.tvIncome)
        tvExpense = view.findViewById(R.id.tvExpense)

        loadSummary()
        return view
    }

    private fun loadSummary() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("transactions")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var income = 0.0
                var expense = 0.0

                for (item in snapshot.children) {
                    val transaction = item.getValue(Transaction::class.java)
                    if (transaction != null) {
                        if (transaction.type == "receita") income += transaction.amount
                        if (transaction.type == "despesa") expense += transaction.amount
                    }
                }

                val balance = income - expense
                tvIncome.text = "Receitas: R$ %.2f".format(income)
                tvExpense.text = "Despesas: R$ %.2f".format(expense)
                tvBalance.text = "Saldo: R$ %.2f".format(balance)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}