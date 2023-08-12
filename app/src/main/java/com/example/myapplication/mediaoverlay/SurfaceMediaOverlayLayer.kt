package com.example.myapplication.mediaoverlay

import android.app.Presentation
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.VirtualDisplay
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

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
) : SurfaceView(context, attrs, defStyleAttr), LifecycleOwner {
    private class ContainerView(private val renderLayer: SurfaceMediaOverlayLayer) :
        FrameLayout(renderLayer.context) {
        override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            super.requestDisallowInterceptTouchEvent(disallowIntercept)
            renderLayer.parent.requestDisallowInterceptTouchEvent(
                disallowIntercept
            )
        }
    }

    private val mLayerMetrics = MutableStateFlow<LayerMetrics?>(null)
    private val mLifecycle = LifecycleRegistry(this)
    private val mContainerView = ContainerView(this)
    val containerView: ViewGroup
        get() = mContainerView

    fun setContentView(view: View) {
        mContainerView.removeAllViews()
        mContainerView.addView(view)
    }

    override fun onAttachedToWindow() {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDetachedFromWindow()
    }

    override fun getLifecycle(): Lifecycle {
        return mLifecycle
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
                mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                mLayerMetrics.value = LayerMetrics(
                    width,
                    height,
                    context.resources.configuration.densityDpi
                )
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                mLayerMetrics.value = null
            }
        })
        mLifecycle.coroutineScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val viewTreeRenderDelegate = ViewTreeRenderDelegate(
                    context,
                    this@SurfaceMediaOverlayLayer.toString(),
                    holder.surface,
                    containerView
                )
                try {
                    mLayerMetrics.filterNotNull()
                        .distinctUntilChanged()
                        .collectLatest {
                            viewTreeRenderDelegate.resize(it)
                        }
                } finally {
                    viewTreeRenderDelegate.dismiss()
                }
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        return mContainerView.dispatchTouchEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        if (mLifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mLayerMetrics.value = LayerMetrics(
                width,
                height,
                context.resources.configuration.densityDpi
            )
        }
    }
}