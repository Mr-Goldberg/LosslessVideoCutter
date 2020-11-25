package com.goldberg.losslessvideocutter

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.Html
import android.text.Html.FROM_HTML_MODE_LEGACY
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isInvisible
import androidx.lifecycle.ViewModelProvider
import com.goldberg.losslessvideocutter.Constants.MIME_TYPE_VIDEO
import com.google.android.material.slider.RangeSlider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import kotlin.math.abs

// TODO make filemanager open for output location 1. android-native 2. 3rd party 3. verify everything is working in both device and emulator
// -- after release --
// TODO extract strings
// TODO localize Russian/Ukrainian
// TODO extract key frames from ffmpeg to make user cut experience better
// TODO check any button double presses
class MainActivity : AppCompatActivity()
{
    private lateinit var viewModel: MainViewModel
    private var progressDialog: ProgressDialog? = null
    private var selectedVideoSource = Constants.DEFAULT_VIDEO_SOURCE

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        selectedVideoSource = Settings(this).videoSourceChecked

        //
        // Setup UI
        //

        // Image buttons should be disabled programmatically, xml doesn't work
        input_video_play_button.isEnabled = false
        output_video_share_button.isEnabled = false
        enableOutputVideoActions(false)

        open_video_button.setOnClickListener(this::onPickVideoButtonClick)
        cut_video_button.setOnClickListener(this::onCutVideoButtonClick)
        input_video_play_button.setOnClickListener { playVideo(viewModel.inputFile.value) }
        output_video_play_button.setOnClickListener { playVideo(viewModel.outputFile.value) }
        output_video_share_button.setOnClickListener(this::shareOutputFile)
        output_video_delete_button.setOnClickListener(this::deleteOutputFile)

        input_video_path_text_view.apply {
            isSingleLine = true
            setOnClickListener(FilePathClickListener(true))
        }

        output_video_path_text_view.apply {
            isSingleLine = true
            setOnClickListener(FilePathClickListener(true))
        }

        video_source_spinner.apply {
            val items = VideoSource.STRING_RES_IDS.map { getString(it) }
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, items)
                .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            setSelection(selectedVideoSource.ordinal, false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener
            {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
                {
                    parent ?: return
                    val item: String = parent.getItemAtPosition(position) as String
                    selectedVideoSource = enumValueOf(VideoSource::class.java, id.toInt())
                    Settings(this@MainActivity).videoSourceChecked = selectedVideoSource
                    Log.d(TAG, "onItemSelectedListener $id $item")
                }

                override fun onNothingSelected(parent: AdapterView<*>?)
                {
                }
            }
        }

