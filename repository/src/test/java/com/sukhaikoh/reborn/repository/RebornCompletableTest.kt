package com.sukhaikoh.reborn.repository

import com.sukhaikoh.reborn.result.Result
import com.sukhaikoh.reborn.testhelper.SchedulersTestExtension
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.exceptions.CompositeException
import org.junit.jupiter.api.Assertions.*
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
}