package com.example.offhand.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.max

object ImageProcess {
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        // 计算裁剪区域
//        val scale = max(512.toFloat() / originalWidth, 288.toFloat() / originalHeight)
//        val scaledWidth = (originalWidth * scale).toInt()
//        val scaledHeight = (originalHeight * scale).toInt()
//        Bitmap = Bitmap.createScaledBitmap(bitmap, 512, 288, true)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val width = image.width
        val height = image.height
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // 创建 ARGB_8888 格式的 Bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val byteBuffer = ByteBuffer.wrap(bytes)
        bitmap.copyPixelsFromBuffer(byteBuffer)

        return bitmap
    }

    fun resizeImage(imageData: ByteArray, width: Int, height: Int): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        return outputStream.toByteArray()
    }
}

