package com.example.myapplication.mediaoverlay

import android.app.Presentation
import android.content.Context
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.util.DisplayMetrics
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.Space
import kotlinx.coroutines.yield
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ViewTreeRenderDelegate constructor(
    private val context: Context,
    private val name: String,
    private val surface: Surface,
    private val contentView: View,
) {
    private var virtualDisplayPresentation: Pair<VirtualDisplay, Presentation>? = null
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
            android.R.style.Theme_Material_NoActionBar
        ) {
            private var mWrapper: Resources? = null
            private val mDisplayMetrics = DisplayMetrics()
            override fun getResources(): Resources {
                val base = super.getResources()
                display.getMetrics(mDisplayMetrics)
                return if (base.displayMetrics == mDisplayMetrics) {
                    base
                } else {
                    var wrapper = mWrapper
                    if (wrapper == null || wrapper.displayMetrics != mDisplayMetrics) {
                        wrapper = Resources(
                            base.assets,
                            mDisplayMetrics,
                            base.configuration
                        )
                        mWrapper = wrapper
                    }
                    wrapper
                }
            }
        }
        val window = requireNotNull(presentation.window)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        window.setBackgroundDrawable(null)
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
            if (presentation.findViewById<View>(android.R.id.content) !is Space) {
                presentation.setContentView(Space(context.applicationContext))
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