package com.example.myapplication

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 24f
        strokeCap = Paint.Cap.BUTT
    }

    private val rect = RectF()

    private var attendanceRate = 100f
    private var lateRate = 0f
    private var absentRate = 0f
    private var animatedProgress = 0f

    private val attendanceColor = ContextCompat.getColor(context, R.color.primarybase)
    private val lateColor = android.graphics.Color.parseColor("#9C27B0")
    private val absentColor = android.graphics.Color.parseColor("#D00000")
    private val emptyColor = android.graphics.Color.parseColor("#E3E3E5")

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = paint.strokeWidth / 2 + 4
        rect.set(padding, padding, width - padding, height - padding)

        paint.color = emptyColor
        canvas.drawArc(rect, 0f, 360f, false, paint)

        var startAngle = -90f

        val attendanceSweep = -360f * (attendanceRate / 100f) * animatedProgress
        val lateSweep = -360f * (lateRate / 100f) * animatedProgress
        val absentSweep = -360f * (absentRate / 100f) * animatedProgress

        paint.color = attendanceColor
        canvas.drawArc(rect, startAngle, attendanceSweep, false, paint)
        startAngle += attendanceSweep

        paint.color = lateColor
        canvas.drawArc(rect, startAngle, lateSweep, false, paint)
        startAngle += lateSweep

        paint.color = absentColor
        canvas.drawArc(rect, startAngle, absentSweep, false, paint)
    }

    fun setData(
        attendance: Float,
        late: Float,
        absent: Float
    ) {
        attendanceRate = attendance
        lateRate = late
        absentRate = absent
        startAnimation()
    }

    private fun startAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900
            addUpdateListener {
                animatedProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
}