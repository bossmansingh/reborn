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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class InMemoryCacheTest {

    private companion object {
        const val KEY = "a key"
    }

    @Test
    fun `when cache contains a key, then contains return true`() {
        InMemoryCache.put(KEY, 123)

        assertTrue(InMemoryCache.contains(KEY))

        InMemoryCache.remove(KEY)
    }

    @Test
    fun `when cache does not contain the key, then contains return false`() {
        assertFalse(InMemoryCache.contains(KEY))
    }

    @Test
    fun `remove will remove the value`() {
        InMemoryCache.put(KEY, 123)

        assertEquals(123, InMemoryCache.getInt(KEY))

        InMemoryCache.remove(KEY)

        assertEquals(-1, InMemoryCache.getInt(KEY, -1))
    }

    @Test
    fun `putBoolean will set boolean`() {
        InMemoryCache.putBoolean(KEY, true)
        assertTrue(InMemoryCache.getBoolean(KEY))

        InMemoryCache.remove(KEY)
    }

    @Test
    fun `putInt will set int`() {
        InMemoryCache.putInt(KEY, 123)
        assertEquals(123, InMemoryCache.getInt(KEY))

        InMemoryCache.remove(KEY)
    }

    @Test
    fun `putLong will set long`() {
        InMemoryCache.putLong(KEY, 123L)
        assertEquals(123L, InMemoryCache.getLong(KEY))

        InMemoryCache.remove(KEY)
    }

    @Test
    fun `putDouble will set double`() {
        InMemoryCache.putDouble(KEY, 123.0)
        assertEquals(123.0, InMemoryCache.getDouble(KEY))

        InMemoryCache.remove(KEY)
    }

    @Test
    fun `putFloat will set float`() {
        InMemoryCache.putFloat(KEY, 123f)
        assertEquals(123f, InMemoryCache.getFloat(KEY))

        InMemoryCache.remove(KEY)
    }

    @Test
    fun `putString will set string`() {
        InMemoryCache.putString(KEY, "something")
        assertEquals("something", InMemoryCache.getString(KEY))

        InMemoryCache.remove(KEY)
    }

    @Test
    fun `put will set an object`() {
        val dummy = Dummy(123, "456")

        InMemoryCache.put(KEY, dummy)
        assertEquals(dummy, InMemoryCache.get(KEY, Dummy::class.java))

        InMemoryCache.remove(KEY)
    }

    @Test
    fun `get a value with wrong type of object will throw ClassCastException`() {
        testClassCastException({ putBoolean(KEY, true) }, { getInt(KEY) })
        testClassCastException({ putInt(KEY, 123) }, { getLong(KEY) })
        testClassCastException({ putLong(KEY, 123L) }, { getDouble(KEY) })
        testClassCastException({ putDouble(KEY, 123.0) }, { getFloat(KEY) })
        testClassCastException({ putFloat(KEY, 123f) }, { getString(KEY) })
        testClassCastException({ putString(KEY, "something") }, { getInt(KEY) })
        testClassCastException({ put(KEY, Dummy(123, "456")) }, { getInt(KEY) })
    }

    private fun testClassCastException(
        put: InMemoryCache.() -> Unit,
        get: InMemoryCache.() -> Unit
    ) {
        put(InMemoryCache)
        try {
            get(InMemoryCache)
            fail { "put a value then get with wrong type of object should throw ClassCastException" }
        } catch (ignored: ClassCastException) {
        }

        InMemoryCache.remove(KEY)
    }

    private data class Dummy(
        val data1: Int,
        val data2: String
    )
}