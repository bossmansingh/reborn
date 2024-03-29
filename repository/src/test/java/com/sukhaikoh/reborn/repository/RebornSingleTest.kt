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

package com.sukhaikoh.reborn.repository

import com.sukhaikoh.reborn.result.Result
import com.sukhaikoh.reborn.testhelper.SchedulersTestExtension
import io.reactivex.Single
import io.reactivex.exceptions.CompositeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SchedulersTestExtension::class)
class RebornSingleTest {

    @Test
    fun `when Single result with data then downstream receive Result success with upstream data`() {
        val data = "data"

        Single.just(data)
            .result()
            .test()
            .assertValues(Result.success(data))
    }

    @Test
    fun `when Single result with upstream error then downstream receive Result error`() {
        val throwable = Throwable()

        Single.error<Nothing>(throwable)
            .result()
            .test()
            .assertValues(Result.error(throwable))
    }

    @Test
    fun `when Single doOnSuccess with Result success then call mapper`() {
        val data = "data"
        var called = false

        Single.just(Result.success(data))
            .doOnResultSuccess {
                assertEquals(Result.success(data), it)
                called = true
            }
            .test()
            .assertComplete()

        assertTrue(called)
    }

    @Test
    fun `when Single doOnSuccess with Result loading then do not call mapper`() {
        var called = false

        Single.just(Result.loading<Nothing>())
            .doOnResultSuccess { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Single doOnSuccess with Result error then do not call mapper`() {
        var called = false

        Single.just(Result.error<Nothing>(Throwable()))
            .doOnResultSuccess { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Single doOnFailure with Result error then call mapper`() {
        val data = "data"
        val throwable = Throwable()
        var called = false

        Single.just(Result.error(throwable, data))
            .doOnFailure {
                assertEquals(Result.error(throwable, data), it)
                called = true
            }
            .test()
            .assertComplete()

        assertTrue(called)
    }

    @Test
    fun `when Single doOnFailure with Result loading then do not call mapper`() {
        var called = false

        Single.just(Result.loading<Nothing>())
            .doOnFailure { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Single doOnFailure with Result success then do not call mapper`() {
        var called = false

        Single.just(Result.success<Nothing>())
            .doOnFailure { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Single doOnLoading with Result loading then call mapper`() {
        val data = "data"
        var called = false

        Single.just(Result.loading(data))
            .doOnLoading {
                assertEquals(Result.loading(data), it)
                called = true
            }
            .test()
            .assertComplete()

        assertTrue(called)
    }

    @Test
    fun `when Single doOnLoading with Result error then do not call mapper`() {
        var called = false

        Single.just(Result.error<Nothing>(Throwable()))
            .doOnLoading { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Single doOnLoading with Result success then do not call mapper`() {
        var called = false

        Single.just(Result.success<Nothing>())
            .doOnLoading { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Single load with skip return true then do no call mapper`() {
        var called = false
        val data = "data"
        val data1 = "data1"

        Single.just(Result.success(data))
            .load({ true }) {
                called = true
                Single.just(Result.success(data1))
            }
            .test()
            .assertValues(Result.success(data))

        assertFalse(called)
    }

    @Test
    fun `when Single load with skip return false then call mapper`() {
        val data1 = "data1"
        val data2 = "data2"

        Single.just(Result.success(data1))
            .load({ false }) {
                Single.just(Result.success(data2))
            }
            .test()
            .assertValues(Result.success(data2))
    }

    @Test
    fun `when Single load and mapper throws error then map to Result error`() {
        val throwable = Throwable()
        val data = "data"

        Single.just(Result.success(data))
            .load { throw throwable }
            .test()
            .assertValues(Result.error(throwable, data))
    }

    @Test
    fun `when Single load and mapper emit error then map to Result error`() {
        val throwable = Throwable()
        val data = "data"

        Single.just(Result.success(data))
            .load { Single.error(throwable) }
            .test()
            .assertValues(Result.error(throwable, data))
    }

    @Test
    fun `when Single load and mapper throws error and upstream also is error then map to Result CompositeException`() {
        val upstreamThrowable = NullPointerException()
        val throwable = IllegalArgumentException()
        val data = "data"

        val result = Single.just(Result.error(upstreamThrowable, data))
            .load { throw throwable }
            .test()
            .values()
            .last()

        assertTrue(result.cause != null)
        assertTrue(result.cause is CompositeException)
        assertEquals(upstreamThrowable, (result.cause as CompositeException).exceptions[0])
        assertEquals(throwable, (result.cause as CompositeException).exceptions[1])
        assertEquals(data, result.data)
    }

    @Test
    fun `when Single load and mapper emit error and upstream also is error then map to Result CompositeException`() {
        val upstreamThrowable = NullPointerException()
        val throwable = IllegalArgumentException()
        val data = "data"

        val result = Single.just(Result.error(upstreamThrowable, data))
            .load { Single.error(throwable) }
            .test()
            .values()
            .last()

        assertTrue(result.cause != null)
        assertTrue(result.cause is CompositeException)
        assertEquals(upstreamThrowable, (result.cause as CompositeException).exceptions[0])
        assertEquals(throwable, (result.cause as CompositeException).exceptions[1])
        assertEquals(data, result.data)
    }

    @Test
    fun `when Single load and mapper emit result then same result will be passed to downstream`() {
        val data1 = "data1"
        val data2 = "data2"

        Single.just(Result.success(data1))
            .load { Single.just(data2).result() }
            .test()
            .assertValues(Result.success(data2))
    }

    @Test
    fun `when Single execute() is called and upstream has no error then emit Result success`() {
        val data = "data"

        Single.just(data)
            .execute()
            .test()
            .assertValues(Result.success(data))
    }

    @Test
    fun `when Single execute() is called and upstream has error then emit Result error`() {
        val error = Throwable()

        Single.just(123)
            .flatMap { Single.error<Int>(error) }
            .execute()
            .test()
            .assertValues(Result.error(error))
    }
}