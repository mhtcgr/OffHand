package com.example.offhand;

import android.content.Intent;
import android.os.Bundle;
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

        findViewById(R.id.btnStartTraining).setOnClickListener(v -> {
            startActivity(new Intent(TrainingActivity.this, TrainingSessionActivity.class));
        });
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

    private void displayTrainingData(MainApiResponse.MainData data) {
        // 填充最新训练数据
        LatestTrainingSummary latest = data.latestTrainingSummary;
        if (latest != null) {
            tvLastDate.setText(latest.trainingDate != null ? latest.trainingDate : "暂无日期");
            tvLastHit.setText(String.format("命中：%d次", latest.hits));
            tvLastAttempt.setText(String.format("尝试：%d次", latest.attempts));
            tvShootingType.setText(String.format("投篮类型：%s", latest.shootingType != null ? latest.shootingType : "未知"));
            tvLastSuggestions.setText(String.format("建议：%s", latest.suggestions != null ? latest.suggestions : "暂无建议"));
        }

        // 填充近七天统计
        RecentDaysTrainingSummary recent = data.recentDaysTrainingSummary;
        if (recent != null) {
            tvSevenDaysCount.setText(String.format("训练次数：%d次", recent.trainingCount));
            tvSevenDaysHit.setText(String.format("命中：%d次", recent.hits));
            tvSevenDaysAttempt.setText(String.format("尝试：%d次", recent.attempts));
            tvSevenDaysSuggestions.setText(String.format("建议：%s", recent.suggestions != null ? recent.suggestions : "暂无建议"));
        }
    }
}