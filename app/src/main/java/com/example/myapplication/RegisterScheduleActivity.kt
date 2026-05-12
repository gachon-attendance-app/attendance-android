package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast

class RegisterScheduleActivity : Activity() {

    private lateinit var etCourseCode: EditText
    private lateinit var btnAddClass: Button
    private lateinit var btnConfirmSchedule: Button
    private lateinit var classBlockLayer: FrameLayout

    private val selectedCourses = mutableListOf<Course>()

    private val mockCourseData = mapOf(
        "MOB001" to Course(
            code = "MOB001",
            name = "모바일프로그래밍 (영어강의)",
            professor = "민홍",
            classroom = "AI관-301",
            schedules = listOf(
                CourseTime(day = "화", startHour = 14, endHour = 15),
                CourseTime(day = "목", startHour = 13, endHour = 15)
            )
        ),
        "DATA001" to Course(
            code = "DATA001",
            name = "자료구조 및 실습 (영어강의)",
            professor = "김교수",
            classroom = "AI관-511",
            schedules = listOf(
                CourseTime(day = "화", startHour = 10, endHour = 11),
                CourseTime(day = "목", startHour = 10, endHour = 12)
            )
        ),
        "SW001" to Course(
            code = "SW001",
            name = "소프트웨어공학 (신기술화상강의)",
            professor = "박교수",
            classroom = "화상강의실",
            schedules = listOf(
                CourseTime(day = "금", startHour = 10, endHour = 12)
            )
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_schedule)

        etCourseCode = findViewById(R.id.etCourseCode)
        btnAddClass = findViewById(R.id.btnAddClass)
        btnConfirmSchedule = findViewById(R.id.btnConfirmSchedule)
        classBlockLayer = findViewById(R.id.classBlockLayer)

        btnAddClass.setOnClickListener {
            val inputCode = etCourseCode.text.toString().trim()

            if (inputCode.isEmpty()) {
                Toast.makeText(this, "과목 코드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val course = mockCourseData[inputCode]

            if (course == null) {
                Toast.makeText(this, "등록되지 않은 과목 코드입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedCourses.any { it.code == course.code }) {
                Toast.makeText(this, "이미 추가된 과목입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (hasTimeConflict(course)) {
                Toast.makeText(this, "이미 등록된 수업과 시간이 겹칩니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            selectedCourses.add(course)
            etCourseCode.text.clear()

            addCourseToTimeTable(course)

            Toast.makeText(this, course.name + " 수업이 추가되었습니다.", Toast.LENGTH_SHORT).show()
        }

        btnConfirmSchedule.setOnClickListener {
            if (selectedCourses.isEmpty()) {
                Toast.makeText(this, "최소 1개 이상의 수업을 추가해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveScheduleToBackend(selectedCourses)

            Toast.makeText(this, "시간표가 저장되었습니다.", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun addCourseToTimeTable(course: Course) {
        val colors = listOf(
            "#8FA2C7",
            "#B9AAA5",
            "#79B2B8",
            "#A7B58D",
            "#C39DA4"
        )

        val color = colors[(selectedCourses.size - 1) % colors.size]

        for (time in course.schedules) {
            val block = TextView(this).apply {
                text = course.name + "\n" + course.classroom
                setTextColor(Color.WHITE)
                textSize = 10f
                gravity = Gravity.CENTER
                setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                setBackgroundColor(Color.parseColor(color))
            }

            val params = FrameLayout.LayoutParams(
                getColumnWidth(),
                getBlockHeight(time.startHour, time.endHour)
            )

            params.leftMargin = getLeftMarginByDay(time.day)
            params.topMargin = getTopMarginByHour(time.startHour)

            classBlockLayer.addView(block, params)
        }
    }

    private fun hasTimeConflict(newCourse: Course): Boolean {
        for (selectedCourse in selectedCourses) {
            for (selectedTime in selectedCourse.schedules) {
                for (newTime in newCourse.schedules) {
                    val sameDay = selectedTime.day == newTime.day
                    val overlap = selectedTime.startHour < newTime.endHour &&
                            newTime.startHour < selectedTime.endHour

                    if (sameDay && overlap) {
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun saveScheduleToBackend(courses: List<Course>) {
        // 지금은 프론트 테스트용이라 실제 백엔드 저장은 하지 않음.
        // 백엔드 연결 후 여기에서 시간표 저장 API를 호출하면 됨.
    }

    private fun getColumnWidth(): Int {
        val width = classBlockLayer.width

        return if (width > 0) {
            width / 5
        } else {
            val screenWidth = resources.displayMetrics.widthPixels
            val horizontalPadding = dpToPx(24 + 24 + 18 + 8 + 28)
            (screenWidth - horizontalPadding) / 5
        }
    }

    private fun getLeftMarginByDay(day: String): Int {
        val columnWidth = getColumnWidth()

        return when (day) {
            "월" -> columnWidth * 0
            "화" -> columnWidth * 1
            "수" -> columnWidth * 2
            "목" -> columnWidth * 3
            "금" -> columnWidth * 4
            else -> 0
        }
    }

    private fun getTopMarginByHour(hour: Int): Int {
        val oneHourHeight = dpToPx(52)

        return when (hour) {
            9 -> oneHourHeight * 0
            10 -> oneHourHeight * 1
            11 -> oneHourHeight * 2
            12 -> oneHourHeight * 3
            13 -> oneHourHeight * 4
            14 -> oneHourHeight * 5
            else -> 0
        }
    }

    private fun getBlockHeight(startHour: Int, endHour: Int): Int {
        val oneHourHeight = dpToPx(52)
        return (endHour - startHour) * oneHourHeight
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}

data class Course(
    val code: String,
    val name: String,
    val professor: String,
    val classroom: String,
    val schedules: List<CourseTime>
)

data class CourseTime(
    val day: String,
    val startHour: Int,
    val endHour: Int
)