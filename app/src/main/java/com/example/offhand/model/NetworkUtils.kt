package com.example.offhand.model

// NetworkUtils.kt
import ApiResponse
import ProbeRequest
import TrainSettingRequest
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
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

object NetworkUtils {
    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS) // 整个请求的超时时间
        .connectTimeout(30, TimeUnit.SECONDS) // 连接超时时间
        .readTimeout(30, TimeUnit.SECONDS) // 读取超时时间
        .writeTimeout(10, TimeUnit.SECONDS) // 写入超时时间
        .build()
    private val baseUrl = GlobalVariables.getBaseUrl()
    private val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6IjEiLCJqdGkiOiI4ZDliMTcxOC1lMDA0LTQ3OWItYWIwYy02YjZkN2NlYTBkOWYiLCJleHAiOjE3NDUyNTkzNTIsImlhdCI6MTc0MTY1OTM1Miwic3ViIjoiUGVyaXBoZXJhbHMiLCJpc3MiOiJUaWFtIn0.1SBagf2D_HQ2k3J63VAsrYinRMp7yzukMsg4xXjk13I"

    fun sendGetRequest(
        userId: String,
        recordId: String,
        onSuccess: (ApiResponse, String) -> Unit, // 增加成功时的响应内容参数
        onFailure: (Int, String) -> Unit // 增加失败时的错误码和错误信息参数
    ) {
        val url = "$baseUrl/detailedAnalysis/getByRecordId?userId=$userId&recordId=$recordId"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", token)
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

    fun sendGetGIFRequest(
        userId: String,
        onSuccess: (InputStream) -> Unit, // 成功时返回 GIF 的 InputStream
        onFailure: (Int, String) -> Unit // 失败时返回错误码和错误信息
    ) {
        val url = "$baseUrl/detailedAnalysis/getGIFByUserId?userId=$userId"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "your_token_here") // 替换为你的授权 Token
            .build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(-1, e.localizedMessage ?: "网络请求失败") // 传递错误码和错误信息
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onFailure(response.code, response.body?.string() ?: "无返回内容") // 传递 HTTP 状态码和错误信息
                    } else {
                        val inputStream = response.body?.byteStream() // 获取 GIF 图片的 InputStream
                        if (inputStream != null) {
                            val bufferedInputStream = BufferedInputStream(inputStream)
                            onSuccess(bufferedInputStream) // 传递支持标记的 InputStream
                        } else {
                            onFailure(-1, "无法获取 GIF 图片")
                        }
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
            .addHeader("Authorization", token)
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

    fun probeLLMRequest(
        userId: String,
        recordId: String,
        question: String,
        onSuccess: (responseBody: String) -> Unit,
        onFailure: (errorMessage: String) -> Unit
    ) {
        val url = "$baseUrl/llm/consult"
        val jsonString = Gson().toJson(ProbeRequest(
            userId = userId,
            recordId = recordId,
            inquiry = question,
            context = true
        ))

        // 构建 RequestBody
        val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", token)
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

    fun postTrainSettingRequest(
        userId: String,
        theme: String,
        trainingMethod: String,
        onSuccess: (responseBody: String) -> Unit,
        onFailure: (errorMessage: String) -> Unit
    ) {
        val url = "$baseUrl/detection/setting"
        val trainSettingRequest = TrainSettingRequest(userId, theme, trainingMethod)

        val json = Gson().toJson(trainSettingRequest)

        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", token)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "未知错误")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onFailure("HTTP ${response.code}, 错误信息: ${response.body?.string() ?: "无返回内容"}")
                    } else {
                        val responseBody = response.body?.string() ?: "无返回内容"
                        onSuccess(responseBody)
                    }
                }
            }
        })
    }
}