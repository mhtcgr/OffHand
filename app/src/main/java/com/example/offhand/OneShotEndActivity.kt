package com.example.offhand

import ApiResponse
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.offhand.model.NetworkUtils
import org.json.JSONException
import org.json.JSONObject
import pl.droidsonroids.gif.GifDrawable
import java.io.IOException

@Suppress("DEPRECATION")
class OneShotEndActivity : AppCompatActivity() {
    private lateinit var nextShotButton: Button
    private lateinit var returnButton: Button
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
            findViewById<TextView>(R.id.tv_aim_elbow).text = String.format(data.data.shootingAngles.aimingElbowAngle.toString()+"°")
            findViewById<TextView>(R.id.tv_aim_arm).text = String.format(data.data.shootingAngles.aimingArmAngle.toString()+"°")
            findViewById<TextView>(R.id.tv_aim_knee).text = String.format(data.data.shootingAngles.aimingKneeAngle.toString()+"°")
            findViewById<TextView>(R.id.tv_aim_body).text = String.format(data.data.shootingAngles.aimingBodyAngle.toString()+"°")
            findViewById<TextView>(R.id.tv_release_elbow).text = String.format(data.data.shootingAngles.releaseElbowAngle.toString()+"°")
            findViewById<TextView>(R.id.tv_release_arm).text = String.format(data.data.shootingAngles.releaseArmAngle.toString()+"°")
            findViewById<TextView>(R.id.tv_release_knee).text = String.format(data.data.shootingAngles.releaseKneeAngle.toString()+"°")
            findViewById<TextView>(R.id.tv_release_body).text = String.format(data.data.shootingAngles.releaseBodyAngle.toString()+"°")
            // ...其他角度数据

            // 分析建议
            findViewById<TextView>(R.id.tv_analysis).text = "肘关节角度：瞄准姿势中，肘关节通常应保持 \u200B90° 的弯曲，以确保手臂的稳定性和控制性。\n" +
                    "\u200B身体角度：身体应保持 \u200B直立或略微前倾\u200B（约 \u200B10°-20°），以维持平衡和专注。\n" +
                    "\u200B膝盖角度：膝盖应略微弯曲（约 \u200B120°-140°），以提供支撑和灵活性。\n" +
                    "\u200B大臂角度：大臂应与身体成 \u200B45°-60°，便于瞄准和控制。"

            // 弱点提示
            findViewById<TextView>(R.id.tv_weakness).text = "您的肘关节角度不一致，可能是手臂位置过高或过低，建议调整手臂位置至 \u200B90°。\n" +
                    "如果大臂角度过大或过小，可能会影响瞄准的准确性，建议调整至 \u200B45°-60°。\n" +
                    "身体角度和膝盖角度的调整可以提高整体姿势的稳定性，减少疲劳。"

            // 使用实际数据设置弱点提示
            val weaknessText = data.data.weaknessPoints // 确保这是从API返回的真实数据
            findViewById<TextView>(R.id.tv_weakness).text = weaknessText

            // 发送弱点数据到手表（新增代码）
            sendWeaknessToWatch(weaknessText)
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

        nextShotButton = findViewById(R.id.next_shot_button)
        returnButton = findViewById(R.id.return_button)
        //继续投篮按钮，点击后跳转到OneShotActivity
        nextShotButton.setOnClickListener {
            val intent = Intent(this, OneShotActivity::class.java)
            startActivity(intent)
            finish()
        }

        //退出投篮按钮，点击后跳转到TrainSessionActivity
        returnButton.setOnClickListener {
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
                                val responseData = jsonResponse.getString("data")
                                
                                answerArea.text = "根据网上的篮球知识以及您的身体姿态数据，以下是调整手臂姿势的建议：\n" +
                                        "        1.瞄准蓄力阶段：\n" +
                                        "        手肘正对篮筐，确保投篮手大臂与躯干夹角约80°-90°，小臂与大臂呈90°(“L型”)。这样可以确保发力方向直线指向篮筐，减少左右偏差。四点一线(手腕-手肘-膝盖-脚尖)应接近直线，增强力量传导效率，减少能量损耗。\n" +
                                        "        2.出手阶段\n" +
                                        "        手臂应完全伸展，手肘接近180°，大臂与躯干夹角扩大至120°-130°，确保篮球沿直线推出。\n" +
                                        "        手腕快速下压(拨腕)，食指与中指最后离球，赋予篮球后旋(约3-5转/秒)，提升入筐容错率。\n" +
                                        "        这些调整有助于优化投篮的准确性和稳定性。坚持练习，您会看到显著的进步!"
                            }
                            else -> {
                                //Toast.makeText(this, "未知响应: $responseBody", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        //Toast.makeText(this, "解析响应失败: $responseBody", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onFailure = { errorMessage ->
                runOnUiThread {
                    loadingProgressBar.visibility = View.GONE
                   //Toast.makeText(this, "上传失败: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun sendWeaknessToWatch(weakness: String) {
        try {
            val data = "weakness:$weakness\n".toByteArray()
            DeviceScanActivity.connectedSocket?.outputStream?.write(data)
            Log.d("Bluetooth", "弱点数据已发送")
        } catch (e: IOException) {
            Log.e("Bluetooth", "发送失败", e)
        }
    }
}