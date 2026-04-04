package com.borgeiz.meutcc2026

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddTransactionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add_transaction, container, false)

        val spType = view.findViewById<Spinner>(R.id.spType)
        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etAmount = view.findViewById<EditText>(R.id.etAmount)
        val etCategory = view.findViewById<EditText>(R.id.etCategory)
        val etDate = view.findViewById<EditText>(R.id.etDate)
        val etDescription = view.findViewById<EditText>(R.id.etDescription)
        val btnSave = view.findViewById<Button>(R.id.btnSaveTransaction)

        spType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("receita", "despesa")
        )

        btnSave.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val ref = FirebaseDatabase.getInstance().reference
                .child("users").child(uid).child("transactions")
            val key = ref.push().key ?: return@setOnClickListener

            val data = mapOf(
                "id" to key,
                "type" to spType.selectedItem.toString(),
                "title" to etTitle.text.toString(),
                "amount" to (etAmount.text.toString().toDoubleOrNull() ?: 0.0),
                "category" to etCategory.text.toString(),
                "date" to etDate.text.toString(),
                "description" to etDescription.text.toString()
            )

            ref.child(key).setValue(data)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Transação salva", Toast.LENGTH_SHORT).show()
                    etTitle.text.clear()
                    etAmount.text.clear()
                    etCategory.text.clear()
                    etDate.text.clear()
                    etDescription.text.clear()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Erro: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        return view
    }
}
