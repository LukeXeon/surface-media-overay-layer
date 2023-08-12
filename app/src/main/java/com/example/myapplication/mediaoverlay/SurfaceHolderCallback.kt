package com.example.myapplication.mediaoverlay

import android.view.SurfaceHolder

interface SurfaceHolderCallback : SurfaceHolder.Callback2 {
    override fun surfaceCreated(holder: SurfaceHolder) {
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

    override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
    }
}