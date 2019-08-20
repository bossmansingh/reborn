package com.sukhaikoh.roctopus.core

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Flowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class Repository2Test {

    private val dao: Dao = mock()
    private val api: Api = mock()
    private val id = "this is id"
    private val daoData = "dao data"
    private val daoData2 = "dao data 2"
    private val apiData = "api data"

    @BeforeEach
    fun setup() {
        whenever(dao.load(id)).thenReturn(Flowable.just(daoData, apiData))
        whenever(api.getData(id)).thenReturn(Flowable.just(apiData))
    }

    @Test
    fun `sf as`() {
        val id = "this is id"
        var counter = 0

        dao.load(id)
            .result()
            .load { upstream ->
                api.getData(id)
                    .result()
                    .filter {
                        counter++ == 0
                    }
                    .doOnSuccess {
                        val d = it.data
                        if (d != null) {
                            dao.update(id, d)
                        }
                    }
                    .map { upstream.toLoading() }
            }
            .startWith(Result.loading())
            .test()
            .assertValues(
                Result.loading(),
                Result.loading(daoData),
                Result.success(apiData)
            )

//        Repository2.load(dao.load(id))
//            .load { upstream ->
//                api.getData(id)
//                    .result()
//                    .distinctUntilChanged { v ->
//
//                    }
//                    .filter {
//                        counter++ == 0
//                    }
//                    .doOnNext {
//                        dao.update(id, it.data!!)
//                    }
//                    .doOnNext {
//                        print(it.data)
//                    }
//                    .asNonPrimarySource(upstream)
//            }
//            .asFlowable()
//            .test()
//            .assertValues(
//                Result.loading(),
//                Result.loading(daoData),
//                Result.success(apiData)
//            )

        verify(dao, times(1)).update(id, apiData)

//        Repository2.load(dao.load(id))
//            .load { upstream -> api.getData(id)
//                .startWith { upstream }
//                .rate(Rate.once())
//                .ignoreElements()
//                .onSuccess { result ->
//                    result.onSuccess {
//                        if (it.data != null) {
//                            dao.update(id, it.data!!)
//                        }
//                    }
//                }
//            }
//            .load { upstream -> api.getData(id)
//                .filter { upstream.isFailure }
//                .startWith { upstream }
//                .rate(Rate.once())
//                .ignoreElements()
//                .onSuccess { result ->
//                    result.onSuccess { dao.update(id, it.data) }
//                }
//            }
    }
}