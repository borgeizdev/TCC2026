package com.borgeiz.meutcc2026

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnDashboard = findViewById<Button>(R.id.btnDashboard)
        val btnTransactions = findViewById<Button>(R.id.btnTransactions)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val btnReports = findViewById<Button>(R.id.btnReports)
        val btnProfile = findViewById<Button>(R.id.btnProfile)

        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, DashboardFragment())
            .commit()

        btnDashboard.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, DashboardFragment())
                .commit()
        }

        btnTransactions.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, TransactionsFragment())
                .commit()
        }

        btnAdd.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, AddTransactionFragment())
                .commit()
        }

        btnReports.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, ReportsFragment())
                .commit()
        }

        btnProfile.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, ProfileFragment())
                .commit()
        }
    }
}
