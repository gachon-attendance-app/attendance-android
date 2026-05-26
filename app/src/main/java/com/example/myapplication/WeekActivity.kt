package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class WeekActivity : ComponentActivity() {

    private val expandedMap = mutableMapOf<Int, Boolean>()

    private val defaultTimes = listOf(
        "10:00 - 10:15",
        "10:20 - 10:25"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.week_1)

        initDetailRowsWithEmptyStatus()
        initClickEvents()
        loadUwbCheckDataFromFirebase()
    }

    private fun initClickEvents() {
        setExpandableClick(1)
        setExpandableClick(2)
        setExpandableClick(3)
        setExpandableClick(4)
        setExpandableClick(5)
    }

    private fun setExpandableClick(index: Int) {
        expandedMap[index] = false

        val item = findViewById<LinearLayout>(getItemId(index))
        val collapseButton = findViewById<TextView>(getCollapseButtonId(index))

        item.setOnClickListener {
            toggleDetail(index)
        }

        collapseButton.setOnClickListener {
            toggleDetail(index)
        }
    }

    private fun toggleDetail(index: Int) {
        val listContainer = findViewById<LinearLayout>(R.id.listContainer)
        val detailArea = findViewById<LinearLayout>(getDetailAreaId(index))

        val isExpanded = expandedMap[index] ?: false

        val transition = AutoTransition()
        transition.duration = 180
        TransitionManager.beginDelayedTransition(listContainer, transition)

        if (isExpanded) {
            detailArea.visibility = View.GONE
            expandedMap[index] = false
        } else {
            detailArea.visibility = View.VISIBLE
            expandedMap[index] = true
        }
    }

    private fun initDetailRowsWithEmptyStatus() {
        for (index in 1..5) {
            val emptyRows = defaultTimes.map { time ->
                UwbCheckRow(
                    time = time,
                    status = ""
                )
            }

            renderDetailRows(index, emptyRows)
        }
    }

    private fun renderDetailRows(index: Int, rows: List<UwbCheckRow>) {
        val container = findViewById<LinearLayout>(getDetailRowsContainerId(index))
        container.removeAllViews()

        val finalRows = if (rows.isEmpty()) {
            defaultTimes.map { time ->
                UwbCheckRow(
                    time = time,
                    status = ""
                )
            }
        } else {
            rows
        }

        for (row in finalRows) {
            val rowLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(17)
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val timeText = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = row.time
                textSize = 12f
                setTextColor(Color.parseColor("#555555"))
            }

            val statusText = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = row.status
                textSize = 12f
                setTextColor(Color.parseColor("#555555"))
                gravity = Gravity.END
            }

            rowLayout.addView(timeText)
            rowLayout.addView(statusText)
            container.addView(rowLayout)
        }
    }

    private fun loadUwbCheckDataFromFirebase() {
        val prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)

        val studentId = prefs.getString("userId", null)
            ?: prefs.getString("studentId", null)
            ?: prefs.getString("id", null)
            ?: ""

        val selectedDate = "2026-04-02"

        /*
            백엔드 데이터 경로 예시:

            weeklyAttendance
              └─ 학생ID
                  └─ 2026-04-02
                      └─ items
                          └─ 1
                              └─ uwbChecks
                                  └─ 0
                                      ├─ time: "10:00 - 10:15"
                                      └─ status: "미출석"
                                  └─ 1
                                      ├─ time: "10:20 - 10:25"
                                      └─ status: "미출석"

            여기서 실제 백엔드 경로가 다르면 아래 reference 경로만 맞춰주면 됨.
        */

        if (studentId.isBlank()) {
            return
        }

        val reference = FirebaseDatabase.getInstance()
            .getReference("weeklyAttendance")
            .child(studentId)
            .child(selectedDate)
            .child("items")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (index in 1..5) {
                    val itemSnapshot = snapshot.child(index.toString()).child("uwbChecks")

                    val rows = mutableListOf<UwbCheckRow>()

                    for (checkSnapshot in itemSnapshot.children) {
                        val timeFromBackend = checkSnapshot.child("time").getValue(String::class.java)
                        val statusFromBackend = checkSnapshot.child("status").getValue(String::class.java)

                        val fallbackIndex = rows.size
                        val fixedTime = if (fallbackIndex < defaultTimes.size) {
                            defaultTimes[fallbackIndex]
                        } else {
                            timeFromBackend ?: ""
                        }

                        rows.add(
                            UwbCheckRow(
                                time = timeFromBackend ?: fixedTime,
                                status = statusFromBackend ?: ""
                            )
                        )
                    }

                    if (rows.isEmpty()) {
                        renderDetailRows(
                            index,
                            defaultTimes.map { time ->
                                UwbCheckRow(
                                    time = time,
                                    status = ""
                                )
                            }
                        )
                    } else {
                        renderDetailRows(index, rows)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                initDetailRowsWithEmptyStatus()
            }
        })
    }

    private fun getItemId(index: Int): Int {
        return when (index) {
            1 -> R.id.itemAttendance1
            2 -> R.id.itemAttendance2
            3 -> R.id.itemAttendance3
            4 -> R.id.itemAttendance4
            else -> R.id.itemAttendance5
        }
    }

    private fun getDetailAreaId(index: Int): Int {
        return when (index) {
            1 -> R.id.detailArea1
            2 -> R.id.detailArea2
            3 -> R.id.detailArea3
            4 -> R.id.detailArea4
            else -> R.id.detailArea5
        }
    }

    private fun getDetailRowsContainerId(index: Int): Int {
        return when (index) {
            1 -> R.id.detailRowsContainer1
            2 -> R.id.detailRowsContainer2
            3 -> R.id.detailRowsContainer3
            4 -> R.id.detailRowsContainer4
            else -> R.id.detailRowsContainer5
        }
    }

    private fun getCollapseButtonId(index: Int): Int {
        return when (index) {
            1 -> R.id.btnCollapse1
            2 -> R.id.btnCollapse2
            3 -> R.id.btnCollapse3
            4 -> R.id.btnCollapse4
            else -> R.id.btnCollapse5
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    data class UwbCheckRow(
        val time: String = "",
        val status: String = ""
    )
}