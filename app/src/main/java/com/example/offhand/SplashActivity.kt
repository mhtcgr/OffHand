package com.example.offhand

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DISPLAY_LENGTH: Long = 1000 // 1秒

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 设置淡入动画
        val fadeIn = AlphaAnimation(0.0f, 1.0f)
        fadeIn.duration = 500 // 0.5秒淡入

        val logoImage = findViewById<ImageView>(R.id.logo_image)
        val appNameText = findViewById<TextView>(R.id.app_name_text)
        val taglineText = findViewById<TextView>(R.id.tagline_text)

        logoImage.startAnimation(fadeIn)
        
        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            
            override fun onAnimationEnd(animation: Animation?) {
                // Logo淡入后，让文字也淡入
                val textFadeIn = AlphaAnimation(0.0f, 1.0f)
                textFadeIn.duration = 300
                
                appNameText.startAnimation(textFadeIn)
                taglineText.startAnimation(textFadeIn)
            }
            
            override fun onAnimationRepeat(animation: Animation?) {}
        })

        // 1秒后跳转到StartActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val mainIntent = Intent(this@SplashActivity, StartActivity::class.java)
            startActivity(mainIntent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish() // 关闭开屏页面，避免按返回键返回
        }, SPLASH_DISPLAY_LENGTH)
    }
} 