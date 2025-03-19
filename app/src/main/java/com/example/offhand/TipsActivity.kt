package com.example.offhand

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.offhand.model.NetworkUtils
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import org.json.JSONObject
import android.bluetooth.BluetoothSocket

class TipsActivity : AppCompatActivity() {
    private var connectedSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isDestroyed = false

    companion object {
        private const val TAG = "TipsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_tips)

        // 获取蓝牙连接
        connectedSocket = DeviceScanActivity.connectedSocket
        initBluetoothConnection()

        val selectedTheme = intent.getStringExtra("selectedTheme") ?: "默认主题"
        val selectedMethod = intent.getStringExtra("selectedMethod") ?: "默认方法"
        val nextButton: Button = findViewById(R.id.nextButton)
        val closeButton: Button = findViewById(R.id.closeButton)

        nextButton.setOnClickListener {
            if (selectedMethod == "multiple") {
                // 跳转到多次投篮页面
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("selectedTheme", selectedTheme)
                sendTrainingMode(selectedMethod)
                startActivity(intent)
                finish()
            } else {
                // 跳转到单次投篮页面
                val intent = Intent(this, OneShotActivity::class.java)
                intent.putExtra("selectedTheme", selectedTheme)
                sendTrainingMode(selectedMethod)
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

    private fun initBluetoothConnection() {
        try {
            connectedSocket?.let {
                inputStream = it.inputStream
                outputStream = it.outputStream
                startListening()
            } ?: run {
                //Toast.makeText(this, "蓝牙未连接", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "蓝牙未连接")
            }
        } catch (e: IOException) {
            Log.e(TAG, "获取蓝牙流失败", e)
        }
    }

    private fun startListening() {
        Thread {
            val buffer = ByteArray(1024)
            while (!isDestroyed) {
                try {
                    val bytes = inputStream?.read(buffer) ?: break
                    if (bytes > 0) {
                        val message = String(buffer, 0, bytes).trim()
                        Log.d(TAG, "收到消息: $message")
                        handleReceivedMessage(message)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "蓝牙连接断开", e)
//                    runOnUiThread {
//                        Toast.makeText(this, "蓝牙连接已断开", Toast.LENGTH_SHORT).show()
//                    }
                    break
                }
            }
        }.start()
    }

    private fun handleReceivedMessage(message: String) {
        when {
            message.startsWith("mode:") -> {
                val mode = message.removePrefix("mode:").trim()
                handler.post {
                    navigateToTrainingActivity(mode)
                }
            }
        }
    }

    private fun sendTrainingMode(mode: String) {
        try {
            val data = "mode:$mode\n".toByteArray()
            outputStream?.write(data)
            Log.d(TAG, "已发送训练模式: $mode")
        } catch (e: IOException) {
            Log.e(TAG, "发送数据失败", e)
//            runOnUiThread {
//                Toast.makeText(this, "发送数据到手表失败", Toast.LENGTH_SHORT).show()
//            }
        }
    }

    private fun navigateToTrainingActivity(mode: String) {
        val selectedTheme = intent.getStringExtra("selectedTheme") ?: "three point"

        val intent = when (mode) {
            "multiple" -> Intent(this, MainActivity::class.java)
            else -> Intent(this, OneShotActivity::class.java)
        }.apply {
            putExtra("selectedTheme", selectedTheme)
            putExtra("selectedMethod", mode)
        }

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isDestroyed = true
        try {
            inputStream?.close()
            outputStream?.close()
            connectedSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "关闭连接失败", e)
        }
    }
}