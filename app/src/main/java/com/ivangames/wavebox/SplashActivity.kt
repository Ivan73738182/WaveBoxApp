package com.ivangames.wavebox

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SplashActivity : AppCompatActivity() {

    private val serverUrl = "https://waveboxserver.onrender.com/stations"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        loadStations()
    }

    private fun loadStations() {
        Executors.newSingleThreadExecutor().execute {
            var loadedStations: List<Station>? = null
            try {
                Log.d("WaveBox", "Splash: загрузка станций...")
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder().url(serverUrl).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val json = response.body?.string()
                    val type = object : TypeToken<List<Station>>() {}.type
                    loadedStations = Gson().fromJson(json, type)
                    Log.d("WaveBox", "Splash: загружено ${loadedStations?.size} станций")
                } else {
                    Log.e("WaveBox", "Splash: ошибка сервера ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("WaveBox", "Splash: ошибка загрузки", e)
            }

            runOnUiThread {
                if (loadedStations.isNullOrEmpty()) {
                    // Если не загрузилось — показываем сообщение и запускаем с запасными
                    Toast.makeText(this, "Не удалось загрузить, используются стандартные", Toast.LENGTH_SHORT).show()
                }
                
                // Передаем список в MainActivity
                val intent = Intent(this, MainActivity::class.java)
                // Мы не можем передать List<Station> через Intent напрямую, 
                // поэтому MainActivity сама загрузит список, но уже с кэшем или запасным вариантом.
                // Для простоты: просто запускаем MainActivity.
                startActivity(intent)
                finish()
            }
        }
    }
}
