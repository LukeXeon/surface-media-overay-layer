package com.example.myapplication

import android.app.Presentation
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * 一种特殊的[View]，能将普通[View]渲染成[SurfaceView]的形式
 * 可以用作播放视频时的中间叠层
 * 使用[Presentation]和[VirtualDisplay]实现
 * 在渲染[View]时传统方式无差异，同样支持硬件加速，并且在主线程执行
 * 但是他不是一个[ViewGroup]，因为实际的[View]是在另一个[android.view.Window]渲染的
 * 需要访问渲染的内容时，使用[contentView]和[setContentView]
 * */
class SurfaceMediaOverlayLayer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "SurfaceMediaOverlay"
    }

    private val mContentView = object : FrameLayout(context) {
        override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            super.requestDisallowInterceptTouchEvent(disallowIntercept)
            this@SurfaceMediaOverlayLayer.parent.requestDisallowInterceptTouchEvent(
                disallowIntercept
            )
        }
    }
    private var mVirtualDisplayPresentation: VirtualDisplayPresentation? = null
    val contentView: View
        get() = mContentView

    fun setContentView(view: View) {
        mContentView.removeAllViews()
        mContentView.addView(view)
    }

    private data class VirtualDisplayPresentation(
        val virtualDisplay: VirtualDisplay,
        val presentation: Presentation
    )

    init {
        val array = context.obtainStyledAttributes(
            attrs,
            R.styleable.SurfaceMediaOverlayLayer,
            defStyleAttr,
            0
        )
        val layoutId = array.getResourceId(R.styleable.SurfaceMediaOverlayLayer_layout, NO_ID)
        array.recycle()
        if (layoutId != NO_ID) {
            LayoutInflater.from(context).inflate(
                layoutId,
                mContentView, true
            )
        }
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                val displayManager =
                    context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val virtualDisplay = displayManager.createVirtualDisplay(
                    TAG,
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi,
                    holder.surface,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                )
                val presentation = Presentation(
                    context,
                    virtualDisplay.display
                )
                presentation.setContentView(mContentView)
                presentation.show()
                presentation.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                mVirtualDisplayPresentation =
                    VirtualDisplayPresentation(virtualDisplay, presentation)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                val renderer = mVirtualDisplayPresentation ?: return
                renderer.virtualDisplay.resize(
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi
                )
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                val renderer = mVirtualDisplayPresentation ?: return
                renderer.presentation.dismiss()
                renderer.virtualDisplay.release()
                val parent = mContentView.parent
                if (parent is ViewGroup) {
                    parent.removeView(mContentView)
                }
            }

        })
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        return mContentView.dispatchTouchEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        val renderer = mVirtualDisplayPresentation
        if (renderer != null && newConfig != null) {
            renderer.virtualDisplay.resize(
                width,
                height,
                context.resources.displayMetrics.densityDpi
            )
        }
    }
}