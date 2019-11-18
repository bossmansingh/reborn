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
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.exceptions.CompositeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SchedulersTestExtension::class)
class RebornCompletableTest {

    @Test
    fun `when Completable load with skip return true then do no call mapper`() {
        var called = false
        val data = "data"

        Completable.complete()
            .load<String>({ true }) {
                called = true
                Flowable.just(Result.success(data))
            }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Completable load with skip return false then call mapper`() {
        val data1 = "data1"

        Completable.complete()
            .load<String>({ false }) {
                Flowable.just(Result.success(data1))
            }
            .test()
            .assertValues(Result.success(data1))
    }

    @Test
    fun `when Completable load and mapper throws error then map to Result error`() {
        val throwable = Throwable()

        Completable.complete()
            .load<String> { throw throwable }
            .test()
            .assertValues(Result.error(throwable))
    }

    @Test
    fun `when Completable load and mapper emit error then map to Result error`() {
        val throwable = Throwable()

        Completable.complete()
            .load<String> { Flowable.error(throwable) }
            .test()
            .assertValues(Result.error(throwable))
    }

    @Test
    fun `when Completable load and mapper throws error and upstream also is error then map to Result CompositeException`() {
        val upstreamThrowable = NullPointerException()
        val throwable = IllegalArgumentException()

        val result = Completable.error(upstreamThrowable)
            .load<String> { throw throwable }
            .test()
            .values()
            .last()

        assertTrue(result.cause != null)
        assertTrue(result.cause is CompositeException)
        assertEquals(upstreamThrowable, (result.cause as CompositeException).exceptions[0])
        assertEquals(throwable, (result.cause as CompositeException).exceptions[1])
    }

    @Test
    fun `when Completable load and mapper emit error and upstream also is error then map to Result CompositeException`() {
        val upstreamThrowable = NullPointerException()
        val throwable = IllegalArgumentException()

        val result = Completable.error(upstreamThrowable)
            .load<String> { Flowable.error(throwable) }
            .test()
            .values()
            .last()

        assertTrue(result.cause != null)
        assertTrue(result.cause is CompositeException)
        assertEquals(upstreamThrowable, (result.cause as CompositeException).exceptions[0])
        assertEquals(throwable, (result.cause as CompositeException).exceptions[1])
    }

    @Test
    fun `when Completable load and mapper emit result then same result will be passed to downstream`() {
        val data1 = "data1"

        Completable.complete()
            .load<String> { Flowable.just(data1).result() }
            .test()
            .assertValues(Result.success(data1))
    }

    @Test
    fun `when Completable result() is called and upstream is completed then emit Result success`() {
        Completable.complete()
            .result()
            .test()
            .assertValues(Result.success())
    }

    @Test
    fun `when Completable result() is called and upstream has error then emit Resource error`() {
        val error = Throwable()

        Completable.error(error)
            .result()
            .test()
            .assertValues(Result.error(error))
    }

    @Test
    fun `when Completable execute() is called and upstream is completed then emit Result success`() {
        Completable.complete()
            .execute()
            .test()
            .assertValues(Result.success())
    }

    @Test
    fun `when Completable execute() is called and upstream has error then emit Resource error`() {
        val error = Throwable()

        Completable.error(error)
            .execute()
            .test()
            .assertValues(Result.error(error))
    }
}