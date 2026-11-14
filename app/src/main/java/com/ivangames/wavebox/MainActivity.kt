package com.ivangames.wavebox

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playPauseButton: ImageButton
    private lateinit var nowPlayingText: TextView

    // Список станций (пока тестовый)
    private val stations = listOf(
        Station("Европа Плюс", "Популярная музыка", "http://ep128.hostingradio.ru:8030/ep128"),
        Station("Ретро FM", "Музыка 80-х и 90-х", "http://retroserver.streamr.ru:8043/retro256.mp3")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playPauseButton = findViewById(R.id.playPauseButton)
        nowPlayingText = findViewById(R.id.nowPlayingText)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        // Настраиваем плеер
        player = ExoPlayer.Builder(this).build()

        // Настраиваем список
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = StationAdapter(stations) { station ->
            playStation(station)
        }

        // Кнопка паузы/плея
        playPauseButton.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
                playPauseButton.setImageResource(android.R.drawable.ic_media_play)
            } else {
                player.play()
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    private fun playStation(station: Station) {
        nowPlayingText.text = station.name
        val mediaItem = MediaItem.fromUri(station.url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
