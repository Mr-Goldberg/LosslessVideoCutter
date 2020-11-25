package com.goldberg.losslessvideocutter

import android.app.Application
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Handler
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.FFprobe
import com.goldberg.losslessvideocutter.Constants.KEYFRAME_TIMING_EXTRACTION_REGEX
import com.goldberg.losslessvideocutter.Constants.MIME_TYPE_VIDEO
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application)
{
    // Set by this (Model) class, observed by Activity

    val inputFile = MutableLiveData<File>()
    val inputFileDuration = MutableLiveData<Float>()
    val inputFileKeyframeTimings = MutableLiveData<Array<Float>>()
    val outputFile = MutableLiveData<File>()
    val outputFileUri = MutableLiveData<Uri>()

    // Set by Activity class

    var outputCutRange: List<Float>? = null

    private val context = getApplication<Application>().applicationContext
    private val backgroundTaskExecutor = ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, ArrayBlockingQueue<Runnable>(50))
    private val mainThreadHandler = Handler()

    fun isOutputFileExists(): Boolean
    {
        val inputFile = inputFile.value ?: throw IllegalStateException("No input file selected")
        val outputCutRange = outputCutRange ?: throw IllegalStateException("No cut range selected")

        return Storage.isOutputFileExists(inputFile, outputCutRange)
    }

    fun setVideoFileAsync(uri: Uri, completion: (error: String?) -> Unit)
    {
        backgroundTaskExecutor.execute {

            // Extract file path

            val videoPath = PathResolver.getPath(context, uri)
            Log.d(TAG, "setVideoFile() path: $videoPath")
            val videoFile = checkInputFile(videoPath)
            if (videoFile == null || videoPath == null)
            {
                inputFile.postValue(null)
                inputFileDuration.postValue(null)
                inputFileKeyframeTimings.postValue(null)
                mainThreadHandler.post { completion("File does not exist or no access") }
                return@execute
            }

            inputFile.postValue(videoFile)

            // Read video duration

            // FIXME crashes sometimes. Maybe this happens only on "Apply changes and restart Activity" action in Android Studio
            // 2020-11-25 17:34:13.638 8783-9012/com.goldberg.losslessvideocutter E/System: Unable to open zip file: /data/app/com.goldberg.losslessvideocutter-FA7ZoIxAdT4oiZjlnVDeiw==/base.apk
            // 2020-11-25 17:34:13.641 8783-9012/com.goldberg.losslessvideocutter E/System: java.io.FileNotFoundException: File doesn't exist: /data/app/com.goldberg.losslessvideocutter-FA7ZoIxAdT4oiZjlnVDeiw==/base.apk
            // java.lang.UnsatisfiedLinkError: dlopen failed: library "libavfilter.so" not found

            val info = FFprobe.getMediaInformation(videoPath)
            Log.d(TAG, "setVideoFile() duration: ${info.duration}")
            val duration = info.duration.toFloatOrNull()
            if (duration == null || duration <= 0)
            {
                crashlyticsRecordException("setVideoFileAsync() error: Can't read video info")
                inputFileDuration.postValue(null)
                inputFileKeyframeTimings.postValue(null)
                mainThreadHandler.post { completion("Can't read video info") }
                return@execute
            }

            inputFileDuration.postValue(duration)

            // Read video keyframe timings

            val keyframeTimings = getKeyframeTimings(videoPath)
            if (keyframeTimings.isNullOrEmpty())
            {
                crashlyticsRecordException("setVideoFileAsync() error: Can't read video keyframes")
                inputFileKeyframeTimings.postValue(null)
                mainThreadHandler.post { completion("Can't read video keyframes") }
                return@execute
            }

            inputFileKeyframeTimings.postValue(keyframeTimings)
            mainThreadHandler.post { completion(null) }
        }
    }

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
                Log.e(TAG, "cutAsync() error: $result")
                crashlyticsRecordException("cutAsync() error: $result")
                error = "Error happened while processing video"
            }

            this.outputFile.postValue(outputFile)
            MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), arrayOf(MIME_TYPE_VIDEO)) { path, uri ->
                Log.d(TAG, "cutAsync() scanFile: $path $uri")
                outputFileUri.postValue(uri)
            }

            mainThreadHandler.post { completion(error) }
        }
    }

    fun deleteOutputFileAsync(completion: (error: String?) -> Unit)
    {
        val outputFile = outputFile.value
        if (outputFile == null)
        {
            completion(null)
            return
        }

        backgroundTaskExecutor.execute {

            var success = true
            if (outputFile.exists())
            {
                success = outputFile.delete()
            }

            MediaStoreHelper.removePathsFromMediaStore(context, arrayOf(outputFile.absolutePath))
            verifyInputFileExists()

            if (success)
            {
                this.outputFile.postValue(null)
                outputFileUri.postValue(null)
                mainThreadHandler.post { completion(null) }
            }
            else
            {
                mainThreadHandler.post { completion("Unable to delete file") }
            }
        }
    }

    fun deleteAllOutputFilesAsync(completion: (message: String) -> Unit)
    {
        backgroundTaskExecutor.execute {

            outputFile.postValue(null)
            outputFileUri.postValue(null)

            val outputDir = Storage.getOutputDir()
            val files = outputDir.listFiles()
            if (files.isNullOrEmpty())
            {
                mainThreadHandler.post { completion("No files to delete") }
                return@execute
            }

            var filesDeleted = 0
            val filePaths = ArrayList<String>(files.size)
            for (file in files)
            {
                if (file.isFile)
                {
                    if (file.delete()) ++filesDeleted
                    filePaths.add(file.absolutePath)
                }
                else if (file.isDirectory)
                {
                    // This app layout files in plain hierarchy, without any directories.
                    // If any directory is found - it means it was created by the user or by the system. Just delete it recursively.

                    if (file.deleteRecursively()) ++filesDeleted
                }
            }

            MediaStoreHelper.removePathsFromMediaStore(context, filePaths.toTypedArray())
            verifyInputFileExists()

            // TODO resolve files/files word depending on 'filesDeleted' number
            val message =
                if (filesDeleted == files.size)
                {
                    "Deleted $filesDeleted files"
                }
                else
                {
                    "Deleted $filesDeleted/${files.size} files"
                }

            mainThreadHandler.post { completion(message) }
        }
    }

    /**
     * Check if 'input files' was deleted too. In case if it was in the output dir.
     */
    private fun verifyInputFileExists()
    {
        val inputFile = inputFile.value
        if (inputFile != null && !inputFile.exists())
        {
            this.inputFile.postValue(null)
            inputFileDuration.postValue(null)
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

        private fun getKeyframeTimings(path: String): Array<Float>?
        {
            val durationMeter = DurationMeter.start()

            // '-loglevel error' affects only this particular command, not the whole library state
            val result = FFprobe.execute("-loglevel error -select_streams v:0 -show_entries packet=pts_time,flags -of csv=print_section=0 $path")
            if (result != RETURN_CODE_SUCCESS)
            {
                Log.i(TAG, "getKeyframeTimings() ffprobe result: $result, can't process video")
                return null
            }

            val output = Config.getLastCommandOutput()
            val matchResults = KEYFRAME_TIMING_EXTRACTION_REGEX.findAll(output)
            val keyframeTimings = ArrayList<Float>()
            for (match in matchResults)
            {
                try
                {
                    val value = match.groupValues[1].toFloat()
                    keyframeTimings.add(value)
                }
                catch (ex: RuntimeException)
                {
                    Log.i(TAG, "getKeyframeTimings() exception: ${ex.message}")
                }
            }

            durationMeter.stopAndPrint(TAG, "getKeyframeTimings()")

            return keyframeTimings.toTypedArray()
        }

        private fun cut(inputFile: File, outputFile: File, outputCutRange: List<Float>): Int
        {
            val command = "-ss ${outputCutRange[0]} -i ${inputFile.absolutePath} -to ${outputCutRange[1]} -c copy ${outputFile.absolutePath}"
            Log.d(TAG, "cut() '$command'")

            val returnCode = FFmpeg.execute(command)
            if (BuildConfig.DEBUG)
            {
                Config.printLastCommandOutput(Log.DEBUG)
            }

            Log.d(TAG, "cut() executed: $returnCode")

            return returnCode
        }
    }
}
