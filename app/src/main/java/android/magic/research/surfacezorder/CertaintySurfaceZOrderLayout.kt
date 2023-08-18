package android.magic.research.surfacezorder

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.allViews
import androidx.core.view.ancestors

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

    private val mSortedSurfaceViews = ArrayList<SurfaceView>()
    private val mAllSurfaceViews = ArrayList<SurfaceView>()

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!ancestors.any { it is CertaintySurfaceZOrderLayout }) {
            mAllSurfaceViews.clear()
            mAllSurfaceViews.addAll(allViews.filterIsInstance<SurfaceView>())
            if (mAllSurfaceViews != mSortedSurfaceViews) {
                mSortedSurfaceViews.clear()
                mSortedSurfaceViews.addAll(mAllSurfaceViews)
                // 从8.0开始系统悄悄的改变了排序规则
                val orderList = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    mSortedSurfaceViews.asReversed()
                } else {
                    mSortedSurfaceViews
                }
                orderList.forEach { view ->
                    sReAttachToWindowMethodSequence.runCatching {
                        forEach {
                            it.invoke(view)
                        }
                    }
                    val tempVisibility = view.visibility
                    view.visibility = View.GONE
                    view.visibility = tempVisibility
                }
                viewTreeObserver.dispatchOnPreDraw()
            }
        } else {
            mAllSurfaceViews.clear()
            mSortedSurfaceViews.clear()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mSortedSurfaceViews.clear()
        mAllSurfaceViews.clear()
    }
}