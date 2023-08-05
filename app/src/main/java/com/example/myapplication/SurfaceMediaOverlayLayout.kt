package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Picture
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.RecordingCanvas
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.PixelCopy
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.core.util.Pools
import androidx.core.view.children

class SurfaceMediaOverlayLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "SurfaceMediaOverlay"
    }

    private val mContent: View
    private val mRenderLayer = RenderLayer(context)
    private val mTakeCaptureDelegate: View

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
        mTakeCaptureDelegate = object : View(context) {
            init {
                setWillNotDraw(false)
            }

            @SuppressLint("MissingSuperCall")
            override fun draw(canvas: Canvas) {
                mRenderLayer.takeCapture(mContent)
            }

            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                setMeasuredDimension(0, 0)
            }
        }
        addView(
            mTakeCaptureDelegate,
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
        if (mRenderLayer != child && mTakeCaptureDelegate != child) {
            mTakeCaptureDelegate.invalidate()
        }
    }

    private class RenderLayer constructor(
        context: Context,
    ) : SurfaceView(context) {

        companion object {
            private const val MSG_DROP_FIRST = 101
            private const val MSG_DRAW_PICTURE = 102
        }

        private class Snapshot {
            val position = Point()
            val picture = Picture()
        }

        private var mRenderThreadHandler: Handler? = null
        private val mPreparedPool = Pools.SynchronizedPool<Snapshot>(1)
        private val mBackupPool = Pools.SynchronizedPool<Snapshot>(2)

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
                                        val canvas =
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                holder.lockHardwareCanvas()
                                            } else {
                                                holder.lockCanvas()
                                            }
                                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                                        holder.unlockCanvasAndPost(canvas)
                                    } catch (e: Throwable) {
                                        Log.e(TAG, "draw error", e)
                                    }
                                }

                                MSG_DRAW_PICTURE -> {
                                    val snapshot = mPreparedPool.acquire() ?: return
                                    try {
                                        val canvas =
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                holder.lockHardwareCanvas()
                                            } else {
                                                holder.lockCanvas()
                                            }
                                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                                        canvas.translate(
                                            snapshot.position.x.toFloat(),
                                            snapshot.position.y.toFloat(),
                                        )
                                        canvas.drawPicture(snapshot.picture)
                                        holder.unlockCanvasAndPost(canvas)
                                    } catch (e: Throwable) {
                                        Log.e(TAG, "draw error", e)
                                    } finally {
                                        mBackupPool.release(snapshot)
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

        fun takeCapture(view: View): Boolean {
            val renderThreadHandler = mRenderThreadHandler
            if (renderThreadHandler != null) {
                val snapshot = mPreparedPool.acquire() ?: mBackupPool.acquire() ?: Snapshot()
                snapshot.position.set(view.left, view.top)
                val canvas = snapshot.picture.beginRecording(view.width, view.height)
                view.draw(canvas)
                snapshot.picture.endRecording()
                mPreparedPool.release(snapshot)
                renderThreadHandler.sendEmptyMessage(MSG_DRAW_PICTURE)
                return true
            }
            return false
        }
    }
}