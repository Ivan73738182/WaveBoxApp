package com.ivangames.wavebox

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.ComponentName
import android.os.Bundle
import android.view.animation.LinearInterpolator
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
    private var pulseAnimator: ObjectAnimator? = null

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

        // Настраиваем анимацию пульсации
        val scaleUp = PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f)
        val scaleUpY = PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f)
        val scaleDown = PropertyValuesHolder.ofFloat("scaleX", 1.1f, 1f)
        val scaleDownY = PropertyValuesHolder.ofFloat("scaleY", 1.1f, 1f)

        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(bigIcon, scaleUp, scaleUpY, scaleDown, scaleDownY).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = LinearInterpolator()
        }

        val sessionToken = SessionToken(this, ComponentName(this, RadioService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            controller = controllerFuture.get()
            
            updateUI(controller?.currentMediaItem)
            updatePlayPauseButton(controller?.isPlaying == true)

            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlayPauseButton(isPlaying)
                    if (isPlaying) pulseAnimator?.start() else pulseAnimator?.cancel()
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
            finish()
        }
    }

    private fun updateUI(mediaItem: MediaItem?) {
        val title = mediaItem?.mediaMetadata?.title?.toString() ?: "Не играет"
        val artist = mediaItem?.mediaMetadata?.artist?.toString() ?: ""
        stationNameText.text = title
        stationDescText.text = artist
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
        pulseAnimator?.cancel()
        MediaController.releaseFuture(controllerFuture)
    }
}
