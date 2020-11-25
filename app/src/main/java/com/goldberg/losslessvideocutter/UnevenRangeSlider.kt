package com.goldberg.losslessvideocutter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import com.google.android.material.slider.RangeSlider

class UnevenRangeSlider
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RangeSlider(context, attrs, defStyleAttr)
{
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f // TODO to dp
        color = Color.GREEN
    }

    private var _steps: Array<Float>? = null
    var steps: Array<Float>?
        get() = _steps
        set(value)
        {
            _steps = value
            stepsToDraw = null
            invalidate()
        }

    private var stepsToDraw: FloatArray? = null

    private fun calculateStepsToDraw(): FloatArray?
    {
//        Log.d(TAG, "calculateStepsToDraw()")

        val steps = _steps
        if (steps.isNullOrEmpty()) return null

        val stepsToDraw = FloatArray(steps.size * 4)
        val pixelsPerSecond = trackWidth / (valueTo - valueFrom)
        val y0 = 0f
        val y1 = height.toFloat()
        val offsetX = (width - trackWidth) / 2f
        var x: Float
        var baseTargetIndex: Int
        steps.forEachIndexed { index, value ->
            baseTargetIndex = index * 4
            x = value * pixelsPerSecond + offsetX
            stepsToDraw[baseTargetIndex] = x
            stepsToDraw[baseTargetIndex + 1] = y0
            stepsToDraw[baseTargetIndex + 2] = x
            stepsToDraw[baseTargetIndex + 3] = y1
        }

        return stepsToDraw
    }

    override fun onDraw(canvas: Canvas)
    {
        var stepsToDraw = this.stepsToDraw
        if (stepsToDraw == null)
        {
            stepsToDraw = calculateStepsToDraw()
        }

        if (stepsToDraw != null)
        {
            this.stepsToDraw = stepsToDraw
            canvas.drawLines(stepsToDraw, paint)
        }

        super.onDraw(canvas)
    }

    companion object
    {
        private const val TAG = "UnevenRangeSlider"
    }
}
