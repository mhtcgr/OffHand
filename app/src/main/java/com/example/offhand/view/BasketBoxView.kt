package com.example.offhand.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class BasketBoxView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.YELLOW // 设置画笔颜色为黄色
        style = Paint.Style.STROKE // 设置画笔样式为描边
        strokeWidth = 5f // 设置描边宽度
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 计算方框的位置和大小
        val boxWidth = width / 4f // 方框宽度为 View 宽度的 1/4
        val boxHeight = height / 4f // 方框高度为 View 高度的 1/4
        val boxX = width * 5 / 8f // 方框左上角 x 坐标
        val boxY = height * 1 / 8f // 方框左上角 y 坐标

        // 绘制黄色边缘方框
        canvas.drawRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, paint)
    }
}