package com.borgeiz.meutcc2026

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.borgeiz.meutcc2026.adapter.TransactionAdapter
import com.borgeiz.meutcc2026.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class TransactionsFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var spFilterType: Spinner
    private lateinit var spFilterMonth: Spinner
    private val allTransactions = mutableListOf<Transaction>()

    private var txListener: ValueEventListener? = null
    private var txRef: com.google.firebase.database.DatabaseReference? = null

    private val monthLabels = listOf(
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
        val view = inflater.inflate(R.layout.fragment_transactions, container, false)

        recycler      = view.findViewById(R.id.recyclerTransactions)
        spFilterType  = view.findViewById(R.id.spFilterType)
        spFilterMonth = view.findViewById(R.id.spFilterMonth)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        val typeOptions = listOf("Todos", "Receitas", "Despesas")
        spFilterType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            typeOptions
        )

        // Pré-seleciona o mês atual
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        spFilterMonth.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            monthLabels
        )
        spFilterMonth.setSelection(currentMonth)

        val filterListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) = applyFilter()
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spFilterType.onItemSelectedListener  = filterListener
        spFilterMonth.onItemSelectedListener = filterListener

        loadTransactions()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        txRef?.let { ref -> txListener?.let { ref.removeEventListener(it) } }
        txListener = null
        txRef = null
    }

    private fun loadTransactions() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        txRef = FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("transactions")
        txListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                allTransactions.clear()
                for (item in snapshot.children) {
                    val t = item.getValue(Transaction::class.java)
                    if (t != null) allTransactions.add(t)
                }
                allTransactions.sortByDescending { it.date }
                applyFilter()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        txRef!!.addValueEventListener(txListener!!)
    }

    private fun applyFilter() {
        if (!::spFilterType.isInitialized || !::spFilterMonth.isInitialized) return

        val typePos  = spFilterType.selectedItemPosition   // 0=Todos, 1=Receitas, 2=Despesas
        val monthPos = spFilterMonth.selectedItemPosition  // 0=Todos, 1-12=mês

        val filtered = allTransactions.filter { t ->
            val typeOk = when (typePos) {
                1 -> t.type == "receita"
                2 -> t.type == "despesa"
                else -> true
            }
            val monthOk = if (monthPos == 0) {
                true
            } else {
                // date formato "yyyy-MM-dd"
                val parts = t.date.split("-")
                parts.size >= 2 && parts[1].toIntOrNull() == monthPos
            }
            typeOk && monthOk
        }

        val tvEmpty = view?.findViewById<TextView>(R.id.tvEmptyTransactions)
        if (filtered.isEmpty()) {
            tvEmpty?.visibility = View.VISIBLE
            recycler.visibility = View.GONE
        } else {
            tvEmpty?.visibility = View.GONE
            recycler.visibility = View.VISIBLE
        }
        recycler.adapter = TransactionAdapter(filtered)
    }
}
