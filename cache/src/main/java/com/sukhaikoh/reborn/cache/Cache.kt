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
 * An interface that represents the class is a type of data source that
 * cache data.
 */
interface Cache {
    /**
     * Check if this data source contains a value with the given [key].
     *
     * @param key the key that stores the value.
     * @return `true` if this data source contains a value with the given [key],
     * `false` otherwise.
     */
    fun contains(key: String): Boolean

    /**
     * Remove the value with the given [key] from this data source if it exists.
     *
     * @param key the key that stores the value.
     */
    fun remove(key: String)

    /**
     * Put the [value] with the given [key] into this data source.
     * Note, this will override any existing value that matches the
     * given [key].
     *
     * @param key the key that stores the [value].
     * @param value the value to be stored into this data source.
     */
    fun putBoolean(key: String, value: Boolean)

    /**
     * Put the [value] with the given [key] into this data source.
     * Note, this will override any existing value that matches the
     * given [key].
     *
     * @param key the key that stores the [value].
     * @param value the value to be stored into this data source.
     */
    fun putInt(key: String, value: Int)

    /**
     * Put the [value] with the given [key] into this data source.
     * Note, this will override any existing value that matches the
     * given [key].
     *
     * @param key the key that stores the [value].
     * @param value the value to be stored into this data source.
     */
    fun putLong(key: String, value: Long)

    /**
     * Put the [value] with the given [key] into this data source.
     * Note, this will override any existing value that matches the
     * given [key].
     *
     * @param key the key that stores the [value].
     * @param value the value to be stored into this data source.
     */
    fun putDouble(key: String, value: Double)

    /**
     * Put the [value] with the given [key] into this data source.
     * Note, this will override any existing value that matches the
     * given [key].
     *
     * @param key the key that stores the [value].
     * @param value the value to be stored into this data source.
     */
    fun putFloat(key: String, value: Float)

    /**
     * Put the [value] with the given [key] into this data source.
     * Note, this will override any existing value that matches the
     * given [key].
     *
     * @param key the key that stores the [value].
     * @param value the value to be stored into this data source.
     */
    fun putString(key: String, value: String?)

    /**
     * Put the [value] with the given [key] into this data source.
     * Note, this will override any existing value that matches the
     * given [key].
     *
     * @param T the type of the value.
     * @param key the key that stores the [value].
     * @param value the value to be stored into this data source.
     */
    fun <T> put(key: String, value: T?)

    /**
     * Retrieve a boolean value from this data source.
     *
     * @param key the key that stores the value.
     * @param defaultValue a default value to be returned if the value
     * is not found with the given [key].
     * @return the value in this data source if it exists, otherwise default value.
     * Throws [ClassCastException] if there is a value with this [key] that is not
     * a boolean.
     *
     * @throws ClassCastException if there is a value with this [key] that is not
     * a boolean.
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    /**
     * Retrieve an int value from this data source.
     *
     * @param key the key that stores the value.
     * @param defaultValue a default value to be returned if the value
     * is not found with the given [key].
     * @return the value in this data source if it exists, otherwise default value.
     * Throws [ClassCastException] if there is a value with this [key] that is not
     * an int.
     *
     * @throws ClassCastException if there is a value with this [key] that is not
     * an int.
     */
    fun getInt(key: String, defaultValue: Int = 0): Int

    /**
     * Retrieve a long value from this data source.
     *
     * @param key the key that stores the value.
     * @param defaultValue a default value to be returned if the value
     * is not found with the given [key].
     * @return the value in this data source if it exists, otherwise default value.
     * Throws [ClassCastException] if there is a value with this [key] that is not
     * a long.
     *
     * @throws ClassCastException if there is a value with this [key] that is not
     * a long.
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long

    /**
     * Retrieve a double value from this data source.
     *
     * @param key the key that stores the value.
     * @param defaultValue a default value to be returned if the value
     * is not found with the given [key].
     * @return the value in this data source if it exists, otherwise default value.
     * Throws [ClassCastException] if there is a value with this [key] that is not
     * a double.
     *
     * @throws ClassCastException if there is a value with this [key] that is not
     * a double.
     */
    fun getDouble(key: String, defaultValue: Double = 0.0): Double

    /**
     * Retrieve a float value from this data source.
     *
     * @param key the key that stores the value.
     * @param defaultValue a default value to be returned if the value
     * is not found with the given [key].
     * @return the value in this data source if it exists, otherwise default value.
     * Throws [ClassCastException] if there is a value with this [key] that is not
     * a float.
     *
     * @throws ClassCastException if there is a value with this [key] that is not
     * a float.
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Float

    /**
     * Retrieve a string value from this data source.
     *
     * @param key the key that stores the value.
     * @param defaultValue a default value to be returned if the value
     * is not found with the given [key].
     * @return the value in this data source if it exists, otherwise default value.
     * Throws [ClassCastException] if there is a value with this [key] that is not
     * a string.
     *
     * @throws ClassCastException if there is a value with this [key] that is not
     * a string.
     */
    fun getString(key: String, defaultValue: String? = null): String?

    /**
     * Retrieve a type [T] value from this data source.
     *
     * @param T the type of the value.
     * @param key the key that stores the value.
     * @param classType the class type of [T].
     * @param defaultValue a default value to be returned if the value
     * is not found with the given [key].
     * @return the value in this data source if it exists, otherwise default value.
     * Throws [ClassCastException] if there is a value with this [key] that is not
     * a type [T].
     *
     * @throws ClassCastException if there is a value with this [key] that is not
     * a type [T].
     */
    fun <T> get(key: String, classType: Class<T>, defaultValue: T? = null): T?
}