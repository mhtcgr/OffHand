package com.example.offhand;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.offhand.model.MainApiResponse;
import com.example.offhand.model.LatestTrainingSummary;
import com.example.offhand.model.RecentDaysTrainingSummary;
import com.google.gson.Gson;

public class TrainingActivity extends AppCompatActivity {
    // 最新训练数据视图
    private TextView tvLastDate, tvLastHit, tvLastAttempt, tvShootingType, tvLastSuggestions;
    // 近七天统计视图
    private TextView tvSevenDaysCount, tvSevenDaysHit, tvSevenDaysAttempt, tvSevenDaysSuggestions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);

        // 初始化视图
        initViews();

        String jsonData = getIntent().getStringExtra("trainingData");
        MainApiResponse response = new Gson().fromJson(jsonData, MainApiResponse.class);

        // 绑定数据到视图
        if (response != null && response.data != null) {
            displayTrainingData(response.data);
        }

        setupBottomNavigation();
        setupContent();
    }

    private void initViews() {
        // 绑定最新训练数据视图
        tvLastDate = findViewById(R.id.tv_last_date);
        tvLastHit = findViewById(R.id.tv_last_hit);
        tvLastAttempt = findViewById(R.id.tv_last_attempt);
        tvShootingType = findViewById(R.id.tv_shooting_type);
        tvLastSuggestions = findViewById(R.id.tv_last_suggestions);

        // 绑定近七天统计视图
        tvSevenDaysCount = findViewById(R.id.tv_seven_days_count);
        tvSevenDaysHit = findViewById(R.id.tv_seven_days_hit);
        tvSevenDaysAttempt = findViewById(R.id.tv_seven_days_attempt);
        tvSevenDaysSuggestions = findViewById(R.id.tv_seven_days_suggestions);
    }

    @SuppressLint("DefaultLocale")
    private void displayTrainingData(MainApiResponse.MainData data) {
        // 填充最新训练数据
        LatestTrainingSummary latest = data.latestTrainingSummary;
        if (latest != null) {
            tvLastDate.setText(latest.trainingDate != null ? latest.trainingDate : "暂无日期");
            tvLastHit.setText(String.format("命中：%d次", latest.hits));
            tvLastAttempt.setText(String.format("尝试：%d次", latest.attempts));
            tvShootingType.setText(String.format("投篮类型：中距离投篮"));
            tvLastSuggestions.setText(String.format("%s", latest.suggestions != null ? latest.suggestions : "暂无建议"));
        }

        // 填充近七天统计
        RecentDaysTrainingSummary recent = data.recentDaysTrainingSummary;
        if (recent != null) {
            tvSevenDaysCount.setText(String.format("训练次数：%d次", recent.trainingCount));
            tvSevenDaysHit.setText(String.format("命中：%d次", recent.hits));
            tvSevenDaysAttempt.setText(String.format("尝试：%d次", recent.attempts));
            tvSevenDaysSuggestions.setText(String.format("%s", recent.suggestions != null ? recent.suggestions : "暂无建议"));
        }
    }

    private void setupContent() {
        Button btnStartTraining = findViewById(R.id.btnStartTraining);
        btnStartTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TrainingActivity.this, TrainingSessionActivity.class);
                startActivity(intent);
            }
        });
        
        // 示例：从SharedPreferences或数据库获取投篮类型，这里用静态值演示
        String shootingType = "middle_shoot"; // 或 "three_point_shoot"
        
        if ("middle_shoot".equals(shootingType)) {
            tvShootingType.setText("中距离投篮");
        } else if ("three_point_shoot".equals(shootingType)) {
            tvShootingType.setText("三分投篮");
        }
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
        
        // 设置训练按钮高亮
        trainingIcon.setColorFilter(getResources().getColor(R.color.primaryColor));
        trainingText.setTextColor(getResources().getColor(R.color.primaryColor));
        
        // 设置其他按钮为非高亮
        homeIcon.setColorFilter(getResources().getColor(R.color.gray));
        homeText.setTextColor(getResources().getColor(R.color.gray));
        
        tutorialIcon.setColorFilter(getResources().getColor(R.color.gray));
        tutorialText.setTextColor(getResources().getColor(R.color.gray));

        // 设置首页按钮点击事件
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TrainingActivity.this, StartActivity.class);
                startActivity(intent);
                finish();
            }
        });
        
        // 当前页面是训练页面，不需要为训练按钮设置点击事件
        
        // 设置教学按钮点击事件
        btnTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TrainingActivity.this, TutorialActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}