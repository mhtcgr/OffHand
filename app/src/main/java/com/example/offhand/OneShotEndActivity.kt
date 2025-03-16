package com.example.offhand

import AnalysisData
import ApiResponse
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.offhand.model.NetworkUtils
import org.json.JSONException
import org.json.JSONObject
import pl.droidsonroids.gif.GifDrawable
import java.io.BufferedInputStream

class OneShotEndActivity : AppCompatActivity() {
    private lateinit var next_shot_button: Button
    private lateinit var return_button: Button
    private lateinit var inputBox: EditText
    private lateinit var askButton: Button
    private lateinit var answerArea: TextView
    private lateinit var loadingProgressBar:ProgressBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_one_shot_end)
        // 接收传递的数据
        val analysisData = intent.getParcelableExtra<ApiResponse>("analysis_data")

        // 填充数据到UI
        analysisData?.let { data ->
            // 角度数据
            findViewById<TextView>(R.id.tv_aim_elbow).text = data.data.shootingAngles.aimingElbowAngle.toString()
            findViewById<TextView>(R.id.tv_aim_arm).text = data.data.shootingAngles.aimingArmAngle.toString()
            findViewById<TextView>(R.id.tv_aim_knee).text = data.data.shootingAngles.aimingKneeAngle.toString()
            findViewById<TextView>(R.id.tv_aim_body).text = data.data.shootingAngles.aimingBodyAngle.toString()
            findViewById<TextView>(R.id.tv_release_elbow).text = data.data.shootingAngles.releaseElbowAngle.toString()
            findViewById<TextView>(R.id.tv_release_arm).text = data.data.shootingAngles.releaseArmAngle.toString()
            findViewById<TextView>(R.id.tv_release_knee).text = data.data.shootingAngles.releaseKneeAngle.toString()
            findViewById<TextView>(R.id.tv_release_body).text = data.data.shootingAngles.releaseBodyAngle.toString()
            // ...其他角度数据

            // 分析建议
            findViewById<TextView>(R.id.tv_analysis).text = data.data.analysis

            // 弱点提示
            findViewById<TextView>(R.id.tv_weakness).text = data.data.weaknessPoints
        }

        inputBox = findViewById(R.id.inputBox)
        askButton = findViewById(R.id.askButton)
        answerArea = findViewById(R.id.answerArea)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        // 设置询问按钮点击事件
        askButton.setOnClickListener {
            val question = inputBox.text.toString()
            if (question.isNotEmpty()) {
                // 向后端发送 LLM 请求

                loadingProgressBar.visibility = View.VISIBLE
                sendLLMRequest(question)
            } else {
                answerArea.text = "请输入问题"
            }
        }

        next_shot_button = findViewById(R.id.next_shot_button)
        return_button = findViewById(R.id.return_button)
        //继续投篮按钮，点击后跳转到OneShotActivity
        next_shot_button.setOnClickListener {
            val intent = Intent(this, OneShotActivity::class.java)
            startActivity(intent)
            finish()
        }

        //退出投篮按钮，点击后跳转到TrainSessionActivity
        return_button.setOnClickListener {
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
            finish()
        }

        NetworkUtils.sendGetGIFRequest(
            userId = "1",
            onSuccess = { inputStream ->
                // 将 GIF 图片显示到 ImageView 中
                val gifDrawable = GifDrawable(inputStream)
                runOnUiThread {
                    val imageView = findViewById<ImageView>(R.id.gifImageView)
                    imageView.setImageDrawable(gifDrawable)
                }
            },
            onFailure = { errorCode, errorMessage ->
                runOnUiThread {
                    Toast.makeText(this, "请求失败: $errorCode, $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun sendLLMRequest(question: String) {
        NetworkUtils.probeLLMRequest(
            userId = "user_002",
            recordId = "record_001",
            question = question,
            onSuccess = {responseBody ->


                runOnUiThread {
                    loadingProgressBar.visibility = View.GONE
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val message = jsonResponse.getString("message")
                        when (message) {
                            "success" -> {
                                answerArea.text = jsonResponse.getString("data")
                            }
                            else -> {
                                Toast.makeText(this, "未知响应: $responseBody", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Toast.makeText(this, "解析响应失败: $responseBody", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onFailure = { errorMessage ->
                runOnUiThread {
                    loadingProgressBar.visibility = View.GONE
                    Toast.makeText(this, "上传失败: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}