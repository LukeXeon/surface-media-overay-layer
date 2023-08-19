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
import kotlin.math.max

class CertaintySurfaceZOrderLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private lateinit var mZOrderSorter: ZOrderSorter

    private class ZOrderSorter(private val mHost: View) : ViewTreeObserver.OnGlobalLayoutListener {
        companion object {
            private val REATTACH_TO_WINDOW_METHOD_SEQUENCE = sequenceOf(
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

        private var mHasNewLayout = false
        private val mTempNewSurfaceViews = ArrayList<SurfaceView>()
        private val mTempOldSurfaceViews = ArrayList<SurfaceView>()
        private val mSortedSurfaceViews = WeakHashMap<SurfaceView, Int>()

        fun markLayout() {
            mHasNewLayout = true
        }

        fun clear() {
            mHasNewLayout = false
            mSortedSurfaceViews.clear()
            mTempOldSurfaceViews.clear()
            mTempNewSurfaceViews.clear()
        }

        override fun onGlobalLayout() {
            if (!mHasNewLayout) {
                return
            }
            mHasNewLayout = false
            mTempNewSurfaceViews.addAll(mHost.allViews.filterIsInstance<SurfaceView>())
            mTempOldSurfaceViews.apply {
                ensureCapacity(mSortedSurfaceViews.size)
                addAll(
                    mSortedSurfaceViews.entries.asSequence().sortedBy { it.value }.map { it.key }
                )
            }
            if (max(mTempOldSurfaceViews.size, mTempNewSurfaceViews.size) > 0
                && mTempOldSurfaceViews != mTempNewSurfaceViews
            ) {
                for (index in mTempNewSurfaceViews.indices) {
                    mSortedSurfaceViews[mTempNewSurfaceViews[index]] = index
                    val order = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        // 从8.0开始系统悄悄的改变了排序规则
                        mTempNewSurfaceViews.lastIndex - index
                    } else {
                        index
                    }
                    val surfaceView = mTempNewSurfaceViews[order]
                    val tempVisibility = surfaceView.visibility
                    surfaceView.visibility = View.GONE
                    REATTACH_TO_WINDOW_METHOD_SEQUENCE.runCatching {
                        forEach {
                            it.invoke(surfaceView)
                        }
                    }
                    surfaceView.visibility = tempVisibility
                }
            }
            mTempOldSurfaceViews.clear()
            mTempNewSurfaceViews.clear()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (this::mZOrderSorter.isInitialized) {
            mZOrderSorter.markLayout()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!ancestors.any { it is CertaintySurfaceZOrderLayout }) {
            if (!this::mZOrderSorter.isInitialized) {
                mZOrderSorter = ZOrderSorter(this)
            }
            viewTreeObserver.addOnGlobalLayoutListener(mZOrderSorter)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (this::mZOrderSorter.isInitialized) {
            viewTreeObserver.removeOnGlobalLayoutListener(mZOrderSorter)
            mZOrderSorter.clear()
        }
    }
}