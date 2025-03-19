package com.example.offhand

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.offhand.model.NetworkUtils
import org.json.JSONObject

class TipsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_tips)
        val selectedTheme = intent.getStringExtra("selectedTheme") ?: "默认主题"
        val selectedMethod = intent.getStringExtra("selectedMethod") ?: "默认方法"
        val nextButton: Button = findViewById(R.id.nextButton)
        val closeButton: Button = findViewById(R.id.closeButton)

        nextButton.setOnClickListener {
            if (selectedMethod == "multiple") {
                // 跳转到多次投篮页面
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("selectedTheme", selectedTheme)
                startActivity(intent)
                finish()
            } else {
                // 跳转到单次投篮页面
                val intent = Intent(this, OneShotActivity::class.java)
                intent.putExtra("selectedTheme", selectedTheme)
                startActivity(intent)
                finish()
            }

            // 从 Intent 中提取参数

            //加一个post请求
            NetworkUtils.postTrainSettingRequest(
                userId = "1",
                theme = selectedTheme,
                trainingMethod = selectedMethod,
                onSuccess = { responseBody ->
                    runOnUiThread {
                        val jsonResponse = JSONObject(responseBody)
                        val message = jsonResponse.getString("message")
                        when(message){
                            "Operation succeeded"-> {
                                //Toast.makeText(this, "请求成功: $responseBody", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                //Toast.makeText(this, "请求失败: $message", Toast.LENGTH_SHORT).show()
                            }
                        }

                    }
                },
                onFailure = { errorMessage ->
                    runOnUiThread {
                        //Toast.makeText(this, "请求失败: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
            )


        }
        closeButton.setOnClickListener {
            // 关闭当前活动
            finish()
        }
    }
}