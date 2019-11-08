/*
 * Copyright (C) 2019 Su Khai Koh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sukhaikoh.reborn.cache

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson

/**
 * A type of [Cache] that stores data in the Android [SharedPreferences].
 *
 * @param sharedPreferences the [SharedPreferences].
 * @param gson the gson that uses for serializing and deserializing complex
 * data type from and to [sharedPreferences].
 */
class SharedPrefsCache(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) : Cache {
    override fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    override fun remove(key: String) {
        sharedPreferences.edit { remove(key) }
    }

    override fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }

    override fun putInt(key: String, value: Int) {
        sharedPreferences.edit { putInt(key, value) }
    }

    override fun putLong(key: String, value: Long) {
        sharedPreferences.edit { putLong(key, value) }
    }

    override fun putDouble(key: String, value: Double) {
        val json = gson.toJson(value)
        putString(key, json)
    }

    override fun putFloat(key: String, value: Float) {
        sharedPreferences.edit { putFloat(key, value) }
    }

    override fun putString(key: String, value: String?) {
        sharedPreferences.edit { putString(key, value) }
    }

    override fun <T> put(key: String, value: T?) {
        val json = gson.toJson(value)
        putString(key, json)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        val json = sharedPreferences.getString(key, null)
        return if (json != null) {
            try {
                gson.fromJson(json, Double::class.java)
            } catch (ignored: Throwable) {
                throw ClassCastException(
                    "The value stored with '$key' is not a type of Double. json value: $json"
                )
            }
        } else {
            defaultValue
        }
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    override fun <T> get(key: String, classType: Class<T>, defaultValue: T?): T? {
        val json = sharedPreferences.getString(key, null)

        return if (json != null) {
            try {
                gson.fromJson(json, classType)
            } catch (ignored: Throwable) {
                throw ClassCastException(
                    "The value stored with '$key' is not a type of $classType. json value: $json"
                )
            }
        } else {
            defaultValue
        }
    }
}