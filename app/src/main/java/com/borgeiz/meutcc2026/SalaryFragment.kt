package com.borgeiz.meutcc2026

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
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

    private lateinit var containerEntries: LinearLayout
    private lateinit var btnAddEntry: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var tvStatus: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_salary, container, false)

        containerEntries = view.findViewById(R.id.containerSalaryEntries)
        btnAddEntry      = view.findViewById(R.id.btnAddSalaryEntry)
        btnSave          = view.findViewById(R.id.btnSaveSalary)
        tvStatus         = view.findViewById(R.id.tvSalaryStatus)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return view
        val db  = FirebaseDatabase.getInstance().reference.child("users").child(uid)

        db.child("salaryConfig").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val config = snapshot.getValue(SalaryConfig::class.java)
                val entries = config?.resolvedEntries() ?: emptyList()
                containerEntries.removeAllViews()
                if (entries.isEmpty()) addEntryRow() else entries.forEach { addEntryRow(it.dayOfMonth, it.amount) }
                updateStatusText(entries)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        btnAddEntry.setOnClickListener { addEntryRow() }

        btnSave.setOnClickListener {
            val entries = collectEntries() ?: return@setOnClickListener
            val config = SalaryConfig(
                amount     = entries.firstOrNull()?.amount ?: 0.0,
                dayOfMonth = entries.firstOrNull()?.dayOfMonth ?: 1,
                entries    = entries
            )
            db.child("salaryConfig").setValue(config)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Salário configurado!", Toast.LENGTH_SHORT).show()
                    updateStatusText(entries)
                    checkAndPostSalaryIfNeeded(uid, config)
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Erro: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        return view
    }

    private fun addEntryRow(day: Int = 0, amount: Double = 0.0) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(12) }
        }

        val tilDay = TextInputLayout(requireContext(), null,
            com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox).apply {
            hint = "Dia (1–31)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginEnd = dpToPx(8)
            }
            setBoxCornerRadii(12f, 12f, 12f, 12f)
        }
        val etDay = TextInputEditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            if (day > 0) setText(day.toString())
        }
        tilDay.addView(etDay)

        val tilAmount = TextInputLayout(requireContext(), null,
            com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox).apply {
            hint = "Valor (R$)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f).also {
                it.marginEnd = dpToPx(8)
            }
            setBoxCornerRadii(12f, 12f, 12f, 12f)
        }
        val etAmount = TextInputEditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            if (amount > 0) setText("%.2f".format(amount))
        }
        tilAmount.addView(etAmount)

        val btnRemove = ImageButton(requireContext()).apply {
            setImageResource(android.R.drawable.ic_delete)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(56)).also {
                it.gravity = android.view.Gravity.CENTER_VERTICAL
            }
            contentDescription = "Remover"
            setOnClickListener {
                if (containerEntries.childCount > 1) containerEntries.removeView(row)
                else Toast.makeText(requireContext(), "Precisa ter ao menos um salário.", Toast.LENGTH_SHORT).show()
            }
        }

        row.addView(tilDay)
        row.addView(tilAmount)
        row.addView(btnRemove)
        containerEntries.addView(row)
    }

    private fun collectEntries(): List<SalaryEntry>? {
        val result = mutableListOf<SalaryEntry>()
        for (i in 0 until containerEntries.childCount) {
            val row = containerEntries.getChildAt(i) as? LinearLayout ?: continue
            val tilDay    = row.getChildAt(0) as? TextInputLayout ?: continue
            val tilAmount = row.getChildAt(1) as? TextInputLayout ?: continue

            val dayStr    = tilDay.editText?.text?.toString()?.trim() ?: ""
            val amountStr = tilAmount.editText?.text?.toString()?.trim()?.replace(",", ".") ?: ""

            val day = dayStr.toIntOrNull()
            if (day == null || day < 1 || day > 31) { tilDay.error = "Informe um dia entre 1 e 31"; return null } else tilDay.error = null

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) { tilAmount.error = "Informe um valor válido"; return null } else tilAmount.error = null

            result.add(SalaryEntry(dayOfMonth = day, amount = amount))
        }
        return result.ifEmpty { null }
    }

    private fun checkAndPostSalaryIfNeeded(uid: String, config: SalaryConfig) {
        val cal   = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year  = cal.get(Calendar.YEAR)
        val txRef = FirebaseDatabase.getInstance().reference.child("users").child(uid).child("transactions")

        txRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existingDates = snapshot.children.mapNotNull { item ->
                    val t = item.getValue(Transaction::class.java)
                    if (t?.title == "Salário" && t.type == "receita") t.date else null
                }.toSet()

                config.resolvedEntries().forEach { entry ->
                    if (today < entry.dayOfMonth) return@forEach
                    val expectedDate = "%04d-%02d-%02d".format(year, month, entry.dayOfMonth)
                    if (expectedDate !in existingDates) {
                        val key = txRef.push().key ?: return@forEach
                        txRef.child(key).setValue(Transaction(
                            id = key, type = "receita", title = "Salário",
                            amount = entry.amount, category = "Salário",
                            date = expectedDate, description = "Salário automático"
                        )).addOnSuccessListener {
                            Toast.makeText(requireContext(),
                                "💰 R$ ${"%.2f".format(entry.amount)} lançado para $expectedDate",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateStatusText(entries: List<SalaryEntry>) {
        if (entries.isEmpty()) { tvStatus.text = "Configure seu salário acima e salve."; return }
        val sb = StringBuilder("💡 Receitas automáticas configuradas:\n")
        entries.forEach { e -> sb.appendLine("  • Dia ${e.dayOfMonth} → R$ ${"%.2f".format(e.amount)}") }
        sb.append("O lançamento ocorre ao abrir o app após o dia configurado.")
        tvStatus.text = sb.toString()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}