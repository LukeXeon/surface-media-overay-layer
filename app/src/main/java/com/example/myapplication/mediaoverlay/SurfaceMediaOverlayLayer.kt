package com.example.myapplication.mediaoverlay

import android.app.Presentation
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Space
import com.example.myapplication.R

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

    private inner class VirtualDisplayPresentation {
        private val mDisplayManager = context.getSystemService(
            Context.DISPLAY_SERVICE
        ) as DisplayManager
        private val mVirtualDisplay = mDisplayManager.createVirtualDisplay(
            this@SurfaceMediaOverlayLayer.toString(),
            width,
            height,
            context.resources.configuration.densityDpi,
            holder.surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
        )
        private val mPresentation = createNewPresentation()
        private val mDismissAction = Runnable {
            mPresentation.dismiss()
            mVirtualDisplay.release()
        }
        private val mHandler = Handler(Looper.myLooper()!!)

        /**
         * 这里要异步销毁，等待系统将窗口Stop完，否则会崩在系统里，（这咖喱味的代码...）
         * */
        fun dispose() {
            if (mVirtualDisplay.surface != null) {
                mVirtualDisplay.surface = null
                mPresentation.setContentView(Space(context))
                val parent = mContainerView.parent
                if (parent is ViewGroup) {
                    parent.removeView(mContainerView)
                }
                mHandler.post(mDismissAction)
            }
        }

        fun resize() {
            if (mVirtualDisplay.surface != null) {
                mVirtualDisplay.resize(
                    width,
                    height,
                    context.resources.configuration.densityDpi,
                )
                invalidate()
                mContainerView.invalidate()
            }
        }

        private fun createNewPresentation(): AppCompatPresentation {
            val presentation = AppCompatPresentation(
                context,
                mVirtualDisplay.display
            )
            presentation.window?.setBackgroundDrawable(null)
            presentation.setCancelable(false)
            val parent = mContainerView.parent
            if (parent is ViewGroup) {
                parent.removeView(mContainerView)
            }
            presentation.setContentView(mContainerView)
            presentation.show()
            return presentation
        }
    }

    private class ContainerView(private val renderLayer: SurfaceMediaOverlayLayer) :
        FrameLayout(renderLayer.context) {
        override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            super.requestDisallowInterceptTouchEvent(disallowIntercept)
            renderLayer.parent.requestDisallowInterceptTouchEvent(
                disallowIntercept
            )
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
                mVirtualDisplayPresentation = VirtualDisplayPresentation()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                mVirtualDisplayPresentation?.resize()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                val presentation = mVirtualDisplayPresentation ?: return
                presentation.dispose()
                mVirtualDisplayPresentation = null
            }
        })
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        return mContainerView.dispatchTouchEvent(event)
    }
}