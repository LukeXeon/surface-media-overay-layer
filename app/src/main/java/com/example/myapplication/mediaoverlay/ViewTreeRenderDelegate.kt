package com.example.myapplication.mediaoverlay

import android.app.Presentation
import android.content.Context
import android.content.res.Resources
import android.graphics.Picture
import android.graphics.drawable.PictureDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ViewTreeRenderDelegate(
    private val context: Context,
    private val surface: Surface,
    private val contentView: View,
) {
    private val substituteView by lazy {
        SubstituteView(
            context.applicationContext
        )
    }
    private val displayManager = context.getSystemService(
        Context.DISPLAY_SERVICE
    ) as DisplayManager
    private var virtualDisplayPresentation: VirtualDisplayPresentation? = null

    private data class VirtualDisplayPresentation(
        val virtualDisplay: VirtualDisplay,
        val presentation: Presentation
    )

    private class SubstituteView(context: Context) : View(context) {
        private val picture = Picture()

        init {
            background = PictureDrawable(picture)
        }

        fun takeCapture(view: View) {
            val width = view.width
            val height = view.width
            if (width * height > 0) {
                view.invalidate()
                val canvas = picture.beginRecording(width, height)
                view.draw(canvas)
                picture.endRecording()
            }
        }
    }

    private suspend fun createNewDisplay(
        layerMetrics: LayerMetrics
    ): VirtualDisplay {
        return suspendCoroutine { con ->
            var virtualDisplay: VirtualDisplay? = null
            displayManager.registerDisplayListener(
                object : DisplayListenerAdapter {
                    override fun onDisplayAdded(displayId: Int) {
                        val vd = virtualDisplay
                        if (vd != null && vd.display.displayId == displayId) {
                            con.resume(vd)
                            displayManager.unregisterDisplayListener(this)
                        }
                    }
                },
                Handler(Looper.getMainLooper())
            )
            virtualDisplay = displayManager.createVirtualDisplay(
                surface.toString(),
                layerMetrics.width,
                layerMetrics.height,
                layerMetrics.densityDpi,
                null,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
            )
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun createNewPresentation(
        virtualDisplay: VirtualDisplay
    ): Presentation {
        val presentation = object : Presentation(
            context,
            virtualDisplay.display,
            android.R.style.Theme_Translucent_NoTitleBar_Fullscreen
        ) {
            private var mOverrideResources: Resources? = null
            private val mDisplayMetrics = DisplayMetrics()
            override fun getResources(): Resources {
                val base = super.getResources()
                display.getMetrics(mDisplayMetrics)
                return if (base.displayMetrics == mDisplayMetrics) {
                    base
                } else {
                    var res = mOverrideResources
                    if (res == null || res.displayMetrics != mDisplayMetrics) {
                        res = Resources(
                            base.assets,
                            mDisplayMetrics,
                            base.configuration
                        )
                        mOverrideResources = res
                    }
                    res
                }
            }
        }
        val window = requireNotNull(presentation.window)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        presentation.setCancelable(false)
        presentation.setContentView(contentView)
        suspendCoroutine { con ->
            presentation.setOnShowListener {
                con.resume(Unit)
                presentation.setOnShowListener(null)
            }
            presentation.show()
        }
        return presentation
    }

    suspend fun resize(layerMetrics: LayerMetrics) {
        dismiss()
        val virtualDisplay = createNewDisplay(layerMetrics)
        val presentation = createNewPresentation(virtualDisplay)
        virtualDisplay.surface = surface
        virtualDisplayPresentation = VirtualDisplayPresentation(virtualDisplay, presentation)
    }

    suspend fun dismiss() {
        val (virtualDisplay, presentation) = virtualDisplayPresentation ?: return
        if (presentation.isShowing) {
            val contentView = presentation.findViewById<View>(android.R.id.content)
            if (contentView !is SubstituteView) {
                val parent = substituteView.parent
                if (parent is ViewGroup) {
                    parent.removeView(substituteView)
                }
                substituteView.takeCapture(contentView)
                presentation.setContentView(substituteView)
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