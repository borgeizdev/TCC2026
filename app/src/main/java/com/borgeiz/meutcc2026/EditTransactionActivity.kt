package com.borgeiz.meutcc2026

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.borgeiz.meutcc2026.model.Category
import com.borgeiz.meutcc2026.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class EditTransactionActivity : AppCompatActivity() {

    private val defaultCategories = listOf("Alimentação", "Transporte", "Lazer", "Farmácia", "Salário")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_transaction)

        val etTitle       = findViewById<TextInputEditText>(R.id.etEditTitle)
        val etAmount      = findViewById<TextInputEditText>(R.id.etEditAmount)
        val spCategory    = findViewById<Spinner>(R.id.spEditCategory)
        val etDate        = findViewById<TextInputEditText>(R.id.etEditDate)
        val etDescription = findViewById<TextInputEditText>(R.id.etEditDescription)
        val btnUpdate     = findViewById<Button>(R.id.btnUpdate)
        val btnDelete     = findViewById<Button>(R.id.btnDelete)

        val id       = intent.getStringExtra("id") ?: ""
        val type     = intent.getStringExtra("type") ?: ""
        val category = intent.getStringExtra("category") ?: ""

        etTitle.setText(intent.getStringExtra("title"))
        etAmount.setText(intent.getDoubleExtra("amount", 0.0).let {
            if (it == 0.0) "" else "%.2f".format(it)
        })
        etDate.setText(intent.getStringExtra("date"))
        etDescription.setText(intent.getStringExtra("description"))

        // DatePicker ao clicar no campo de data
        etDate.isFocusable = false
        etDate.isCursorVisible = false
        etDate.setOnClickListener { showDatePicker(etDate) }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            finish(); return
        }
        val ref = FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("transactions").child(id)

        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("categories")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val cats = mutableListOf<String>()
                    for (item in snapshot.children) {
                        val cat = item.getValue(Category::class.java)
                        if (cat != null && cat.name.isNotBlank()) cats.add(cat.name)
                    }
                    if (cats.isEmpty()) cats.addAll(defaultCategories)

                    spCategory.adapter = ArrayAdapter(
                        this@EditTransactionActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        cats
                    )
                    val idx = cats.indexOfFirst { it.equals(category, ignoreCase = true) }
                    if (idx >= 0) spCategory.setSelection(idx)
                }
                override fun onCancelled(error: DatabaseError) {
                    spCategory.adapter = ArrayAdapter(
                        this@EditTransactionActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        defaultCategories
                    )
                }
            })

        btnUpdate.setOnClickListener {
            val titleStr  = etTitle.text?.toString()?.trim() ?: ""
            val amountStr = etAmount.text?.toString()?.trim()?.replace(",", ".") ?: ""

            if (titleStr.isEmpty()) {
                etTitle.error = "Informe o título"
                return@setOnClickListener
            }
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                etAmount.error = "Informe um valor válido"
                return@setOnClickListener
            }

            val transaction = Transaction(
                id          = id,
                type        = type,
                title       = titleStr,
                amount      = amount,
                category    = spCategory.selectedItem?.toString() ?: "",
                date        = etDate.text?.toString()?.trim() ?: "",
                description = etDescription.text?.toString()?.trim() ?: ""
            )

            ref.setValue(transaction).addOnSuccessListener {
                Toast.makeText(this, "Atualizado!", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Erro: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Excluir transação")
                .setMessage("Tem certeza que deseja excluir esta transação?")
                .setPositiveButton("Excluir") { _, _ ->
                    ref.removeValue().addOnSuccessListener {
                        Toast.makeText(this, "Excluído!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun showDatePicker(etDate: TextInputEditText) {
        val current = etDate.text?.toString() ?: ""
        val cal = Calendar.getInstance()
        // Pré-preenche com a data atual do campo se válida
        if (current.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            val parts = current.split("-")
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                etDate.setText("%04d-%02d-%02d".format(year, month + 1, day))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}