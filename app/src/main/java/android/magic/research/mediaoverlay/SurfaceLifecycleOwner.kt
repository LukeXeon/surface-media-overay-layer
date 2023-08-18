package android.magic.research.mediaoverlay

import android.view.SurfaceHolder
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class SurfaceLifecycleOwner : SurfaceHolderCallbackAdapter,
    LifecycleOwner,
    View.OnAttachStateChangeListener {
    private val mLifecycle = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle {
        return mLifecycle
    }

    init {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onViewAttachedToWindow(v: View) {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onViewDetachedFromWindow(v: View) {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }
}