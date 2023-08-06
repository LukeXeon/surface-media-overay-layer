/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.myapplication.mediaoverlay

import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.content.getSystemService
import androidx.core.view.setPadding
import com.example.myapplication.R

open class AppCompatPresentation(
    outerContext: Context,
    val display: Display
) : Dialog(
    createPresentationContext(outerContext, display),
    R.style.AppCompat_Presentation
) {
    private val mHandler = Handler(
        requireNotNull(
            Looper.myLooper(),
        ) {
            "Presentation must be constructed on a looper thread."
        }
    )
    private val mDisplayManager = requireNotNull(
        context.getSystemService<DisplayManager>()
    )

    /**
     * Gets the [Resources] that should be used to inflate the layout of this presentation.
     * This resources object has been configured according to the metrics of the
     * display that the presentation appears on.
     *
     * @return The presentation resources object.
     */
    val resources: Resources
        get() = context.resources

    override fun onStart() {
        super.onStart()
        mDisplayManager.registerDisplayListener(mDisplayListener, mHandler)
    }

    override fun onStop() {
        mDisplayManager.unregisterDisplayListener(mDisplayListener)
        super.onStop()
    }

    /**
     * Called by the system when the [Display] to which the presentation
     * is attached has been removed.
     *
     *
     * The system automatically calls [.cancel] to dismiss the presentation
     * after sending this event.
     *
     * @see .getDisplay
     */
    fun onDisplayRemoved() {}

    /**
     * Called by the system when the properties of the [Display] to which
     * the presentation is attached have changed.
     *
     *
     * If the display metrics have changed (for example, if the display has been
     * resized or rotated), then the system automatically calls
     * [.cancel] to dismiss the presentation.
     *
     * @see .getDisplay
     */
    fun onDisplayChanged() {}

    private val mDisplayListener = object : DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {
            if (displayId == display.displayId) {
                onDisplayRemoved()
                cancel()
            }
        }

        override fun onDisplayChanged(displayId: Int) {
            if (displayId == display.displayId) {
                onDisplayChanged()
                requireNotNull(window)
            }
        }
    }

    init {
        val w = requireNotNull(window)
        val attr = w.attributes
        attr.token = Binder()
        w.attributes = attr
        w.setGravity(Gravity.FILL)
        w.setType(WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION)
    }

    companion object {
        private const val TAG = "Presentation"
        private fun createPresentationContext(
            outerContext: Context, display: Display
        ): Context {
            val displayContext = outerContext.createDisplayContext(display)
            val resources = object : ResourcesWrapper(displayContext.resources) {
                @Suppress("DEPRECATION")
                override fun getDisplayMetrics(): DisplayMetrics {
                    val dm = DisplayMetrics()
                    display.getMetrics(dm)
                    return dm
                }
            }
            return object : ContextWrapper(displayContext) {
                override fun getResources(): Resources {
                    return resources
                }
            }
        }
    }
}