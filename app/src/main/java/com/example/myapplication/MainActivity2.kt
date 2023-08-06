package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myapplication.mediaoverlay.SurfaceMediaOverlayLayer

class MainActivity2 : AppCompatActivity() {

    companion object {
        private const val URL =
            "https://media0.giphy.com/media/CoWHDBrKOmJm4hZTsp/giphy.gif?cid=ecf05e47q6jtlad76a0gpbbze3hphxdq4yb2z8riwffl06k1&ep=v1_gifs_gifId&rid=giphy.gif"
        private const val TAG = "MainActivity2"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val surface = findViewById<SurfaceMediaOverlayLayer>(R.id.surface)
        val imageView = surface.containerView.findViewById<ImageView>(R.id.image)
        val button = surface.containerView.findViewById<Button>(R.id.button)
        button.isClickable = true
        imageView.isClickable = true
        Glide.with(imageView)
            .asGif()
            .load(URL)
            .into(imageView)
    }
}