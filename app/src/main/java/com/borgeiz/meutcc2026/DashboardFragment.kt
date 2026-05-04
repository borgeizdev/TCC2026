package com.borgeiz.meutcc2026

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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


    private var transactionBalance = 0.0
    private var manualAdjustment   = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        tvBalance = view.findViewById(R.id.tvBalance)
        tvIncome  = view.findViewById(R.id.tvIncome)
        tvExpense = view.findViewById(R.id.tvExpense)

        loadManualAdjustment()
        loadSummary()

        // Clique no saldo abre diálogo para ajustar
        tvBalance.setOnClickListener { showEditBalanceDialog() }

        return view
    }

    private fun loadManualAdjustment() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("balanceAdjustment")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    manualAdjustment = snapshot.getValue(Double::class.java) ?: 0.0
                    updateBalanceDisplay()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadSummary() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("transactions")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var income  = 0.0
                    var expense = 0.0

                    for (item in snapshot.children) {
                        val t = item.getValue(Transaction::class.java) ?: continue
                        if (t.type == "receita") income  += t.amount
                        if (t.type == "despesa") expense += t.amount
                    }

                    transactionBalance = income - expense
                    tvIncome.text  = "R$ %.2f".format(income)
                    tvExpense.text = "R$ %.2f".format(expense)
                    updateBalanceDisplay()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateBalanceDisplay() {
        val total = transactionBalance + manualAdjustment
        tvBalance.text = "R$ %.2f".format(total)
        tvBalance.setTextColor(
            if (total >= 0) android.graphics.Color.parseColor("#FFFFFF")
            else android.graphics.Color.parseColor("#FCA5A5")
        )
    }

    private fun showEditBalanceDialog() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val context = requireContext()

        val input = EditText(context).apply {
            hint = "Ex: 1500.00"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(if (manualAdjustment != 0.0) "%.2f".format(manualAdjustment) else "")
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(context)
            .setTitle("Ajustar saldo")
            .setMessage("Informe um valor de ajuste manual (positivo ou negativo). Esse valor é somado ao saldo das transações.")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val value = input.text.toString().replace(",", ".").toDoubleOrNull()
                if (value == null) {
                    Toast.makeText(context, "Valor inválido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                FirebaseDatabase.getInstance().reference
                    .child("users").child(uid).child("balanceAdjustment")
                    .setValue(value)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Saldo atualizado!", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNeutralButton("Resetar") { _, _ ->
                FirebaseDatabase.getInstance().reference
                    .child("users").child(uid).child("balanceAdjustment")
                    .setValue(0.0)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
