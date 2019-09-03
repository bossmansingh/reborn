package com.sukhaikoh.reborn

import com.sukhaikoh.reborn.result.Result
import com.sukhaikoh.reborn.testhelper.SchedulersTestExtension
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.exceptions.CompositeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SchedulersTestExtension::class)
class RebornObservableTest {

    @Test
    fun `when Observable result with data then downstream receive Result success with upstream data`() {
        val data = "data"

        Observable.just(data)
            .result()
            .test()
            .assertValues(Result.success(data))
    }

    @Test
    fun `when Observable result with empty upstream then downstream receive Result success with no data`() {
        Observable.empty<Nothing>()
            .result()
            .test()
            .assertValues(Result.success())
    }

    @Test
    fun `when Observable result with upstream error then downstream receive Result error`() {
        val throwable = Throwable()

        Observable.error<Nothing>(throwable)
            .result()
            .test()
            .assertValues(Result.error(throwable))
    }

    @Test
    fun `when Observable doOnSuccess with Result success then call mapper`() {
        val data = "data"
        var called = false

        Observable.just(Result.success(data))
            .doOnSuccess {
                assertEquals(Result.success(data), it)
                called = true
            }
            .test()
            .assertComplete()

        assertTrue(called)
    }

    @Test
    fun `when Observable doOnSuccess with Result loading then do not call mapper`() {
        var called = false

        Observable.just(Result.loading<Nothing>())
            .doOnSuccess { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Observable doOnSuccess with Result error then do not call mapper`() {
        var called = false

        Observable.just(Result.error<Nothing>(Throwable()))
            .doOnSuccess { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Observable doOnFailure with Result error then call mapper`() {
        val data = "data"
        val throwable = Throwable()
        var called = false

        Observable.just(Result.error(throwable, data))
            .doOnFailure {
                assertEquals(Result.error(throwable, data), it)
                called = true
            }
            .test()
            .assertComplete()

        assertTrue(called)
    }

    @Test
    fun `when Observable doOnFailure with Result loading then do not call mapper`() {
        var called = false

        Observable.just(Result.loading<Nothing>())
            .doOnFailure { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Observable doOnFailure with Result success then do not call mapper`() {
        var called = false

        Observable.just(Result.success<Nothing>())
            .doOnFailure { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Observable doOnLoading with Result loading then call mapper`() {
        val data = "data"
        var called = false

        Observable.just(Result.loading(data))
            .doOnLoading {
                assertEquals(Result.loading(data), it)
                called = true
            }
            .test()
            .assertComplete()

        assertTrue(called)
    }

    @Test
    fun `when Observable doOnLoading with Result error then do not call mapper`() {
        var called = false

        Observable.just(Result.error<Nothing>(Throwable()))
            .doOnLoading { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Observable doOnLoading with Result success then do not call mapper`() {
        var called = false

        Observable.just(Result.success<Nothing>())
            .doOnLoading { called = true }
            .test()
            .assertComplete()

        assertFalse(called)
    }

    @Test
    fun `when Observable load with skip return true then do no call mapper`() {
        var called = false
        val data = "data"
        val data1 = "data1"

        Observable.just(Result.success(data))
            .load({ true }) {
                called = true
                Observable.just(Result.success(data1))
            }
            .test()
            .assertValues(Result.success(data))

        assertFalse(called)
    }

    @Test
    fun `when Observable load with skip return false then call mapper`() {
        val data1 = "data1"
        val data2 = "data2"

        Observable.just(Result.success(data1))
            .load({ false }) {
                Observable.just(Result.success(data2))
            }
            .test()
            .assertValues(Result.success(data2))
    }

    @Test
    fun `when Observable load and mapper throws error then map to Result error`() {
        val throwable = Throwable()
        val data = "data"

        Observable.just(Result.success(data))
            .load { throw throwable }
            .test()
            .assertValues(Result.error(throwable, data))
    }

    @Test
    fun `when Observable load and mapper emit error then map to Result error`() {
        val throwable = Throwable()
        val data = "data"

        Observable.just(Result.success(data))
            .load { Observable.error(throwable) }
            .test()
            .assertValues(Result.error(throwable, data))
    }

    @Test
    fun `when Observable load and mapper throws error and upstream also is error then map to Result CompositeException`() {
        val upstreamThrowable = NullPointerException()
        val throwable = IllegalArgumentException()
        val data = "data"

        val result = Observable.just(Result.error(upstreamThrowable, data))
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
    fun `when Observable load and mapper emit error and upstream also is error then map to Result CompositeException`() {
        val upstreamThrowable = NullPointerException()
        val throwable = IllegalArgumentException()
        val data = "data"

        val result = Observable.just(Result.error(upstreamThrowable, data))
            .load { Observable.error(throwable) }
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
    fun `when Observable load and mapper emit empty Observable then map to Result success`() {
        val data = "data"

        Observable.just(Result.success(data))
            .load { Observable.empty() }
            .test()
            .assertValues(Result.success(data))
    }

    @Test
    fun `when Observable load and mapper emit result then same result will be passed to downstream`() {
        val data1 = "data1"
        val data2 = "data2"

        Observable.just(Result.success(data1))
            .load { Observable.just(data2).result() }
            .test()
            .assertValues(Result.success(data2))
    }

    class Dao {
        private val users = mutableListOf<User>()

        fun loadUsers(): Flowable<List<User>> {
            return Flowable.just(users)
        }

    }

    class Api {

    }

    data class User(
        val name: String,
        val age: Int
    )
}