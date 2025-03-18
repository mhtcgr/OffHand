package com.example.offhand;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.offhand.model.MainApiResponse;
import com.example.offhand.model.NetworkUtils;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

        // 找到底部导航栏中的按钮
        LinearLayout btnHome = findViewById(R.id.btn_home);
        LinearLayout btnTraining = findViewById(R.id.btn_training);


        // 设置首页按钮点击事件
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 如果当前已经在StartActivity，则不需要重新启动
//                if (!(StartActivity.this instanceof StartActivity)) {
//                    Intent intent = new Intent(StartActivity.this, StartActivity.class);
//                    startActivity(intent);
//                    finish(); // 结束当前Activity
//                }
            }
        });

        // 设置训练按钮点击事件
        btnTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 如果当前已经在TrainingActivity，则不需要重新启动
                Intent intent = new Intent(StartActivity.this, TrainingActivity.class);
                startActivity(intent);
                finish(); // 结束当前Activity
            }
        });
        LinearLayout btnTutorial = findViewById(R.id.btn_tutorial);
        btnTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 如果当前已经在TrainingActivity，则不需要重新启动
                Intent intent = new Intent(StartActivity.this, TutorialActivity.class);
                startActivity(intent);
                finish(); // 结束当前Activity
            }
        });

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

        findViewById(R.id.btnUploadVideo).setOnClickListener(v -> {
            //跳转到VideoSessionActivity
            Intent intent = new Intent(StartActivity.this, VideoSessionActivity.class);
            startActivity(intent);
            //openVideoPicker();
        });
    }

    private void fetchTrainingData() {
        OkHttpClient client = new OkHttpClient();
        String baseUrl = GlobalVariables.INSTANCE.getBaseUrl();
        HttpUrl url = HttpUrl.parse(baseUrl+"/history/beforeTraining")
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

    // 打开视频选择器
    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        startActivityForResult(intent, 1);
    }

    // 处理视频选择结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Uri videoUri = data.getData();
            if (videoUri != null) {
                // 上传视频
                uploadVideo(videoUri);
            }
        }
    }

    // 上传视频
    private void uploadVideo(Uri videoUri) {
        // TODO: 实现视频上传逻辑
        new VideoFrameExtractorTask().execute(videoUri);
        Toast.makeText(this, "视频已选择: " + videoUri, Toast.LENGTH_SHORT).show();
    }

    private class VideoFrameExtractorTask extends AsyncTask<Uri, Void, List<byte[]>> {

        @Override
        protected List<byte[]> doInBackground(Uri... uris) {
            Uri videoUri = uris[0];
            List<byte[]> imageList = new ArrayList<>();
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            try {
                retriever.setDataSource(getApplicationContext(), videoUri);

                // 获取视频时长（毫秒）
                String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                assert durationStr != null;
                long durationMs = Long.parseLong(durationStr);

                // 每秒提取 6 帧
                for (long timeMs = 0; timeMs < durationMs; timeMs += 1000 / 6) {
                    // 获取当前时间点的帧
                    Bitmap bitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST);
                    if (bitmap != null) {
                        // 将 Bitmap 转换为字节数组
                        byte[] imageData = bitmapToByteArray(bitmap);
                        imageList.add(imageData);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    retriever.release();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return imageList;
        }

        @Override
        protected void onPostExecute(List<byte[]> imageList) {
            if (imageList != null && !imageList.isEmpty()) {
                // 调用 uploadImages 方法上传图片列表
                NetworkUtils.INSTANCE.uploadImages(
                        imageList,
                        responseBody -> {
                            // 处理上传成功逻辑
                            runOnUiThread(() -> {
                                try {
                                    JSONObject jsonResponse = new JSONObject(responseBody);
                                    String message = jsonResponse.getString("message");
                                    switch (message) {
                                        case "hit":
                                        case "miss":
                                        case "receive":
                                            // 未检测到投篮，仅显示上传成功消息
                                            Toast.makeText(StartActivity.this, "上传成功: " + responseBody, Toast.LENGTH_LONG).show();
                                            break;
                                        default:
                                            // 未知响应，显示警告
                                            Toast.makeText(StartActivity.this, "未知响应: " + responseBody, Toast.LENGTH_LONG).show();
                                            break;
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Toast.makeText(StartActivity.this, "解析响应失败: " + responseBody, Toast.LENGTH_LONG).show();
                                }
                            });
                            return null;
                        },
                        errorMessage -> {
                            // 处理上传失败逻辑
                            runOnUiThread(() -> {
                                Toast.makeText(StartActivity.this, "上传失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                            });
                            return null;
                        }
                );
            } else {
                // 无法提取视频帧
                Toast.makeText(StartActivity.this, "无法提取视频帧", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 将 Bitmap 转换为字节数组
    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        return stream.toByteArray();
    }
}

