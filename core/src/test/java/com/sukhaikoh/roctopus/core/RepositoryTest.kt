package com.sukhaikoh.roctopus.core

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.sukhaikoh.roctopus.core.Repository.Companion.prepare
import com.sukhaikoh.roctopus.testhelper.SchedulersTestExtension
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SchedulersTestExtension::class)
class RepositoryTest {

    private companion object {
        const val data = "my data"
    }

    private lateinit var callOrder: MutableList<Int>
    private var numberOfCalls: Int = 0

    @BeforeEach
    fun setup() {
        callOrder = spy(mutableListOf())
        numberOfCalls = 0

        Repository.setGlobalMainThreadScheduler(null)
        Repository.setGlobalSubscribeOnScheduler(null)
    }

    @Test
    fun `execute with empty handler will return empty Flowable`() {
        prepare<String>()
            .execute()
            .test()
            .assertValues()
    }

    @Test
    fun `execute with only save will call given block`() {
        var called = false

        prepare<String>()
            .save { called = true }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.success()
            )

        assertTrue(called)
    }

    @Test
    fun `execute with multiple save will call given block`() {
        var count = 0

        prepare<String>()
            .save { count++ }
            .save { count++ }
            .save { count++ }
            .execute()
            .test()
            .assertValues(
                Result.loading(),

                // Only 1 success result because the last call will always
                // consume all the upstream results
                Result.success()
            )

        assertEquals(3, count)
    }

    @Test
    fun `execute with multiple save and one failed will not continue the chain`() {
        var count = 0
        val throwable = Throwable()

        prepare<String>()
            .save { count++ }
            .save {
                count++
                throw throwable
            }
            .save { count++ }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.error(throwable)
            )

        assertEquals(2, count)
    }

    @Test
    fun `execute with save then load will call given block`() {
        prepare<String>()
            .save { positionOrder(0) }
            .load {
                positionOrder(1)
                Flowable.just(data)
            }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.success(data)
            )

        verifyCallOrders()
    }

    @Test
    fun `execute with save and filter based on upstream data will run block`() {
        var called = false

        prepare<String>()
            .load { Flowable.just(data) }
            .save(
                block = { called = true },
                configure = {
                    it.filter { upstreamResult ->
                        upstreamResult.data.equals(data)
                    }
                })
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.success(data)
            )
        assertTrue(called)
    }

    @Test
    fun `execute with save and filter to take upstream failure will run block`() {
        var called = false
        val throwable = Throwable()

        prepare<String>()
            .load { Flowable.just(data) }
            .load { throw throwable }
            .save(
                block = { called = true },
                configure = {
                    it.filter { upstreamResult ->
                        upstreamResult.isFailure
                    }.onErrorReturn { _, _ ->
                        fail("onErrorReturn option shouldn't be called but was actually called.")
                    }
                }
            )
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.success(data)
            )
        assertTrue(called)
    }

    @Test
    fun `execute with save and filter to take upstream success will run block`() {
        var called = false
        val throwable = Throwable()

        prepare<String>()
            .load { Flowable.just(data) }
            .load { throw throwable }
            .save(
                block = { called = true },
                configure = {
                    it.filter { upstreamResult ->
                        upstreamResult.isSuccess
                    }.onErrorReturn { _, _ ->
                        fail("onErrorReturn option shouldn't be called but was actually called.")
                    }
                }
            )
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.error(throwable, data)
            )
        assertFalse(called)
    }

    @Test
    fun `execute with save and startWithUpstreamResult will emit upstream result at first`() {
        val data1 = "data 1"
        val data2 = "data 2"

        prepare<String>()
            .load {
                positionOrder(0)
                Flowable.just(data1)
            }
            .save(
                block = { positionOrder(1) },
                configure = { it.startWithUpstreamResult() }
            )
            .load {
                positionOrder(2)
                Flowable.just(data2)
            }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.loading(data1),
                Result.success(data2)
            )

        verifyCallOrders()
    }

    @Test
    fun `save when filter predicate is true then run block`() {
        var called = false

        prepare<String>()
            .load { Flowable.just(data) }
            .save(
                block = { called = true },
                configure = { it.filter { true } }
            )
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.success(data)
            )
        assertTrue(called)
    }

    @Test
    fun `save when filter predicate is false then do not run block`() {
        var called = false

        prepare<String>()
            .load { Flowable.just(data) }
            .save(
                block = { called = true },
                configure = { it.filter { false } }
            )
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.success(data)
            )
        assertFalse(called)
    }

    @Test
    fun `save when block throw Exception then return Result error by default`() {
        val expected = Throwable()

        prepare<String>()
            .save { throw expected }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.error(expected)
            )
    }

    @Test
    fun `save when block throw Exception then return Result error by default with upstream data if any`() {
        val expected = Throwable()

        prepare<String>()
            // This will success, so the `data` should be carried on to the
            // failure below
            .load { Flowable.just(data) }

            // This will fail the stream, but should forward the `data`
            .save { throw expected }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.error(expected, data)
            )
    }

    @Test
    fun `save when block throw Exception and onErrorReturn is provided then execute this block`() {
        val throwable = Throwable()

        prepare<String>()
            .save(
                block = { throw throwable },
                configure = { option ->
                    option.onErrorReturn { t, result ->
                        assertEquals(throwable, t)

                        // The upstream should be loading because this save
                        // is the first method call
                        assertEquals(
                            Result.loading<String>(),
                            result
                        )

                        Result.success(data)
                    }
                }
            )
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.success(data)
            )
    }

    @Test
    fun `save when block throw Exception then call onComplete error block`() {
        val throwable = Throwable()
        var called = false

        prepare<String>()
            .save(
                { throw throwable },
                { options ->
                    options.onComplete({
                        fail("onSuccess should not be called when an error occurred.")
                    }, {
                        called = true
                        assertEquals(throwable, it)
                    })
                }
            )
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.error(throwable)
            )

        assertTrue(called)
    }

    @Test
    fun `save when block is success then call onComplete success block`() {
        var called = false

        prepare<String>()
            .save(
                { },
                {
                    it.onComplete({ result ->
                        called = true
                        assertEquals(Result.success<String>(), result)
                    }, {
                        fail("onError should not be called when success.")
                    })
                }
            )
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.success()
            )

        assertTrue(called)
    }

    @Test
    fun `execute with only load will call given block`() {
        var called = false

        prepare<String>()
            .load {
                called = true
                Flowable.just(data)
            }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.success(data)
            )

        assertTrue(called)
    }

    @Test
    fun `execute with load then save will call given block`() {
        prepare<String>()
            .load {
                positionOrder(0)
                Flowable.just(data)
            }
            .save { positionOrder(1) }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.success(data)
            )

        verifyCallOrders()
    }

    @Test
    fun `execute with multiple load will call given block`() {
        var count = 0
        val data1 = "data 1"
        val data2 = "data 2"
        val data3 = "data 3"

        prepare<String>()
            .load {
                count++
                Flowable.just(data1)
            }
            .load {
                count++
                Flowable.just(data2)
            }
            .load {
                count++
                Flowable.just(data3)
            }
            .execute()
            .test()
            .assertValues(
                Result.loading(),

                // Only 1 success result because the last call will always
                // consume all the upstream results and intercept them
                Result.success(data3)
            )

        assertEquals(3, count)
    }

    @Test
    fun `execute with multiple load and one failed will not continue the chain`() {
        var count = 0
        val data1 = "data 1"
        val data2 = "data 2"
        val throwable = Throwable()

        prepare<String>()
            .load {
                count++
                Flowable.just(data1)
            }
            .load {
                count++
                throw throwable
            }
            .load {
                count++
                Flowable.just(data2)
            }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.error(throwable, data1)
            )

        assertEquals(2, count)
    }

    @Test
    fun `execute with load and filter based on upstream data will run block`() {
        val data1 = "data 1"
        val data2 = "data 2"

        prepare<String>()
            .load { Flowable.just(data1) }
            .load(
                block = { Flowable.just(data2) },
                configure = {
                    it.filter { upstreamResult ->
                        upstreamResult.data.equals(data1)
                    }
                })
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.success(data2)
            )
    }

    @Test
    fun `execute with load and filter to take upstream failure will run block`() {
        val data1 = "data 1"
        val data2 = "data 2"
        val throwable = Throwable()

        prepare<String>()
            .load { Flowable.just(data1) }
            .load { throw throwable }
            .load(
                block = { Flowable.just(data2) },
                configure = {
                    it.filter { upstreamResult ->
                        upstreamResult.isFailure
                    }.onErrorReturn { _, _ ->
                        fail("onErrorReturn option shouldn't be called but was actually called.")
                    }
                }
            )
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.success(data2)
            )
    }

    @Test
    fun `execute with load and filter to take upstream success will run block`() {
        val data1 = "data 1"
        val data2 = "data 2"
        val throwable = Throwable()

        prepare<String>()
            .load { Flowable.just(data1) }
            .load { throw throwable }
            .save(
                block = { Flowable.just(data2) },
                configure = {
                    it.filter { upstreamResult ->
                        upstreamResult.isSuccess
                    }.onErrorReturn { _, _ ->
                        fail("onErrorReturn option shouldn't be called but was actually called.")
                    }
                }
            )
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.error(throwable, data1)
            )
    }

    @Test
    fun `execute with load and startWithUpstreamResult will emit upstream result at first`() {
        val data1 = "data 1"
        val data2 = "data 2"
        val data3 = "data 3"

        prepare<String>()
            .load {
                positionOrder(0)
                Flowable.just(data1)
            }
            .load(
                block = {
                    positionOrder(1)
                    Flowable.just(data2)
                },
                configure = { it.startWithUpstreamResult() }
            )
            .load {
                positionOrder(2)
                Flowable.just(data3)
            }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.loading(data1),

                // It skips data2 because the last load() does not have
                // option of startWithUpstreamResult() and so it intercepts
                // the result coming from second load(), which is data2
                Result.success(data3)
            )

        verifyCallOrders()
    }

    @Test
    fun `load when block return Flowable error then return Result error`() {
        val throwable = Throwable()

        prepare<String>()
            .load { Flowable.error(throwable) }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.error(throwable)
            )
    }

    @Test
    fun `load when block throw Exception then return Result error by default`() {
        val expected = Throwable()

        prepare<String>()
            .load { throw expected }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.error(expected)
            )
    }

    @Test
    fun `load when block throw Exception then return Result error by default with upstream data if any`() {
        val expected = Throwable()

        prepare<String>()
            .load { Flowable.just(data) }
            .load { throw expected }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.error(expected, data)
            )
    }

    @Test
    fun `load when block throw Exception and onErrorReturn is provided then execute this rescue block`() {
        val throwable = Throwable()

        prepare<String>()
            .load(
                block = { throw throwable },
                configure = {
                    it.onErrorReturn { t, result ->
                        assertEquals(throwable, t)

                        // This should be loading because this getFromLocal is
                        // the first call
                        assertEquals(Result.loading<String>(), result)

                        // This should be the final result
                        Result.success(data)
                    }
                }
            )
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.success(data)
            )
    }

    @Test
    fun `load when block throw Exception then call onError`() {
        val throwable = Throwable()
        var called = false

        prepare<String>()
            .load(
                { throw throwable },
                { option ->
                    option.onError {
                        called = true
                        assertEquals(throwable, it)
                    }.onSuccess {
                        fail("onSuccess should not be called when an error occurred.")
                    }
                }
            )
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.error(throwable)
            )

        assertTrue(called)
    }

    @Test
    fun `load when block is success then call onComplete success block`() {
        var called = false

        prepare<String>()
            .load(
                { Flowable.just(data) },
                {
                    it.onComplete({ result ->
                        called = true
                        assertEquals(Result.success(data), result)
                    }, {
                        fail("onError should not be called when success.")
                    })
                }
            )
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.success(data)
            )

        assertTrue(called)
    }

    @Test
    fun `load when block throw Exception then call onComplete error block`() {
        val throwable = Throwable()
        var called = false

        prepare<String>()
            .load(
                { throw throwable },
                { options ->
                    options.onComplete({
                        fail("onSuccess should not be called when an error occurred.")
                    }, {
                        called = true
                        assertEquals(throwable, it)
                    })
                }
            )
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.error(throwable)
            )

        assertTrue(called)
    }

    @Test
    fun `load when upstream result failed then block will not get called`() {
        var called = false
        val throwable = Throwable()

        prepare<String>()
            .load { throw throwable }
            .load {
                called = true
                Flowable.just(data)
            }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.error(throwable)
            )

        assertFalse(called)
    }

    @Test
    fun `simulate load data with dao as primary source and network as secondary source`() {
        val data1 = "data 1"
        val data2 = "data 2"
        val data3 = "data 3"
        val id = "my id"
        val dao: Dao = mock()
        val api: Api = mock()
        var onErrorCalled = false

        whenever(dao.load(id)).thenReturn(Flowable.just(data1))

        whenever(api.getData(id))
            .thenReturn(Flowable.just(data2))

        prepare<String>()
            .load { dao.load(id) }
            .load(
                { api.getData(id) },
                { option ->
                    option.startWithUpstreamResult()
                        .onSuccess { result ->
                            result.onSuccess {
                                if (data != null) {
                                    dao.update(id, data!!)

                                    // To simulate dao.save successfully
                                    // modified the data source
                                    whenever(dao.load(id)).thenReturn(Flowable.just(data3))
                                }
                            }
                        }
                        .onError { onErrorCalled = true }
                })
            .load { dao.load(id) }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.loading(data1),
                Result.success(data3)
            )

        assertFalse(onErrorCalled)
        verify(dao, times(1)).update(id, data2)
    }

    @Test
    fun `simulate insert data with dao as primary source and network as secondary source`() {
        val id = "my id"
        val dao: Dao = mock()
        val api: Api = mock()

        whenever(api.addData(id, data)).thenReturn(Completable.complete())

        prepare<String>()
            .save { dao.insert(id, data) }
            .load { api.addData(id, data).toFlowable() }
            .execute()
            .test()
            .assertValues(
                Result.loading(),
                Result.success()
            )

        verify(dao, times(1)).insert(id, data)
        verify(api, times(1)).addData(id, data)
    }

    @Test
    fun `setGlobalMainThreadScheduler will set observeOn scheduler when execute`() {
        val repository = prepare<String>()

        assertFalse(repository.hasSetObserveOnScheduler)

        Repository.setGlobalMainThreadScheduler(Schedulers.newThread())

        repository.save { }
            .execute()

        assertTrue(repository.hasSetObserveOnScheduler)
    }

    @Test
    fun `setGlobalSubscribeOnScheduler will set subscribeOn scheduler when execute`() {
        val repository = prepare<String>()

        assertFalse(repository.hasSetObserveOnScheduler)

        Repository.setGlobalSubscribeOnScheduler(Schedulers.newThread())

        repository.save { }
            .execute()

        assertTrue(repository.hasSetSubscribeScheduler)
    }

//    @Test
//    fun `try something`() {
//        var called1 = false
//        var called2 = false
//
//        val flowable = prepare<String>()
//            .something("my data", { called1 = true }, { called2 = true })
//
//        assertFalse(called1)
//        assertFalse(called2)
//
//        val disposable = flowable.subscribe()
//
//        assertFalse(called1)
//        assertFalse(called2)
//
//        disposable.dispose()
//        assertTrue(called1)
//        assertTrue(called2)
//    }

    private fun positionOrder(position: Int) {
        callOrder.add(position)
        numberOfCalls++
    }

    private fun verifyCallOrders() {
        verify(callOrder, times(numberOfCalls)).add(any())

        for (i in 0 until numberOfCalls) {
            assertEquals(
                i,
                callOrder[i],
                "Call order is different. Expected: $i, Actual: ${callOrder[i]}"
            )
        }
    }
}

interface Dao {
    fun insert(key: String, data: String)
    fun update(key: String, newData: String)
    fun load(key: String): Flowable<String>
}

interface Api {
    fun addData(key: String, data: String): Completable
    fun getData(key: String): Flowable<String>
    fun getData2(key: String): Flowable<List<String>>
}