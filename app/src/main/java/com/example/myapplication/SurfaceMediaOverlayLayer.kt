package com.example.myapplication

import android.app.Presentation
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

class SurfaceMediaOverlayLayer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "SurfaceMediaOverlay"
    }

    val contentView: View
    private var mRenderer: PresentationRenderer? = null

    private data class PresentationRenderer(
        val virtualDisplay: VirtualDisplay,
        val presentation: Presentation
    )

    init {
        val array = context.obtainStyledAttributes(
            attrs,
            R.styleable.SurfaceMediaOverlayLayer,
            defStyleAttr,
            0
        )
        try {
            val id = array.getResourceId(R.styleable.SurfaceMediaOverlayLayer_layout, NO_ID)
            contentView = if (id != NO_ID) {
                LayoutInflater.from(context).inflate(
                    id,
                    FrameLayout(context), false
                )
            } else {
                throw IllegalArgumentException("no layout id")
            }
        } finally {
            array.recycle()
        }
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                val virtualDisplay = displayManager.createVirtualDisplay(
                    TAG,
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi,
                    holder.surface,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                )
                val presentation = Presentation(
                    context,
                    virtualDisplay.display
                )
                presentation.setContentView(contentView)
                presentation.show()
                presentation.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                mRenderer = PresentationRenderer(virtualDisplay, presentation)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                val renderer = mRenderer ?: return
                renderer.virtualDisplay.resize(
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi
                )
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                val renderer = mRenderer ?: return
                renderer.presentation.dismiss()
                renderer.virtualDisplay.release()
                val parent = contentView.parent
                if (parent is ViewGroup) {
                    parent.removeView(contentView)
                }
            }

        })
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        val renderer = mRenderer
        if (renderer != null && newConfig != null) {
            renderer.virtualDisplay.resize(
                width,
                height,
                context.resources.displayMetrics.densityDpi
            )
        }
    }
}