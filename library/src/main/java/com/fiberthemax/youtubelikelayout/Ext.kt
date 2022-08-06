package com.fiberthemax.youtubelikelayout

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import java.lang.IllegalStateException

internal fun Int.dpToPx(): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()
}

internal fun Int.measureSpecToString(): String {
    return buildString {
        append("[")
        append(
            when (View.MeasureSpec.getMode(this@measureSpecToString)) {
                View.MeasureSpec.AT_MOST -> "AT_MOST"
                View.MeasureSpec.EXACTLY -> "EXACTLY"
                View.MeasureSpec.UNSPECIFIED -> "UNSPECIFIED"
                else -> throw IllegalStateException("Invalid MeasureSpec")
            }
        )
        append(", ")
        append(View.MeasureSpec.getSize(this@measureSpecToString))
        append("]")
    }
}
