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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

// TODO 'share' button for output video
// TODO 'delete' button for output video
// TODO 'delete all output' button
// TODO extract strings
// -- later --
// TODO localize Russian/Ukrainian
// TODO extract key frames from ffmpeg to make user cut experience better
class MainActivity : AppCompatActivity()
{
    private lateinit var viewModel: MainViewModel
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        // Image buttons should be disabled programmatically, xml doesn't work
        input_video_play_button.isEnabled = false
        enableOutputVideoActions(false)

        viewModel.inputFile.observe(this, Observer { file ->
            val text = file?.absolutePath ?: ""
            input_video_path_text_view.text = "Input file: $text"
            input_video_play_button.isEnabled = text.isNotEmpty()
        })

        viewModel.outputFile.observe(this, Observer { file ->
            val text = file?.absolutePath ?: ""
            output_video_path_text_view.text = "Output file: $text"
            enableOutputVideoActions(text.isNotEmpty())
        })

        viewModel.inputFileDuration.observe(this, Observer { duration ->
            duration ?: return@Observer
            video_cut_range_slider.valueTo = duration
            val cutRange = listOf(0.0f, duration)
            viewModel.outputCutRange = cutRange
            video_cut_range_slider.values = cutRange
            enableCutControls(true)
        })

        open_video_file_manager_button.setOnClickListener { pickVideo(Intent.ACTION_GET_CONTENT) }
        open_video_gallery_button.setOnClickListener { pickVideo(Intent.ACTION_PICK) }
        cut_video_button.setOnClickListener { onCutVideoButtonClick() }

        video_cut_range_slider.addOnChangeListener { slider, value, fromUser ->
            viewModel.outputCutRange = slider.values
            Log.d(TAG, "SliderChangeListener ${slider.values} $value")
        }

        input_video_play_button.setOnClickListener { playVideo(viewModel.inputFile.value) }
        output_video_play_button.setOnClickListener { playVideo(viewModel.outputFile.value) }

        if (!hasPermissions(*PERMISSIONS))
        {
            requestPermissions()
            return
        }
    }

    //
    // Controls
    //

    private fun onCutVideoButtonClick()
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
                if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show()
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
        intent.setDataAndType(Uri.parse(file.absolutePath), "video/*")
        startActivity(Intent.createChooser(intent, "Open video using"))
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

    private fun pickVideo(action: String)
    {
        val intent = Intent()
        intent.type = "video/*"
        intent.action = action
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
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableCutControls(enable: Boolean)
    {
        video_cut_range_slider.isEnabled = enable
        cut_video_button.isEnabled = enable
    }

    private fun enableOutputVideoActions(enable: Boolean)
    {
        output_video_play_button.isEnabled = enable
        output_video_share_button.isEnabled = enable
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

    companion object
    {
        private const val TAG = "MainActivity"

        private val PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        private const val PERMISSIONS_REQUEST_CODE = 100
        private const val PICK_VIDEO_REQUEST_CODE = 101
    }
}
