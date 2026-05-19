package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.myapplication.model.LoginRequest
import com.example.myapplication.model.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : Activity() {

    private lateinit var etId: EditText
    private lateinit var etPw: EditText
    private var cbAutoLogin: CheckBox? = null
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        etId = findViewByIdName("etId")
            ?: findViewByIdName("editTextId")
                    ?: findViewByIdName("loginId")
                    ?: createMissingEditText("아이디 입력창 ID를 etId로 맞춰주세요.")

        etPw = findViewByIdName("etPw")
            ?: findViewByIdName("editTextPw")
                    ?: findViewByIdName("password")
                    ?: createMissingEditText("비밀번호 입력창 ID를 etPw로 맞춰주세요.")

        cbAutoLogin = findViewByIdName("cbAutoLogin")
            ?: findViewByIdName("checkAutoLogin")
                    ?: findViewByIdName("autoLoginCheckBox")

        btnLogin = findViewByIdName("btnLogin")
            ?: createMissingButton("로그인 버튼 ID를 btnLogin으로 맞춰주세요.")

        btnLogin.setOnClickListener {
            requestLogin()
        }

        val signupView: TextView? = findViewByIdName("btnSignup")
            ?: findViewByIdName("tvSignup")
            ?: findViewByIdName("signupText")

        signupView?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("openPage", "SIGNUP")
            startActivity(intent)
        }
    }

    private fun requestLogin() {
        val loginId = etId.text.toString().trim()
        val password = etPw.text.toString().trim()

        if (loginId.isEmpty()) {
            Toast.makeText(this, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = LoginRequest(
            loginId = loginId,
            password = password
        )

        ApiClient.apiService.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(
                call: Call<LoginResponse>,
                response: Response<LoginResponse>
            ) {
                val body = response.body()

                if (response.isSuccessful && body?.success == true) {
                    val role = body.role ?: "STUDENT"
                    val userId = body.userId ?: -1
                    val name = body.name ?: loginId
                    val token = body.accessToken ?: ""

                    saveLoginInfo(
                        accessToken = token,
                        userId = userId,
                        loginId = loginId,
                        name = name,
                        role = role
                    )

                    moveToMain(role)
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        body?.message ?: "아이디 또는 비밀번호가 올바르지 않습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                /*
                 * 백엔드 서버 연결 전 임시 테스트 로그인
                 */
                when {
                    loginId == "test" && password == "1234" -> {
                        saveLoginInfo(
                            accessToken = "temp-student-token",
                            userId = 1,
                            loginId = loginId,
                            name = "테스트학생",
                            role = "STUDENT"
                        )
                        moveToMain("STUDENT")
                    }

                    loginId == "professor" && password == "1234" -> {
                        saveLoginInfo(
                            accessToken = "temp-professor-token",
                            userId = 2,
                            loginId = loginId,
                            name = "테스트교수",
                            role = "PROFESSOR"
                        )
                        moveToMain("PROFESSOR")
                    }

                    else -> {
                        Toast.makeText(
                            this@LoginActivity,
                            "서버 연결 실패: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun saveLoginInfo(
        accessToken: String,
        userId: Int,
        loginId: String,
        name: String,
        role: String
    ) {
        val autoLoginChecked = cbAutoLogin?.isChecked ?: false

        /*
         * 새 API 연결용 저장소
         */
        getSharedPreferences("userPrefs", MODE_PRIVATE)
            .edit()
            .putString("accessToken", accessToken)
            .putInt("userId", userId)
            .putString("loginId", loginId)
            .putString("name", name)
            .putString("role", role)
            .putBoolean("autoLogin", autoLoginChecked)
            .apply()

        /*
         * 기존 MainActivity에서 쓰던 저장소도 같이 유지
         */
        getSharedPreferences("LOGIN_INFO", MODE_PRIVATE)
            .edit()
            .putString("userId", loginId)
            .putString("userName", name)
            .putString("userRole", if (role == "PROFESSOR") "professor" else "student")
            .apply()

        /*
         * 기존 LoadingActivity에서 쓰던 자동 로그인 저장소도 같이 유지
         */
        getSharedPreferences("login_pref", MODE_PRIVATE)
            .edit()
            .putBoolean("auto_login", autoLoginChecked)
            .apply()
    }

    private fun moveToMain(role: String?) {
        val intent = Intent(this, MainActivity::class.java)

        if (role == "PROFESSOR" || role == "professor") {
            intent.putExtra("startRole", "PROFESSOR")
        } else {
            intent.putExtra("startRole", "STUDENT")
        }

        startActivity(intent)
        finish()
    }

    private inline fun <reified T> findViewByIdName(idName: String): T? {
        val id = resources.getIdentifier(idName, "id", packageName)
        return if (id != 0) findViewById(id) else null
    }

    private fun createMissingEditText(message: String): EditText {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        return EditText(this)
    }

    private fun createMissingButton(message: String): Button {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        return Button(this)
    }
}