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
import org.json.JSONObject

class RegisterScheduleActivity : Activity() {

    private lateinit var etCourseCode: EditText
    private lateinit var btnAddClass: Button
    private lateinit var btnConfirmSchedule: Button
    private lateinit var classBlockLayer: FrameLayout

    private val selectedCourses = mutableListOf<Course>()
    private var userId: Int = 1

    private val localCourseData = mapOf(
        "MOB001" to Course(
            classId = 10,
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
            classId = 11,
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
            classId = 12,
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

        userId = getSharedPreferences("LOGIN_INFO", MODE_PRIVATE).getInt("userId", 1)

        etCourseCode = findViewById(R.id.etCourseCode)
        btnAddClass = findViewById(R.id.btnAddClass)
        btnConfirmSchedule = findViewById(R.id.btnConfirmSchedule)
        classBlockLayer = findViewById(R.id.classBlockLayer)

        btnAddClass.setOnClickListener {
            val inputCode = etCourseCode.text.toString().trim().uppercase()

            if (inputCode.isEmpty()) {
                Toast.makeText(this, "과목 코드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lookupCourse(inputCode)
        }

        btnConfirmSchedule.setOnClickListener {
            if (selectedCourses.isEmpty()) {
                Toast.makeText(this, "최소 1개 이상의 수업을 추가해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveScheduleToFirebase()
        }
    }

    private fun lookupCourse(courseCode: String) {
        FirebaseClient.get("courses/$courseCode") { json ->
            val firebaseCourse = FirebaseParsers.course(json, courseCode)
            val course = firebaseCourse ?: localCourseData[courseCode]

            if (course == null) {
                Toast.makeText(
                    this,
                    "등록되지 않은 과목 코드입니다. 테스트 코드: MOB001, DATA001, SW001",
                    Toast.LENGTH_LONG
                ).show()
                return@get
            }

            addCourseIfPossible(course)
        }
    }

    private fun addCourseIfPossible(course: Course) {
        if (selectedCourses.any { it.code == course.code }) {
            Toast.makeText(this, "이미 추가된 과목입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (hasTimeConflict(course)) {
            Toast.makeText(this, "이미 등록된 수업과 시간이 겹칩니다.", Toast.LENGTH_SHORT).show()
            return
        }

        selectedCourses.add(course)
        etCourseCode.text.clear()
        addCourseToTimeTable(course)
        Toast.makeText(this, course.name + " 수업이 추가되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun addCourseToTimeTable(course: Course) {
        val colors = listOf("#8FA2C7", "#B9AAA5", "#79B2B8", "#A7B58D", "#C39DA4")
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

                    if (sameDay && overlap) return true
                }
            }
        }
        return false
    }

    private fun saveScheduleToFirebase() {
        val body = JSONObject()
            .put("success", true)
            .put("studentId", userId)
            .put("classes", FirebaseParsers.courseListToJson(selectedCourses))
            .put("message", "시간표 저장 성공")

        FirebaseClient.put("studentSchedules/$userId", body) {
            Toast.makeText(this, "시간표가 저장되었습니다.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun getColumnWidth(): Int {
        val width = classBlockLayer.width
        return if (width > 0) width / 5 else (resources.displayMetrics.widthPixels - dpToPx(120)) / 5
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
        return (endHour - startHour) * dpToPx(52)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}