package com.example.myapplication.mediaoverlay

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.Space
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VirtualDisplayPresentation private constructor(
    private val mVirtualDisplay: VirtualDisplay,
    private val mPresentation: Presentation,
    private val mOnRemoveListeners: CopyOnWriteArrayList<Runnable>
) {
    companion object {
        fun create(
            name: String,
            context: Context,
            surface: Surface,
            contentView: View,
            densityDpi: Int,
            width: Int,
            height: Int
        ): VirtualDisplayPresentation {
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
            val onRemoveListeners = CopyOnWriteArrayList<Runnable>()
            val presentation = object : Presentation(
                context,
                virtualDisplay.display,
                android.R.style.Theme_Material_NoActionBar
            ) {
                override fun onDisplayRemoved() {
                    onRemoveListeners.forEach {
                        it.run()
                    }
                }
            }
            presentation.window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            presentation.window?.setBackgroundDrawable(null)
            presentation.setCancelable(false)
            presentation.setContentView(contentView)
            presentation.show()
            return VirtualDisplayPresentation(
                virtualDisplay,
                presentation,
                onRemoveListeners
            )
        }
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
            coroutineScope {
                joinAll(
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        suspendCoroutine { con ->
                            val listener = object : Runnable {
                                override fun run() {
                                    con.resume(Unit)
                                    mOnRemoveListeners.remove(this)
                                }
                            }
                            mOnRemoveListeners.add(listener)
                        }
                    },
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        suspendCoroutine { con ->
                            mPresentation.setOnDismissListener {
                                con.resume(Unit)
                                mPresentation.setOnDismissListener(null)
                            }
                        }
                    }
                )
            }
        }
    }
}