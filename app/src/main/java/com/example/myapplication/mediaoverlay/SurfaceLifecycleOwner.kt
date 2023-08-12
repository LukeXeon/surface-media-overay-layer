package com.example.myapplication.mediaoverlay

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class SurfaceLifecycleOwner : LifecycleOwner {
    private val mLifecycle = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle {
        return mLifecycle
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        mLifecycle.handleLifecycleEvent(event)
    }
}