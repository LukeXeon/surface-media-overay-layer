package android.magic.research.mediaoverlay

import android.hardware.display.DisplayManager

interface DisplayListenerAdapter : DisplayManager.DisplayListener {
    override fun onDisplayAdded(displayId: Int) {
    }

    override fun onDisplayRemoved(displayId: Int) {
    }

    override fun onDisplayChanged(displayId: Int) {
    }
}