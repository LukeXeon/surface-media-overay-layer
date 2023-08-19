package android.magic.research.surfacezorder

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.SurfaceView
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.core.view.allViews
import androidx.core.view.ancestors
import java.util.WeakHashMap

class CertaintySurfaceZOrderLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    companion object {
        private val RE_ATTACH_TO_WINDOW_METHOD_SEQUENCE by lazy {
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

    private var mHasNewLayout = false
    private lateinit var mSortedSurfaceViews: WeakHashMap<SurfaceView, Int>
    private lateinit var mTempSurfaceViews: Array<ArrayList<SurfaceView>>
    private lateinit var mOnGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mHasNewLayout = true
    }

    private fun onPostLayout() {
        if (!this::mTempSurfaceViews.isInitialized) {
            mTempSurfaceViews = Array(2) { ArrayList(childCount) }
        }
        mTempSurfaceViews[0].addAll(allViews.filterIsInstance<SurfaceView>())
        if (!this::mSortedSurfaceViews.isInitialized) {
            mSortedSurfaceViews = WeakHashMap()
        } else {
            mTempSurfaceViews[1].ensureCapacity(mSortedSurfaceViews.size)
            mTempSurfaceViews[1].addAll(
                mSortedSurfaceViews.entries.asSequence().sortedBy { it.value }.map { it.key }
            )
        }
        if (mTempSurfaceViews.all { it.isNotEmpty() } && mTempSurfaceViews[0] != mTempSurfaceViews[1]) {
            for (index in mTempSurfaceViews[0].indices) {
                mSortedSurfaceViews[mTempSurfaceViews[0][index]] = index
                val order = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    // 从8.0开始系统悄悄的改变了排序规则
                    mTempSurfaceViews[0].lastIndex - index
                } else {
                    index
                }
                val surfaceView = mTempSurfaceViews[0][order]
                val tempVisibility = surfaceView.visibility
                surfaceView.visibility = View.GONE
                RE_ATTACH_TO_WINDOW_METHOD_SEQUENCE.runCatching {
                    forEach {
                        it.invoke(surfaceView)
                    }
                }
                surfaceView.visibility = tempVisibility
            }
            viewTreeObserver.dispatchOnPreDraw()
        }
        mTempSurfaceViews.forEach {
            it.clear()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!ancestors.any { it is CertaintySurfaceZOrderLayout }) {
            if (!this::mOnGlobalLayoutListener.isInitialized) {
                mOnGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                    if (mHasNewLayout) {
                        mHasNewLayout = false
                        onPostLayout()
                    }
                }
            }
            viewTreeObserver.addOnGlobalLayoutListener(mOnGlobalLayoutListener)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (this::mOnGlobalLayoutListener.isInitialized) {
            viewTreeObserver.removeOnGlobalLayoutListener(mOnGlobalLayoutListener)
        }
        if (this::mTempSurfaceViews.isInitialized) {
            mTempSurfaceViews.forEach {
                it.clear()
            }
        }
        if (this::mSortedSurfaceViews.isInitialized) {
            mSortedSurfaceViews.clear()
        }
    }
}