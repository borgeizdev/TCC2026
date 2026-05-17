package com.borgeiz.meutcc2026

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.borgeiz.meutcc2026.model.SalaryConfig
import com.borgeiz.meutcc2026.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    // IDs dos labels da bottom bar para colorir aba ativa
    private lateinit var labelDashboard:    TextView
    private lateinit var labelTransactions: TextView
    private lateinit var labelReports:      TextView
    private lateinit var labelProfile:      TextView

    // Aba ativa atual
    private var currentTab = 0  // 0=Home, 1=Transações, 2=Relatórios, 3=Perfil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnDashboard    = findViewById<LinearLayout>(R.id.btnDashboard)
        val btnTransactions = findViewById<LinearLayout>(R.id.btnTransactions)
        val btnAdd          = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnAdd)
        val btnReports      = findViewById<LinearLayout>(R.id.btnReports)
        val btnProfile      = findViewById<LinearLayout>(R.id.btnProfile)

        labelDashboard    = findViewById(R.id.labelDashboard)
        labelTransactions = findViewById(R.id.labelTransactions)
        labelReports      = findViewById(R.id.labelReports)
        labelProfile      = findViewById(R.id.labelProfile)

        // Abre dashboard na inicialização
        navigateTo(DashboardFragment(), 0)

        btnDashboard.setOnClickListener {
            navigateTo(DashboardFragment(), 0)
        }
        btnTransactions.setOnClickListener {
            navigateTo(TransactionsFragment(), 1)
        }
        btnAdd.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, AddTransactionFragment())
                .addToBackStack(null)
                .commit()
        }
        btnReports.setOnClickListener {
            navigateTo(ReportsFragment(), 2)
        }
        btnProfile.setOnClickListener {
            navigateTo(ProfileFragment(), 3)
        }

        checkAndPostSalaryIfNeeded()
    }

    private fun navigateTo(fragment: Fragment, tab: Int) {
        currentTab = tab
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, fragment)
            .commit()
        updateBottomNav()
    }

    private fun updateBottomNav() {
        val activeColor   = ContextCompat.getColor(this, R.color.primary)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_hint)

        // Reseta todos para inativo
        listOf(labelDashboard, labelTransactions, labelReports, labelProfile).forEach {
            it.setTextColor(inactiveColor)
            it.typeface = android.graphics.Typeface.DEFAULT
        }

        // Ativa o label da aba atual
        val activeLabel = when (currentTab) {
            0 -> labelDashboard
            1 -> labelTransactions
            2 -> labelReports
            3 -> labelProfile
            else -> labelDashboard
        }
        activeLabel.setTextColor(activeColor)
        activeLabel.typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private fun checkAndPostSalaryIfNeeded() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db  = FirebaseDatabase.getInstance().reference.child("users").child(uid)

        db.child("salaryConfig").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val config  = snapshot.getValue(SalaryConfig::class.java) ?: return
                val entries = config.resolvedEntries()
                if (entries.isEmpty()) return

                val cal   = Calendar.getInstance()
                val today = cal.get(Calendar.DAY_OF_MONTH)
                val month = cal.get(Calendar.MONTH) + 1
                val year  = cal.get(Calendar.YEAR)
                val txRef = db.child("transactions")

                txRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(txSnap: DataSnapshot) {
                        val existingDates = txSnap.children.mapNotNull { item ->
                            val t = item.getValue(Transaction::class.java)
                            if (t?.title == "Salário" && t.type == "receita") t.date else null
                        }.toSet()

                        entries.forEach { entry ->
                            if (today < entry.dayOfMonth) return@forEach
                            val expectedDate = "%04d-%02d-%02d".format(year, month, entry.dayOfMonth)
                            if (expectedDate !in existingDates) {
                                val key = txRef.push().key ?: return@forEach
                                txRef.child(key).setValue(Transaction(
                                    id = key, type = "receita", title = "Salário",
                                    amount = entry.amount, category = "Salário",
                                    date = expectedDate, description = "Salário automático"
                                ))
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}