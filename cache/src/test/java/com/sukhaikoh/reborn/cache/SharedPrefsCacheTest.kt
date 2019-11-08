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
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SharedPrefsCacheTest {
    private companion object {
        const val KEY = "a key"
    }

    private val editor: SharedPreferences.Editor = mock()
    private val sharedPreferences: SharedPreferences = mock()
    private val gson = Gson()

    private lateinit var sharedPrefsCache: SharedPrefsCache

    @BeforeEach
    fun beforeEach() {
        whenever(sharedPreferences.edit()).thenReturn(editor)

        sharedPrefsCache = SharedPrefsCache(sharedPreferences, gson)
    }

    @Test
    fun `when contains is called, it will call SharedPreferences contains`() {
        sharedPrefsCache.contains(KEY)

        verify(sharedPreferences).contains(KEY)
    }

    @Test
    fun `remove will call SharedPreferences remove`() {
        sharedPrefsCache.remove(KEY)

        verify(sharedPreferences).edit()
        verify(editor).remove(KEY)
    }

    @Test
    fun `all put methods will call SharedPreferences put methods`() {
        val dummy = Dummy(123, "456")

        verifyPut({ putBoolean(KEY, true) }, { putBoolean(KEY, true) })
        verifyPut({ putInt(KEY, 123) }, { putInt(KEY, 123) })
        verifyPut({ putLong(KEY, 123L) }, { putLong(KEY, 123L) })
        verifyPut({ putDouble(KEY, 123.0) }, { putString(KEY, "123.0") })
        verifyPut({ putFloat(KEY, 123f) }, { putFloat(KEY, 123f) })
        verifyPut({ putString(KEY, "a str") }, { putString(KEY, "a str") })
        verifyPut({ put(KEY, dummy) }, { putString(KEY, gson.toJson(dummy)) })
    }

    @Test
    fun `all get methods will call SharedPreferences get methods`() {
        verifyGet({ getBoolean(KEY, true) }, { getBoolean(KEY, true) })
        verifyGet({ getInt(KEY, 123) }, { getInt(KEY, 123) })
        verifyGet({ getLong(KEY, 123L) }, { getLong(KEY, 123L) })
        verifyGet({ getDouble(KEY, 123.0) }, { getString(KEY, null) })
        verifyGet({ getFloat(KEY, 123f) }, { getFloat(KEY, 123f) })
        verifyGet({ getString(KEY, "a str") }, { getString(KEY, "a str") })
        verifyGet({ get(KEY, Dummy::class.java) }, { getString(KEY, null) })
    }

    private fun verifyPut(
        put: SharedPrefsCache.() -> Unit,
        verifyMethod: SharedPreferences.Editor.() -> Unit
    ) {
        val mockPrefs: SharedPreferences = mock()
        val mockEditor: SharedPreferences.Editor = mock()
        whenever(mockPrefs.edit()).thenReturn(mockEditor)

        sharedPrefsCache = SharedPrefsCache(mockPrefs, gson)

        put(sharedPrefsCache)
        verify(mockPrefs).edit()
        verifyMethod(verify(mockEditor))
    }

    private fun verifyGet(
        get: SharedPrefsCache.() -> Unit,
        verifyMethod: SharedPreferences.() -> Unit
    ) {
        val mockPrefs: SharedPreferences = mock()

        sharedPrefsCache = SharedPrefsCache(mockPrefs, gson)

        get(sharedPrefsCache)
        verifyMethod(verify(mockPrefs))
    }

    private data class Dummy(
        val data1: Int,
        val data2: String
    )
}