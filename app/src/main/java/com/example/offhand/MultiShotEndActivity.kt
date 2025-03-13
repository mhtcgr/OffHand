package com.example.offhand

import ApiResponse
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
            sendGetRequest()
        }
    }

    private fun sendGetRequest() {
        // 使用HttpUrl.Builder安全构建URL
        val url = "http://10.52.34.249:8080/detailedAnalysis/getByRecordId?userId=user_001&recordId=record_001"

        val request = Request.Builder()
            .url(url)
            .get()  // 明确指定GET方法
            .addHeader("Authorization",
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6IjEiLCJqdGkiOiI4ZDliMTcxOC1lMDA0LTQ3OWItYWIwYy02YjZkN2NlYTBkOWYiLCJleHAiOjE3NDUyNTkzNTIsImlhdCI6MTc0MTY1OTM1Miwic3ViIjoiUGVyaXBoZXJhbHMiLCJpc3MiOiJUaWFtIn0.1SBagf2D_HQ2k3J63VAsrYinRMp7yzukMsg4xXjk13I")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this@MultiShotEndActivity,
                        "请求失败: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseCode = response.code
                    val responseBody = response.body?.string() ?: "无返回内容"
                    runOnUiThread {
                        if (!response.isSuccessful) {
                            Toast.makeText(
                                this@MultiShotEndActivity,
                                "请求失败: HTTP $responseCode, 错误信息: $responseBody",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            // 使用Gson解析JSON
                            val gson = Gson()
                            val apiResponse = gson.fromJson(responseBody, ApiResponse::class.java)

                            // 处理解析后的数据
                            handleApiResponse(apiResponse)
                        }
                    }
                    println("Response Code: $responseCode")
                    println("Response Body: $responseBody")
                }
            }
        })
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