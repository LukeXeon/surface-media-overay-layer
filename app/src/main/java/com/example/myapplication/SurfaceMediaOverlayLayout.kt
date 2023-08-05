package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import java.lang.ref.WeakReference

class SurfaceMediaOverlayLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "SurfaceMediaOverlay"
    }

    private val mContent: View
    private val mRenderLayer = RenderLayer(context)
    private val mRenderTrigger: View

    var isUseSurfaceRenderLayer: Boolean
        get() = mRenderLayer.visibility == View.VISIBLE
        set(value) {
            mRenderLayer.visibility = if (value) View.VISIBLE else View.GONE
        }

    init {
        val array = context.obtainStyledAttributes(
            attrs,
            R.styleable.SurfaceMediaOverlayLayout,
            defStyleAttr,
            0
        )
        try {
            val id = array.getResourceId(R.styleable.SurfaceMediaOverlayLayout_layout, NO_ID)
            mContent = if (id != NO_ID) {
                LayoutInflater.from(context).inflate(id, this, false)
            } else {
                throw IllegalArgumentException("no layout id")
            }
            addView(
                mContent,
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                )
            )
        } finally {
            array.recycle()
        }
        addView(
            mRenderLayer,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )
        mRenderTrigger = object : View(context) {
            init {
                setWillNotDraw(false)
            }

            @SuppressLint("MissingSuperCall")
            override fun draw(canvas: Canvas) {
                mRenderLayer.notifyViewUpdated(mContent)
            }

            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                setMeasuredDimension(0, 0)
            }
        }
        addView(
            mRenderTrigger,
            LayoutParams(
                0,
                0
            )
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        children.forEach {
            it.measure(
                getChildMeasureSpec(
                    widthMeasureSpec,
                    0,
                    it.layoutParams.width
                ),
                getChildMeasureSpec(
                    heightMeasureSpec,
                    0,
                    it.layoutParams.height
                )
            )
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        children.forEach {
            it.layout(0, 0, it.measuredWidth, it.measuredHeight)
        }
    }

    override fun onDescendantInvalidated(child: View, target: View) {
        super.onDescendantInvalidated(child, target)
        if (isUseSurfaceRenderLayer && mRenderLayer != child && mRenderTrigger != child) {
            mRenderTrigger.invalidate()
        }
    }


    private class RenderLayer constructor(
        context: Context,
    ) : SurfaceView(context) {

        companion object {
            private const val MSG_DROP_FIRST = 101
            private const val MSG_DRAW_VIEW = 102
        }


        private var mRenderThreadHandler: Handler? = null
        private val mPreparedToDraw = arrayOfNulls<WeakReference<View>>(1)

        private fun lockSupportedCanvas(): Canvas? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.lockHardwareCanvas()
            } else {
                holder.lockCanvas()
            }
        }

        init {
            holder.setFormat(PixelFormat.TRANSPARENT)
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    val renderThreadHandler = object : Handler(
                        HandlerThread(TAG).apply {
                            start()
                        }.looper
                    ) {
                        override fun handleMessage(msg: Message) {
                            when (msg.what) {
                                MSG_DROP_FIRST -> {
                                    try {
                                        val canvas = lockSupportedCanvas() ?: return
                                        canvas.drawColor(
                                            Color.TRANSPARENT,
                                            PorterDuff.Mode.CLEAR
                                        )
                                        holder.unlockCanvasAndPost(canvas)
                                    } catch (e: Throwable) {
                                        Log.e(TAG, "draw error", e)
                                    }
                                }

                                MSG_DRAW_VIEW -> {
                                    val targetView = synchronized(mPreparedToDraw) {
                                        val view = mPreparedToDraw[0]?.get()
                                        mPreparedToDraw[0] = null
                                        view
                                    } ?: return
                                    try {
                                        val canvas = lockSupportedCanvas() ?: return
                                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                                        targetView.draw(canvas)
                                        holder.unlockCanvasAndPost(canvas)
                                    } catch (e: Throwable) {
                                        Log.e(TAG, "draw error", e)
                                    }
                                }
                            }
                        }
                    }
                    renderThreadHandler.sendEmptyMessage(MSG_DROP_FIRST)
                    mRenderThreadHandler = renderThreadHandler
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {

                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    mRenderThreadHandler?.looper?.quit()
                    mRenderThreadHandler = null
                }
            })
        }

        fun notifyViewUpdated(view: View): Boolean {
            val renderThreadHandler = mRenderThreadHandler
            if (renderThreadHandler != null) {
                synchronized(mPreparedToDraw) {
                    if (mPreparedToDraw[0]?.get() != view) {
                        mPreparedToDraw[0] = WeakReference(view)
                    }
                }
                renderThreadHandler.sendEmptyMessage(MSG_DRAW_VIEW)
                return true
            }
            return false
        }
    }
}