package com.example.myapplication.mediaoverlay

import android.app.Presentation
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.VirtualDisplay
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
 * */
class SurfaceMediaOverlayLayer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr) {

    private val mLayerMetrics = MutableStateFlow<LayerMetrics?>(null)
    private val mSurfaceLifecycleOwner = SurfaceLifecycleOwner()
    private val mContainerView = object : FrameLayout(getContext()) {
        override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            super.requestDisallowInterceptTouchEvent(disallowIntercept)
            this@SurfaceMediaOverlayLayer.parent.requestDisallowInterceptTouchEvent(
                disallowIntercept
            )
        }
    }
    val containerView: ViewGroup
        get() = mContainerView

    fun setContentView(view: View) {
        mContainerView.removeAllViews()
        mContainerView.addView(view)
    }

    init {
        val viewStub = ViewStub(context, attrs, defStyleAttr)
        if (viewStub.layoutResource != 0) {
            mContainerView.addView(viewStub)
            viewStub.inflate()
        }
        setZOrderMediaOverlay(true)
        addOnAttachStateChangeListener(mSurfaceLifecycleOwner)
        holder.setFormat(PixelFormat.TRANSPARENT)
        holder.addCallback(mSurfaceLifecycleOwner)
        holder.addCallback(object : SurfaceHolderCallbackAdapter {
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
        })
        mSurfaceLifecycleOwner.lifecycleScope.launch {
            mSurfaceLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val viewTreeRenderDelegate = ViewTreeRenderDelegate(
                    context,
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
        if (mSurfaceLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mLayerMetrics.value = LayerMetrics(
                width,
                height,
                context.resources.configuration.densityDpi
            )
        }
    }
}