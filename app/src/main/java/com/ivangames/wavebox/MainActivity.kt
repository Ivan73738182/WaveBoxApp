package com.ivangames.wavebox

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null
    private lateinit var playPauseButton: ImageButton
    private lateinit var stopButton: ImageButton
    private lateinit var nowPlayingText: TextView
    private lateinit var adapter: StationAdapter
    private lateinit var repository: StationRepository
    private lateinit var tabLayout: TabLayout

    private var stations: List<Station> = emptyList()
    private val serverUrl = "https://waveboxserver.onrender.com/stations"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = StationRepository(this)
        playPauseButton = findViewById(R.id.playPauseButton)
        stopButton = findViewById(R.id.stopButton)
        nowPlayingText = findViewById(R.id.nowPlayingText)
        tabLayout = findViewById(R.id.tabLayout)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val playerBar = findViewById<LinearLayout>(R.id.playerBar)

        playerBar.setOnClickListener {
            if (controller?.currentMediaItem != null) {
                startActivity(Intent(this, PlayerActivity::class.java))
                overridePendingTransition(R.anim.slide_up, R.anim.slide_down)
            } else {
                Toast.makeText(this, "Сначала выберите станцию", Toast.LENGTH_SHORT).show()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        tabLayout.addTab(tabLayout.newTab().setText("Все станции"))
        tabLayout.addTab(tabLayout.newTab().setText("Избранное"))

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StationAdapter(
            emptyList(),
            { station -> playStationSafe(station) },
            { station -> toggleFavorite(station) },
            repository
        )
        recyclerView.adapter = adapter

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> adapter.updateStations(stations)
                    1 -> adapter.updateStations(repository.getFavorites())
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        loadStationsFromServer()

        val sessionToken = SessionToken(this, ComponentName(this, RadioService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            controller = controllerFuture.get()
            updatePlayPauseButton(controller?.isPlaying == true)

            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlayPauseButton(isPlaying)
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val title = mediaItem?.mediaMetadata?.title?.toString() ?: "Неизвестно"
                    nowPlayingText.text = title
                    adapter.setPlayingStation(title)
                }
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("WaveBox", "Player error: ${error.errorCodeName}", error)
                    Toast.makeText(this@MainActivity, "Ошибка: ${error.errorCodeName}", Toast.LENGTH_LONG).show()
                    nowPlayingText.text = "Ошибка воспроизведения"
                }
            })
        }, MoreExecutors.directExecutor())

        playPauseButton.setOnClickListener {
            controller?.let {
                if (it.isPlaying) it.pause() else it.play()
            }
        }

        stopButton.setOnClickListener {
            try {
                controller?.let { ctrl ->
                    if (ctrl.currentMediaItem != null) {
                        ctrl.stop()
                        ctrl.clearMediaItems()
                    }
                }
                nowPlayingText.text = "Выберите станцию"
                adapter.setPlayingStation(null)
                updatePlayPauseButton(false)
            } catch (e: Exception) {
                Log.e("WaveBox", "Error stopping", e)
            }
        }
    }

    private fun loadStationsFromServer() {
        Executors.newSingleThreadExecutor().execute {
            try {
                Log.d("WaveBox", "Начинаем загрузку станций с: $serverUrl")
                
                // Настраиваем клиент с увеличенным таймаутом
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()
                
                val request = Request.Builder().url(serverUrl).build()
                val response = client.newCall(request).execute()
                Log.d("WaveBox", "Ответ сервера: ${response.code}")

                if (response.isSuccessful) {
                    val json = response.body?.string()
                    Log.d("WaveBox", "Получен JSON: $json")
                    val type = object : TypeToken<List<Station>>() {}.type
                    val loadedStations: List<Station> = Gson().fromJson(json, type)
                    Log.d("WaveBox", "Загружено станций: ${loadedStations.size}")

                    runOnUiThread {
                        stations = loadedStations
                        adapter.updateStations(stations)
                        if (stations.isEmpty()) {
                            Toast.makeText(this, "Список станций пуст", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("WaveBox", "Ошибка сервера: ${response.code}")
                    runOnUiThread { showFallbackStations() }
                }
            } catch (e: Exception) {
                Log.e("WaveBox", "Ошибка загрузки станций", e)
                runOnUiThread { showFallbackStations() }
            }
        }
    }

    private fun showFallbackStations() {
        Log.d("WaveBox", "Показываем станции по умолчанию")
        stations = listOf(
            Station("Европа Плюс", "Популярная музыка", "http://ep128.hostingradio.ru:8030/ep128"),
            Station("Ретро FM", "Музыка 80-х и 90-х", "http://retroserver.streamr.ru:8043/retro256.mp3")
        )
        adapter.updateStations(stations)
        Toast.makeText(this, "Сервер недоступен, показаны станции по умолчанию", Toast.LENGTH_LONG).show()
    }

    private fun toggleFavorite(station: Station) {
        if (repository.isFavorite(station)) {
            repository.removeFavorite(station)
        } else {
            repository.addFavorite(station)
        }
        if (tabLayout.selectedTabPosition == 1) {
            adapter.updateStations(repository.getFavorites())
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        if (isPlaying) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun playStationSafe(station: Station) {
        if (controller == null) {
            Toast.makeText(this, "Плеер ещё не готов", Toast.LENGTH_SHORT).show()
            return
        }
        if (station.url.isBlank()) {
            Toast.makeText(this, "У станции нет ссылки", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            nowPlayingText.text = "Загрузка: ${station.name}..."
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(station.name)
                .setArtist(station.desc)
                .build()
            val mediaItem = MediaItem.Builder()
                .setUri(station.url)
                .setMediaMetadata(mediaMetadata)
                .build()
            controller?.setMediaItem(mediaItem)
            controller?.prepare()
            controller?.play()
            adapter.setPlayingStation(station.name)
        } catch (e: Exception) {
            Log.e("WaveBox", "Ошибка запуска станции", e)
            Toast.makeText(this, "Ошибка запуска: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaController.releaseFuture(controllerFuture)
    }
}
