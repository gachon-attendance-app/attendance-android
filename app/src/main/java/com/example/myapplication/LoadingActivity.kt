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

        FirebaseSeedData.seedIfAllowed()

        Handler(Looper.getMainLooper()).postDelayed({
            val pref = getSharedPreferences("login_pref", MODE_PRIVATE)
            val isAutoLogin = pref.getBoolean("auto_login", false)

            if (isAutoLogin) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, loadingTime)
    }
}