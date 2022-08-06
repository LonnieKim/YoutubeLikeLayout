package com.fiberthemax.youtubelikelayout

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout

class SideViewBehavior(context: Context, attrs: AttributeSet) :
    CoordinatorLayout.Behavior<View>(context, attrs) {
    private var topLayoutBehavior: TopLayout.Behavior? = null
    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        if (DEBUG) Log.d(
            TAG,
            "layoutDependsOn : ${child.javaClass.simpleName}, ${dependency.javaClass.simpleName}"
        )
        return dependency is TopLayout
    }

    @SuppressLint("RestrictedApi")
    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        if (DEBUG) Log.d(
            TAG,
            "onDependentViewChanged : ${child.javaClass.simpleName}, ${dependency.javaClass.simpleName}"
        )
        this.topLayoutBehavior = (dependency as? TopLayout)?.behavior as? TopLayout.Behavior
        child.requestLayout()
        return true
    }

    override fun onDependentViewRemoved(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ) {
        this.topLayoutBehavior = null
        child.requestLayout()
    }

    override fun onMeasureChild(
        parent: CoordinatorLayout,
        child: View,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
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

        //TODO consider margin
        val topLayoutBehavior = this.topLayoutBehavior
        if (topLayoutBehavior != null) {
            val childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                parent.width - topLayoutBehavior.right,
                View.MeasureSpec.EXACTLY
            )

            val childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                topLayoutBehavior.bottom - topLayoutBehavior.top,
                View.MeasureSpec.EXACTLY
            )
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
        }
        return true
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: View,
        layoutDirection: Int
    ): Boolean {
        if (DEBUG) Log.d(
            TAG,
            "onLayoutChild : ${child.javaClass.simpleName}, $layoutDirection"
        )
        val topLayoutBehavior = this.topLayoutBehavior
        val left = topLayoutBehavior?.right ?: 0
        val top = topLayoutBehavior?.top ?: 0
        val right = left + child.measuredWidth
        val bottom = topLayoutBehavior?.bottom ?: 0
        child.layout(left, top, right, bottom)
        if (DEBUG) Log.d(
            TAG,
            "child.layout : $left, $top, $right, $bottom"
        )
        return true
    }

    companion object {
        private const val TAG = "SideViewBehavior"

        @Suppress("SimplifyBooleanWithConstants")
        private val DEBUG = BuildConfig.DEBUG && true
    }
}