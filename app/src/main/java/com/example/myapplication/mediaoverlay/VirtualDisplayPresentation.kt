package com.example.myapplication.mediaoverlay

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.Space
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VirtualDisplayPresentation(
    name: String,
    context: Context,
    surface: Surface,
    contentView: View,
    densityDpi: Int,
    width: Int,
    height: Int
) {
    private val mVirtualDisplay: VirtualDisplay
    private val mPresentation: Presentation
    private val mOnRemoveContinuations = CopyOnWriteArrayList<Continuation<Unit>>()

    init {
        val displayManager = context.getSystemService(
            Context.DISPLAY_SERVICE
        ) as DisplayManager
        val virtualDisplay = displayManager.createVirtualDisplay(
            name,
            width,
            height,
            densityDpi,
            surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
        )
        mVirtualDisplay = virtualDisplay
        val presentation = object : Presentation(
            context,
            virtualDisplay.display,
            android.R.style.Theme_Material_NoActionBar
        ) {
            override fun onDisplayRemoved() {
                mOnRemoveContinuations.forEach {
                    it.resume(Unit)
                }
                mOnRemoveContinuations.clear()
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
            // 需要等待系统确认虚拟显示器已经被移除，
            // 否则使用Surface创建虚拟显示器的时候系统内部会冲突
            // 这个操作不可取消
            suspendCoroutine { con ->
                mOnRemoveContinuations.add(con)
            }
        }
    }
}