package com.example.myapplication.mediaoverlay

import android.app.Presentation
import android.content.Context
import android.content.res.Resources
import android.graphics.Picture
import android.graphics.drawable.PictureDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.util.DisplayMetrics
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Space
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import com.example.myapplication.R
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ViewTreeRenderDelegate constructor(
    private val context: Context,
    private val name: String,
    private val surface: Surface,
    private val contentView: View,
) {

    private var virtualDisplayPresentation: Pair<VirtualDisplay, Presentation>? = null

    @Suppress("DEPRECATION")
    private fun createNew(layerMetrics: LayerMetrics): Pair<VirtualDisplay, Presentation> {
        val displayManager = context.getSystemService(
            Context.DISPLAY_SERVICE
        ) as DisplayManager
        val virtualDisplay = displayManager.createVirtualDisplay(
            name,
            layerMetrics.width,
            layerMetrics.height,
            layerMetrics.densityDpi,
            surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
        )
        val presentation = object : Presentation(
            context,
            virtualDisplay.display,
            android.R.style.Theme_Translucent_NoTitleBar_Fullscreen
        ) {
            private var mOverrideRes: Resources? = null
            private val mDisplayMetrics = DisplayMetrics()
            override fun getResources(): Resources {
                val base = super.getResources()
                display.getMetrics(mDisplayMetrics)
                return if (base.displayMetrics == mDisplayMetrics) {
                    base
                } else {
                    var res = mOverrideRes
                    if (res == null || res.displayMetrics != mDisplayMetrics) {
                        res = Resources(
                            base.assets,
                            mDisplayMetrics,
                            base.configuration
                        )
                        mOverrideRes = res
                    }
                    res
                }
            }
        }
        val window = requireNotNull(presentation.window)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        presentation.setCancelable(false)
        presentation.setContentView(contentView)
        presentation.show()
        return virtualDisplay to presentation
    }

    suspend fun resize(layerMetrics: LayerMetrics) {
        dismiss()
        virtualDisplayPresentation = createNew(layerMetrics)
    }

    suspend fun dismiss() {
        val (virtualDisplay, presentation) = virtualDisplayPresentation ?: return
        if (presentation.isShowing) {
            val contentView = presentation.findViewById<View>(android.R.id.content)
            if (contentView.getTag(R.id.snapshot_view) !is Unit) {
                val snapshotView = View(context)
                snapshotView.setTag(R.id.snapshot_view, Unit)
                val width = contentView.width
                val height = contentView.width
                if (width * height > 0) {
                    val picture = Picture()
                    val canvas = picture.beginRecording(width, height)
                    contentView.draw(canvas)
                    picture.endRecording()
                    snapshotView.background = PictureDrawable(picture)
                }
                presentation.setContentView(snapshotView, contentView.layoutParams)
            }
            suspendCoroutine { con ->
                presentation.setOnDismissListener {
                    con.resume(Unit)
                    presentation.setOnDismissListener(null)
                }
                virtualDisplay.release()
            }
            virtualDisplayPresentation = null
        }
    }
}