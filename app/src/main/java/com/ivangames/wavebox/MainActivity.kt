package com.ivangames.wavebox

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
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
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class MainActivity : AppCompatActivity() {

    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null
    private lateinit var playPauseButton: ImageButton
    private lateinit var stopButton: ImageButton
    private lateinit var nowPlayingText: TextView
    private lateinit var adapter: StationAdapter

    private val stations = listOf(
        Station("Европа Плюс", "Популярная музыка", "http://ep128.hostingradio.ru:8030/ep128"),
        Station("Ретро FM", "Музыка 80-х и 90-х", "http://retroserver.streamr.ru:8043/retro256.mp3")
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Без разрешения уведомление не появится", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playPauseButton = findViewById(R.id.playPauseButton)
        stopButton = findViewById(R.id.stopButton)
        nowPlayingText = findViewById(R.id.nowPlayingText)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val sessionToken = SessionToken(this, ComponentName(this, RadioService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            controller = controllerFuture.get()
            
            updatePlayPauseButton(controller?.isPlaying == true)
            // Восстанавливаем выделение текущей станции при открытии
            updateSelectedStation(controller?.currentMediaItem?.mediaMetadata?.title?.toString())

            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlayPauseButton(isPlaying)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val title = mediaItem?.mediaMetadata?.title?.toString() ?: "Неизвестно"
                    nowPlayingText.text = title
                    updateSelectedStation(title)
                }

                override fun onPlayerError(error: PlaybackException) {
                    Toast.makeText(this@MainActivity, "Ошибка: ${error.errorCodeName}", Toast.LENGTH_LONG).show()
                    nowPlayingText.text = "Ошибка воспроизведения"
                }
            })
        }, MoreExecutors.directExecutor())

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StationAdapter(stations) { station ->
            playStation(station)
        }
        recyclerView.adapter = adapter

        playPauseButton.setOnClickListener {
            controller?.let {
                if (it.isPlaying) it.pause() else it.play()
            }
        }

        stopButton.setOnClickListener {
            controller?.stop()
            controller?.clearMediaItems()
            nowPlayingText.text = "Выберите станцию"
            updateSelectedStation(null)
            updatePlayPauseButton(false)
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        if (isPlaying) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun updateSelectedStation(currentTitle: String?) {
        adapter.setPlayingStation(currentTitle)
    }

    private fun playStation(station: Station) {
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
        updateSelectedStation(station.name)
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaController.releaseFuture(controllerFuture)
    }
}
