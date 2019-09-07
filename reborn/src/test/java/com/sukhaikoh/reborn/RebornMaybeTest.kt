package com.sukhaikoh.reborn

import com.sukhaikoh.reborn.result.Result
import com.sukhaikoh.reborn.testhelper.SchedulersTestExtension
import io.reactivex.Maybe
import io.reactivex.exceptions.CompositeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SchedulersTestExtension::class)
class RebornMaybeTest {

    @Test
    fun `when Maybe result with data then downstream receive Result success with upstream data`() {
        val data = "data"

        Maybe.just(data)
            .result()
            .test()
            .assertValues(Result.success(data))
    }

    @Test
    fun `when Maybe result with empty upstream then downstream receive Result success with no data`() {
        Maybe.empty<Nothing>()
            .result()
            .test()
            .assertValues(Result.success())
    }

    @Test
    fun `when Maybe result with upstream error then downstream receive Result error`() {
        val throwable = Throwable()

        Maybe.error<Nothing>(throwable)
            .result()
            .test()
            .assertValues(Result.error(throwable))
    }

    @Test
    fun `when Maybe doOnSuccess with Result success then call mapper`() {
        val data = "data"
        var called = false

        Maybe.just(Result.success(data))
            .doOnResultSuccess {
                assertEquals(Result.success(data), it)
                called = true
            }
            .test()
            .assertComplete()

        assertTrue(called)
    }

    @Test
    fun `when Maybe doOnSuccess with Result loading then do not call mapper`() {
        var called = false

        Maybe.just(Result.loading<Nothing>())
            .doOnResultSuccess { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Maybe doOnSuccess with Result error then do not call mapper`() {
        var called = false

        Maybe.just(Result.error<Nothing>(Throwable()))
            .doOnResultSuccess { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Maybe doOnFailure with Result error then call mapper`() {
        val data = "data"
        val throwable = Throwable()
        var called = false

        Maybe.just(Result.error(throwable, data))
            .doOnFailure {
                assertEquals(Result.error(throwable, data), it)
                called = true
            }
            .test()
            .assertComplete()

        assertTrue(called)
    }

    @Test
    fun `when Maybe doOnFailure with Result loading then do not call mapper`() {
        var called = false

        Maybe.just(Result.loading<Nothing>())
            .doOnFailure { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Maybe doOnFailure with Result success then do not call mapper`() {
        var called = false

        Maybe.just(Result.success<Nothing>())
            .doOnFailure { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Maybe doOnLoading with Result loading then call mapper`() {
        val data = "data"
        var called = false

        Maybe.just(Result.loading(data))
            .doOnLoading {
                assertEquals(Result.loading(data), it)
                called = true
            }
            .test()
            .assertComplete()

        assertTrue(called)
    }

    @Test
    fun `when Maybe doOnLoading with Result error then do not call mapper`() {
        var called = false

        Maybe.just(Result.error<Nothing>(Throwable()))
            .doOnLoading { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Maybe doOnLoading with Result success then do not call mapper`() {
        var called = false

        Maybe.just(Result.success<Nothing>())
            .doOnLoading { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Maybe load with skip return true then do no call mapper`() {
        var called = false
        val data = "data"
        val data1 = "data1"

        Maybe.just(Result.success(data))
            .load({ true }) {
                called = true
                Maybe.just(Result.success(data1))
            }
            .test()
            .assertValues(Result.success(data))

        assertFalse(called)
    }

    @Test
    fun `when Maybe load with skip return false then call mapper`() {
        val data1 = "data1"
        val data2 = "data2"

        Maybe.just(Result.success(data1))
            .load({ false }) {
                Maybe.just(Result.success(data2))
            }
            .test()
            .assertValues(Result.success(data2))
    }

    @Test
    fun `when Maybe load and mapper throws error then map to Result error`() {
        val throwable = Throwable()
        val data = "data"

        Maybe.just(Result.success(data))
            .load { throw throwable }
            .test()
            .assertValues(Result.error(throwable, data))
    }

    @Test
    fun `when Maybe load and mapper emit error then map to Result error`() {
        val throwable = Throwable()
        val data = "data"

        Maybe.just(Result.success(data))
            .load { Maybe.error(throwable) }
            .test()
            .assertValues(Result.error(throwable, data))
    }

    @Test
    fun `when Maybe load and mapper throws error and upstream also is error then map to Result CompositeException`() {
        val upstreamThrowable = NullPointerException()
        val throwable = IllegalArgumentException()
        val data = "data"

        val result = Maybe.just(Result.error(upstreamThrowable, data))
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
    fun `when Maybe load and mapper emit error and upstream also is error then map to Result CompositeException`() {
        val upstreamThrowable = NullPointerException()
        val throwable = IllegalArgumentException()
        val data = "data"

        val result = Maybe.just(Result.error(upstreamThrowable, data))
            .load { Maybe.error(throwable) }
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
    fun `when Maybe load and mapper emit empty Maybe then map to Result success`() {
        val data = "data"

        Maybe.just(Result.success(data))
            .load { Maybe.empty() }
            .test()
            .assertValues(Result.success(data))
    }

    @Test
    fun `when Maybe load and mapper emit result then same result will be passed to downstream`() {
        val data1 = "data1"
        val data2 = "data2"

        Maybe.just(Result.success(data1))
            .load { Maybe.just(data2).result() }
            .test()
            .assertValues(Result.success(data2))
    }
}