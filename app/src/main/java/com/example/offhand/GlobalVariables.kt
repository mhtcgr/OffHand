package com.example.offhand

object GlobalVariables {
    var ipAddress: String = "10.52.34.249"

    // 返回完整的 URL
    fun getBaseUrl(): String {
        return "http://$ipAddress:8080"
    }
}