package com.example.offhand
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
//class sendMessageActivity : AppCompatActivity() {
//    private val client = OkHttpClient()
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_send_message)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//        val button = findViewById<Button>(R.id.button1)
//        button.setOnClickListener {
//            sendPostRequest()
//        }
//    }
//
//    private fun sendPostRequest() {
//        val url = "http://10.52.34.249:8080/detection/setting"
//        val json = """
//            {
//                "userId": "1",
//                "trainingMethod": "single",
//                "theme": "three point"
//            }
//        """.trimIndent()
//
//        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())
//
//        val request = Request.Builder()
//            .url(url)
//            .post(requestBody)
//            .addHeader("Authorization", "valid_token")
//            .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                e.printStackTrace()
//                runOnUiThread {
//                    Toast.makeText(this@sendMessageActivity, "上传失败: ${'$'}{e.localizedMessage}", Toast.LENGTH_LONG).show()
//                }
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                response.use {
//                    val responseCode = response.code
//                    val responseBody = response.body?.string() ?: "无返回内容"
//                    runOnUiThread {
//                        if (!response.isSuccessful) {
//                            Toast.makeText(
//                                this@sendMessageActivity,
//                                "上传失败: HTTP ${'$'}responseCode, 错误信息: ${'$'}responseBody",
//                                Toast.LENGTH_LONG
//                            ).show()
//                        } else {
//                            Toast.makeText(this@sendMessageActivity, "上传成功: ${'$'}responseBody", Toast.LENGTH_LONG).show()
//                        }
//                    }
//                    println("Response Code: ${'$'}responseCode")
//                    println("Response Body: ${'$'}responseBody")
//                }
//            }
//        })
//    }
//}

class sendMessageActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_send_message)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val button = findViewById<Button>(R.id.button1)
        button.setOnClickListener {
            sendPostRequest()
        }
    }

    private fun sendPostRequest() {
        val url = "http://10.52.34.249:8080/detection/uploadImages"

        // 获取图片文件路径（请替换为你的实际路径）
        val imageFile = File(getExternalFilesDir(null), "IMG_0687.JPG")

        if (!imageFile.exists()) {
            val createSuccess = createImage(imageFile)
            if (!createSuccess) {
                Toast.makeText(this, "图片创建失败", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 压缩图片
        val compressedImageData = compressImage(imageFile)
        if (compressedImageData == null) {
            Toast.makeText(this, "图片压缩失败", Toast.LENGTH_SHORT).show()
            return
        }

        // 创建 MultipartBody
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("userId", "1")
            .addFormDataPart(
                "images", imageFile.name,
                compressedImageData.toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        // 创建请求
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6IjEiLCJqdGkiOiI4ZDliMTcxOC1lMDA0LTQ3OWItYWIwYy02YjZkN2NlYTBkOWYiLCJleHAiOjE3NDUyNTkzNTIsImlhdCI6MTc0MTY1OTM1Miwic3ViIjoiUGVyaXBoZXJhbHMiLCJpc3MiOiJUaWFtIn0.1SBagf2D_HQ2k3J63VAsrYinRMp7yzukMsg4xXjk13I")
            .addHeader("Cache-Control", "no-cache")
            .build()

        // 发起请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@sendMessageActivity, "上传失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseCode = response.code
                    val responseBody = response.body?.string() ?: "无返回内容"
                    runOnUiThread {
                        if (!response.isSuccessful) {
                            Toast.makeText(
                                this@sendMessageActivity,
                                "上传失败: HTTP $responseCode, 错误信息: $responseBody",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(this@sendMessageActivity, "上传成功: $responseBody", Toast.LENGTH_LONG).show()
                        }
                    }
                    println("Response Code: $responseCode")
                    println("Response Body: $responseBody")
                }
            }
        })
    }

    private fun createImage(imageFile: File): Boolean {
        return try {
            // 创建红色背景的测试图片
            val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.RED)  // 填充红色背景
            }

            // 添加文字标识
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                color = Color.WHITE
                textSize = 40f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Test Image", bitmap.width/2f, bitmap.height/2f, paint)

            // 保存文件
            FileOutputStream(imageFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                fos.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "图片创建失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }
    /**
     * 压缩图片
     * @param file 图片文件
     * @return 压缩后的字节数组
     */
    private fun compressImage(file: File): ByteArray? {
        return try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = false  // 解析完整图片
            val bitmap = BitmapFactory.decodeStream(FileInputStream(file), null, options)

            val outputStream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 50, outputStream) // 质量压缩到 50%
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}