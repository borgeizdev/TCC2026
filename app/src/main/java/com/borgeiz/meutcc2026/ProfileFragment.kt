package com.borgeiz.meutcc2026

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
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

class ProfileFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAvatar: TextView
    private lateinit var ref: DatabaseReference

    private val primaryBlue get() = 0xFF2563EB.toInt()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        tvName  = view.findViewById(R.id.tvProfileName)
        tvEmail = view.findViewById(R.id.tvProfileEmail)
        tvAvatar = view.findViewById(R.id.tvProfileAvatar)

        val btnLogout   = view.findViewById<MaterialButton>(R.id.btnLogout)
        val rowSettings = view.findViewById<LinearLayout>(R.id.rowSettings)

        val uid = auth.currentUser?.uid ?: return view
        ref = FirebaseDatabase.getInstance().reference.child("users").child(uid).child("profile")

        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("preferences").child("nightMode")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val saved = snapshot.getValue(Int::class.java) ?: return
                    val ctx = context ?: return
                    ctx.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                        .edit().putInt("night_mode", saved).apply()
                    if (AppCompatDelegate.getDefaultNightMode() != saved) {
                        AppCompatDelegate.setDefaultNightMode(saved)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name  = snapshot.child("name").value?.toString() ?: ""
                val email = snapshot.child("email").value?.toString()
                    ?: auth.currentUser?.email ?: ""
                tvName.text   = name.ifBlank { "Usuario" }
                tvEmail.text  = email
                tvAvatar.text = name.trim().firstOrNull()?.uppercase() ?: "U"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        rowSettings.setOnClickListener { showSettingsMenu(uid) }

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        return view
    }

    private fun showSettingsMenu(uid: String) {
        val ctx = requireContext()

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(12))
        }

        data class MenuItem(
            val iconRes: Int, val iconTintRes: Int, val iconBgRes: Int,
            val title: String, val subtitle: String
        )
        val menuItems = listOf(
            MenuItem(R.drawable.ic_person,    R.color.primary, R.color.settings_badge_profile, "Configurações do perfil",   "Editar nome de usuário"),
            MenuItem(R.drawable.ic_palette,   R.color.primary, R.color.settings_badge_display, "Configurações de exibição", "Tema claro, escuro ou sistema"),
            MenuItem(R.drawable.ic_wallet,    R.color.income,  R.color.settings_badge_salary,  "Receita fixa",               "Salário e entradas automáticas")
        )

        val dialog = AlertDialog.Builder(ctx)
            .setTitle("Configurações")
            .setView(root)
            .create()

        menuItems.forEachIndexed { index, item ->
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(12), dpToPx(14), dpToPx(12), dpToPx(14))
                isClickable = true
                isFocusable = true
                background = ctx.obtainStyledAttributes(
                    intArrayOf(android.R.attr.selectableItemBackground)
                ).let { ta -> ta.getDrawable(0).also { ta.recycle() } }
                setOnClickListener {
                    dialog.dismiss()
                    when (index) {
                        0 -> showEditProfileDialog()
                        1 -> showDisplaySettingsDialog()
                        2 -> showSalaryConfigDialog(uid)
                    }
                }
            }

            val iconBadge = ImageView(ctx).apply {
                setImageResource(item.iconRes)
                imageTintList = ColorStateList.valueOf(ctx.getColor(item.iconTintRes))
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
                layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(44)).also {
                    it.marginEnd = dpToPx(14)
                }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(ctx.getColor(item.iconBgRes))
                }
            }

            val textBlock = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvTitle = TextView(ctx).apply {
                text = item.title
                textSize = 15f
                setTextColor(ctx.getColor(R.color.text_heading))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val tvSub = TextView(ctx).apply {
                text = item.subtitle
                textSize = 12f
                setTextColor(ctx.getColor(R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = dpToPx(2) }
            }
            textBlock.addView(tvTitle)
            textBlock.addView(tvSub)

            val chevron = TextView(ctx).apply {
                text = "›"
                textSize = 22f
                setTextColor(ctx.getColor(R.color.text_hint))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginStart = dpToPx(8) }
            }

            row.addView(iconBadge)
            row.addView(textBlock)
            row.addView(chevron)
            root.addView(row)

            if (index < menuItems.lastIndex) {
                root.addView(View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)
                    ).also { it.marginStart = dpToPx(70) }
                    setBackgroundColor(ctx.getColor(R.color.divider))
                })
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.GradientDrawable().apply {
                setColor(ctx.getColor(R.color.bg_card))
                cornerRadius = dpToPx(20).toFloat()
            }
        )
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
            .setTitle("Configurações do perfil")
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
            .also { d ->
                d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(primaryBlue)
                d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(primaryBlue)
                d.window?.setBackgroundDrawable(
                    android.graphics.drawable.GradientDrawable().apply {
                        setColor(ctx.getColor(R.color.bg_card))
                        cornerRadius = dpToPx(20).toFloat()
                    }
                )
            }
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
            .setTitle("Configurações de exibição")
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                val newMode = when (which) {
                    0    -> AppCompatDelegate.MODE_NIGHT_NO
                    1    -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(newMode)
                ctx.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    .edit().putInt("night_mode", newMode).apply()
                val uid = auth.currentUser?.uid ?: return@setSingleChoiceItems
                FirebaseDatabase.getInstance().reference
                    .child("users").child(uid).child("preferences").child("nightMode")
                    .setValue(newMode)
                dialog.dismiss()
            }
            .setNegativeButton("Fechar", null)
            .show()
            .also { d ->
                d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(primaryBlue)
                d.window?.setBackgroundDrawable(
                    android.graphics.drawable.GradientDrawable().apply {
                        setColor(ctx.getColor(R.color.bg_card))
                        cornerRadius = dpToPx(20).toFloat()
                    }
                )
            }
    }

    private fun showSalaryConfigDialog(uid: String) {
        val ctx = requireContext()
        val db  = FirebaseDatabase.getInstance().reference.child("users").child(uid)

        val dialog = android.app.Dialog(ctx)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(true)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(24))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(ctx.getColor(R.color.bg_card))
                cornerRadius = dpToPx(20).toFloat()
            }
        }

        val tvTitle = TextView(ctx).apply {
            text = "Receita fixa"
            textSize = 18f
            setTextColor(ctx.getColor(R.color.text_heading))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(4) }
        }
        root.addView(tvTitle)

        val tvInfo = TextView(ctx).apply {
            text = "Configure os valores que entram fixo na sua conta. O dia é opcional."
            textSize = 13f
            setTextColor(0xFF64748B.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(20) }
        }
        root.addView(tvInfo)

        val containerEntries = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(containerEntries)

        fun addRow(day: Int = 0, amount: Double = 0.0) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(12) }
            }

            val etDay = EditText(ctx).apply {
                hint = "Dia"
                setText(if (day > 0) day.toString() else "")
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                isSingleLine = true
                gravity = Gravity.CENTER
                setHintTextColor(ctx.getColor(R.color.text_hint))
                setTextColor(ctx.getColor(R.color.text_heading))
                background = androidx.core.content.ContextCompat.getDrawable(ctx, R.drawable.bg_outlined_input)
                setPadding(dpToPx(12), dpToPx(14), dpToPx(12), dpToPx(14))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .also { it.marginEnd = dpToPx(8) }
            }

            val etAmount = EditText(ctx).apply {
                hint = "Valor (R$)"
                setText(if (amount > 0) "%.2f".format(amount) else "")
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                isSingleLine = true
                setHintTextColor(ctx.getColor(R.color.text_hint))
                setTextColor(ctx.getColor(R.color.text_heading))
                background = androidx.core.content.ContextCompat.getDrawable(ctx, R.drawable.bg_outlined_input)
                setPadding(dpToPx(12), dpToPx(14), dpToPx(12), dpToPx(14))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
                    .also { it.marginEnd = dpToPx(8) }
            }

            val btnRemove = ImageButton(ctx).apply {
                setImageDrawable(androidx.core.content.ContextCompat.getDrawable(ctx, R.drawable.ic_remove))
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(44))
                    .also { it.gravity = Gravity.CENTER_VERTICAL }
                contentDescription = "Remover"
                setOnClickListener { containerEntries.removeView(row) }
            }

            row.addView(etDay)
            row.addView(etAmount)
            row.addView(btnRemove)
            containerEntries.addView(row)
        }

        fun collectEntries(): List<SalaryEntry>? {
            val result = mutableListOf<SalaryEntry>()
            for (i in 0 until containerEntries.childCount) {
                val row    = containerEntries.getChildAt(i) as? LinearLayout ?: continue
                val etDay  = row.getChildAt(0) as? EditText ?: continue
                val etAmt  = row.getChildAt(1) as? EditText ?: continue
                val dayStr = etDay.text.toString().trim()
                val amtStr = etAmt.text.toString().trim().replace(",", ".")
                val day: Int
                if (dayStr.isEmpty()) {
                    day = 0
                } else {
                    val parsed = dayStr.toIntOrNull()
                    if (parsed == null || parsed < 1 || parsed > 31) {
                        etDay.error = "1 a 31"; return null
                    }
                    etDay.error = null
                    day = parsed
                }
                val amount = amtStr.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    etAmt.error = "Valor inválido"; return null
                }
                etAmt.error = null
                result.add(SalaryEntry(dayOfMonth = day, amount = amount))
            }
            return result
        }

        val btnAddEntry = MaterialButton(ctx).apply {
            text = "+ Adicionar entrada"
            textSize = 13f
            setBackgroundColor(primaryBlue)
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = dpToPx(4); it.bottomMargin = dpToPx(20) }
        }
        root.addView(btnAddEntry)

        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val btnCancel = MaterialButton(
            ctx, null,
            com.google.android.material.R.attr.borderlessButtonStyle
        ).apply {
            text = "Cancelar"
            setTextColor(0xFF64748B.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnSave = MaterialButton(ctx).apply {
            text = "Salvar"
            setBackgroundColor(primaryBlue)
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .also { it.marginStart = dpToPx(8) }
        }

        btnRow.addView(btnCancel)
        btnRow.addView(btnSave)
        root.addView(btnRow)

        db.child("salaryConfig").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val config  = snapshot.getValue(SalaryConfig::class.java)
                val entries = config?.resolvedEntries() ?: emptyList()
                containerEntries.removeAllViews()
                entries.forEach { addRow(it.dayOfMonth, it.amount) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        btnAddEntry.setOnClickListener { addRow() }
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val entries = collectEntries() ?: return@setOnClickListener
            val config  = SalaryConfig(entries = entries)
            db.child("salaryConfig").setValue(config)
                .addOnSuccessListener {
                    Toast.makeText(ctx, "Receita fixa salva!", Toast.LENGTH_SHORT).show()
                    if (entries.isNotEmpty()) checkAndPostSalaryIfNeeded(uid, config)
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(ctx, "Erro: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        // Frame full-screen transparente — toque fora fecha o dialog
        val frame = FrameLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setOnClickListener { dialog.dismiss() }
        }
        val wrapper = FrameLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            val h = dpToPx(24)
            val w = dpToPx(20)
            setPadding(w, h, w, h)
            setOnClickListener { /* consome o toque para não fechar */ }
        }
        wrapper.addView(root)
        frame.addView(wrapper)

        dialog.setContentView(frame)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.5f)
        }
        dialog.show()
    }

    private fun updateSalaryStatusText(tv: TextView, entries: List<SalaryEntry>) {
        tv.text = if (entries.isEmpty()) {
            "Configure os dias e valores acima e salve."
        } else {
            val plural = if (entries.size > 1) "${entries.size} entradas configuradas" else "1 entrada configurada"
            "Ativo — $plural. Lançamento automático ao abrir o app após o dia configurado."
        }
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
                    if (entry.dayOfMonth == 0) return@forEach
                    if (today < entry.dayOfMonth) return@forEach
                    val expectedDate = "%04d-%02d-%02d".format(year, month, entry.dayOfMonth)
                    if (expectedDate !in existingDates) {
                        val key = txRef.push().key ?: return@forEach
                        txRef.child(key).setValue(
                            Transaction(
                                type        = "receita",
                                title       = "Salário",
                                amount      = entry.amount,
                                category    = "Salário",
                                date        = expectedDate,
                                description = "Salário automático"
                            )
                        ).addOnSuccessListener {
                            if (isAdded) Toast.makeText(
                                requireContext(),
                                "R$ ${"%.2f".format(entry.amount)} lançado para $expectedDate",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