        video_cut_range_slider.apply {
            setLabelFormatter { toDisplayTime(it) }

            addOnChangeListener { slider, value, fromUser ->
                val values = slider.values
                viewModel.outputCutRange = values
                video_cut_range_start_textview.text = toDisplayTime(values[0])
                video_cut_range_end_textview.text = toDisplayTime(values[1])
//                Log.d(TAG, "SliderChangeListener ${slider.values} $value $fromUser")
            }

            addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener
            {
                override fun onStopTrackingTouch(slider: RangeSlider)
                {
                    val outputCutRange = viewModel.outputCutRange
                    val keyframeTimings = viewModel.inputFileKeyframeTimings.value
                    if (outputCutRange == null || keyframeTimings == null) return

                    val cutRangeStart = outputCutRange[0]
                    var minTimeDifference = Float.MAX_VALUE
                    var nearestKeyframeTiming = 0.0f
                    for (timing in keyframeTimings)
                    {
                        val diff = abs(cutRangeStart - timing)
                        if (diff < minTimeDifference)
                        {
                            minTimeDifference = diff
                            nearestKeyframeTiming = timing
                        }
                    }

                    if (cutRangeStart == nearestKeyframeTiming) return

                    // This will call SliderChangeListener (addOnChangeListener()) and update all values in UI and in the model.

                    video_cut_range_slider.values = listOf(nearestKeyframeTiming, outputCutRange[1])
                }

                override fun onStartTrackingTouch(slider: RangeSlider) = Unit
            })
        }

        //
        // Setup observers
        //

        viewModel.inputFile.observe(this) { file ->
            val text = file?.absolutePath ?: ""
            input_video_path_text_view.text = text
            input_video_play_button.isEnabled = text.isNotEmpty()
        }

        viewModel.outputFile.observe(this) { file ->
            val text = file?.absolutePath ?: ""
            output_video_path_text_view.text = text
            enableOutputVideoActions(text.isNotEmpty())
        }

        viewModel.outputFileUri.observe(this) { uri ->
            output_video_share_button.isEnabled = (uri != null)
        }

        viewModel.inputFileDuration.observe(this) { duration ->
            enableCutControls(duration != null)
            duration ?: return@observe
            video_cut_range_slider.valueTo = duration
            val cutRange = listOf(0.0f, duration)
            viewModel.outputCutRange = cutRange
            video_cut_range_slider.values = cutRange
        }

        viewModel.inputFileKeyframeTimings.observe(this) { keyframeTimings ->
            video_cut_range_slider.steps = keyframeTimings
        }
    }

    //
    // Actions
    //

    private fun onPickVideoButtonClick(@Suppress("UNUSED_PARAMETER") button: View)
    {
        if (!hasPermissions())
        {
            requestPermissions()
            return
        }

        pickVideo()
    }

    private fun onCutVideoButtonClick(@Suppress("UNUSED_PARAMETER") button: View)
    {
        val isOutputFileExists: Boolean
        try
        {
            isOutputFileExists = viewModel.isOutputFileExists()
        }
        catch (ex: IllegalStateException)
        {
            Toast.makeText(this, ex.message, Toast.LENGTH_SHORT).show()
            return
        }

        if (!isOutputFileExists)
        {
            cutVideo(false)
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Output file exists")
            .setMessage("Chose how to save the output video file")
            .setPositiveButton("Overwrite") { _, _ -> cutVideo(true) }
            .setNegativeButton("Create new") { _, _ -> cutVideo(false) }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun cutVideo(overwrite: Boolean)
    {
        showProgressDialog("Processing video")
        try
        {
            viewModel.cutAsync(overwrite) { error ->
                dismissProgressDialog()
                showErrorToastIfNeeded(error, true)
            }
        }
        catch (ex: IllegalStateException)
        {
            dismissProgressDialog()
            Toast.makeText(this, ex.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun playVideo(file: File?)
    {
        if (file == null)
        {
            Toast.makeText(this, "Video is not set", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(file.absolutePath), MIME_TYPE_VIDEO)
        startActivity(Intent.createChooser(intent, "Open video using"))
    }

    private fun shareOutputFile(@Suppress("UNUSED_PARAMETER") button: View)
    {
        val outputUri = viewModel.outputFileUri.value
        if (outputUri == null)
        {
            output_video_share_button.isEnabled = false
            showToast("Unable to share")
            return
        }

        val intent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, outputUri)
            putExtra(Intent.EXTRA_SUBJECT, "Video from LosslessVideoCutter") // Subject in email
            putExtra(Intent.EXTRA_TEXT, "Video from LosslessVideoCutter") // Text in email body or telegram message
            type = MIME_TYPE_VIDEO
        }
        startActivity(Intent.createChooser(intent, "Share to"))
    }

    private fun deleteOutputFile(@Suppress("UNUSED_PARAMETER") button: View)
    {
        val outputFile = viewModel.outputFile.value
        if (outputFile == null)
        {
            showToast("Unable to find file")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm deletion")
            .setMessage("Are you sure you want to delete file ${outputFile.name} ?\nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->

                showProgressDialog("Removing file")
                viewModel.deleteOutputFileAsync { error ->
                    dismissProgressDialog()
                    if (error == null)
                    {
                        showToast("File deleted")
                    }
                    else
                    {
                        showToast(error)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //
    // UI control
    //

    private fun enableCutControls(enable: Boolean)
    {
        video_cut_range_slider.isEnabled = enable
        video_cut_range_start_textview.isInvisible = !enable
        video_cut_range_end_textview.isInvisible = !enable
        cut_video_button.isEnabled = enable
    }

    private fun enableOutputVideoActions(enable: Boolean)
    {
        output_video_play_button.isEnabled = enable
        output_video_delete_button.isEnabled = enable
    }

    private fun showProgressDialog(message: String)
    {
        dismissProgressDialog()
        progressDialog = ProgressDialog(this).apply {
            setMessage(message)
            setCancelable(false)
            show()
        }
    }

    private fun dismissProgressDialog()
    {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showToast(message: String, isLong: Boolean = false)
    {
        val duration = if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        Toast.makeText(this, message, duration).show()
    }

    private fun showErrorToastIfNeeded(error: String?, isLong: Boolean = false)
    {
        if (error != null) showToast(error, isLong)
    }

    /**
     * TextView.isSingleLine is somehow inaccessible to get.
     * This is a workaround. We must store its 'isSingleLine' by ourselves.
     */
    private class FilePathClickListener(var isSingleLine: Boolean) : View.OnClickListener
    {
        override fun onClick(view: View)
        {
            view as TextView
            isSingleLine = !isSingleLine
            view.isSingleLine = isSingleLine
        }
    }

    //
    // Options menu
    //

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId)
        {
            R.id.open_output_dir ->
            {
                val outputDir = Storage.getOutputDir().absolutePath
                AlertDialog.Builder(this)
                    .setTitle("Open output directory")
                    .setMessage("The output directory is\n'$outputDir'\nIf can't be opened, open the path manually.")
                    .setPositiveButton("Open") { _, _ ->

                        // Solution is here, but it doesn't actually working https://stackoverflow.com/a/60694663/5035991
                        startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(outputDir), DocumentsContract.Document.MIME_TYPE_DIR)
                                },
                                "Open directory with"
                            )
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

                return true
            }
            R.id.clean_output_dir ->
            {
                AlertDialog.Builder(this)
                    .setTitle("Clear output directory")
                    .setMessage("Are you sure you want clear the output directory?\nThis action cannot be undone.")
                    .setPositiveButton("Clear") { _, _ ->

                        showProgressDialog("Cleaning up")
                        viewModel.deleteAllOutputFilesAsync { message ->
                            dismissProgressDialog()
                            showToast(message)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

                return true
            }
            R.id.show_info ->
            {
                AlertDialog.Builder(this)
                    .setTitle("Information")
                    .setMessage(Html.fromHtml(getString(R.string.info_text), FROM_HTML_MODE_LEGACY))
                    .setPositiveButton("Close", null)
                    .show()
                    .findViewById<TextView>(android.R.id.message).apply {
                        if (this == null)
                        {
                            Log.i(TAG, "onOptionsItemSelected(R.id.show_info) can't find message view of dialog to set up links.")
                            return@apply
                        }

                        movementMethod = LinkMovementMethod.getInstance()
                    }

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    //
    // Permissions
    //

    /**
     * Proper scheme of using [shouldShowRequestPermissionRationale]:
     * https://stackoverflow.com/a/34612503/5035991
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSIONS_REQUEST_CODE) return

        if (grantResults[0] == PackageManager.PERMISSION_DENIED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE))
            {
                AlertDialog.Builder(this)
                    .setMessage("Access to device storage is mandatory to open, edit, and save the video files.")
                    .setPositiveButton("Allow") { _, _ -> requestPermissions() }
                    .setNegativeButton("Deny", null)
                    .show()
            }

            return
        }

        // Only one permission is requested solely when the 'open file' button is clicked,
        // so we can proceed picking file, if the permission is granted.

        pickVideo()
    }

    private fun requestPermissions()
    {
        ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)
    }

    private fun hasPermissions(): Boolean
    {
        return (ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
    }

    //
    // File picker
    //

    private fun pickVideo()
    {
        val intent = Intent().apply {
            type = MIME_TYPE_VIDEO
            action = when (selectedVideoSource)
            {
                VideoSource.Gallery -> Intent.ACTION_PICK
                VideoSource.FileManager -> Intent.ACTION_GET_CONTENT
            }
        }

        startActivityForResult(Intent.createChooser(intent, "Select Video"), PICK_VIDEO_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK || requestCode != PICK_VIDEO_REQUEST_CODE || data == null) return
        val selectedVideoUri = data.data ?: return

        showProgressDialog("Reading video")
        enableCutControls(false)
        viewModel.setVideoFileAsync(selectedVideoUri) { error ->
            dismissProgressDialog()
            showErrorToastIfNeeded(error)
        }
    }

    companion object
    {
        private const val TAG = "MainActivity"

        private const val PERMISSIONS_REQUEST_CODE = 100
        private const val PICK_VIDEO_REQUEST_CODE = 101
    }
}
