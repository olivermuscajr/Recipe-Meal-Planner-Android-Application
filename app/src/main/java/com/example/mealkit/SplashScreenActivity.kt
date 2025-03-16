package com.example.mealkit

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var textureView: TextureView
    private lateinit var videoPlaceholder: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        textureView = findViewById(R.id.textureView)
        videoPlaceholder = findViewById(R.id.videoPlaceholder)
        textureView.surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        val videoPath = "android.resource://" + packageName + "/" + R.raw.mealkit_logo_splash
        val uri = Uri.parse(videoPath)
        mediaPlayer = MediaPlayer.create(this, uri)

        mediaPlayer.setSurface(Surface(surface))

        mediaPlayer.setOnPreparedListener {
            videoPlaceholder.visibility = View.GONE
            mediaPlayer.start()
        }

        mediaPlayer.setOnCompletionListener {
            startActivity(Intent(this@SplashActivity, LoginActivity2::class.java))
            finish()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        mediaPlayer.release()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
}