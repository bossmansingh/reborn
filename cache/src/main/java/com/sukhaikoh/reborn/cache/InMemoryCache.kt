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

/**
 * A type of [Cache] that stores data in memory. All the data that
 * are stored in this cache will only last as long as the application
 * alive, meaning all data will be lost when the application is killed.
 *
 * If you want to persist data into a [Cache] that will
 * remain even after the application is killed, use [SharedPrefsCache].
 */
object InMemoryCache : Cache {
    private val map = mutableMapOf<String, Any?>()

    override fun contains(key: String): Boolean {
        return map.containsKey(key)
    }

    override fun remove(key: String) {
        map.remove(key)
    }

    override fun putBoolean(key: String, value: Boolean) {
        put(key, value)
    }

    override fun putInt(key: String, value: Int) {
        put(key, value)
    }

    override fun putLong(key: String, value: Long) {
        put(key, value)
    }

    override fun putDouble(key: String, value: Double) {
        put(key, value)
    }

    override fun putFloat(key: String, value: Float) {
        put(key, value)
    }

    override fun putString(key: String, value: String?) {
        put(key, value)
    }

    override fun <T> put(key: String, value: T?) {
        map[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return get(key, Boolean::class.java, defaultValue) ?: defaultValue
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return get(key, Int::class.java, defaultValue) ?: defaultValue
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return get(key, Long::class.java, defaultValue) ?: defaultValue
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        return get(key, Double::class.java, defaultValue) ?: defaultValue
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return get(key, Float::class.java, defaultValue) ?: defaultValue
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return get(key, String::class.java, defaultValue)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: String, classType: Class<T>, defaultValue: T?): T? {
        return if (map.containsKey(key)) {
            try {
                map[key] as T?
            } catch (ignored: Throwable) {
                throw ClassCastException("${map[key]} cannot be casted to $classType")
            }
        } else {
            defaultValue
        }
    }
}