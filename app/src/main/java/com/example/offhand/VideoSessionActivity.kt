package com.example.offhand;

import AnalysisData
import ApiResponse
import ShootingAngles
import TrainSettingRequest
import android.content.Intent
import android.content.res.ColorStateList
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.offhand.model.ImageProcess.bitmapToByteArray
import com.example.offhand.model.ImageProcess.resizeImage
import com.example.offhand.model.NetworkUtils
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


class VideoSessionActivity : AppCompatActivity() {
    private var selectedTheme = ""
    private var selectedMethod = ""
    private var cachedImage: ByteArray? = null // 缓存当前帧的图像数据
    private val authToken =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6IjEiLCJqdGkiOiI4ZDliMTcxOC1lMDA0LTQ3OWItYWIwYy02YjZkN2NlYTBkOWYiLCJleHAiOjE3NDUyNTkzNTIsImlhdCI6MTc0MTY1OTM1Miwic3ViIjoiUGVyaXBoZXJhbHMiLCJpc3MiOiJUaWFtIn0.1SBagf2D_HQ2k3J63VAsrYinRMp7yzukMsg4xXjk13I"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_video_session)
        setupButtonSelection()
        setupReadyButton()
        setupNavigationButton()
    }

    private fun setupNavigationButton() {
        // 找到底部导航栏中的按钮
        val btnHome = findViewById<LinearLayout>(R.id.btn_home)
        val btnTraining = findViewById<LinearLayout>(R.id.btn_training)


        // 设置首页按钮点击事件
        btnHome.setOnClickListener {
            // 如果当前已经在StartActivity，则不需要重新启动
            val intent = Intent(this@VideoSessionActivity, StartActivity::class.java)
            startActivity(intent)
            finish() // 结束当前Activity
        }


        // 设置训练按钮点击事件
        btnTraining.setOnClickListener {
            // 如果当前已经在TrainingActivity，则不需要重新启动
            val intent = Intent(this@VideoSessionActivity, TrainingActivity::class.java)
            startActivity(intent)
            finish() // 结束当前Activity
        }

        val btnTutorial = findViewById<LinearLayout>(R.id.btn_tutorial)
        btnTutorial.setOnClickListener {
            // 如果当前已经在TrainingActivity，则不需要重新启动
            val intent =
                Intent(this@VideoSessionActivity, TutorialActivity::class.java)
            startActivity(intent)
            finish() // 结束当前Activity
        }
    }

    private fun setupButtonSelection() {
        findViewById<View>(R.id.btn_single).setOnClickListener { v: View ->
            selectMethodButton(v as Button, "single")
            deselectMethodButton(findViewById(R.id.btn_multiple))
        }

        findViewById<View>(R.id.btn_multiple).setOnClickListener { v: View ->
            selectMethodButton(v as Button, "multiple")
            deselectMethodButton(findViewById(R.id.btn_single))
        }

        // 训练主题选择
        findViewById<View>(R.id.btn_three_point).setOnClickListener { v: View ->
            selectThemeButton(v as Button, "three point")
            deselectThemeButton(findViewById(R.id.btn_mid_shoot))
        }

        findViewById<View>(R.id.btn_mid_shoot).setOnClickListener { v: View ->
            selectThemeButton(v as Button, "mid shoot")
            deselectThemeButton(findViewById(R.id.btn_three_point))
        }
    }

    private fun selectMethodButton(button: Button, method: String) {
        selectedMethod = method
        button.backgroundTintList = ColorStateList.valueOf(getColor(R.color.selected_color))
    }

    private fun deselectMethodButton(button: Button) {
        button.backgroundTintList = ColorStateList.valueOf(getColor(R.color.unselected_color))
    }

    private fun selectThemeButton(button: Button, theme: String) {
        selectedTheme = theme
        button.backgroundTintList = ColorStateList.valueOf(getColor(R.color.selected_color))
    }

    private fun deselectThemeButton(button: Button) {
        button.backgroundTintList = ColorStateList.valueOf(getColor(R.color.unselected_color))
    }

    private fun setupReadyButton() {
        findViewById<View>(R.id.btn_ready).setOnClickListener { v: View? ->
            if (validateSelection()) {
                postTrainingData()
                jumpToVideoSelection()
//                val intent = Intent(this, OneShotEndActivity::class.java)
//                startActivity(intent)
//                finish()

            } else {
                Toast.makeText(this, "请先选择训练方式和主题", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateSelection(): Boolean {
        return !selectedMethod.isEmpty() && !selectedTheme.isEmpty()
    }

    private fun jumpToVideoSelection() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "video/*"
        startActivityForResult(intent, 1) // 启动视频选择器
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            val videoUri = data.data // 获取选择的视频 URI
            if (videoUri != null) {
                // 提取视频帧并上传
                VideoFrameExtractorTask().execute(videoUri)
                Toast.makeText(this, "视频已选择: $videoUri", Toast.LENGTH_SHORT).show()
            }
            val intent = Intent(this, OneShotEndActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun postTrainingData() {
        val client = OkHttpClient()

        val json = JSONObject()
        try {
            json.put("userId", "1")
            json.put("theme", selectedTheme)
            json.put("trainingMethod", selectedMethod)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val jsonString = Gson().toJson(TrainSettingRequest(
            userId = "1",
            theme = selectedTheme,
            trainingMethod = selectedMethod
        ))

        val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("http://10.54.57.247:8080/detection/setting")
            .post(requestBody)
            .addHeader("Authorization", authToken)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@VideoSessionActivity,
                        "请求失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        //Toast.makeText(this@VideoSessionActivity, "训练开始!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@VideoSessionActivity,
                            "请求失败: " + response.code,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private inner class VideoFrameExtractorTask : AsyncTask<Uri, Void, List<ByteArray>>() {

        override fun doInBackground(vararg uris: Uri): List<ByteArray> {
            val videoUri = uris[0]
            val imageList = mutableListOf<ByteArray>()
            val retriever = MediaMetadataRetriever()

            try {
                retriever.setDataSource(applicationContext, videoUri)

                // 获取视频时长（毫秒）
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMs = durationStr?.toLong() ?: 0

                // 每秒提取 12 帧
                for (timeMs in 0 until durationMs step 1000 / 12) {
                    // 获取当前时间点的帧
                    val bitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                    if (bitmap != null) {
                        // 将 Bitmap 转换为字节数组
                        val imageData = bitmapToByteArray(bitmap)
                        imageList.add(imageData)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }

            return imageList
        }

        override fun onPostExecute(imageList: List<ByteArray>) {
            if (imageList.isNotEmpty()) {
                // 调用 uploadImages 方法上传图片列表
                NetworkUtils.uploadImages(
                    imageList,
                    onSuccess = { responseBody ->
                        // 处理上传成功逻辑
                        runOnUiThread {
                            try {
                                val jsonResponse = JSONObject(responseBody)
                                val message = jsonResponse.getString("message")
                                sendGetRequest()
                                val intent = Intent(this@VideoSessionActivity, OneShotEndActivity::class.java)
                                startActivity(intent)
                                finish()
                                when (message) {
                                    "hit", "miss" -> {
                                        sendGetRequest()
                                        val intent = Intent(this@VideoSessionActivity, OneShotEndActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    "receive" -> {
                                        // 未检测到投篮，仅显示上传成功消息
                                        Toast.makeText(this@VideoSessionActivity, "上传成功: $responseBody", Toast.LENGTH_LONG).show()
                                    }
                                    "release" -> {
                                        // 收到 release 消息，上传当前帧的图像
                                        cachedImage?.let { imageData ->
                                            val resizedImage = resizeImage(imageData, 1024, 576) // 调整图像大小
                                            upLoadImage(listOf(resizedImage))
                                        }
                                    }
                                    else -> {
                                        // 未知响应，显示警告
                                        Toast.makeText(this@VideoSessionActivity, "未知响应: $responseBody", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                                //Toast.makeText(this@VideoSessionActivity, "解析响应失败: $responseBody", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onFailure = { errorMessage ->
                        // 处理上传失败逻辑
                        runOnUiThread {
                            //Toast.makeText(this@VideoSessionActivity, "上传失败: $errorMessage", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                // 无法提取视频帧
                //Toast.makeText(this@VideoSessionActivity, "无法提取视频帧", Toast.LENGTH_SHORT).show()
            }
        }

        private fun upLoadImage(images: List<ByteArray>){
            NetworkUtils.uploadImages(
                images,
                onSuccess = {responseBody ->
                    runOnUiThread {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            when (val message = jsonResponse.getString("message")) {
                                "hit", "miss" -> {

                                }
                                "receive" -> {
                                    // 未检测到投篮，仅显示上传成功消息
                                    Toast.makeText(this@VideoSessionActivity, "上传成功: $responseBody", Toast.LENGTH_LONG).show()
                                }
                                "release" -> {
                                    // 收到 release 消息，上传当前帧的图像
                                    cachedImage?.let { imageData ->
                                        val resizedImage = resizeImage(imageData, 1024, 576) // 调整图像大小
                                        upLoadImage(listOf(resizedImage))
                                    }
                                }
                                else -> {
                                    // 未知响应，显示警告
                                    Toast.makeText(this@VideoSessionActivity, "未知响应: $responseBody", Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            Toast.makeText(this@VideoSessionActivity, "解析响应失败: $responseBody", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onFailure = { errorMessage ->
                    runOnUiThread {
                        Toast.makeText(this@VideoSessionActivity, "上传失败: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        private fun sendGetRequest() {
            NetworkUtils.sendGetRequest(
                userId = "user_001",
                recordId = "record_001",
                onSuccess = { _, responseBody ->
                    // 处理成功响应
                    runOnUiThread {
                        Toast.makeText(this@VideoSessionActivity, "请求成功: $responseBody", Toast.LENGTH_LONG).show()
                    }
                    val apiRespon = ApiResponse(
                        code = "200",
                        message = "Success",
                        data = AnalysisData(
                            shootingAngles = ShootingAngles(
                                userId = "user_001",
                                recordId = "record_001",
                                aimingElbowAngle = 90.0,
                                aimingArmAngle = 80.0,
                                aimingBodyAngle = 10.0,
                                aimingKneeAngle = 140.0,
                                releaseElbowAngle = 180.0,
                                releaseArmAngle = 120.0,
                                releaseBodyAngle = 5.0,
                                releaseKneeAngle = 170.0,
                                releaseWristAngle = 90.0,
                                releaseBallAngle = 50.0,
                                theme = "Standard"
                            ),
                            analysis = "Your shooting form is good.",
                            suggestions = "Keep your elbow aligned.",
                            weaknessPoints = "Slight inconsistency in wrist angle."
                        )
                    )

                    val intent = Intent(this@VideoSessionActivity, OneShotEndActivity::class.java).apply {
                        putExtra("analysis_data", apiRespon) // 直接传递对象
                    }
                    startActivity(intent)
                    finish()
                },
                onFailure = { errorCode, errorMessage ->
                    // 处理失败
                    runOnUiThread {
                        Toast.makeText(this@VideoSessionActivity, "请求失败: $errorCode, $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
            )


        }
    }
}