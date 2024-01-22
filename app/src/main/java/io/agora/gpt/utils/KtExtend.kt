package io.agora.gpt.utils

import android.content.res.Resources
import android.util.TypedValue

val Number.dp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )

val Int.formatDurationTime
    get() = run {
        val hours = this / 3600
        val minutes = (this % 3600) / 60
        val remainingSeconds = this % 60
        if (hours == 0) {
            String.format("%02d:%02d", minutes, remainingSeconds)
        } else {
            String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
        }
    }
