package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class LoginActivity : Activity() {

    private lateinit var etId: EditText
    private lateinit var etPw: EditText
    private lateinit var tvIdError: TextView
    private lateinit var tvPwError: TextView
    private lateinit var cbAutoLogin: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var tvSignup: TextView

    private val localUsers = mapOf(
        "test" to AppUser(
            userId = 1,
            loginId = "test",
            password = "1234",
            name = "테스트학생",
            role = "student",
            department = "소프트웨어학과",
            studentNumber = "202312345",
            professorNumber = null
        ),
        "202312345" to AppUser(
            userId = 1,
            loginId = "202312345",
            password = "1234",
            name = "최은수",
            role = "student",
            department = "소프트웨어학과",
            studentNumber = "202312345",
            professorNumber = null
        ),
        "professor" to AppUser(
            userId = 2,
            loginId = "professor",
            password = "1234",
            name = "테스트교수",
            role = "professor",
            department = "소프트웨어학과",
            studentNumber = null,
            professorNumber = "P001"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val loginPref = getSharedPreferences("login_pref", MODE_PRIVATE)
        val isAutoLogin = loginPref.getBoolean("auto_login", false)

        if (isAutoLogin) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.login)

        etId = findViewById(R.id.etId)
        etPw = findViewById(R.id.etPw)
        tvIdError = findViewById(R.id.tvIdError)
        tvPwError = findViewById(R.id.tvPwError)
        cbAutoLogin = findViewById(R.id.cbAutoLogin)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignup = findViewById(R.id.tvSignup)

        tvIdError.visibility = View.GONE
        tvPwError.visibility = View.GONE

        btnLogin.setOnClickListener {
            requestLogin()
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun requestLogin() {
        val inputId = etId.text.toString().trim()
        val inputPw = etPw.text.toString().trim()

        tvIdError.visibility = View.GONE
        tvPwError.visibility = View.GONE

        if (inputId.isEmpty()) {
            tvIdError.visibility = View.VISIBLE
            tvIdError.text = "아이디를 입력해주세요"
            return
        }

        if (inputPw.isEmpty()) {
            tvPwError.visibility = View.VISIBLE
            tvPwError.text = "비밀번호를 입력해주세요"
            return
        }

        FirebaseClient.get("users/$inputId") { json ->
            val firebaseUser = FirebaseParsers.user(json, inputId)
            val user = firebaseUser ?: localUsers[inputId]

            if (user == null) {
                tvIdError.visibility = View.VISIBLE
                tvIdError.text = "입력하신 아이디를 찾을 수 없습니다"
                return@get
            }

            if (inputPw != user.password) {
                tvPwError.visibility = View.VISIBLE
                tvPwError.text = "비밀번호가 올바르지 않습니다"
                return@get
            }

            saveLoginInfo(user)
            Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun saveLoginInfo(user: AppUser) {
        val rolePref = getSharedPreferences("LOGIN_INFO", MODE_PRIVATE)
        rolePref.edit()
            .putInt("userId", user.userId)
            .putString("loginId", user.loginId)
            .putString("userName", user.name)
            .putString("userRole", user.role)
            .putString("department", user.department)
            .putString("studentNumber", user.studentNumber)
            .putString("professorNumber", user.professorNumber)
            .apply()

        val loginPref = getSharedPreferences("login_pref", MODE_PRIVATE)
        if (cbAutoLogin.isChecked) {
            loginPref.edit()
                .putBoolean("auto_login", true)
                .putString("saved_id", user.loginId)
                .putString("saved_role", user.role)
                .apply()
        } else {
            loginPref.edit()
                .putBoolean("auto_login", false)
                .remove("saved_id")
                .remove("saved_role")
                .apply()
        }
    }
}