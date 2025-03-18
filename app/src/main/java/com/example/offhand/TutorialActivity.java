package com.example.offhand;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class TutorialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        LinearLayout btnHome = findViewById(R.id.btn_home);
        LinearLayout btnTraining = findViewById(R.id.btn_training);
        LinearLayout btnTutorial = findViewById(R.id.btn_tutorial);

        // 设置首页按钮点击事件
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TutorialActivity.this, StartActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 设置训练按钮点击事件
        btnTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TutorialActivity.this, TrainingActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 当前页面是教学页面，不需要为教学按钮设置点击事件
    }
} 