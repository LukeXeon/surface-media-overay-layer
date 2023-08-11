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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ViewTreeRenderDelegate private constructor(
    private val mVirtualDisplay: VirtualDisplay,
    private val mPresentation: Presentation
) {
    companion object {
        fun create(
            name: String,
            context: Context,
            surface: Surface,
            contentView: View,
            layerMetrics: LayerMetrics,
        ): ViewTreeRenderDelegate {
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
            presentation.window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            presentation.window?.setBackgroundDrawable(null)
            presentation.setCancelable(false)
            presentation.setContentView(contentView)
            presentation.show()
            return ViewTreeRenderDelegate(
                virtualDisplay,
                presentation
            )
        }
    }

    fun resize(layerMetrics: LayerMetrics) {
        mVirtualDisplay.resize(
            layerMetrics.width,
            layerMetrics.height,
            layerMetrics.densityDpi
        )
    }

    suspend fun dismiss() {
        if (mPresentation.isShowing) {
            if (mPresentation.findViewById<View>(android.R.id.content) !is Space) {
                mPresentation.setContentView(Space(mPresentation.context))
            }
            mVirtualDisplay.release()
            // 需要等待系统确认虚拟显示器已经被移除，
            // 否则使用Surface创建虚拟显示器的时候系统内部会冲突
            // 这个操作不可取消
            suspendCoroutine { con ->
                mPresentation.setOnDismissListener {
                    con.resume(Unit)
                    mPresentation.setOnDismissListener(null)
                }
            }
        }
    }
}