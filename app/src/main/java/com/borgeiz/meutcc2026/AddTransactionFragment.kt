package com.borgeiz.meutcc2026

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.borgeiz.meutcc2026.model.Category
import com.borgeiz.meutcc2026.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddTransactionFragment : Fragment() {

    private val defaultCategories = listOf("Alimentação", "Transporte", "Lazer", "Farmácia", "Salário")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add_transaction, container, false)

        val spType        = view.findViewById<Spinner>(R.id.spType)
        val spCategory    = view.findViewById<Spinner>(R.id.spCategory)
        val etTitle       = view.findViewById<TextInputEditText>(R.id.etTitle)
        val etAmount      = view.findViewById<TextInputEditText>(R.id.etAmount)
        val etDate        = view.findViewById<TextInputEditText>(R.id.etDate)
        val etDescription = view.findViewById<TextInputEditText>(R.id.etDescription)
        val btnSave       = view.findViewById<Button>(R.id.btnSaveTransaction)

        spType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("receita", "despesa")
        )

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
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
                            requireContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            cats
                        )
                    }
                    override fun onCancelled(error: DatabaseError) {
                        spCategory.adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            defaultCategories
                        )
                    }
                })
        } else {
            spCategory.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                defaultCategories
            )
        }

        btnSave.setOnClickListener {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUid == null) {
                Toast.makeText(requireContext(), "Usuário não autenticado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val titleStr  = etTitle.text?.toString()?.trim() ?: ""
            val amountStr = etAmount.text?.toString()?.trim()?.replace(",", ".") ?: ""
            val dateStr   = etDate.text?.toString()?.trim() ?: ""
            val descStr   = etDescription.text?.toString()?.trim() ?: ""

            if (titleStr.isEmpty()) {
                etTitle.error = "Informe o título"
                return@setOnClickListener
            }
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                etAmount.error = "Informe um valor válido"
                return@setOnClickListener
            }

            val ref = FirebaseDatabase.getInstance().reference
                .child("users").child(currentUid).child("transactions")
            val key = ref.push().key ?: return@setOnClickListener

            // Salva como objeto Transaction (não Map) para leitura correta com getValue()
            val transaction = Transaction(
                id          = key,
                type        = spType.selectedItem.toString(),
                title       = titleStr,
                amount      = amount,
                category    = spCategory.selectedItem?.toString() ?: "",
                date        = dateStr,
                description = descStr
            )

            ref.child(key).setValue(transaction)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Transação salva!", Toast.LENGTH_SHORT).show()
                    etTitle.text?.clear()
                    etAmount.text?.clear()
                    etDate.text?.clear()
                    etDescription.text?.clear()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Erro: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        return view
    }
}