package com.borgeiz.meutcc2026

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EditTransactionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_transaction)

        val etTitle = findViewById<EditText>(R.id.etEditTitle)
        val etAmount = findViewById<EditText>(R.id.etEditAmount)
        val etCategory = findViewById<EditText>(R.id.etEditCategory)
        val etDate = findViewById<EditText>(R.id.etEditDate)
        val etDescription = findViewById<EditText>(R.id.etEditDescription)
        val btnUpdate = findViewById<Button>(R.id.btnUpdate)
        val btnDelete = findViewById<Button>(R.id.btnDelete)

        val id = intent.getStringExtra("id") ?: ""
        val type = intent.getStringExtra("type") ?: ""

        etTitle.setText(intent.getStringExtra("title"))
        etAmount.setText(intent.getDoubleExtra("amount", 0.0).toString())
        etCategory.setText(intent.getStringExtra("category"))
        etDate.setText(intent.getStringExtra("date"))
        etDescription.setText(intent.getStringExtra("description"))

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("transactions").child(id)

        btnUpdate.setOnClickListener {
            val update = mapOf(
                "id" to id,
                "type" to type,
                "title" to etTitle.text.toString(),
                "amount" to (etAmount.text.toString().toDoubleOrNull() ?: 0.0),
                "category" to etCategory.text.toString(),
                "date" to etDate.text.toString(),
                "description" to etDescription.text.toString()
            )

            ref.setValue(update).addOnSuccessListener {
                Toast.makeText(this, "Atualizado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        btnDelete.setOnClickListener {
            ref.removeValue().addOnSuccessListener {
                Toast.makeText(this, "Excluído", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
