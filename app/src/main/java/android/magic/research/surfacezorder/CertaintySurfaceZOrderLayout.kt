package android.magic.research.surfacezorder

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.children

class CertaintySurfaceZOrderLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    companion object {
        private val sReAttachToWindowMethodSequence by lazy {
            sequenceOf(
                "onDetachedFromWindow",
                "onAttachedToWindow"
            ).map {
                View::class.java.runCatching {
                    getDeclaredMethod(it)
                }.getOrNull()
            }.filterNotNull().onEach {
                it.isAccessible = true
            }.toList()
        }
    }

    private val mSurfaceZOrder = ArrayList<SurfaceView>()
    private val mTempViews = ArrayList<SurfaceView>()
    private val mChildViewAttachStateListener = object : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            if (v is SurfaceView) {
                mSurfaceZOrder.ensureCapacity(childCount)
                // 从8.0开始系统悄悄的改变了排序规则
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    mSurfaceZOrder.add(0, v)
                } else {
                    mSurfaceZOrder.add(v)
                }
                mTempViews.addAll(children.filterIsInstance<SurfaceView>())
                if (mTempViews.all { it.isAttachedToWindow }) {
                    var isNeedSync = false
                    for (i in mSurfaceZOrder.indices) {
                        val viewOrder = mTempViews[i]
                        val surfaceOrder = mSurfaceZOrder[i]
                        if (viewOrder != surfaceOrder) {
                            isNeedSync = true
                            break
                        }
                    }
                    if (isNeedSync) {
                        mSurfaceZOrder.forEach { view ->
                            sReAttachToWindowMethodSequence.runCatching {
                                forEach {
                                    it.invoke(view)
                                }
                            }
                            val tempVisibility = view.visibility
                            view.visibility = View.GONE
                            view.visibility = tempVisibility
                            view.viewTreeObserver.dispatchOnPreDraw()
                        }
                        invalidate()
                    }
                }
                mTempViews.clear()
            }
        }

        override fun onViewDetachedFromWindow(v: View) {
            if (v is SurfaceView) {
                mSurfaceZOrder.remove(v)
            }
        }
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        if (child is SurfaceView) {
            child.addOnAttachStateChangeListener(mChildViewAttachStateListener)
        } else {
            throw IllegalArgumentException("Only support SurfaceView")
        }
    }

    override fun onViewRemoved(child: View) {
        super.onViewRemoved(child)
        if (child is SurfaceView) {
            child.removeOnAttachStateChangeListener(mChildViewAttachStateListener)
        } else {
            throw IllegalArgumentException("Only support SurfaceView")
        }
    }

    override fun bringChildToFront(child: View?) {
        child ?: return
        val hasAction = child is SurfaceView && childCount > 0
                && indexOfChild(child) != childCount - 1
        if (hasAction) {
            mChildViewAttachStateListener.onViewDetachedFromWindow(child)
        }
        super.bringChildToFront(child)
        if (hasAction) {
            mChildViewAttachStateListener.onViewAttachedToWindow(child)
        }
    }
}