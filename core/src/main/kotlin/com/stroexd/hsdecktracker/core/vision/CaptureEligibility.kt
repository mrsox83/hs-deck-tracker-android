package com.stroexd.hsdecktracker.core.vision

/** Projection may have a landscape app surface while unrelated window metrics report portrait. */
fun captureSkipReason(screenInteractive: Boolean, physicalWidth: Int, physicalHeight: Int,
    projection: Boolean = false, content: CaptureContentGeometry? = null, isActive: () -> Boolean): Int? {
    val validContent = content?.takeIf { it.width > 0 && it.height > 0 && it.width != it.height }
    return when {
        !screenInteractive -> 1
        projection && validContent != null && validContent.width < validContent.height -> 2
        // Unknown/stale source geometry conservatively retains the prior physical-display policy.
        (!projection || validContent == null) && physicalWidth < physicalHeight -> 2
        !isActive() -> 3
        else -> null
    }
}

/** Keep legacy mapping only in its compatible domain; do not guess an overlay-to-app-window transform. */
fun projectionProcessingSkipReason(contentWidth: Int, contentHeight: Int, reportedWidth: Int, reportedHeight: Int,
    hasOverlayMask: Boolean): Int? = when {
    contentWidth <= 0 || contentHeight <= 0 -> 4
    contentWidth <= contentHeight -> 2
    hasOverlayMask && (reportedWidth <= 0 || reportedHeight <= 0 ||
        // The reader scales each dimension down to an even number: allow only that <2px rounding per axis.
        kotlin.math.abs(contentWidth.toLong() * reportedHeight - contentHeight.toLong() * reportedWidth) >
        2L * (reportedWidth.toLong() + reportedHeight)) -> 4
    else -> null
}
