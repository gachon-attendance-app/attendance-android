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
import com.example.myapplication.model.CourseLookupResponse
import com.example.myapplication.model.SaveScheduleRequest
import com.example.myapplication.model.SaveScheduleResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterScheduleActivity : Activity() {

    private lateinit var etCourseCode: EditText
    private lateinit var btnAddClass: Button
    private lateinit var btnConfirmSchedule: Button
    private lateinit var classBlockLayer: FrameLayout

    private val selectedCourses = mutableListOf<Course>()

    private var userId: Int = -1

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

        userId = getSharedPreferences("userPrefs", MODE_PRIVATE)
            .getInt("userId", -1)

        if (userId == -1) {
            val oldUserId = getSharedPreferences("LOGIN_INFO", MODE_PRIVATE)
                .getString("userId", null)

            userId = oldUserId?.toIntOrNull() ?: 1
        }

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

            lookupCourseFromBackend(inputCode)
        }

        btnConfirmSchedule.setOnClickListener {
            if (selectedCourses.isEmpty()) {
                Toast.makeText(this, "최소 1개 이상의 수업을 추가해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveScheduleToBackend(selectedCourses)
        }
    }

    private fun lookupCourseFromBackend(courseCode: String) {
        ApiClient.apiService.lookupCourse(courseCode)
            .enqueue(object : Callback<CourseLookupResponse> {
                override fun onResponse(
                    call: Call<CourseLookupResponse>,
                    response: Response<CourseLookupResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.success == true) {
                        val course = convertResponseToCourse(body, courseCode)
                        addCourseIfPossible(course)
                    } else {
                        /*
                         * 서버 응답은 왔지만 해당 코드가 없으면 기존 mock 데이터도 확인
                         */
                        addMockCourseIfPossible(courseCode)
                    }
                }

                override fun onFailure(call: Call<CourseLookupResponse>, t: Throwable) {
                    /*
                     * 백엔드 서버 없을 때는 기존 mock 데이터 사용
                     */
                    addMockCourseIfPossible(courseCode)
                }
            })
    }

    private fun convertResponseToCourse(
        body: CourseLookupResponse,
        inputCode: String
    ): Course {
        val day = convertDayToKorean(body.dayOfWeek ?: "월")
        val startHour = extractHour(body.startTime ?: "09:00")
        val endHour = extractHour(body.endTime ?: "10:00")

        return Course(
            code = body.courseCode ?: inputCode,
            name = body.courseName ?: "수업명 없음",
            professor = body.professorName ?: "교수명 없음",
            classroom = body.room ?: "강의실 없음",
            schedules = listOf(
                CourseTime(
                    day = day,
                    startHour = startHour,
                    endHour = endHour
                )
            )
        )
    }

    private fun addMockCourseIfPossible(courseCode: String) {
        val course = mockCourseData[courseCode]

        if (course == null) {
            Toast.makeText(
                this,
                "등록되지 않은 과목 코드입니다. 테스트 코드는 MOB001, DATA001, SW001 입니다.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        addCourseIfPossible(course)
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
        val request = SaveScheduleRequest(
            courseCodes = courses.map { it.code }
        )

        ApiClient.apiService.saveStudentSchedule(userId, request)
            .enqueue(object : Callback<SaveScheduleResponse> {
                override fun onResponse(
                    call: Call<SaveScheduleResponse>,
                    response: Response<SaveScheduleResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.success == true) {
                        Toast.makeText(
                            this@RegisterScheduleActivity,
                            body.message ?: "시간표가 저장되었습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@RegisterScheduleActivity,
                            body?.message ?: "시간표 저장 실패, 임시 저장으로 이동합니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    moveToMain()
                }

                override fun onFailure(call: Call<SaveScheduleResponse>, t: Throwable) {
                    /*
                     * 서버 없을 때도 프론트 테스트 가능하게 이동
                     */
                    Toast.makeText(
                        this@RegisterScheduleActivity,
                        "임시 시간표 저장 완료",
                        Toast.LENGTH_SHORT
                    ).show()

                    moveToMain()
                }
            })
    }

    private fun moveToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
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

    private fun extractHour(time: String): Int {
        return time.substringBefore(":").toIntOrNull() ?: 9
    }

    private fun convertDayToKorean(day: String): String {
        return when (day.uppercase()) {
            "MON", "MONDAY", "월" -> "월"
            "TUE", "TUESDAY", "화" -> "화"
            "WED", "WEDNESDAY", "수" -> "수"
            "THU", "THURSDAY", "목" -> "목"
            "FRI", "FRIDAY", "금" -> "금"
            else -> "월"
        }
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