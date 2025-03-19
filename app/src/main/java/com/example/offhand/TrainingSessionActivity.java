package com.example.offhand;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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
    private Button btnSingle;
    private Button btnMultiple;
    private Button btnThreePoint;
    private Button btnMidShoot;
    private Button btnReady;
    
    private String selectedTrainingType = ""; // "single" or "multiple"
    private String selectedShootingType = ""; // "three_point" or "mid_shoot"
    private final String authToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6IjEiLCJqdGkiOiI4ZDliMTcxOC1lMDA0LTQ3OWItYWIwYy02YjZkN2NlYTBkOWYiLCJleHAiOjE3NDUyNTkzNTIsImlhdCI6MTc0MTY1OTM1Miwic3ViIjoiUGVyaXBoZXJhbHMiLCJpc3MiOiJUaWFtIn0.1SBagf2D_HQ2k3J63VAsrYinRMp7yzukMsg4xXjk13I";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_session);
        
        initViews();
        setupBottomNavigation();
        setupButtonListeners();
    }
    
    private void initViews() {
        btnSingle = findViewById(R.id.btn_single);
        btnMultiple = findViewById(R.id.btn_multiple);
        btnThreePoint = findViewById(R.id.btn_three_point);
        btnMidShoot = findViewById(R.id.btn_mid_shoot);
        btnReady = findViewById(R.id.btn_ready);
    }
    
    private void setupButtonListeners() {
        // 训练方式选择
        btnSingle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTrainingType("single");
            }
        });
        
        btnMultiple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTrainingType("multiple");
            }
        });
        
        // 训练主题选择
        btnThreePoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectShootingType("three_point");
            }
        });
        
        btnMidShoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectShootingType("mid_shoot");
            }
        });
        
        // 准备好了按钮
        btnReady.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTraining();
            }
        });
    }
    
    private void selectTrainingType(String type) {
        selectedTrainingType = type;
        
        if ("single".equals(type)) {
            btnSingle.setBackgroundTintList(getResources().getColorStateList(R.color.primaryColor));
            btnMultiple.setBackgroundTintList(getResources().getColorStateList(R.color.unselected_color));
        } else {
            btnSingle.setBackgroundTintList(getResources().getColorStateList(R.color.unselected_color));
            btnMultiple.setBackgroundTintList(getResources().getColorStateList(R.color.primaryColor));
        }
    }
    
    private void selectShootingType(String type) {
        selectedShootingType = type;
        
        if ("three_point".equals(type)) {
            btnThreePoint.setBackgroundTintList(getResources().getColorStateList(R.color.primaryColor));
            btnMidShoot.setBackgroundTintList(getResources().getColorStateList(R.color.unselected_color));
        } else {
            btnThreePoint.setBackgroundTintList(getResources().getColorStateList(R.color.unselected_color));
            btnMidShoot.setBackgroundTintList(getResources().getColorStateList(R.color.primaryColor));
        }
    }
    
    private void startTraining() {
        // 检查是否选择了训练方式和训练主题
        if (selectedTrainingType.isEmpty() || selectedShootingType.isEmpty()) {
            // 显示提示，要求用户选择训练方式和主题
            return;
        }
        
        Intent intent = new Intent(TrainingSessionActivity.this, TipsActivity.class);
        // 传递训练主题参数
        intent.putExtra("selectedMethod", selectedTrainingType);
        intent.putExtra("selectedTheme", selectedShootingType);
        startActivity(intent);
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
        
        // 设置训练按钮高亮（因为训练会话是训练功能的一部分）
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
                Intent intent = new Intent(TrainingSessionActivity.this, StartActivity.class);
                startActivity(intent);
                finish();
            }
        });
        
        // 设置训练按钮点击事件
        btnTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TrainingSessionActivity.this, TrainingActivity.class);
                startActivity(intent);
                finish();
            }
        });
        
        // 设置教学按钮点击事件
        btnTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TrainingSessionActivity.this, TutorialActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }


}