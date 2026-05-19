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
import com.example.myapplication.model.AttendanceCalendarResponse
import com.example.myapplication.model.AttendanceCheckResponse
import com.example.myapplication.model.AttendanceSummaryResponse
import com.example.myapplication.model.BluetoothCheckRequest
import com.example.myapplication.model.CurrentClassResponse
import com.example.myapplication.model.MyInfoResponse
import com.example.myapplication.model.ProfessorAttendanceStatusResponse
import com.example.myapplication.model.ScheduleResponse
import com.example.myapplication.model.StartAttendanceResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var contentFrame: FrameLayout

    private var currentPageResId: Int = R.layout.main1

    private var userId: Int = -1
    private var role: String = "STUDENT"
    private var currentClassId: Int = 10
    private var currentSessionId: Int = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        readLoginInfo()

        setContentView(R.layout.activity_drawer_host)

        drawerLayout = findViewById(R.id.drawerLayout)
        contentFrame = findViewById(R.id.contentFrame)

        val openPage = intent.getStringExtra("openPage")

        when {
            openPage == "SIGNUP" -> loadPageByName("signup_1")
            role == "PROFESSOR" -> loadPage(R.layout.main_p_1)
            else -> loadPage(R.layout.main1)
        }

        setupDrawerMenuClick()
    }

    private fun readLoginInfo() {
        val userPrefs = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val oldPref = getSharedPreferences("LOGIN_INFO", MODE_PRIVATE)

        userId = userPrefs.getInt("userId", -1)

        val intentRole = intent.getStringExtra("startRole")
        val newRole = userPrefs.getString("role", null)
        val oldRole = oldPref.getString("userRole", null)

        role = when {
            intentRole == "PROFESSOR" -> "PROFESSOR"
            intentRole == "STUDENT" -> "STUDENT"
            newRole == "PROFESSOR" -> "PROFESSOR"
            oldRole == "professor" -> "PROFESSOR"
            else -> "STUDENT"
        }

        if (userId == -1) {
            val oldUserId = oldPref.getString("userId", null)
            userId = oldUserId?.toIntOrNull() ?: if (role == "PROFESSOR") 2 else 1
        }
    }

    private fun loadPage(layoutResId: Int) {
        currentPageResId = layoutResId

        contentFrame.removeAllViews()

        val pageView = LayoutInflater.from(this).inflate(layoutResId, contentFrame, false)
        contentFrame.addView(pageView)

        connectTopMenuButton(pageView)
        connectBottomMenu(pageView)
        runApiForPage(layoutResId, pageView)
    }

    private fun loadPageByName(layoutName: String) {
        val layoutId = resources.getIdentifier(layoutName, "layout", packageName)

        if (layoutId != 0) {
            loadPage(layoutId)
        } else {
            Toast.makeText(this, "$layoutName.xml 파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runApiForPage(layoutResId: Int, pageView: View) {
        when (layoutResId) {
            R.layout.main1 -> {
                requestCurrentClass(pageView)
                pageView.findViewById<View?>(R.id.btnAttendance)?.setOnClickListener {
                    requestBluetoothAttendance(pageView)
                }
            }

            R.layout.main_p_1 -> {
                requestProfessorAttendanceStatus(pageView)
                pageView.findViewById<View?>(R.id.btnProfessorAttendanceCheck)?.setOnClickListener {
                    requestStartAttendance(pageView)
                }
            }

            R.layout.schedule_1 -> {
                requestSchedule(pageView)
            }

            R.layout.week_1,
            R.layout.week_2 -> {
                requestAttendanceCalendar(pageView)
            }

            R.layout.mypage -> {
                requestMyInfo(pageView)
                requestSchedule(pageView)
            }

            R.layout.all_attendance -> {
                requestAttendanceSummary(pageView)
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
            if (role == "PROFESSOR") {
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
            if (role == "PROFESSOR") {
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
        findViewById<View?>(R.id.menuMyPage)?.setOnClickListener {
            moveTo(R.layout.mypage)
        }

        findViewById<View?>(R.id.menuSchedule)?.setOnClickListener {
            moveTo(R.layout.schedule_1)
        }

        findViewById<View?>(R.id.menuWeekAttendance)?.setOnClickListener {
            moveTo(R.layout.week_1)
        }

        findViewById<View?>(R.id.menuAllAttendance)?.setOnClickListener {
            moveTo(R.layout.all_attendance)
        }

        findViewById<View?>(R.id.menuConfirmPeriod)?.setOnClickListener {
            moveTo(R.layout.confirm_1)
        }

        findViewById<View?>(R.id.menuConfirmOfficial)?.setOnClickListener {
            moveTo(R.layout.confirm_2)
        }

        findViewById<View?>(R.id.menuNotice)?.setOnClickListener {
            if (role == "PROFESSOR") {
                moveTo(R.layout.notice_2)
            } else {
                moveTo(R.layout.notice_1)
            }
        }

        findViewById<View?>(R.id.menuCancel)?.setOnClickListener {
            if (role == "PROFESSOR") {
                moveTo(R.layout.cancel_2)
            } else {
                moveTo(R.layout.cancel_1)
            }
        }
    }

    private fun moveTo(layoutResId: Int) {
        drawerLayout.closeDrawer(GravityCompat.END)
        loadPage(layoutResId)
    }

    private fun requestCurrentClass(pageView: View) {
        if (userId == -1) return

        ApiClient.apiService.getCurrentClass(userId)
            .enqueue(object : Callback<CurrentClassResponse> {
                override fun onResponse(
                    call: Call<CurrentClassResponse>,
                    response: Response<CurrentClassResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body != null) {
                        currentClassId = body.classId ?: currentClassId

                        setText(pageView, "tvCurrentClassName", body.courseName ?: "현재 수업 없음")
                        setText(pageView, "tvDate", todayText())
                        setText(pageView, "tvPeriod", "${body.startTime ?: ""} - ${body.endTime ?: ""}")
                        setText(pageView, "tvAttendanceStatus", statusToKorean(body.attendanceStatus ?: "NOT_STARTED"))
                        setText(pageView, "tvDetailRoom", body.room ?: "")
                    } else {
                        showTemporaryCurrentClass(pageView)
                    }
                }

                override fun onFailure(call: Call<CurrentClassResponse>, t: Throwable) {
                    showTemporaryCurrentClass(pageView)
                }
            })
    }

    private fun showTemporaryCurrentClass(pageView: View) {
        currentClassId = 10
        setText(pageView, "tvCurrentClassName", "모바일 프로그래밍")
        setText(pageView, "tvDate", todayText())
        setText(pageView, "tvPeriod", "09:00 - 10:30")
        setText(pageView, "tvAttendanceStatus", "출석 전")
        setText(pageView, "tvDetailRoom", "AI관 301호")
    }

    private fun requestBluetoothAttendance(pageView: View) {
        val request = BluetoothCheckRequest(
            sessionId = currentSessionId,
            studentId = userId,
            classId = currentClassId,
            detectedDeviceId = "TEMP_BLUETOOTH_DEVICE",
            rssi = -55,
            checkedAt = nowText()
        )

        ApiClient.apiService.bluetoothCheck(request)
            .enqueue(object : Callback<AttendanceCheckResponse> {
                override fun onResponse(
                    call: Call<AttendanceCheckResponse>,
                    response: Response<AttendanceCheckResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body != null) {
                        val message = body.message ?: statusToKorean(body.status ?: "PRESENT")
                        setText(pageView, "tvAttendanceStatus", message)
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    } else {
                        showTemporaryAttendanceSuccess(pageView)
                    }
                }

                override fun onFailure(call: Call<AttendanceCheckResponse>, t: Throwable) {
                    showTemporaryAttendanceSuccess(pageView)
                }
            })
    }

    private fun showTemporaryAttendanceSuccess(pageView: View) {
        setText(pageView, "tvAttendanceStatus", "출석 완료")
        Toast.makeText(this, "임시 출석 완료", Toast.LENGTH_SHORT).show()
    }

    private fun requestStartAttendance(pageView: View) {
        ApiClient.apiService.startAttendance(currentClassId)
            .enqueue(object : Callback<StartAttendanceResponse> {
                override fun onResponse(
                    call: Call<StartAttendanceResponse>,
                    response: Response<StartAttendanceResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body != null) {
                        currentSessionId = body.sessionId ?: currentSessionId
                        showPin(pageView, body.pinCode ?: "0000")
                        Toast.makeText(this@MainActivity, "출석 세션이 시작되었습니다.", Toast.LENGTH_SHORT).show()
                        requestProfessorAttendanceStatus(pageView)
                    } else {
                        showTemporaryProfessorStart(pageView)
                    }
                }

                override fun onFailure(call: Call<StartAttendanceResponse>, t: Throwable) {
                    showTemporaryProfessorStart(pageView)
                }
            })
    }

    private fun showTemporaryProfessorStart(pageView: View) {
        currentSessionId = 100
        showPin(pageView, "4821")
        Toast.makeText(this, "임시 출석 세션 시작", Toast.LENGTH_SHORT).show()
        requestProfessorAttendanceStatus(pageView)
    }

    private fun requestProfessorAttendanceStatus(pageView: View) {
        ApiClient.apiService.getProfessorAttendanceStatus(currentClassId)
            .enqueue(object : Callback<ProfessorAttendanceStatusResponse> {
                override fun onResponse(
                    call: Call<ProfessorAttendanceStatusResponse>,
                    response: Response<ProfessorAttendanceStatusResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body != null) {
                        setText(pageView, "tvClassName", body.courseName ?: "수업명")
                        setText(pageView, "tvClassTime", body.classTime ?: "")
                        setText(pageView, "tvAttendanceRate", "${body.attendanceRate ?: 0}%")
                        setText(pageView, "tvLateRate", "${body.lateRate ?: 0}%")
                        setText(pageView, "tvAbsentRate", "${body.absentRate ?: 0}%")
                        setText(pageView, "tvUwbCheckCount", "${body.uwbCheckCount ?: 0}회")

                        val rows = findChildByIdName<LinearLayout>(pageView, "layoutStudentAttendanceRows")
                        rows?.removeAllViews()

                        body.students?.forEach {
                            addStudentRow(
                                pageView = pageView,
                                studentId = it.studentId ?: "",
                                name = it.name ?: "",
                                status = it.status ?: ""
                            )
                        }
                    } else {
                        showTemporaryProfessorStatus(pageView)
                    }
                }

                override fun onFailure(call: Call<ProfessorAttendanceStatusResponse>, t: Throwable) {
                    showTemporaryProfessorStatus(pageView)
                }
            })
    }

    private fun showTemporaryProfessorStatus(pageView: View) {
        setText(pageView, "tvClassName", "모바일 프로그래밍")
        setText(pageView, "tvClassTime", "월 09:00 - 10:30")
        setText(pageView, "tvAttendanceRate", "80%")
        setText(pageView, "tvLateRate", "10%")
        setText(pageView, "tvAbsentRate", "10%")
        setText(pageView, "tvUwbCheckCount", "0회")

        val rows = findChildByIdName<LinearLayout>(pageView, "layoutStudentAttendanceRows")
        rows?.removeAllViews()

        addStudentRow(pageView, "202312345", "최은수", "PRESENT")
        addStudentRow(pageView, "202312346", "홍길동", "LATE")
        addStudentRow(pageView, "202312347", "김가천", "ABSENT")
    }

    private fun requestSchedule(pageView: View) {
        if (userId == -1) return

        ApiClient.apiService.getStudentSchedule(userId)
            .enqueue(object : Callback<ScheduleResponse> {
                override fun onResponse(
                    call: Call<ScheduleResponse>,
                    response: Response<ScheduleResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body != null) {
                        val first = body.classes?.firstOrNull()

                        setText(pageView, "tvCurrentClassName", first?.courseName ?: "등록된 시간표 없음")
                        setText(pageView, "tvDetailProfessor", first?.professorName ?: "")
                        setText(pageView, "tvDetailTime", "${first?.dayOfWeek ?: ""} ${first?.startTime ?: ""} - ${first?.endTime ?: ""}")
                        setText(pageView, "tvDetailRoom", first?.room ?: "")
                        setText(pageView, "tvDetailCourseCode", first?.courseCode ?: "")

                        val layer = findChildByIdName<LinearLayout>(pageView, "classBlockLayer")
                        layer?.removeAllViews()

                        body.classes?.forEach {
                            addSimpleTextBlock(
                                pageView = pageView,
                                parentIdName = "classBlockLayer",
                                text = "${it.courseName} / ${it.dayOfWeek} ${it.startTime}-${it.endTime} / ${it.room}"
                            )
                        }
                    } else {
                        showTemporarySchedule(pageView)
                    }
                }

                override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                    showTemporarySchedule(pageView)
                }
            })
    }

    private fun showTemporarySchedule(pageView: View) {
        setText(pageView, "tvCurrentClassName", "모바일 프로그래밍")
        setText(pageView, "tvDetailProfessor", "김가천")
        setText(pageView, "tvDetailTime", "월 09:00 - 10:30")
        setText(pageView, "tvDetailRoom", "AI관 301호")
        setText(pageView, "tvDetailCourseCode", "MOB001")
    }

    private fun requestAttendanceCalendar(pageView: View) {
        val month = SimpleDateFormat("yyyy-MM", Locale.KOREA).format(Date())

        ApiClient.apiService.getAttendanceCalendar(userId, month)
            .enqueue(object : Callback<AttendanceCalendarResponse> {
                override fun onResponse(
                    call: Call<AttendanceCalendarResponse>,
                    response: Response<AttendanceCalendarResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body != null) {
                        val result = body.days?.joinToString("\n") {
                            "${it.date} / ${it.courseName} / ${statusToKorean(it.status ?: "")}"
                        } ?: "출결 데이터 없음"

                        setText(pageView, "tvAttendanceCalendar", result)
                        addSimpleTextBlock(pageView, "layoutAttendanceCalendar", result)
                    } else {
                        showTemporaryCalendar(pageView)
                    }
                }

                override fun onFailure(call: Call<AttendanceCalendarResponse>, t: Throwable) {
                    showTemporaryCalendar(pageView)
                }
            })
    }

    private fun showTemporaryCalendar(pageView: View) {
        val temp = "2026-05-03 / 모바일 프로그래밍 / 결석\n2026-05-10 / 자료구조 / 지각"
        setText(pageView, "tvAttendanceCalendar", temp)
        addSimpleTextBlock(pageView, "layoutAttendanceCalendar", temp)
    }

    private fun requestAttendanceSummary(pageView: View) {
        ApiClient.apiService.getAttendanceSummary(userId)
            .enqueue(object : Callback<AttendanceSummaryResponse> {
                override fun onResponse(
                    call: Call<AttendanceSummaryResponse>,
                    response: Response<AttendanceSummaryResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body != null) {
                        val summary = body.courses?.joinToString("\n") {
                            "${it.courseName}: 출석 ${it.presentRate}% / 지각 ${it.lateRate}% / 결석 ${it.absentRate}%"
                        } ?: "출결 통계 없음"

                        setText(pageView, "tvAttendanceSummary", summary)
                        addSimpleTextBlock(pageView, "layoutAttendanceSummary", summary)
                    } else {
                        showTemporarySummary(pageView)
                    }
                }

                override fun onFailure(call: Call<AttendanceSummaryResponse>, t: Throwable) {
                    showTemporarySummary(pageView)
                }
            })
    }

    private fun showTemporarySummary(pageView: View) {
        val temp = "모바일 프로그래밍: 출석 80% / 지각 10% / 결석 10%"
        setText(pageView, "tvAttendanceSummary", temp)
        addSimpleTextBlock(pageView, "layoutAttendanceSummary", temp)
    }

    private fun requestMyInfo(pageView: View) {
        ApiClient.apiService.getMyInfo(userId)
            .enqueue(object : Callback<MyInfoResponse> {
                override fun onResponse(
                    call: Call<MyInfoResponse>,
                    response: Response<MyInfoResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body != null) {
                        setText(pageView, "tvUserName", body.name ?: "")
                        setText(pageView, "tvUserRole", body.role ?: "")
                        setText(pageView, "tvDepartment", body.department ?: "")
                        setText(pageView, "tvStudentNumber", body.studentNumber ?: body.professorNumber ?: "")
                    } else {
                        showTemporaryMyInfo(pageView)
                    }
                }

                override fun onFailure(call: Call<MyInfoResponse>, t: Throwable) {
                    showTemporaryMyInfo(pageView)
                }
            })
    }

    private fun showTemporaryMyInfo(pageView: View) {
        val prefs = getSharedPreferences("userPrefs", MODE_PRIVATE)
        setText(pageView, "tvUserName", prefs.getString("name", "사용자") ?: "사용자")
        setText(pageView, "tvUserRole", role)
        setText(pageView, "tvDepartment", "소프트웨어학과")
        setText(pageView, "tvStudentNumber", if (role == "PROFESSOR") "P001" else "202312345")
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
        row.addView(makeRowText(statusToKorean(status), 1f))

        parent.addView(row)
    }

    private fun makeRowText(textValue: String, weight: Float): TextView {
        return TextView(this).apply {
            text = textValue
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

    private fun addSimpleTextBlock(pageView: View, parentIdName: String, text: String) {
        val parent = findChildByIdName<LinearLayout>(pageView, parentIdName) ?: return

        val tv = TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.parseColor("#222222"))
            setPadding(16, 12, 16, 12)
        }

        parent.addView(tv)
    }

    private fun setText(pageView: View, idName: String, value: String) {
        val tv = findChildByIdName<TextView>(pageView, idName)
        tv?.text = value
    }

    private inline fun <reified T> findChildByIdName(pageView: View, idName: String): T? {
        val id = resources.getIdentifier(idName, "id", packageName)
        return if (id != 0) pageView.findViewById(id) else null
    }

    private fun statusToKorean(status: String): String {
        return when (status) {
            "PRESENT" -> "출석"
            "LATE" -> "지각"
            "ABSENT" -> "결석"
            "NOT_STARTED" -> "출석 전"
            else -> status
        }
    }

    private fun todayText(): String {
        return SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date())
    }

    private fun nowText(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA).format(Date())
    }

    private fun logout() {
        getSharedPreferences("LOGIN_INFO", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        getSharedPreferences("login_pref", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        getSharedPreferences("userPrefs", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        Toast.makeText(this, "로그아웃되었습니다", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}