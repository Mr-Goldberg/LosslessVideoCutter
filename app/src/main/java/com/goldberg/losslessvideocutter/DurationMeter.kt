package com.goldberg.losslessvideocutter

import android.util.Log

/**
 * Class to measure duration of operations.
 * Measurement happend betwen [start] and [stop] calls.
 *
 *  TODO Remove all functions with proguard for the release builds
 */
class DurationMeter
{
    private var startTime = 0L
    private var stopTime = 0L

    fun start()
    {
        startTime = System.currentTimeMillis()
    }

    fun stop()
    {
        stopTime = System.currentTimeMillis()
    }

    fun print(tag: String, message: String = "")
    {
        Log.i(tag, "$message, duration: ${stopTime - startTime}ms")
    }

    fun stopAndPrint(tag: String, message: String = "")
    {
        stop()
        print(tag, message)
    }

    companion object
    {
        fun start(): DurationMeter = DurationMeter().apply { start() }
    }
}
