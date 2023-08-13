package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myapplication.mediaoverlay.SurfaceMediaOverlayLayer

class MainActivity2 : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity2"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val surface = findViewById<SurfaceMediaOverlayLayer>(R.id.surface)
        val videoView = findViewById<VideoView>(R.id.video)
        val container = findViewById<ViewGroup>(R.id.container)
        videoView.setVideoURI(Uri.parse(getString(R.string.video_url)))
        videoView.start()
        val imageView = surface.containerView.findViewById<ImageView>(R.id.image)
        val button = surface.containerView.findViewById<Button>(R.id.button)
        button.isClickable = true
        imageView.isClickable = true
        button.setOnClickListener {
            startActivity(Intent(this, MainActivity2::class.java))
        }
        Glide.with(imageView)
            .asGif()
            .load(getString(R.string.gif_url))
            .into(imageView)
    }
}