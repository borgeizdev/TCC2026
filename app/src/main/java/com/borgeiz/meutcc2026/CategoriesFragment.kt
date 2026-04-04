package com.borgeiz.meutcc2026

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.borgeiz.meutcc2026.model.Category
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CategoriesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_categories, container, false)

        val etName = view.findViewById<EditText>(R.id.etCategoryName)
        val btnSave = view.findViewById<Button>(R.id.btnSaveCategory)
        val tvCategories = view.findViewById<TextView>(R.id.tvCategories)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return view
        val ref = FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("categories")

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) return@setOnClickListener
            val key = ref.push().key!!
            ref.child(key).setValue(Category(key, name))
            etName.text.clear()
        }

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<String>()
                for (item in snapshot.children) {
                    val cat = item.getValue(Category::class.java)
                    if (cat != null) list.add(cat.name)
                }
                tvCategories.text = "Categorias:\n\n" + list.joinToString("\n")
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        return view
    }
}