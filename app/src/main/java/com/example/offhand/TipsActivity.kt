package com.example.offhand

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class TipsActivity : AppCompatActivity() {
//    private val mode = intent.getStringExtra("mode") // 获取模式
    private val mode = "multiShot"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_tips)

        val nextButton: Button = findViewById(R.id.nextButton)
        val closeButton: Button = findViewById(R.id.closeButton)

        nextButton.setOnClickListener {
            if (mode == "multiShot") {
                // 跳转到多次投篮页面
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // 跳转到单次投篮页面
                val intent = Intent(this, OneShotActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        closeButton.setOnClickListener {
            // 关闭当前活动
            finish()
        }
    }
}