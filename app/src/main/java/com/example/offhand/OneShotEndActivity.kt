package com.example.offhand

import AnalysisData
import ApiResponse
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class OneShotEndActivity : AppCompatActivity() {
    private lateinit var next_shot_button: Button
    private lateinit var return_button: Button
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
            findViewById<TextView>(R.id.tv_last_attempt).text = data.data.shootingAngles.aimingArmAngle.toString()

            // ...其他角度数据

            // 分析建议
            findViewById<TextView>(R.id.tv_analysis).text = data.data.analysis

            // 弱点提示
            findViewById<TextView>(R.id.tv_weakness).text = data.data.weaknessPoints
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
            val intent = Intent(this, TrainingSessionActivity::class.java)
            startActivity(intent)
            finish()
        }


    }
}