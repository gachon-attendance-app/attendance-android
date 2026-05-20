package com.example.myapplication

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Firebase REST 통신.
 * Firebase SDK 없이 Realtime Database URL 직접 호출.
 */
object FirebaseClient {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun get(path: String, callback: (JSONObject?) -> Unit) {
        request(method = "GET", path = path, body = null, callback = callback)
    }

    fun put(path: String, body: JSONObject, callback: (JSONObject?) -> Unit = {}) {
        request(method = "PUT", path = path, body = body, callback = callback)
    }

    fun patch(path: String, body: JSONObject, callback: (JSONObject?) -> Unit = {}) {
        request(method = "PATCH", path = path, body = body, callback = callback)
    }

    private fun request(
        method: String,
        path: String,
        body: JSONObject?,
        callback: (JSONObject?) -> Unit
    ) {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val cleanPath = path.trim().trim('/')
                val urlText = if (cleanPath.isEmpty()) {
                    "${FirebaseConfig.BASE_URL}/.json"
                } else {
                    "${FirebaseConfig.BASE_URL}/$cleanPath.json"
                }

                connection = URL(urlText).openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Accept", "application/json")

                if (body != null) {
                    connection.doOutput = true
                    OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(body.toString())
                        writer.flush()
                    }
                }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.let {
                    BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader ->
                        reader.readText()
                    }
                }.orEmpty()

                val json = when {
                    response.isBlank() -> null
                    response == "null" -> null
                    response.trim().startsWith("{") -> JSONObject(response)
                    else -> JSONObject().put("value", response)
                }

                mainHandler.post { callback(json) }
            } catch (e: Exception) {
                mainHandler.post { callback(null) }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}