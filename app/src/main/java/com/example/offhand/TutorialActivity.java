package com.example.offhand;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

        // 获取所有导航栏按钮的图标和文本
        ImageView homeIcon = (ImageView) ((LinearLayout) btnHome).getChildAt(0);
        TextView homeText = (TextView) ((LinearLayout) btnHome).getChildAt(1);
        
        ImageView trainingIcon = (ImageView) ((LinearLayout) btnTraining).getChildAt(0);
        TextView trainingText = (TextView) ((LinearLayout) btnTraining).getChildAt(1);
        
        ImageView tutorialIcon = (ImageView) ((LinearLayout) btnTutorial).getChildAt(0);
        TextView tutorialText = (TextView) ((LinearLayout) btnTutorial).getChildAt(1);
        
        // 重置所有按钮为非高亮状态
        homeIcon.setColorFilter(getResources().getColor(R.color.gray));
        homeText.setTextColor(getResources().getColor(R.color.gray));
        
        trainingIcon.setColorFilter(getResources().getColor(R.color.gray));
        trainingText.setTextColor(getResources().getColor(R.color.gray));
        
        tutorialIcon.setColorFilter(getResources().getColor(R.color.primaryColor));
        tutorialText.setTextColor(getResources().getColor(R.color.primaryColor));

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