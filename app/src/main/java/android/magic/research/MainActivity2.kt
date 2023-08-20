package android.magic.research

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myapplication.R
import android.magic.research.mediaoverlay.SurfaceMediaOverlayLayer

class MainActivity2 : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity2"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val surface = findViewById<SurfaceMediaOverlayLayer>(R.id.surface)
        val videoView = findViewById<SurfaceView>(R.id.video)
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(this, Uri.parse(getString(R.string.video_url)))
        mediaPlayer.prepareAsync()
        videoView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                mediaPlayer.setSurface(holder.surface)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                mediaPlayer.setSurface(null)
            }
        })

        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
        }
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

    override fun onResume() {
        super.onResume()

    }
}