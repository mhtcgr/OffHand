package com.example.offhand

import ApiResponse
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.offhand.model.NetworkUtils
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException

class MultiShotEndActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var shotsTextView: TextView
    private lateinit var button_next: Button
    private val client = OkHttpClient()
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
                    // 处理成功响应
                    runOnUiThread {
                        Toast.makeText(this, "请求成功: $responseBody", Toast.LENGTH_LONG).show()
                    }
                    //这里还需要加一个跳转到ywh写的新页面的逻辑
                    val intent = Intent(this, OneShotEndActivity::class.java).apply {
                        putExtra("analysis_data", apiResponse) // 直接传递对象
                    }
                    startActivity(intent)
                    finish()
                    //handleApiResponse(apiResponse) // 处理解析后的数据
                },
                onFailure = { errorCode, errorMessage ->
                    // 处理失败
                    runOnUiThread {
                        Toast.makeText(this, "请求失败: $errorCode, $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }


    private fun handleApiResponse(apiResponse: ApiResponse) {
        // 检查状态码
        if (apiResponse.code == "200") {
            // 获取数据
            val shootingAngles = apiResponse.data.shootingAngles
            val analysis = apiResponse.data.analysis
            val suggestions = apiResponse.data.suggestions
            val weaknessPoints = apiResponse.data.weaknessPoints

            // 在UI中显示数据
//            displayShootingAngles(shootingAngles)
//            displayAnalysis(analysis)
//            displaySuggestions(suggestions)
//            displayWeaknessPoints(weaknessPoints)
        } else {
            Toast.makeText(
                this@MultiShotEndActivity,
                "请求失败: ${apiResponse.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}