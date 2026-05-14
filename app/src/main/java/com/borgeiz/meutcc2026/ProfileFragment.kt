package com.borgeiz.meutcc2026

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAvatar: TextView
    private lateinit var ref: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        tvName   = view.findViewById(R.id.tvProfileName)
        tvEmail  = view.findViewById(R.id.tvProfileEmail)
        tvAvatar = view.findViewById(R.id.tvProfileAvatar)

        val btnLogout      = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLogout)
        val rowSettings    = view.findViewById<LinearLayout>(R.id.rowSettings)
        val rowDisplayConf = view.findViewById<LinearLayout>(R.id.rowDisplayConf)

        val uid = auth.currentUser?.uid ?: return view
        ref = FirebaseDatabase.getInstance().reference.child("users").child(uid).child("profile")

        // Carrega preferencia de modo salva
        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("preferences").child("nightMode")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val saved = snapshot.getValue(Int::class.java) ?: return
                    AppCompatDelegate.setDefaultNightMode(saved)
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name  = snapshot.child("name").value?.toString() ?: ""
                val email = snapshot.child("email").value?.toString()
                    ?: auth.currentUser?.email ?: ""
                tvName.text  = name.ifBlank { "Usuario" }
                tvEmail.text = email
                tvAvatar.text = name.trim().firstOrNull()?.uppercase() ?: "U"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        rowSettings.setOnClickListener { showEditProfileDialog() }
        rowDisplayConf.setOnClickListener { showDisplaySettingsDialog() }

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        return view
    }

    private fun showEditProfileDialog() {
        val ctx = requireContext()
        val dialogView = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 16)
        }
        val tilName = TextInputLayout(
            ctx, null,
            com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox
        ).apply {
            hint = "Nome de usuario"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBoxCornerRadii(12f, 12f, 12f, 12f)
        }
        val etName = TextInputEditText(ctx).apply { setText(tvName.text) }
        tilName.addView(etName)
        dialogView.addView(tilName)

        AlertDialog.Builder(ctx)
            .setTitle("Configuracoes do perfil")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val newName = etName.text?.toString()?.trim() ?: ""
                if (newName.isBlank()) {
                    Toast.makeText(ctx, "Informe um nome.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                ref.child("name").setValue(newName).addOnSuccessListener {
                    tvName.text   = newName
                    tvAvatar.text = newName.firstOrNull()?.uppercase() ?: "U"
                    Toast.makeText(ctx, "Nome atualizado!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDisplaySettingsDialog() {
        val ctx = requireContext()
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val options = arrayOf("Claro", "Escuro", "Seguir sistema")
        val checkedItem = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_NO  -> 0
            AppCompatDelegate.MODE_NIGHT_YES -> 1
            else                              -> 2
        }
        AlertDialog.Builder(ctx)
            .setTitle("Configuracoes de exibicao")
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                val newMode = when (which) {
                    0    -> AppCompatDelegate.MODE_NIGHT_NO
                    1    -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(newMode)
                val uid = auth.currentUser?.uid ?: return@setSingleChoiceItems
                FirebaseDatabase.getInstance().reference
                    .child("users").child(uid).child("preferences").child("nightMode")
                    .setValue(newMode)
                dialog.dismiss()
            }
            .setNegativeButton("Fechar", null)
            .show()
    }
}
