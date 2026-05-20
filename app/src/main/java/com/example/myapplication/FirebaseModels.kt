package com.example.myapplication

import org.json.JSONArray
import org.json.JSONObject

/**
 * users/{loginId}
 * - 로그인/마이페이지 사용자 정보
 */
data class AppUser(
    val userId: Int,
    val loginId: String,
    val password: String,
    val name: String,
    val role: String,
    val department: String,
    val studentNumber: String?,
    val professorNumber: String?
)

/**
 * courses/{courseCode}
 * - Firebase 강의 데이터
 */
data class FirebaseCourse(
    val classId: Int,
    val courseCode: String,
    val courseName: String,
    val professorName: String,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val room: String,
    val semester: String?
)

/**
 * register_schedule.xml
 * - 시간표 블록 요일/시간
 */
data class CourseTime(
    val day: String,
    val startHour: Int,
    val endHour: Int
)

/**
 * register_schedule.xml, schedule_1.xml
 * - 앱 내부 시간표 수업 데이터
 */
data class Course(
    val classId: Int,
    val code: String,
    val name: String,
    val professor: String,
    val classroom: String,
    val schedules: List<CourseTime>
)

/**
 * currentClasses/{userId}
 * - main1.xml 현재 수업 정보
 */
data class CurrentClassData(
    val success: Boolean,
    val hasClass: Boolean,
    val classId: Int,
    val courseCode: String,
    val courseName: String,
    val professorName: String,
    val room: String,
    val startTime: String,
    val endTime: String,
    val attendanceStatus: String,
    val attendanceMessage: String,
    val sessionId: Int
)

/**
 * professorAttendanceStatus/{classId}
 * - main_p_1.xml 학생별 출석 상태
 */
data class StudentAttendance(
    val studentId: String,
    val name: String,
    val status: String
)

/**
 * Firebase JSON 변환 함수 모음
 */
object FirebaseParsers {

    fun user(json: JSONObject?, fallbackLoginId: String = "test"): AppUser? {
        if (json == null) return null

        return AppUser(
            userId = json.optInt("userId", if (fallbackLoginId == "professor") 2 else 1),
            loginId = json.optString("loginId", fallbackLoginId),
            password = json.optString("password", "1234"),
            name = json.optString(
                "name",
                if (fallbackLoginId == "professor") "테스트교수" else "테스트학생"
            ),
            role = json.optString(
                "role",
                if (fallbackLoginId == "professor") "professor" else "student"
            ),
            department = json.optString("department", "소프트웨어학과"),
            studentNumber = json.optStringOrNull("studentNumber"),
            professorNumber = json.optStringOrNull("professorNumber")
        )
    }

    fun course(json: JSONObject?, inputCode: String): Course? {
        if (json == null) return null

        val courseCode = json.optString("courseCode", inputCode)
        val times = json.optJSONArray("courseTimes")

        val schedules = if (times != null && times.length() > 0) {
            val list = mutableListOf<CourseTime>()

            for (i in 0 until times.length()) {
                val item = times.optJSONObject(i) ?: continue

                list.add(
                    CourseTime(
                        day = item.optString(
                            "day",
                            convertDayToKorean(json.optString("dayOfWeek", "월"))
                        ),
                        startHour = item.optInt(
                            "startHour",
                            extractHour(json.optString("startTime", "09:00"))
                        ),
                        endHour = item.optInt(
                            "endHour",
                            extractHour(json.optString("endTime", "10:00"))
                        )
                    )
                )
            }

            list
        } else {
            listOf(
                CourseTime(
                    day = convertDayToKorean(json.optString("dayOfWeek", "월")),
                    startHour = extractHour(json.optString("startTime", "09:00")),
                    endHour = extractHour(json.optString("endTime", "10:00"))
                )
            )
        }

        return Course(
            classId = json.optInt("classId", 0),
            code = courseCode,
            name = json.optString("courseName", "수업명 없음"),
            professor = json.optString("professorName", "교수명 없음"),
            classroom = json.optString("room", "강의실 없음"),
            schedules = schedules
        )
    }

    fun courseList(json: JSONObject?): List<Course> {
        if (json == null) return emptyList()

        val array = json.optJSONArray("classes") ?: return emptyList()
        return jsonArrayToCourseList(array)
    }

    fun jsonArrayToCourseList(array: JSONArray): List<Course> {
        val result = mutableListOf<Course>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val code = item.optString("courseCode", "")

            course(item, code)?.let { result.add(it) }
        }

        return result
    }

    fun currentClass(json: JSONObject?): CurrentClassData? {
        if (json == null) {
            return CurrentClassData(
                success = true,
                hasClass = true,
                classId = 10,
                courseCode = "MOB001",
                courseName = "모바일프로그래밍 (영어강의)",
                professorName = "민홍",
                room = "AI관-301",
                startTime = "14:00",
                endTime = "15:00",
                attendanceStatus = "NOT_STARTED",
                attendanceMessage = "출석 전",
                sessionId = 100
            )
        }

        return CurrentClassData(
            success = json.optBoolean("success", true),
            hasClass = json.optBoolean("hasClass", true),
            classId = json.optInt("classId", 10),
            courseCode = json.optString("courseCode", "MOB001"),
            courseName = json.optString("courseName", "모바일프로그래밍 (영어강의)"),
            professorName = json.optString("professorName", "민홍"),
            room = json.optString("room", "AI관-301"),
            startTime = json.optString("startTime", "14:00"),
            endTime = json.optString("endTime", "15:00"),
            attendanceStatus = json.optString("attendanceStatus", "NOT_STARTED"),
            attendanceMessage = json.optString("attendanceMessage", "출석 전"),
            sessionId = json.optInt("sessionId", 100)
        )
    }

    fun studentAttendanceList(json: JSONObject?): List<StudentAttendance> {
        val array = json?.optJSONArray("students") ?: return emptyList()
        val result = mutableListOf<StudentAttendance>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue

            result.add(
                StudentAttendance(
                    studentId = item.optString("studentId", ""),
                    name = item.optString("name", ""),
                    status = item.optString("status", "NOT_STARTED")
                )
            )
        }

        return result
    }

    fun courseToJson(course: Course): JSONObject {
        val times = JSONArray()

        course.schedules.forEach { time ->
            times.put(
                JSONObject()
                    .put("day", time.day)
                    .put("startHour", time.startHour)
                    .put("endHour", time.endHour)
            )
        }

        return JSONObject()
            .put("classId", course.classId)
            .put("courseCode", course.code)
            .put("courseName", course.name)
            .put("professorName", course.professor)
            .put("room", course.classroom)
            .put("courseTimes", times)
            .put("semester", "2026-1")
    }

    fun courseListToJson(courses: List<Course>): JSONArray {
        val array = JSONArray()

        courses.forEach { course ->
            array.put(courseToJson(course))
        }

        return array
    }

    fun JSONObject.optStringOrNull(name: String): String? {
        return if (has(name) && !isNull(name)) {
            optString(name)
        } else {
            null
        }
    }

    fun convertDayToKorean(day: String): String {
        return when (day.uppercase()) {
            "MON", "MONDAY", "월" -> "월"
            "TUE", "TUESDAY", "화" -> "화"
            "WED", "WEDNESDAY", "수" -> "수"
            "THU", "THURSDAY", "목" -> "목"
            "FRI", "FRIDAY", "금" -> "금"
            else -> "월"
        }
    }

    fun extractHour(time: String): Int {
        return time.substringBefore(":").toIntOrNull() ?: 9
    }
}