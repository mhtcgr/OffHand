package com.example.offhand;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.offhand.model.MainApiResponse;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StartActivity extends AppCompatActivity {
    private static final String TAG = "StartActivity";
    private static final String USER_ID = "user_001";
    private static final String AUTH_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6IjEiLCJqdGkiOiI4ZDliMTcxOC1lMDA0LTQ3OWItYWIwYy02YjZkN2NlYTBkOWYiLCJleHAiOjE3NDUyNTkzNTIsImlhdCI6MTc0MTY1OTM1Miwic3ViIjoiUGVyaXBoZXJhbHMiLCJpc3MiOiJUaWFtIn0.1SBagf2D_HQ2k3J63VAsrYinRMp7yzukMsg4xXjk13I";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // 蓝牙手表按钮
        findViewById(R.id.btnBluetoothWatch).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        });

        // 蓝牙耳机按钮
        findViewById(R.id.btnBluetoothEarphone).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        });

        // 开始训练按钮
        findViewById(R.id.btnStartTraining).setOnClickListener(v -> {
            fetchTrainingData();
        });
    }

    private void fetchTrainingData() {
        OkHttpClient client = new OkHttpClient();
        HttpUrl url = HttpUrl.parse("http://10.52.34.249:8080/history/beforeTraining")
                .newBuilder()
                .addQueryParameter("userId", USER_ID)
                .build();

        Log.d(TAG, "尝试请求URL: " + url.toString()); // 添加请求日志

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", AUTH_TOKEN)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d(TAG, "收到响应，状态码: " + response.code()); // 记录状态码

                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    Log.d(TAG, "原始响应数据: " + jsonData); // 输出原始JSON

                    Gson gson = new Gson();
                    MainApiResponse apiResponse = gson.fromJson(jsonData, MainApiResponse.class);

                    Intent intent = new Intent(StartActivity.this, TrainingActivity.class);
                    intent.putExtra("trainingData", jsonData);
                    startActivity(intent);
                } else {
                    Log.e(TAG, "服务器返回错误，HTTP状态码: " + response.code());
                    String errorBody = response.body().string();
                    Log.e(TAG, "错误响应内容: " + errorBody);
                }

            }
        });
    }
}