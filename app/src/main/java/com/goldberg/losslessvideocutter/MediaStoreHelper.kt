package com.goldberg.losslessvideocutter

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log

object MediaStoreHelper
{
    private const val TAG = "MediaStoreHelper"

    private val PROJECTION = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATA)
    private const val SELECTION_DATA = "${MediaStore.MediaColumns.DATA}=?"

    /**
     * Will remove all paths from the MediaStore.
     * Will not remove actual files from disk.
     */
    fun removePathsFromMediaStore(context: Context, paths: Array<String>)
    {
        if (paths.isEmpty()) return

        val selectionBuilder = StringBuilder()
        for (path in paths)
        {
            if (selectionBuilder.isNotEmpty())
            {
                selectionBuilder.append(" OR ")
            }

            selectionBuilder.append(SELECTION_DATA)
        }

        var cursor: Cursor? = null
        try
        {
            cursor = context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, PROJECTION, selectionBuilder.toString(), paths, null)
            if (cursor == null)
            {
                Log.i(TAG, "removePathsFromMediaStore() cursor is null")
                return
            }

            cursor.moveToFirst()
            var totalDeletedRows = 0
            while (!cursor.isAfterLast)
            {
                val id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                totalDeletedRows += context.contentResolver.delete(uri, null, null)
                cursor.moveToNext()
            }

            Log.d(TAG, "removeAllForPaths() deleted rows: $totalDeletedRows")
        }
        catch (ex: Exception)
        {
            Log.i(TAG, "removePathsFromMediaStore() exception: ${ex.message}")
        }
        finally
        {
            cursor?.close()
        }
    }
}
