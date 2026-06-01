package com.borgeiz.meutcc2026

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.borgeiz.meutcc2026.model.SalaryConfig
import com.borgeiz.meutcc2026.model.SalaryEntry
import com.borgeiz.meutcc2026.model.Transaction
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Calendar

class SalaryFragment : Fragment() {

    private val entries = mutableListOf<SalaryEntry>()
    private lateinit var adapter: EntryAdapter
    private lateinit var rvEntries: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_salary, container, false)

        rvEntries = view.findViewById(R.id.rvEntries)
        tvEmpty   = view.findViewById(R.id.tvEmpty)

        adapter = EntryAdapter(
            items    = entries,
            onEdit   = { pos -> showEntryDialog(pos) },
            onDelete = { pos ->
                entries.removeAt(pos)
                adapter.notifyItemRemoved(pos)
                adapter.notifyItemRangeChanged(pos, entries.size)
                refreshEmptyState()
            }
        )
        rvEntries.layoutManager = LinearLayoutManager(requireContext())
        rvEntries.adapter = adapter

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return view
        val db  = FirebaseDatabase.getInstance().reference.child("users").child(uid)

        db.child("salaryConfig").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val config = snapshot.getValue(SalaryConfig::class.java)
                entries.clear()
                entries.addAll(config?.resolvedEntries() ?: emptyList())
                adapter.notifyDataSetChanged()
                refreshEmptyState()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        view.findViewById<MaterialButton>(R.id.btnAddEntry).setOnClickListener {
            showEntryDialog(null)
        }

        view.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val config = SalaryConfig(
                amount     = entries.firstOrNull()?.amount ?: 0.0,
                dayOfMonth = entries.firstOrNull()?.dayOfMonth ?: 1,
                entries    = entries.toList()
            )
            db.child("salaryConfig").setValue(config)
                .addOnSuccessListener {
                    if (!isAdded) return@addOnSuccessListener
                    Toast.makeText(requireContext(), "Configuração salva!", Toast.LENGTH_SHORT).show()
                    if (entries.isNotEmpty()) checkAndPostSalaryIfNeeded(uid, config)
                }
                .addOnFailureListener {
                    if (!isAdded) return@addOnFailureListener
                    Toast.makeText(requireContext(), "Erro ao salvar.", Toast.LENGTH_LONG).show()
                }
        }

        return view
    }

    private fun showEntryDialog(position: Int?) {
        val editing = position != null
        val current = if (editing) entries[position!!] else null

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_salary_entry, null)

        val tilDay    = dialogView.findViewById<TextInputLayout>(R.id.tilDay)
        val tilAmount = dialogView.findViewById<TextInputLayout>(R.id.tilAmount)
        val etDay     = dialogView.findViewById<TextInputEditText>(R.id.etDay)
        val etAmount  = dialogView.findViewById<TextInputEditText>(R.id.etAmount)

        if (editing && current != null) {
            if (current.dayOfMonth > 0) etDay.setText(current.dayOfMonth.toString())
            etAmount.setText("%.2f".format(current.amount))
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (editing) "Editar entrada" else "Nova entrada")
            .setView(dialogView)
            .setPositiveButton("Confirmar", null)
            .setNegativeButton("Cancelar", null)
            .show()
        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.GradientDrawable().apply {
                setColor(requireContext().getColor(R.color.bg_card))
                cornerRadius = 20 * resources.displayMetrics.density
            }
        )

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val dayStr = etDay.text?.toString()?.trim() ?: ""
            val amtStr = etAmount.text?.toString()?.trim()?.replace(",", ".") ?: ""

            val day: Int
            if (dayStr.isEmpty()) {
                day = 0
            } else {
                val parsed = dayStr.toIntOrNull()
                if (parsed == null || parsed < 1 || parsed > 31) {
                    tilDay.error = "Informe um dia entre 1 e 31 ou deixe em branco"
                    return@setOnClickListener
                }
                day = parsed
            }
            tilDay.error = null

            val amount = amtStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                tilAmount.error = "Informe um valor válido"
                return@setOnClickListener
            }
            tilAmount.error = null

            val entry = SalaryEntry(dayOfMonth = day, amount = amount)
            if (editing) {
                entries[position!!] = entry
                adapter.notifyItemChanged(position)
            } else {
                entries.add(entry)
                adapter.notifyItemInserted(entries.size - 1)
            }
            refreshEmptyState()
            dialog.dismiss()
        }
    }

    private fun refreshEmptyState() {
        val empty = entries.isEmpty()
        tvEmpty.visibility   = if (empty) View.VISIBLE else View.GONE
        rvEntries.visibility = if (empty) View.GONE   else View.VISIBLE
    }

    private fun checkAndPostSalaryIfNeeded(uid: String, config: SalaryConfig) {
        val cal   = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year  = cal.get(Calendar.YEAR)
        val txRef = FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("transactions")

        txRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existingDates = snapshot.children.mapNotNull {
                    val t = it.getValue(Transaction::class.java)
                    if (t?.title == "Salário" && t.type == "receita") t.date else null
                }.toSet()

                config.resolvedEntries().forEach { entry ->
                    if (entry.dayOfMonth == 0) return@forEach
                    if (today < entry.dayOfMonth) return@forEach
                    val expectedDate = "%04d-%02d-%02d".format(year, month, entry.dayOfMonth)
                    if (expectedDate !in existingDates) {
                        val key = txRef.push().key ?: return@forEach
                        txRef.child(key).setValue(
                            Transaction(
                                id = key, type = "receita", title = "Salário",
                                amount = entry.amount, category = "Salário",
                                date = expectedDate, description = "Salário automático"
                            )
                        ).addOnSuccessListener {
                            if (!isAdded) return@addOnSuccessListener
                            Toast.makeText(
                                requireContext(),
                                "Salário de R$ ${"%.2f".format(entry.amount)} lançado.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private class EntryAdapter(
        private val items: MutableList<SalaryEntry>,
        private val onEdit: (Int) -> Unit,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<EntryAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvDay    : TextView    = v.findViewById(R.id.tvDay)
            val tvAmount : TextView    = v.findViewById(R.id.tvAmount)
            val btnEdit  : TextView    = v.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_salary_entry, parent, false)
        )

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val entry = items[position]
            holder.tvDay.text    = if (entry.dayOfMonth > 0) "Dia ${entry.dayOfMonth} de cada mês" else "Sem dia fixo"
            holder.tvAmount.text = "R$ ${"%.2f".format(entry.amount).replace(".", ",")}"
            holder.btnEdit.setOnClickListener   { onEdit(holder.adapterPosition) }
            holder.btnDelete.setOnClickListener { onDelete(holder.adapterPosition) }
        }
    }
}
