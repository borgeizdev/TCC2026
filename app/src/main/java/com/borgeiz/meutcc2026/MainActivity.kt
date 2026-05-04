package com.borgeiz.meutcc2026

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.borgeiz.meutcc2026.model.SalaryConfig
import com.borgeiz.meutcc2026.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnDashboard    = findViewById<android.view.View>(R.id.btnDashboard)
        val btnTransactions = findViewById<android.view.View>(R.id.btnTransactions)
        val btnAdd          = findViewById<android.view.View>(R.id.btnAdd)
        val btnReports      = findViewById<android.view.View>(R.id.btnReports)
        val btnProfile      = findViewById<android.view.View>(R.id.btnProfile)

        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, DashboardFragment())
            .commit()

        btnDashboard.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, DashboardFragment()).commit()
        }
        btnTransactions.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, TransactionsFragment()).commit()
        }
        btnAdd.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, AddTransactionFragment()).commit()
        }
        btnReports.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, ReportsFragment()).commit()
        }
        btnProfile.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, ProfileFragment()).commit()
        }

        checkAndPostSalaryIfNeeded()
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