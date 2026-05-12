package com.example.myapplication

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class WeekActivity : ComponentActivity() {

    private var isExpanded = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.week_2)

        findViewById<LinearLayout>(R.id.itemAttendance2).setOnClickListener {
            toggleAttendanceItem()
        }

        findViewById<TextView>(R.id.btnCollapse2).setOnClickListener {
            toggleAttendanceItem()
        }
    }

    private fun toggleAttendanceItem() {

        val item = findViewById<LinearLayout>(R.id.itemAttendance2)
        val detailArea = findViewById<LinearLayout>(R.id.detailArea2)
        val divider = findViewById<View>(R.id.detailDivider2)

        val startHeight = item.height

        val endHeight = if (isExpanded) {
            dpToPx(48)
        } else {
            dpToPx(138)
        }

        val animator = ValueAnimator.ofInt(startHeight, endHeight)

        animator.duration = 250

        animator.addUpdateListener {

            val value = it.animatedValue as Int

            item.layoutParams.height = value
            item.requestLayout()
        }

        animator.start()

        if (isExpanded) {
            detailArea.visibility = View.GONE
            divider.visibility = View.GONE
        } else {
            detailArea.visibility = View.VISIBLE
            divider.visibility = View.VISIBLE
        }

        isExpanded = !isExpanded
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}