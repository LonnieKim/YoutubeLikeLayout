package com.fiberthemax.youtubelikelayout

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.OverScroller
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class TopLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), CoordinatorLayout.AttachedBehavior {

    init {
        isClickable = true
        isFocusableInTouchMode = true
    }

    private val behavior = Behavior(context)

    override fun getBehavior(): CoordinatorLayout.Behavior<*> {
        return behavior
    }

    internal class Behavior(context: Context) : CoordinatorLayout.Behavior<TopLayout>() {
        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private var lastMotionY: Int = 0
        private var viewCaptured = false
        private var isBeingDragged = false

        private var velocityTracker: VelocityTracker? = null
        private val scroller = OverScroller(context)
        private var flingRunnable: Runnable? = null

        var left: Int = 0
            private set
        var right: Int = 0
            private set
        var bottom: Int = 0
            private set
        var top: Int = 0
            private set
        private var minTop: Int = 0
        private var maxTop: Int = 0

        override fun onInterceptTouchEvent(
            parent: CoordinatorLayout,
            child: TopLayout,
            ev: MotionEvent
        ): Boolean {
            if (DEBUG) Log.d(
                TAG,
                "onInterceptTouchEvent : ${child.javaClass.simpleName}, $ev, ${child.javaClass.simpleName}"
            )
            val x = ev.x.toInt()
            val y = ev.y.toInt()
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    //TODO add capture behavior to side view
                    if (parent.isPointInChildBounds(child, x, y)) {
                        if (!scroller.isFinished) scroller.abortAnimation()
                        viewCaptured = true
                        lastMotionY = y
                        ensureVelocityTracker()
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    val yDiff = abs(y - lastMotionY)
                    if (yDiff > touchSlop && viewCaptured) {
                        lastMotionY = y
                        isBeingDragged = true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    resetDrag()
                    recycleVelocityTracker()
                }
            }
            velocityTracker?.addMovement(ev)
            return isBeingDragged
        }

        override fun onTouchEvent(
            parent: CoordinatorLayout,
            child: TopLayout,
            ev: MotionEvent
        ): Boolean {
            if (DEBUG) Log.d(
                TAG,
                "onTouchEvent : ${child.javaClass.simpleName}, $ev, ${child.javaClass.simpleName}"
            )
            val x = ev.x.toInt()
            val y = ev.y.toInt()
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (parent.isPointInChildBounds(child, x, y)) {
                        if (!scroller.isFinished) scroller.abortAnimation()
                        viewCaptured = true
                        lastMotionY = y
                        ensureVelocityTracker()
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isBeingDragged) {
                        val yDiff = abs(y - lastMotionY)
                        if (yDiff > touchSlop && viewCaptured) {
                            lastMotionY = y
                            isBeingDragged = true
                        }
                    } else {
                        val dy = y - lastMotionY
                        setTop(parent, child, top + dy)
                        lastMotionY = y
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    velocityTracker?.let {
                        it.computeCurrentVelocity(1000)
                        fling(parent, child, it.yVelocity)
                    }
                    resetDrag()
                    recycleVelocityTracker()
                }
            }
            velocityTracker?.addMovement(ev)
            return super.onTouchEvent(parent, child, ev)
        }

        private fun fling(
            coordinatorLayout: CoordinatorLayout,
            child: TopLayout,
            velocityY: Float
        ): Boolean {
            if (DEBUG) Log.d(TAG, "Fling : $velocityY")
            if (flingRunnable != null) {
                child.removeCallbacks(flingRunnable)
                flingRunnable = null
            }
            if (velocityY > 0) {
                scroller.fling(
                    0,
                    top,  // startY
                    0,
                    max(velocityY.roundToInt(), 8000),  // velocityY.
                    0,
                    0,  // x
                    maxTop,
                    maxTop // y
                )
            } else {
                scroller.fling(
                    0,
                    top,  // startY
                    0,
                    min(velocityY.roundToInt(), -8000),  // velocityY.
                    0,
                    0,  // x
                    0,
                    0 // y
                )
            }
            return if (scroller.computeScrollOffset()) {
                ViewCompat.postOnAnimation(
                    child,
                    FlingRunnable(coordinatorLayout, child)
                        .also { flingRunnable = it }
                )
                true
            } else {
                onFlingFinished(coordinatorLayout, child)
                false
            }
        }

        private fun resetDrag() {
            viewCaptured = false
            isBeingDragged = false
            lastMotionY = 0
        }

        override fun onMeasureChild(
            parent: CoordinatorLayout, child: TopLayout,
            parentWidthMeasureSpec: Int, widthUsed: Int,
            parentHeightMeasureSpec: Int, heightUsed: Int
        ): Boolean {
            if (DEBUG) Log.d(
                TAG,
                "onMeasureChild : ${
                    child.javaClass.simpleName
                },${
                    parentWidthMeasureSpec.measureSpecToString()
                }, $widthUsed,${
                    parentHeightMeasureSpec.measureSpecToString()
                }, $heightUsed"
            )

            minTop = 0
            maxTop = parent.height - MINI_HEIGHT

            parent.onMeasureChild(child, parentWidthMeasureSpec, 0, parentHeightMeasureSpec, 0)

            val heightRatio = (1f - ((top.toFloat() - minTop) / (maxTop - minTop)))
            val widthRatio = if (heightRatio < 0.05f) heightRatio / 0.05f
            else 1f

            val newWidth =
                (MINI_WIDTH + (child.measuredWidth - MINI_WIDTH) * widthRatio).toInt()
            val newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY)

            val newHeight =
                (MINI_HEIGHT + (child.measuredHeight - MINI_HEIGHT) * heightRatio).toInt()
            val newHeightMeasureSpec =
                MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY)

            child.measure(newWidthMeasureSpec, newHeightMeasureSpec)

            left = 0
            top = top
            right = left + child.measuredWidth
            bottom = top + child.measuredHeight

            return true
        }

        override fun onLayoutChild(
            parent: CoordinatorLayout,
            child: TopLayout,
            layoutDirection: Int
        ): Boolean {
            if (DEBUG) Log.d(
                TAG,
                "onLayoutChild : ${child.javaClass.simpleName}, $layoutDirection"
            )
            child.layout(left, top, right, bottom)
            if (DEBUG) Log.d(
                TAG,
                "child.layout : $left, $top, $right, $bottom"
            )
            return true
        }

        private fun setTop(parent: CoordinatorLayout, topLayout: TopLayout, top: Int) {
            this.top = top.coerceAtLeast(minTop).coerceAtMost(maxTop)
            topLayout.requestLayout()
        }


        private fun ensureVelocityTracker() {
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain()
            }
        }

        private fun recycleVelocityTracker() {
            if (velocityTracker != null) {
                velocityTracker!!.recycle()
                velocityTracker = null
            }
        }

        fun onFlingFinished(parent: CoordinatorLayout?, child: TopLayout) {
            // no-op
        }

        private inner class FlingRunnable(
            private val parent: CoordinatorLayout,
            private val child: TopLayout
        ) : Runnable {
            override fun run() {
                if (scroller.computeScrollOffset()) {
                    setTop(parent, child, scroller.currY)
                    ViewCompat.postOnAnimation(child, this)
                } else {
                    onFlingFinished(parent, child)
                }
            }
        }

        companion object {
            private const val TAG = "Behavior"

            //TODO move to attrs
            private val MINI_WIDTH = 100.dpToPx()
            private val MINI_HEIGHT = 50.dpToPx()

            @Suppress("SimplifyBooleanWithConstants")
            private val DEBUG = BuildConfig.DEBUG && true
        }
    }
}