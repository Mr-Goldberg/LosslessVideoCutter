package com.goldberg.losslessvideocutter

import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.text.DecimalFormat

val fractionOfSecondFormat = DecimalFormat(".00").apply {
    maximumIntegerDigits = 0
}

fun toDisplayTime(seconds: Float): String
{
    if (seconds < 0.0f) throw IllegalArgumentException("seconds argument should be non-negative")

    var secondsRest = seconds.toInt()
    val hours = secondsRest / (60 * 60)
    secondsRest -= hours * 60 * 60
    val minutes = secondsRest / 60
    secondsRest -= minutes * 60

    return String.format("%d:%02d:%02d%s", hours, minutes, secondsRest, fractionOfSecondFormat.format(seconds))
}

fun crashlyticsRecordException(message: String)
{
    FirebaseCrashlytics.getInstance().recordException(Exception(message))
}
