package com.goldberg.losslessvideocutter

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.goldberg.losslessvideocutter.Constants.MIME_TYPE_VIDEO
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

// TODO check any button double presses
// TODO check if UI is really disabled during async operation
// TODO chose theme
// TODO check filesystem on android 11
// Proper layout
// -- after release --
// TODO extract strings
// TODO localize Russian/Ukrainian
// TODO extract key frames from ffmpeg to make user cut experience better
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

        open_video_file_manager_button.setOnClickListener { pickVideo() }
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

        video_cut_range_slider.addOnChangeListener { slider, value, _ ->
            viewModel.outputCutRange = slider.values
            //Log.d(TAG, "SliderChangeListener ${slider.values} $value")
        }

        //
        // Setup observers
        //

        viewModel.inputFile.observe(this, Observer { file ->
            val text = file?.absolutePath ?: ""
            input_video_path_text_view.text = text
            input_video_play_button.isEnabled = text.isNotEmpty()
        })

        viewModel.outputFile.observe(this, Observer { file ->
            val text = file?.absolutePath ?: ""
            output_video_path_text_view.text = text
            enableOutputVideoActions(text.isNotEmpty())
        })

        viewModel.outputFileUri.observe(this, Observer { uri ->
            output_video_share_button.isEnabled = (uri != null)
        })

        viewModel.inputFileDuration.observe(this, Observer { duration ->
            video_cut_range_slider.isEnabled = (duration != null)
            duration ?: return@Observer
            video_cut_range_slider.valueTo = duration
            val cutRange = listOf(0.0f, duration)
            viewModel.outputCutRange = cutRange
            video_cut_range_slider.values = cutRange
            enableCutControls(true)
        })

        //
        // Check permissions
        //

        if (!hasPermissions(*PERMISSIONS))
        {
            requestPermissions()
            return
        }
    }

    //
    // Actions
    //

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
        val dialog = ProgressDialog(this)
        dialog.setMessage(message)
        dialog.show()
        progressDialog = dialog
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
                // TODO
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
                // TODO
            }
        }

        return super.onOptionsItemSelected(item)
    }

    //
    // Permissions
    //

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSIONS_REQUEST_CODE) return

        if (!hasPermissions())
        {
            // TODO provide rationale
            // https://stackoverflow.com/questions/32347532/android-m-permissions-confused-on-the-usage-of-shouldshowrequestpermissionrati
//            if (shouldShowRequestPermissionRationale(PERMISSIONS[0]))
//            {
//                AlertDialog.Builder(this)
//                    .setTitle("Please grant storage access permission.")
//                    .setMessage("Storage access permission is required to read/write video files.")
//                    .show()
//            }

            requestPermissions()
            return
        }
    }

    private fun requestPermissions()
    {
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }

    private fun hasPermissions() = hasPermissions(*PERMISSIONS)

    private fun hasPermissions(vararg permissions: String): Boolean
    {
        for (permission in permissions)
        {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
            {
                return false
            }
        }

        return true
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

        private val PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        private const val PERMISSIONS_REQUEST_CODE = 100
        private const val PICK_VIDEO_REQUEST_CODE = 101
    }
}
