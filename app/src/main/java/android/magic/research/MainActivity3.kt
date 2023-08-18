package android.magic.research

import android.app.Activity
import android.media.MediaPlayer
import android.net.Uri
import com.example.myapplication.R

class MainActivity3 : Activity() {

    private var mMedia: MediaPlayer? = null

    override fun onResume() {
        super.onResume()
        moveTaskToBack(true)
        mMedia = MediaPlayer.create(
            this,
            Uri.parse(getString(R.string.video_url))
        )
        mMedia?.setSurface(intent.getParcelableExtra("surface"))
        mMedia?.start()
    }

    override fun onDestroy() {
        mMedia?.release()
        super.onDestroy()
    }
}