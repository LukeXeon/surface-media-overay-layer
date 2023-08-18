package android.magic.research.surfacezorder

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.allViews
import androidx.core.view.ancestors
import java.util.WeakHashMap

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

    private val mSortedSurfaceViews = WeakHashMap<SurfaceView, Int>()
    private val mTempSurfaceViews = Array(2) { ArrayList<SurfaceView>() }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!ancestors.any { it is CertaintySurfaceZOrderLayout }) {
            mTempSurfaceViews[0].clear()
            mTempSurfaceViews[0].addAll(allViews.filterIsInstance<SurfaceView>())
            mTempSurfaceViews[1].ensureCapacity(mSortedSurfaceViews.size)
            mTempSurfaceViews[1].addAll(
                mSortedSurfaceViews.entries.asSequence().sortedBy { it.value }.map { it.key }
            )
            if (mTempSurfaceViews[0] != mTempSurfaceViews[1]) {
                mTempSurfaceViews[0].forEachIndexed { index, surfaceView ->
                    mSortedSurfaceViews[surfaceView] = index
                }
                var orderList: List<SurfaceView> = mTempSurfaceViews[0]
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    // 从8.0开始系统悄悄的改变了排序规则
                    orderList = mTempSurfaceViews[0].asReversed()
                }
                orderList.forEach { view ->
                    val tempVisibility = view.visibility
                    view.visibility = View.GONE
                    sReAttachToWindowMethodSequence.runCatching {
                        forEach {
                            it.invoke(view)
                        }
                    }
                    view.visibility = tempVisibility
                }
                viewTreeObserver.dispatchOnPreDraw()
            }
            mTempSurfaceViews.forEach {
                it.clear()
            }
        } else {
            clearAllState()
        }
    }

    private fun clearAllState() {
        mTempSurfaceViews.forEach {
            it.clear()
        }
        mSortedSurfaceViews.clear()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearAllState()
    }
}