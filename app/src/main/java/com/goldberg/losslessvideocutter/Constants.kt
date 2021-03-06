package com.goldberg.losslessvideocutter

object Constants
{
    const val MIN_CUT_DURATION_SECONDS = 0.1f
    const val CUT_RANGE_START_ADJUSTMENT_SECONDS = 0.01f
    const val MIME_TYPE_VIDEO = "video/*"
    val DEFAULT_VIDEO_SOURCE = VideoSource.Gallery
    val KEYFRAME_TIMING_EXTRACTION_REGEX = Regex("^ *([0-9]+\\.[0-9]+),K_\$", RegexOption.MULTILINE)
}
