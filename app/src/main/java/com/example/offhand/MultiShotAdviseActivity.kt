package com.example.offhand

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Button

class MultiShotAdviseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_multi_shot_advise)
        val btnQuit = findViewById<Button>(R.id.btn_quit)
        btnQuit.setOnClickListener {
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
            finish()
        }
        val btnContinue = findViewById<Button>(R.id.btn_continue)
        btnContinue.setOnClickListener {
            val intent = Intent(this, TrainingSessionActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}