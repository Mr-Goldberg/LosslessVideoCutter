package com.goldberg.losslessvideocutter

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.FFprobe
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application)
{
    val inputFile = MutableLiveData<File>()
    val inputFileDuration = MutableLiveData<Float>()
    val outputFile = MutableLiveData<File>()
    var outputCutRange: List<Float>? = null

    private val context = getApplication<Application>().applicationContext
    private val backgroundTaskExecutor = ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, ArrayBlockingQueue<Runnable>(50))
    private val mainThreadHandler = Handler()

    fun setVideoFileAsync(uri: Uri, completion: (error: String?) -> Unit)
    {
        backgroundTaskExecutor.execute {

            // Extract file path

            val videoPath = PathResolver.getPath(context, uri)
            Log.d(TAG, "setVideoFile() path: $videoPath")
            val videoFile = checkInputFile(videoPath)
            inputFile.postValue(videoFile)
            if (videoFile == null)
            {
                mainThreadHandler.post { completion("File does not exist or no access") }
                return@execute
            }

            // Read video duration

            val info = FFprobe.getMediaInformation(videoFile.absolutePath)
            Log.d(TAG, "setVideoFile() duration: ${info.duration}")
            val duration = info.duration.toFloatOrNull()
            if (duration == null || duration <= 0)
            {
                inputFileDuration.postValue(null)
                mainThreadHandler.post { completion("Can't read video info") }
                return@execute
            }

            inputFileDuration.postValue(duration)
            mainThreadHandler.post { completion(null) }
        }
    }

    // TODO handle exceptions
    fun isOutputFileExists(): Boolean
    {
        val inputFile = inputFile.value ?: throw IllegalStateException("No input file selected")
        val outputCutRange = outputCutRange ?: throw IllegalStateException("No cut range selected")

        return Storage.isOutputFileExists(inputFile, outputCutRange)
    }

    // TODO handle exceptions
    fun cutAsync(overwrite: Boolean, completion: (error: String?) -> Unit)
    {
        val inputFile = inputFile.value ?: throw IllegalStateException("No input file selected")
        val outputCutRange = outputCutRange ?: throw IllegalStateException("No cut range selected")

        backgroundTaskExecutor.execute {

            val outputFile = Storage.makeOutputFileChecked(inputFile, outputCutRange, overwrite)
            if (outputFile == null)
            {
                mainThreadHandler.post { completion("Cannot create output directory") }
                return@execute
            }

            val result = cut(inputFile, outputFile, outputCutRange)
            var error: String? = null
            if (result != 0)
            {
                Log.i(TAG, "cutAsync() error: $result")
                // TODO Crashlytics log
                error = "Error happened while processing video"
            }

            this.outputFile.postValue(outputFile)
            mainThreadHandler.post { completion(error) }
        }
    }

    companion object
    {
        private const val TAG = "MainViewModel"

        private fun checkInputFile(path: String?): File?
        {
            if (path.isNullOrBlank()) return null

            val file = File(path)
            if (!file.exists()) return null

            return file
        }

        fun cut(inputFile: File, outputFile: File, outputCutRange: List<Float>): Int
        {
            val command = "-ss ${outputCutRange[0]} -i ${inputFile.absolutePath} -to ${outputCutRange[1]} -c copy ${outputFile.absolutePath}"
            Log.d(TAG, "cut() '$command'")

            val returnCode = FFmpeg.execute(command)
            Config.printLastCommandOutput(Log.DEBUG) // TODO rm for release
            Log.d(TAG, "cut() executed: $returnCode")
            return returnCode
        }
    }
}
