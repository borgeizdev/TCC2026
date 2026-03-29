package com.borgeiz.meutcc2026

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SecondActivity : AppCompatActivity() {

    private lateinit var btnVoltar1: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        btnVoltar1 = findViewById(R.id.btnVoltar1)

        btnVoltar1.setOnClickListener {
            finish()
        }
    }
}