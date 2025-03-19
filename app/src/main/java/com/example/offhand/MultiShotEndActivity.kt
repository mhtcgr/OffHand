package com.example.offhand

import ApiResponse
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.offhand.model.NetworkUtils

class MultiShotEndActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var shotsTextView: TextView
    private lateinit var button_next: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_shot_end)

        timerTextView = findViewById(R.id.timerTextView)
        scoreTextView = findViewById(R.id.scoreTextView)
        shotsTextView = findViewById(R.id.shotsTextView)
        button_next = findViewById(R.id.button_next)
        val timer = intent.getStringExtra("TIMER")
        val score = intent.getStringExtra("SCORE")
        val shots = intent.getStringExtra("SHOTS")

        timerTextView.text = "训练时长: $timer"
        scoreTextView.text = "进球数: $score"
        shotsTextView.text = "投篮数: $shots"

        button_next.setOnClickListener {
            NetworkUtils.sendGetRequest(
                userId = "user_001",
                recordId = "record_001",
                onSuccess = { apiResponse, responseBody ->
                    //这里还需要加一个跳转到ywh写的新页面的逻辑
                    val intent = Intent(this, OneShotEndActivity::class.java).apply {
                        putExtra("analysis_data", apiResponse) // 直接传递对象
                    }
                    startActivity(intent)
                    finish()
                },
                onFailure = { errorCode, errorMessage ->
                    // 处理失败
                    runOnUiThread {
                    }
                }
            )
        val intent = Intent(this, MultiShotAdviseActivity::class.java)
        startActivity(intent)
        finish()
        }
    }
}