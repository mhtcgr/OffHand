package com.example.offhand;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TrainingSessionActivity extends AppCompatActivity {
    private String selectedTheme = "";
    private String selectedMethod = "";
    private final String authToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6IjEiLCJqdGkiOiI4ZDliMTcxOC1lMDA0LTQ3OWItYWIwYy02YjZkN2NlYTBkOWYiLCJleHAiOjE3NDUyNTkzNTIsImlhdCI6MTc0MTY1OTM1Miwic3ViIjoiUGVyaXBoZXJhbHMiLCJpc3MiOiJUaWFtIn0.1SBagf2D_HQ2k3J63VAsrYinRMp7yzukMsg4xXjk13I";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_session);

        setupButtonSelection();
        setupReadyButton();
        // 找到底部导航栏中的按钮
        LinearLayout btnHome = findViewById(R.id.btn_home);
        LinearLayout btnTraining = findViewById(R.id.btn_training);


        // 设置首页按钮点击事件
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 如果当前已经在StartActivity，则不需要重新启动
                Intent intent = new Intent(TrainingSessionActivity.this, StartActivity.class);
                startActivity(intent);
                finish(); // 结束当前Activity
            }
        });

        // 设置训练按钮点击事件
        btnTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 如果当前已经在TrainingActivity，则不需要重新启动
                Intent intent = new Intent(TrainingSessionActivity.this, TrainingActivity.class);
                startActivity(intent);
                finish(); // 结束当前Activity
            }
        });

        LinearLayout btnTutorial = findViewById(R.id.btn_tutorial);
        btnTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 如果当前已经在TrainingActivity，则不需要重新启动
                Intent intent = new Intent(TrainingSessionActivity.this, TutorialActivity.class);
                startActivity(intent);
                finish(); // 结束当前Activity
            }
        });
    }

    private void setupButtonSelection() {
        // 训练方式选择，"mid shoot"字段有问题！
        findViewById(R.id.btn_single).setOnClickListener(v -> {
            selectMethodButton((Button) v, "single");
            deselectMethodButton(findViewById(R.id.btn_multiple));
        });

        findViewById(R.id.btn_multiple).setOnClickListener(v -> {
            selectMethodButton((Button) v, "multiple");
            deselectMethodButton(findViewById(R.id.btn_single));
        });

        // 训练主题选择
        findViewById(R.id.btn_three_point).setOnClickListener(v -> {
            selectThemeButton((Button) v, "three point");
            deselectThemeButton(findViewById(R.id.btn_mid_shoot));
        });

        findViewById(R.id.btn_mid_shoot).setOnClickListener(v -> {
            selectThemeButton((Button) v, "mid shoot");
            deselectThemeButton(findViewById(R.id.btn_three_point));
        });
    }

    private void selectMethodButton(Button button, String method) {
        selectedMethod = method;
        button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.selected_color)));
    }

    private void deselectMethodButton(Button button) {
        button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.unselected_color)));
    }

    private void selectThemeButton(Button button, String theme) {
        selectedTheme = theme;
        button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.selected_color)));
    }

    private void deselectThemeButton(Button button) {
        button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.unselected_color)));
    }

    private void setupReadyButton() {
        findViewById(R.id.btn_ready).setOnClickListener(v -> {
            if (validateSelection()) {
                postTrainingData();
                jumpToTips();
            } else {
                Toast.makeText(this, "请先选择训练方式和主题", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateSelection() {
        return !selectedMethod.isEmpty() && !selectedTheme.isEmpty();
    }

    private void postTrainingData() {
        OkHttpClient client = new OkHttpClient();

        JSONObject json = new JSONObject();
        try {
            json.put("userId", "1");
            json.put("theme", selectedTheme);
            json.put("trainingMethod", selectedMethod);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Request request = new Request.Builder()
                .url("http://10.52.34.249:8080/detection/setting")
                .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                .addHeader("Authorization", authToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(TrainingSessionActivity.this, "请求失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(TrainingSessionActivity.this, "训练开始!", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(TrainingSessionActivity.this, "请求失败: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void jumpToTips() {
        Intent intent = new Intent(this, TipsActivity.class);

        // 将参数添加到 Intent 中
        intent.putExtra("selectedTheme", selectedTheme);
        intent.putExtra("selectedMethod", selectedMethod);

        // 启动目标 Activity
        startActivity(intent);
    }
}