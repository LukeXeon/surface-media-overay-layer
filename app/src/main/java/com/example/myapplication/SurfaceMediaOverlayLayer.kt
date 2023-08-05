package com.example.myapplication

import android.app.Presentation
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout

/**
 * 一种特殊的[View]，能将普通[View]渲染成[SurfaceView]的形式
 * 可以用作播放视频时视频层和弹幕层之间的中间叠层（这里讨论的是大家都用[SurfaceView]渲染的情况）
 * 使用[Presentation]和[VirtualDisplay]实现
 * 在渲染[View]时与传统方式无差异，同样支持硬件加速，并且在主线程执行
 * 但是他不是一个[ViewGroup]，因为实际的[View]是在另[Presentation]的[android.view.Window]中渲染的
 * 需要访问渲染的内容时，使用[containerView]和[setContentView]
 * 注：[Presentation]是一种特殊的[android.app.Dialog]，他允许你指定他渲染到的[android.view.Surface]
 * 已知问题：在部分模拟器上他与overScroll有冲突，请设置外侧可滑动布局android:overScrollMode="never"
 * */
class SurfaceMediaOverlayLayer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "SurfaceMediaOverlay"
        private const val MSG_POST_DISMISS = 101
    }

    private data class VirtualDisplayPresentation(
        val virtualDisplay: VirtualDisplay,
        val presentation: Presentation
    )

    private class ContainerView(
        private val renderLayer: SurfaceMediaOverlayLayer
    ) : FrameLayout(renderLayer.context) {
        override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            super.requestDisallowInterceptTouchEvent(disallowIntercept)
            renderLayer.parent.requestDisallowInterceptTouchEvent(
                disallowIntercept
            )
        }
    }

    private val mHandler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            if (msg.what == MSG_POST_DISMISS) {
                val presentation = msg.obj as VirtualDisplayPresentation
                presentation.presentation.dismiss()
                presentation.virtualDisplay.release()
            }
        }
    }
    private val mContainerView = ContainerView(this)
    private var mVirtualDisplayPresentation: VirtualDisplayPresentation? = null
    val containerView: ViewGroup
        get() = mContainerView

    fun setContentView(view: View) {
        mContainerView.removeAllViews()
        mContainerView.addView(view)
    }

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
                mContainerView, true
            )
        }
        holder.setFormat(PixelFormat.TRANSPARENT)
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
                presentation.setContentView(mContainerView)
                presentation.setCancelable(false)
                presentation.show()
                presentation.window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
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
                val presentation = mVirtualDisplayPresentation ?: return
                presentation.virtualDisplay.resize(
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi
                )
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                val presentation = mVirtualDisplayPresentation ?: return
                mHandler.obtainMessage(MSG_POST_DISMISS, presentation).sendToTarget()
                val parent = mContainerView.parent
                if (parent is ViewGroup) {
                    parent.removeView(mContainerView)
                }
                mVirtualDisplayPresentation = null
            }
        })
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        return mContainerView.dispatchTouchEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        val presentation = mVirtualDisplayPresentation
        if (presentation != null && newConfig != null) {
            presentation.virtualDisplay.resize(
                width,
                height,
                context.resources.displayMetrics.densityDpi
            )
        }
    }
}