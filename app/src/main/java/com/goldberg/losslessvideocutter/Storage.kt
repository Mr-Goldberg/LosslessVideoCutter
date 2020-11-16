package com.goldberg.losslessvideocutter

import android.os.Environment
import android.util.Log
import java.io.File
import kotlin.math.roundToInt

object Storage
{
    private const val TAG = "Storage"
    private const val STORAGE_DIR = "LosslessVideoCutter"

    fun isOutputFileExists(inputFile: File, outputCutRange: List<Float>): Boolean
    {
        val outputFile = makeOutputFile(inputFile, intRange(outputCutRange))
        return outputFile.exists()
    }

    fun makeOutputFileChecked(inputFile: File, outputCutRange: List<Float>, overwrite: Boolean): File?
    {
        // Check if output dir can be created

        val outputDir = getOutputDir()
        if (!outputDir.exists())
        {
            if (!outputDir.mkdirs()) return null
        }

        //

        var outputFile = makeOutputFile(inputFile, intRange(outputCutRange))
        if (!outputFile.exists()) return outputFile

        if (overwrite)
        {
            outputFile.delete()
            return outputFile
        }

        var index = 0
        while (true)
        {
            outputFile = makeOutputFile(inputFile, intRange(outputCutRange), ++index)
            if (!outputFile.exists()) return outputFile
        }
    }

    fun getOutputDir() = File(Environment.getExternalStorageDirectory(), STORAGE_DIR)

    private fun makeOutputFile(inputFile: File, outputCutRange: IntRange): File
    {
        val outputFileName = "${inputFile.nameWithoutExtension}_${outputCutRange.first}_${outputCutRange.last}.${inputFile.extension}"
        Log.d(TAG, "makeOutputFile() name: $outputFileName")

        return File(getOutputDir(), outputFileName)
    }

    private fun makeOutputFile(inputFile: File, outputCutRange: IntRange, index: Int): File
    {
        val outputFileName = "${inputFile.nameWithoutExtension}_${outputCutRange.first}_${outputCutRange.last}_${index}.${inputFile.extension}"
        Log.d(TAG, "makeOutputFile() name: $outputFileName")

        return File(getOutputDir(), outputFileName)
    }

    private fun intRange(range: List<Float>): IntRange
    {
        return IntRange(range[0].roundToInt(), range[1].roundToInt())
    }
}
