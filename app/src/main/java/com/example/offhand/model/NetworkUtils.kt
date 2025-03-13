package com.example.offhand.model

// NetworkUtils.kt
import ApiResponse
import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

fun sendGetRequest(
    userId: String,
    recordId: String,
    onSuccess: (ApiResponse, String) -> Unit, // 增加成功时的响应内容参数
    onFailure: (Int, String) -> Unit // 增加失败时的错误码和错误信息参数
) {
    val url = "http://10.52.34.249:8080/detailedAnalysis/getByRecordId?userId=$userId&recordId=$recordId"

    val request = Request.Builder()
        .url(url)
        .get()
        .addHeader(
            "Authorization",
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6IjEiLCJqdGkiOiI4ZDliMTcxOC1lMDA0LTQ3OWItYWIwYy02YjZkN2NlYTBkOWYiLCJleHAiOjE3NDUyNTkzNTIsImlhdCI6MTc0MTY1OTM1Miwic3ViIjoiUGVyaXBoZXJhbHMiLCJpc3MiOiJUaWFtIn0.1SBagf2D_HQ2k3J63VAsrYinRMp7yzukMsg4xXjk13I"
        )
        .build()

    val client = OkHttpClient()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onFailure(-1, e.localizedMessage ?: "网络请求失败") // 传递错误码和错误信息
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string() ?: "无返回内容"
                if (!response.isSuccessful) {
                    onFailure(response.code, responseBody) // 传递 HTTP 状态码和错误信息
                } else {
                    val gson = Gson()
                    val apiResponse = gson.fromJson(responseBody, ApiResponse::class.java)
                    onSuccess(apiResponse, responseBody) // 传递解析后的数据和响应内容
                }
            }
        }
    })
}