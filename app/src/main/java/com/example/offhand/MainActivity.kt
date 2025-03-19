package com.example.offhand

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.widget.Button
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
import com.example.offhand.model.ImageProcess.bitmapToByteArray
import com.example.offhand.model.ImageProcess.imageProxyToBitmap
import com.example.offhand.model.ImageProcess.resizeImage
import com.example.offhand.model.NetworkUtils
import com.google.common.util.concurrent.ListenableFuture
import org.json.JSONException
import org.json.JSONObject
import java.io.File
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
    private var cachedImage: ByteArray? = null // 缓存当前帧的图像数据
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
                            //Log.d("CameraX", "Processing frame at: $currentTime")
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
                //Log.e("CameraX", "Use case binding failed", exc)
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
                val images = mutableListOf<ByteArray>()
                repeat(6) {
                    imageQueue.poll()?.let { images.add(it) }
                }
                if (images.size == 6) {
                    // 调用 NetworkUtils 上传图片
                    upLoadImage(images)
                }
            }
        }
    }

    private fun upLoadImage(images: List<ByteArray>){
        NetworkUtils.uploadImages(
            images,
            onSuccess = {responseBody ->
                runOnUiThread {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val message = jsonResponse.getString("message")
                        when (message) {
                            "hit" -> {
                                // 检测到投篮
                                incrementShots()
                                incrementScore()
                            }
                            "miss" -> {
                                incrementShots()
                            }
                            "receive" -> {
                                // 未检测到投篮，仅显示上传成功消息
                                //Toast.makeText(this, "上传成功: $responseBody", Toast.LENGTH_LONG).show()
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
                    //Toast.makeText(this, "上传失败: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        )
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
                //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) // 强制横屏
                finish()
            }
            .setNegativeButton("再想想", null)
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            }
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
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}