package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var contentFrame: FrameLayout

    private var currentPageResId: Int = R.layout.main1
    private var userId: String = ""
    private var userName: String = ""
    private var userRole: String = "student"
    private var currentSubjectCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        readLoginInfo()

        setContentView(R.layout.activity_drawer_host)

        drawerLayout = findViewById(R.id.drawerLayout)
        contentFrame = findViewById(R.id.contentFrame)

        if (userRole == "professor") {
            loadPage(R.layout.main_p_1)
        } else {
            loadPage(R.layout.main1)
        }

        setupDrawerMenuClick()
    }

    private fun readLoginInfo() {
        val pref = getSharedPreferences("LOGIN_INFO", MODE_PRIVATE)
        userId = pref.getString("userId", "") ?: ""
        userName = pref.getString("userName", "") ?: ""
        userRole = pref.getString("userRole", "student") ?: "student"
    }

    private fun loadPage(layoutResId: Int) {
        currentPageResId = layoutResId
        contentFrame.removeAllViews()

        val pageView = LayoutInflater.from(this).inflate(layoutResId, contentFrame, false)
        contentFrame.addView(pageView)

        connectTopMenuButton(pageView)
        connectBottomMenu(pageView)
        loadJsonDataForPage(layoutResId, pageView)
    }

    private fun loadJsonDataForPage(layoutResId: Int, pageView: View) {
        when (layoutResId) {
            R.layout.main1 -> {
                loadCurrentClass(pageView)
                pageView.findViewById<View?>(R.id.btnAttendance)?.setOnClickListener {
                    saveAttendanceRecord(pageView)
                }
            }

            R.layout.main_p_1 -> {
                loadProfessorPage(pageView)
                pageView.findViewById<View?>(R.id.btnProfessorAttendanceCheck)?.setOnClickListener {
                    startAttendanceSession(pageView)
                }
            }

            R.layout.schedule_1 -> {
                loadSchedule(pageView)
            }

            R.layout.mypage -> {
                loadMyPage(pageView)
                loadSchedule(pageView)
            }

            R.layout.week_1,
            R.layout.week_2 -> {
                loadAttendanceCalendar(pageView)
            }

            R.layout.all_attendance -> {
                loadAttendanceSummary(pageView)
            }
        }
    }

    private fun connectTopMenuButton(pageView: View) {
        pageView.findViewById<View?>(R.id.btnMenu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun connectBottomMenu(pageView: View) {
        val btnHome = pageView.findViewById<View?>(R.id.btnBottomHome)
        val btnRefresh = pageView.findViewById<View?>(R.id.btnBottomRefresh)
        val btnNotice = pageView.findViewById<View?>(R.id.btnBottomNotice)
        val btnSchedule = pageView.findViewById<View?>(R.id.btnBottomSchedule)
        val btnLogout = pageView.findViewById<View?>(R.id.btnBottomLogout)

        btnHome?.setOnClickListener {
            if (userRole == "professor") {
                loadPage(R.layout.main_p_1)
            } else {
                loadPage(R.layout.main1)
            }
        }

        btnRefresh?.setOnClickListener {
            loadPage(currentPageResId)
            Toast.makeText(this, "새로고침되었습니다", Toast.LENGTH_SHORT).show()
        }

        btnNotice?.setOnClickListener {
            if (userRole == "professor") {
                loadPage(R.layout.notice_2)
            } else {
                loadPage(R.layout.notice_1)
            }
        }

        btnSchedule?.setOnClickListener {
            loadPage(R.layout.schedule_1)
        }

        btnLogout?.setOnClickListener {
            logout()
        }
    }

    private fun setupDrawerMenuClick() {
        findViewById<View?>(R.id.menuMyPage)?.setOnClickListener { moveTo(R.layout.mypage) }
        findViewById<View?>(R.id.menuSchedule)?.setOnClickListener { moveTo(R.layout.schedule_1) }
        findViewById<View?>(R.id.menuWeekAttendance)?.setOnClickListener { moveTo(R.layout.week_1) }
        findViewById<View?>(R.id.menuAllAttendance)?.setOnClickListener { moveTo(R.layout.all_attendance) }
        findViewById<View?>(R.id.menuConfirmPeriod)?.setOnClickListener { moveTo(R.layout.confirm_1) }
        findViewById<View?>(R.id.menuConfirmOfficial)?.setOnClickListener { moveTo(R.layout.confirm_2) }

        findViewById<View?>(R.id.menuNotice)?.setOnClickListener {
            if (userRole == "professor") moveTo(R.layout.notice_2) else moveTo(R.layout.notice_1)
        }

        findViewById<View?>(R.id.menuCancel)?.setOnClickListener {
            if (userRole == "professor") moveTo(R.layout.cancel_2) else moveTo(R.layout.cancel_1)
        }
    }

    private fun moveTo(layoutResId: Int) {
        drawerLayout.closeDrawer(GravityCompat.END)
        loadPage(layoutResId)
    }

    private fun loadCurrentClass(pageView: View) {
        FirebaseClient.get("Enrollment/$userId") { enrollmentJson ->
            val subjectCode = enrollmentJson?.keys()?.asSequence()?.firstOrNull()

            if (subjectCode.isNullOrBlank()) {
                setText(pageView, "tvDate", todayText())
                setText(pageView, "tvPeriod", "현재 수업 없음")
                setText(pageView, "tvAttendanceStatus", "출석 전")
                return@get
            }

            currentSubjectCode = subjectCode

            FirebaseClient.get("Subjects/$subjectCode") { subjectJson ->
                val subject = FirebaseParsers.subject(subjectJson, subjectCode)

                if (subject == null) {
                    setText(pageView, "tvDate", todayText())
                    setText(pageView, "tvPeriod", "수업 정보 없음")
                    setText(pageView, "tvAttendanceStatus", "출석 전")
                    return@get
                }

                val firstSchedule = subject.schedules.firstOrNull()
                val firstPeriod = firstSchedule?.periods?.firstOrNull()
                val lastPeriod = firstSchedule?.periods?.lastOrNull()

                setText(pageView, "tvDate", todayText())
                setText(
                    pageView,
                    "tvPeriod",
                    "${firstPeriod?.startTime ?: ""} - ${lastPeriod?.endTime ?: ""}"
                )
                setText(pageView, "tvAttendanceStatus", "출석 전")
                setText(pageView, "tvCurrentClassName", subject.subjectName)
            }
        }
    }

    private fun saveAttendanceRecord(pageView: View) {
        if (currentSubjectCode.isBlank()) {
            Toast.makeText(this, "현재 수업 정보가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        val today = apiDateText()

        val body = JSONObject()
            .put("finalStatus", "출석")
            .put("missedCount", 0)

        FirebaseClient.put("Attendance_Records/$currentSubjectCode/$today/$userId", body) {
            setText(pageView, "tvAttendanceStatus", "출석")
            Toast.makeText(this, "출석 완료", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAttendanceSession(pageView: View) {
        if (currentSubjectCode.isBlank()) {
            currentSubjectCode = "14454001"
        }

        val today = apiDateText()

        val body = JSONObject()
            .put("authMethod", "BLUETOOTH")
            .put("pinCode", 1234)
            .put("status", "READY")

        FirebaseClient.put("Attendance_Session/$currentSubjectCode/$today", body) {
            showPin(pageView, "1234")
            Toast.makeText(this, "출석 세션 시작", Toast.LENGTH_SHORT).show()
            loadProfessorPage(pageView)
        }
    }

    private fun loadProfessorPage(pageView: View) {
        FirebaseClient.get("Subjects") { subjectsJson ->
            val firstSubjectCode = subjectsJson?.keys()?.asSequence()?.firstOrNull() ?: "14454001"
            currentSubjectCode = firstSubjectCode

            FirebaseClient.get("Subjects/$firstSubjectCode") { subjectJson ->
                val subject = FirebaseParsers.subject(subjectJson, firstSubjectCode)
                setText(pageView, "tvClassName", subject?.subjectName ?: "")
                setText(pageView, "tvClassTime", subject?.schedules?.joinToString(" / ") {
                    "${FirebaseParsers.convertDayToKorean(it.dayOfWeek)} ${it.periods.firstOrNull()?.startTime ?: ""}-${it.periods.lastOrNull()?.endTime ?: ""}"
                } ?: "")
            }

            FirebaseClient.get("Attendance_Records/$firstSubjectCode") { recordsJson ->
                loadProfessorRows(pageView, recordsJson)
            }
        }
    }

    private fun loadProfessorRows(pageView: View, recordsJson: JSONObject?) {
        val rows = findChildByIdName<LinearLayout>(pageView, "layoutStudentAttendanceRows")
        rows?.removeAllViews()

        FirebaseClient.get("Users") { usersJson ->
            val keys = usersJson?.keys()
            var total = 0
            var present = 0
            var late = 0
            var absent = 0

            if (keys != null) {
                while (keys.hasNext()) {
                    val key = keys.next()
                    val user = FirebaseParsers.user(usersJson.optJSONObject(key), key) ?: continue
                    if (user.userType != "STUDENT") continue

                    val status = findLatestAttendanceStatus(recordsJson, user.userId)

                    total++

                    when (status) {
                        "출석" -> present++
                        "지각" -> late++
                        "결석" -> absent++
                    }

                    addStudentRow(pageView, user.userId, user.name, status)
                }
            }

            if (total == 0) total = 1

            setText(pageView, "tvAttendanceRate", "${present * 100 / total}%")
            setText(pageView, "tvLateRate", "${late * 100 / total}%")
            setText(pageView, "tvAbsentRate", "${absent * 100 / total}%")
            setText(pageView, "tvUwbCheckCount", "0회")
        }
    }

    private fun findLatestAttendanceStatus(recordsJson: JSONObject?, targetUserId: String): String {
        if (recordsJson == null) return "출석 전"

        val dateKeys = recordsJson.keys()
        var result = "출석 전"

        while (dateKeys.hasNext()) {
            val dateKey = dateKeys.next()
            val dateObject = recordsJson.optJSONObject(dateKey) ?: continue
            val userObject = dateObject.optJSONObject(targetUserId) ?: continue
            result = userObject.optString("finalStatus", "출석 전")
        }

        return result
    }

    private fun loadSchedule(pageView: View) {
        FirebaseClient.get("Enrollment/$userId") { enrollmentJson ->
            val subjectCodes = mutableListOf<String>()
            val keys = enrollmentJson?.keys()

            if (keys != null) {
                while (keys.hasNext()) {
                    subjectCodes.add(keys.next())
                }
            }

            val parent = findChildByIdName<FrameLayout>(pageView, "classBlockLayer")
            parent?.removeAllViews()

            if (subjectCodes.isEmpty()) {
                setText(pageView, "tvCurrentClassName", "등록된 시간표 없음")
                return@get
            }

            subjectCodes.forEachIndexed { index, subjectCode ->
                FirebaseClient.get("Subjects/$subjectCode") { subjectJson ->
                    val subject = FirebaseParsers.subject(subjectJson, subjectCode) ?: return@get
                    val course = FirebaseParsers.subjectToCourse(subject)

                    addCourseBlock(parent, course, index)

                    if (index == 0) {
                        setText(pageView, "tvCurrentClassName", subject.subjectName)
                        setText(pageView, "tvDetailProfessor", subject.professorName)
                        setText(pageView, "tvDetailRoom", course.classroom)
                        setText(pageView, "tvDetailCourseCode", subject.subjectCode)
                        setText(pageView, "tvDetailTime", subject.schedules.joinToString(" / ") {
                            "${FirebaseParsers.convertDayToKorean(it.dayOfWeek)} ${it.periods.firstOrNull()?.startTime ?: ""}-${it.periods.lastOrNull()?.endTime ?: ""}"
                        })
                    }
                }
            }
        }
    }

    private fun loadMyPage(pageView: View) {
        FirebaseClient.get("Users/$userId") { userJson ->
            val user = FirebaseParsers.user(userJson, userId)

            if (userRole == "professor") {
                setText(pageView, "tvProfessorName", user?.name ?: userName)
                setText(pageView, "tvProfessorMajor", "소프트웨어학과")
            } else {
                setText(pageView, "tvStudentName", user?.name ?: userName)
                setText(pageView, "tvStudentMajor", "소프트웨어학과")
                setText(pageView, "tvStudentInfo", user?.userId ?: userId)
            }
        }
    }

    private fun loadAttendanceCalendar(pageView: View) {
        FirebaseClient.get("Attendance_Records") { recordsRoot ->
            val result = StringBuilder()

            val subjectKeys = recordsRoot?.keys()
            if (subjectKeys != null) {
                while (subjectKeys.hasNext()) {
                    val subjectCode = subjectKeys.next()
                    val subjectObject = recordsRoot.optJSONObject(subjectCode) ?: continue
                    val dateKeys = subjectObject.keys()

                    while (dateKeys.hasNext()) {
                        val date = dateKeys.next()
                        val userRecord = subjectObject.optJSONObject(date)?.optJSONObject(userId) ?: continue
                        result.append(date)
                            .append(" / ")
                            .append(subjectCode)
                            .append(" / ")
                            .append(userRecord.optString("finalStatus", ""))
                            .append("\n")
                    }
                }
            }

            setText(pageView, "tvAttendanceCalendar", result.toString())
            addSimpleText(pageView, "layoutAttendanceCalendar", result.toString())
        }
    }

    private fun loadAttendanceSummary(pageView: View) {
        FirebaseClient.get("Attendance_Records") { recordsRoot ->
            var present = 0
            var late = 0
            var absent = 0

            val subjectKeys = recordsRoot?.keys()
            if (subjectKeys != null) {
                while (subjectKeys.hasNext()) {
                    val subjectCode = subjectKeys.next()
                    val subjectObject = recordsRoot.optJSONObject(subjectCode) ?: continue
                    val dateKeys = subjectObject.keys()

                    while (dateKeys.hasNext()) {
                        val date = dateKeys.next()
                        val userRecord = subjectObject.optJSONObject(date)?.optJSONObject(userId) ?: continue
                        when (userRecord.optString("finalStatus", "")) {
                            "출석" -> present++
                            "지각" -> late++
                            "결석" -> absent++
                        }
                    }
                }
            }

            val total = (present + late + absent).coerceAtLeast(1)
            val text = "출석 ${present * 100 / total}% / 지각 ${late * 100 / total}% / 결석 ${absent * 100 / total}%"

            setText(pageView, "tvAttendanceSummary", text)
            addSimpleText(pageView, "layoutAttendanceSummary", text)
        }
    }

    private fun addCourseBlock(parent: FrameLayout?, course: Course, index: Int) {
        if (parent == null) return

        val colors = listOf("#8FA2C7", "#B9AAA5", "#79B2B8", "#A7B58D", "#C39DA4")
        val color = colors[index % colors.size]

        course.schedules.forEach { time ->
            val block = TextView(this).apply {
                text = course.name + "\n" + course.classroom
                setTextColor(Color.WHITE)
                textSize = 10f
                gravity = Gravity.CENTER
                setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                setBackgroundColor(Color.parseColor(color))
            }

            val params = FrameLayout.LayoutParams(
                getColumnWidth(parent),
                getBlockHeight(time.startHour, time.endHour)
            )

            params.leftMargin = getLeftMarginByDay(parent, time.day)
            params.topMargin = getTopMarginByHour(time.startHour)

            parent.addView(block, params)
        }
    }

    private fun showPin(pageView: View, pinCode: String) {
        val pin = pinCode.padEnd(4, '0')
        setText(pageView, "tvPinDigit1", pin[0].toString())
        setText(pageView, "tvPinDigit2", pin[1].toString())
        setText(pageView, "tvPinDigit3", pin[2].toString())
        setText(pageView, "tvPinDigit4", pin[3].toString())
    }

    private fun addStudentRow(pageView: View, studentId: String, name: String, status: String) {
        val parent = findChildByIdName<LinearLayout>(pageView, "layoutStudentAttendanceRows") ?: return

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 10, 12, 10)
        }

        row.addView(makeRowText(studentId, 1f))
        row.addView(makeRowText(name, 1f))
        row.addView(makeRowText(if (status == "출석") "○" else "", 1f))
        row.addView(makeRowText(if (status == "결석") "○" else "", 1f))
        row.addView(makeRowText(if (status == "지각") "○" else "", 1f))

        parent.addView(row)
    }

    private fun makeRowText(value: String, weight: Float): TextView {
        return TextView(this).apply {
            text = value
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#222222"))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                weight
            )
        }
    }

    private fun addSimpleText(pageView: View, parentIdName: String, value: String) {
        val parent = findChildByIdName<LinearLayout>(pageView, parentIdName) ?: return
        parent.removeAllViews()
        parent.addView(
            TextView(this).apply {
                text = value
                textSize = 14f
                setTextColor(Color.parseColor("#222222"))
                setPadding(16, 12, 16, 12)
            }
        )
    }

    private fun setText(pageView: View, idName: String, value: String) {
        findChildByIdName<TextView>(pageView, idName)?.text = value
    }

    private inline fun <reified T> findChildByIdName(pageView: View, idName: String): T? {
        val id = resources.getIdentifier(idName, "id", packageName)
        return if (id != 0) pageView.findViewById(id) else null
    }

    private fun getColumnWidth(parent: FrameLayout): Int {
        val width = parent.width
        return if (width > 0) width / 5 else (resources.displayMetrics.widthPixels - dpToPx(120)) / 5
    }

    private fun getLeftMarginByDay(parent: FrameLayout, day: String): Int {
        val columnWidth = getColumnWidth(parent)
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
            15 -> oneHourHeight * 6
            16 -> oneHourHeight * 7
            else -> 0
        }
    }

    private fun getBlockHeight(startHour: Int, endHour: Int): Int {
        return ((endHour - startHour).coerceAtLeast(1)) * dpToPx(52)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun todayText(): String {
        return SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date())
    }

    private fun apiDateText(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
    }

    private fun logout() {
        getSharedPreferences("LOGIN_INFO", MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("login_pref", MODE_PRIVATE).edit().clear().apply()

        Toast.makeText(this, "로그아웃되었습니다", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}