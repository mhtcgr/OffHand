package com.example.offhand

import AnalysisData
import ApiResponse
import ShootingAngles
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.offhand.model.NetworkUtils
import com.google.common.util.concurrent.ListenableFuture
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View


class OneShotActivity : AppCompatActivity() {
    private lateinit var closeButton: Button

    private var previewView: PreviewView? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isRecording = false

    private var lastCaptureTime = 0L  // 记录上次拍摄的时间
    private var FRAME_INTERVAL = 166L  // 每帧间隔（1 秒 6 帧）
    // 使用线程安全的队列存储图片字节数组
    private val imageQueue = ConcurrentLinkedQueue<ByteArray>()
    // 用于同步操作的锁
    private val uploadLock = Any()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_one_shot)

        previewView = findViewById(R.id.previewView)
        closeButton = findViewById(R.id.closeButton)
        cameraExecutor = Executors.newSingleThreadExecutor()
        closeButton.setOnClickListener {
            showExitDialog()
        }

        requestStoragePermission()
        // 检查并请求摄像头权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                101
            )
        }
    }

    private fun startCamera() {
        //cameraProviderFuture 是 CameraX 的核心 API，用于异步获取 ProcessCameraProvider。
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()//Preview 组件用于在 PreviewView 里显示摄像头画面。
            preview.setSurfaceProvider(previewView?.surfaceProvider)// 让 CameraX 将视频流绘制到 UI。
//          录制需要，不知道现在还需不需要？
            val recorder = Recorder.Builder()
                .setExecutor(cameraExecutor)
                .build()//创建 Recorder 用于视频录制，指定 cameraExecutor 作为执行线程。
            videoCapture = VideoCapture.withOutput(recorder)// 将 Recorder 绑定到 videoCapture，以便之后录制视频。

            val imageAnalysis = ImageAnalysis.Builder() //创建一个图像分析器，用于处理摄像头帧。
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) // 直接获取 RGB 图像
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // 只保留最新帧，避免堆积
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->  //让 cameraExecutor 异步运行 processImage(image)。
                        val currentTime = SystemClock.elapsedRealtime()
                        if (currentTime - lastCaptureTime >= FRAME_INTERVAL) {
                            lastCaptureTime = currentTime
                            processImage(image)
                            Log.d("CameraX", "Processing frame at: $currentTime")
                        } else {
                            image.close() // 立即释放不符合时间条件的帧
                        }
                    }
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)//选择后置摄像头。
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, preview, imageAnalysis//videoCapture
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {
        val bitmap = imageProxyToBitmap(imageProxy)
        imageProxy.close()

        // 将Bitmap转换为字节数组并加入队列
        val byteArray = bitmapToByteArray(bitmap)
        bitmap.recycle()

        synchronized(uploadLock) {
            imageQueue.add(byteArray)
            // 当队列达到6张时立即触发上传
            if (imageQueue.size >= 6) {
                val images = imageQueue.take(6).toList()
                imageQueue.clear()
                upLoadImage(images)
            }
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        // 计算裁剪区域
        val scale = max(512.toFloat() / originalWidth, 288.toFloat() / originalHeight)
        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 512, 288, true)
        val byteArrayOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 10, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
//        val buffer = image.planes[0].buffer
//        val bytes = ByteArray(buffer.remaining())
//        buffer.get(bytes)
//        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val width = image.width
        val height = image.height
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // 创建 ARGB_8888 格式的 Bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val byteBuffer = ByteBuffer.wrap(bytes)
        bitmap.copyPixelsFromBuffer(byteBuffer)

        return bitmap
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
                                // 检测到投篮，跳转到结果页面
                                val intent = Intent(this, OneShotEndActivity::class.java)
                                intent.putExtra("result", message) // 传递结果（hit 或 miss）
                                startActivity(intent)
                            }
                            "receive" -> {
                                // 未检测到投篮，仅显示上传成功消息
                                Toast.makeText(this, "上传成功: $responseBody", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                // 未知响应，显示警告
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
                    Toast.makeText(this, "上传失败: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setMessage("确认退出？")
            .setPositiveButton("确认退出") { _, _ ->//点击确认退出触发发送get请求
                NetworkUtils.sendGetRequest(
                    userId = "user_001",
                    recordId = "record_001",
                    onSuccess = { _, responseBody ->
                        // 处理成功响应
                        runOnUiThread {
                            Toast.makeText(this, "请求成功: $responseBody", Toast.LENGTH_LONG).show()
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

                        val intent = Intent(this, OneShotEndActivity::class.java).apply {

                            putExtra("analysis_data", apiRespon) // 直接传递对象
                        }
                        startActivity(intent)
                        finish()
                    },
                    onFailure = { errorCode, errorMessage ->
                        // 处理失败
                        runOnUiThread {
                            Toast.makeText(this, "请求失败: $errorCode, $errorMessage", Toast.LENGTH_LONG).show()
                        }
                    }
                )

                val intent = Intent(this, OneShotEndActivity::class.java)
                startActivity(intent)
                finish()

            }
            .setNegativeButton("再想想", null)
            .show()
    }

}
