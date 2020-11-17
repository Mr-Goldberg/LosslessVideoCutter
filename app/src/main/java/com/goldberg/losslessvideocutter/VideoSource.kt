package com.goldberg.losslessvideocutter

enum class VideoSource
{
    Gallery,
    FileManager;

    companion object
    {
        val STRING_RES_IDS = arrayOf(R.string.gallery, R.string.file_manager)
    }
}
