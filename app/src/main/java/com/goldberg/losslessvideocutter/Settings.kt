package com.goldberg.losslessvideocutter

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class Settings(context: Context)
{
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

//    val inputFile by lazy { StringPreference(preferences, "input_file", "") }

    class IntPreference(preferences: SharedPreferences, key: String, val defaultValue: Int)
        : Preference(preferences, key)
    {
        var value: Int
            get() = preferences.getInt(key, defaultValue)
            set(value) = preferences.edit().putInt(key, value).apply()

        override fun setDefault() = preferences.edit().putInt(key, defaultValue).apply()
    }

    class StringPreference(preferences: SharedPreferences, key: String, val defaultValue: String)
        : Preference(preferences, key)
    {
        var value: String
            get() = preferences.getString(key, defaultValue)!!
            set(value) = preferences.edit().putString(key, value).apply()

        override fun setDefault() = preferences.edit().putString(key, defaultValue).apply()
    }

    abstract class Preference(
        protected val preferences: SharedPreferences,
        protected val key: String)
    {
        val exists: Boolean get() = preferences.contains(key)

        abstract fun setDefault()
    }
}
