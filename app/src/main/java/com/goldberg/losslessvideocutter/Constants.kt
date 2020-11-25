package com.goldberg.losslessvideocutter

object Constants
{
    const val MIME_TYPE_VIDEO = "video/*"
    val DEFAULT_VIDEO_SOURCE = VideoSource.Gallery
    val KEYFRAME_TIMING_EXTRACTION_REGEX = Regex("^ *([0-9]+\\.[0-9]+),K_\$", RegexOption.MULTILINE)
}
