package com.example.myapplication.mediaoverlay

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.Space
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.resume

class VirtualDisplayPresentation(
    context: Context,
    surface: Surface,
    contentView: View,
    densityDpi: Int,
    width: Int,
    height: Int
) {
    companion object {
        private const val TAG = "VirtualDisplayPresentation"
    }

    private val mVirtualDisplay: VirtualDisplay
    private val mPresentation: Presentation
    private val mOnRemoveListeners = CopyOnWriteArrayList<Runnable>()

    init {
        val displayManager = context.getSystemService(
            Context.DISPLAY_SERVICE
        ) as DisplayManager
        val virtualDisplay = displayManager.createVirtualDisplay(
            "$TAG:${UUID.randomUUID()}",
            width,
            height,
            densityDpi,
            surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
        )
        mVirtualDisplay = virtualDisplay
        val presentation = object : Presentation(
            context,
            virtualDisplay.display
        ) {
            override fun onDisplayRemoved() {
                mOnRemoveListeners.forEach {
                    it.run()
                }
                mOnRemoveListeners.clear()
            }
        }
        presentation.window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        presentation.window?.setBackgroundDrawable(null)
        presentation.setCancelable(false)
        presentation.setContentView(contentView)
        presentation.show()
        mPresentation = presentation
    }

    suspend fun dismissAndWaitSystem() {
        if (mPresentation.isShowing) {
            if (mPresentation.findViewById<View>(android.R.id.content) !is Space) {
                mPresentation.setContentView(Space(mPresentation.context))
            }
            mVirtualDisplay.release()
            // 需要等待系统确认虚拟显示器已经被移除，否则使用Surface创建虚拟显示器的时候系统内部会冲突
            suspendCancellableCoroutine { con ->
                val onRemove = object : Runnable {
                    override fun run() {
                        if (!con.isCompleted) {
                            con.resume(Unit)
                            mOnRemoveListeners.remove(this)
                        }
                    }
                }
                con.invokeOnCancellation {
                    mOnRemoveListeners.remove(onRemove)
                }
                mOnRemoveListeners.add(onRemove)
            }
        }
    }
}