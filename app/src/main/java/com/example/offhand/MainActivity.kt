package com.example.offhand


import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


//多次投篮的主要界面

class MainActivity : AppCompatActivity() {
    private var previewView: PreviewView? = null
    private lateinit var timerTextView: TextView
    private lateinit var startPauseButton: Button
    private lateinit var closeButton: Button
    private lateinit var scoreTextView: TextView
    private lateinit var shotsTextView: TextView

    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())

    // 请求码
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    private var score = 0
    private var shots = 0

    private val client = OkHttpClient()

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
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        timerTextView = findViewById(R.id.timerTextView)
        startPauseButton = findViewById(R.id.startPauseButton)
        closeButton = findViewById(R.id.closeButton)
        scoreTextView = findViewById(R.id.scoreTextView)
        shotsTextView = findViewById(R.id.shotsTextView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        startPauseButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

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

    // 摄像头权限请求结果的处理
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，启动摄像头
                startCamera()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("摄像头权限请求")
                    .setMessage("应用需要摄像头权限来录制视频，请在设置中手动开启。")
                    .setPositiveButton("去设置") { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
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

//    private fun processImage(imageProxy: ImageProxy) {
//        val bitmap = imageProxyToBitmap(imageProxy)
//        imageProxy.close()
//        CoroutineScope(Dispatchers.IO).launch {
//            uploadImage(bitmap)
//        }
//    }
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
            val images = mutableListOf<ByteArray>()
            repeat(6) {
                imageQueue.poll()?.let { images.add(it) }
            }
            if (images.size == 6) {
                uploadImages(images)
            }
        }
    }
}

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
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

    private fun uploadImages(imageDataList: List<ByteArray>) {
        val url = "http://10.52.34.249:8080/detection/uploadImages"

        // 构建多部分请求体
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("userId", "1")

        // 添加多个图片文件
        imageDataList.forEachIndexed { index, byteArray ->
            requestBody.addFormDataPart(
                "images",  // 根据后端要求调整参数名（如images[]）
                "frame_${System.currentTimeMillis()}_$index.jpg",
                byteArray.toRequestBody("image/jpeg".toMediaType())
            )
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBody.build())
            .addHeader("Authorization", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6IjEiLCJqdGkiOiI4ZDliMTcxOC1lMDA0LTQ3OWItYWIwYy02YjZkN2NlYTBkOWYiLCJleHAiOjE3NDUyNTkzNTIsImlhdCI6MTc0MTY1OTM1Miwic3ViIjoiUGVyaXBoZXJhbHMiLCJpc3MiOiJUaWFtIn0.1SBagf2D_HQ2k3J63VAsrYinRMp7yzukMsg4xXjk13I")
            .addHeader("Cache-Control", "no-cache")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "上传失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseCode = response.code
                    val responseBody = response.body?.string() ?: "无返回内容"
                    runOnUiThread {
                        if (!response.isSuccessful) {
                            Toast.makeText(
                                this@MainActivity,
                                "上传失败: HTTP $responseCode, 错误信息: $responseBody",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(this@MainActivity, "上传成功: $responseBody", Toast.LENGTH_LONG).show()
                        }
                    }
                    println("Response Code: $responseCode")
                    println("Response Body: $responseBody")
                }
            }
        })
    }

    private fun uploadImage(bitmap: Bitmap) {
        val url = "http://10.52.34.249:8080/detection/uploadImages"
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        val mediaType = "image/jpeg".toMediaType()
//        val requestBody = byteArray.toRequestBody(mediaType, 0, byteArray.size)
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            // 添加文件部分（图片）
            .addFormDataPart(
                "images", // 服务器接收文件的参数名
                "image.jpg", // 文件名
                byteArray.toRequestBody(mediaType, 0, byteArray.size)
            )
            // 添加文本参数（userId=1）
            .addFormDataPart("userId", "1") // 参数名和值
            .build()

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
                    Toast.makeText(this@MainActivity, "上传失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseCode = response.code
                    val responseBody = response.body?.string() ?: "无返回内容"
                    runOnUiThread {
                        if (!response.isSuccessful) {
                            Toast.makeText(
                                this@MainActivity,
                                "上传失败: HTTP $responseCode, 错误信息: $responseBody",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(this@MainActivity, "上传成功: $responseBody", Toast.LENGTH_LONG).show()
                        }
                    }
                    println("Response Code: $responseCode")
                    println("Response Body: $responseBody")
                }
            }
        })
    }


    private val timerRunnable = object : Runnable {
        @SuppressLint("DefaultLocale")
        override fun run() {
            if (isRecording) {
                seconds++
                val minutes = seconds / 60
                val secs = seconds % 60
                timerTextView.text = String.format("%02d:%02d", minutes, secs)
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setMessage("确认退出？")
            .setPositiveButton("确认退出") { _, _ ->

                val intent = Intent(this, MultiShotEndActivity::class.java)
                intent.putExtra("TIMER", timerTextView.text.toString())
                intent.putExtra("SCORE", scoreTextView.text.toString())
                intent.putExtra("SHOTS", shotsTextView.text.toString())
                startActivity(intent)
                finish()
            }
            .setNegativeButton("再想想", null)
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun incrementScore() {
        score++
        scoreTextView.text = score.toString()
    }

    @SuppressLint("SetTextI18n")
    private fun incrementShots() {
        shots++
        shotsTextView.text = shots.toString()
    }

    private fun startRecording() {
        //处理计时器
        isRecording = true
        startPauseButton.text = "暂停"
        handler.post(timerRunnable)

        val videoDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "MyAppVideos")
        if (!videoDir.exists()) {
            videoDir.mkdirs()
        }
        val videoFile = File(videoDir, "recorded_video_${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        recording = videoCapture?.output
            ?.prepareRecording(this, outputOptions)
            ?.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        isRecording = true
                        startPauseButton.text = "停止"
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            Toast.makeText(this, "Video saved: ${videoFile.absolutePath}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Video capture failed: ${recordEvent.error}", Toast.LENGTH_SHORT).show()
                        }
                        isRecording = false
                        startPauseButton.text = "开始"
                    }
                }
            }
    }

    private fun stopRecording() {
        //处理计时器
        isRecording = false
        startPauseButton.text = "开始"
        handler.removeCallbacks(timerRunnable)
        recording?.stop()
        recording = null

        val videoDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "MyAppVideos")
        val latestVideoFile = videoDir.listFiles()?.maxByOrNull { it.lastModified() }

        if (latestVideoFile != null) {
            //uploadVideo(latestVideoFile)
        } else {
            Toast.makeText(this, "未找到录制的视频", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}