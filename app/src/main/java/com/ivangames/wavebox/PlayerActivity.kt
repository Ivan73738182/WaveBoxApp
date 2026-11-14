package com.ivangames.wavebox

import android.content.ComponentName
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class PlayerActivity : AppCompatActivity() {

    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null
    private lateinit var playPauseButton: ImageButton
    private lateinit var stationNameText: TextView
    private lateinit var stationDescText: TextView
    private lateinit var bigIcon: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playPauseButton = findViewById(R.id.playerPlayPauseButton)
        stationNameText = findViewById(R.id.stationNameText)
        stationDescText = findViewById(R.id.stationDescText)
        bigIcon = findViewById(R.id.bigIcon)
        val stopButton = findViewById<ImageButton>(R.id.playerStopButton)
        val backButton = findViewById<ImageButton>(R.id.backButton)

        backButton.setOnClickListener { finish() }

        val sessionToken = SessionToken(this, ComponentName(this, RadioService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            controller = controllerFuture.get()
            
            // Обновляем UI при старте
            updateUI(controller?.currentMediaItem)
            updatePlayPauseButton(controller?.isPlaying == true)

            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlayPauseButton(isPlaying)
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updateUI(mediaItem)
                }
            })
        }, MoreExecutors.directExecutor())

        playPauseButton.setOnClickListener {
            controller?.let {
                if (it.isPlaying) it.pause() else it.play()
            }
        }

        stopButton.setOnClickListener {
            controller?.stop()
            controller?.clearMediaItems()
            finish() // Закрываем экран плеера, если остановили
        }
    }

    private fun updateUI(mediaItem: MediaItem?) {
        val title = mediaItem?.mediaMetadata?.title?.toString() ?: "Не играет"
        val artist = mediaItem?.mediaMetadata?.artist?.toString() ?: ""
        stationNameText.text = title
        stationDescText.text = artist
        // Можно менять иконку в зависимости от станции, пока оставим радио
        bigIcon.text = "📻"
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        if (isPlaying) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaController.releaseFuture(controllerFuture)
    }
}
