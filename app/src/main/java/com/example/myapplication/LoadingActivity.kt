package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class LoadingActivity : Activity() {

    private val loadingTime = 1500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.loading)

        Handler(Looper.getMainLooper()).postDelayed({

            /*
             * 기존 코드에서 쓰던 자동 로그인 저장소
             */
            val oldPref = getSharedPreferences("login_pref", MODE_PRIVATE)
            val oldAutoLogin = oldPref.getBoolean("auto_login", false)

            /*
             * API 로그인 연결 후 쓰는 새 저장소
             */
            val userPrefs = getSharedPreferences("userPrefs", MODE_PRIVATE)
            val newAutoLogin = userPrefs.getBoolean("autoLogin", false)
            val accessToken = userPrefs.getString("accessToken", null)
            val role = userPrefs.getString("role", "STUDENT") ?: "STUDENT"

            if ((oldAutoLogin || newAutoLogin) && !accessToken.isNullOrBlank()) {
                val intent = Intent(this, MainActivity::class.java)

                if (role == "PROFESSOR" || role == "professor") {
                    intent.putExtra("startRole", "PROFESSOR")
                } else {
                    intent.putExtra("startRole", "STUDENT")
                }

                startActivity(intent)
                finish()
            } else if (oldAutoLogin) {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("startRole", "STUDENT")
                startActivity(intent)
                finish()
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }

        }, loadingTime)
    }
}