package com.borgeiz.meutcc2026

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.borgeiz.meutcc2026.adapter.TransactionAdapter
import com.borgeiz.meutcc2026.model.Transaction
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DashboardFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView

    // Busca
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: TextView
    private lateinit var recyclerSearch: RecyclerView
    private lateinit var tvSearchEmpty: TextView
    private val allTransactions = mutableListOf<Transaction>()

    private var transactionBalance = 0.0
    private var manualAdjustment   = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvBalance  = view.findViewById(R.id.tvBalance)
        tvIncome   = view.findViewById(R.id.tvIncome)
        tvExpense  = view.findViewById(R.id.tvExpense)

        // Busca
        etSearch        = view.findViewById(R.id.etSearch)
        btnClearSearch  = view.findViewById(R.id.btnClearSearch)
        recyclerSearch  = view.findViewById(R.id.recyclerSearch)
        tvSearchEmpty   = view.findViewById(R.id.tvSearchEmpty)

        recyclerSearch.layoutManager = LinearLayoutManager(requireContext())

        // Botão Nova Transferência → navega para AddTransactionFragment
        val btnNovaTransferencia = view.findViewById<MaterialButton>(R.id.btnNovaTransferencia)
        btnNovaTransferencia.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, AddTransactionFragment())
                .addToBackStack(null)
                .commit()
        }

        // Listener de busca em tempo real
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                applySearch(query)
            }
        })

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
            recyclerSearch.visibility = View.GONE
            tvSearchEmpty.visibility  = View.GONE
        }

        loadUserName()
        loadManualAdjustment()
        loadSummaryAndTransactions()

        tvBalance.setOnClickListener { showEditBalanceDialog() }

        return view
    }

    private fun applySearch(query: String) {
        if (query.isEmpty()) {
            recyclerSearch.visibility = View.GONE
            tvSearchEmpty.visibility  = View.GONE
            return
        }
        val results = allTransactions.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
        }
        if (results.isEmpty()) {
            recyclerSearch.visibility = View.GONE
            tvSearchEmpty.visibility  = View.VISIBLE
        } else {
            tvSearchEmpty.visibility  = View.GONE
            recyclerSearch.visibility = View.VISIBLE
            recyclerSearch.adapter    = TransactionAdapter(results)
        }
    }

    private fun loadUserName() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("profile").child("name")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.getValue(String::class.java)?.trim() ?: ""
                    tvGreeting.text = if (name.isNotBlank()) "Olá, $name! 👋" else "Olá! 👋"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
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

    private fun loadSummaryAndTransactions() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("transactions")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var income  = 0.0
                    var expense = 0.0
                    allTransactions.clear()
                    for (item in snapshot.children) {
                        val t = item.getValue(Transaction::class.java) ?: continue
                        allTransactions.add(t)
                        if (t.type == "receita") income  += t.amount
                        if (t.type == "despesa") expense += t.amount
                    }
                    allTransactions.sortByDescending { it.date }
                    transactionBalance = income - expense
                    tvIncome.text  = "R$ %.2f".format(income)
                    tvExpense.text = "R$ %.2f".format(expense)
                    updateBalanceDisplay()

                    // Re-aplica busca se já tiver query ativa
                    val query = etSearch.text?.toString()?.trim() ?: ""
                    if (query.isNotEmpty()) applySearch(query)
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
            .setMessage("Informe um valor de ajuste manual (positivo ou negativo).")
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