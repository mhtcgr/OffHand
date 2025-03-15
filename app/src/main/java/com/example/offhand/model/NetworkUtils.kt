package com.example.offhand.model

// NetworkUtils.kt
import ApiResponse
import com.example.offhand.GlobalVariables
import com.example.offhand.GlobalVariables.getBaseUrl
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

object NetworkUtils {
    private val client = OkHttpClient()


    fun sendGetRequest(
        userId: String,
        recordId: String,
        onSuccess: (ApiResponse, String) -> Unit, // 增加成功时的响应内容参数
        onFailure: (Int, String) -> Unit // 增加失败时的错误码和错误信息参数
    ) {
        val baseUrl = getBaseUrl()
        val url = "$baseUrl/detailedAnalysis/getByRecordId?userId=$userId&recordId=$recordId"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader(
                "Authorization",
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6IjEiLCJqdGkiOiI4ZDliMTcxOC1lMDA0LTQ3OWItYWIwYy02YjZkN2NlYTBkOWYiLCJleHAiOjE3NDUyNTkzNTIsImlhdCI6MTc0MTY1OTM1Miwic3ViIjoiUGVyaXBoZXJhbHMiLCJpc3MiOiJUaWFtIn0.1SBagf2D_HQ2k3J63VAsrYinRMp7yzukMsg4xXjk13I"
            )
            .build()

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


    fun uploadImages(
        imageDataList: List<ByteArray>,
        onSuccess: (responseBody: String) -> Unit,
        onFailure: (errorMessage: String) -> Unit
    ) {
        val baseUrl = GlobalVariables.getBaseUrl()
        val url = "$baseUrl/detection/uploadImages"

        // 构建多部分请求体
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("userId", "1")

        // 添加多个图片文件
        imageDataList.forEachIndexed { index, byteArray ->
            requestBody.addFormDataPart(
                "images",  // 根据后端要求调整参数名（如images[]）
                "frame_${System.currentTimeMillis()}_$index.jpg",
                byteArray.toRequestBody("image/jpeg".toMediaType())
            )
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBody.build())
            .addHeader("Authorization", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6IjEiLCJqdGkiOiI4ZDliMTcxOC1lMDA0LTQ3OWItYWIwYy02YjZkN2NlYTBkOWYiLCJleHAiOjE3NDUyNTkzNTIsImlhdCI6MTc0MTY1OTM1Miwic3ViIjoiUGVyaXBoZXJhbHMiLCJpc3MiOiJUaWFtIn0.1SBagf2D_HQ2k3J63VAsrYinRMp7yzukMsg4xXjk13I")
            .addHeader("Cache-Control", "no-cache")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "未知错误")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: "无返回内容"
                if (response.isSuccessful) {
                    onSuccess(responseBody) // 调用页面解析 message
                } else {
                    onFailure("HTTP ${response.code}, 错误信息: $responseBody")
                }
            }
        })
    }


}