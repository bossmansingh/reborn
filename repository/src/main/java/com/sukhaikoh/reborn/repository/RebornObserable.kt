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
import io.reactivex.Observable
import io.reactivex.exceptions.CompositeException

class RebornObserable private constructor() {

    companion object {
        @JvmStatic
        fun <T> load(data: T): Observable<Result<T>> {
            return Observable.just(data)
                .result()
        }

        @JvmStatic
        fun <T> load(observable: Observable<T>): Observable<Result<T>> {
            return observable.result()
        }
    }
}

fun <T> Observable<Result<T>>.load(
    skip: (Result<T>) -> Boolean = { false },
    mapper: (Result<T>) -> Observable<Result<T>>
): Observable<Result<T>> {
    return flatMap { upstream ->
        if (skip(upstream)) {
            Observable.just(upstream)
        } else {
            try {
                mapper(upstream)
            } catch (t: Throwable) {
                Observable.error<Result<T>>(t)
            }
        }.onErrorReturn { t: Throwable ->
            if (upstream.cause != null) {
                Result.error(CompositeException(upstream.cause, t), upstream.data)
            } else {
                upstream.toError(t)
            }
        }.switchIfEmpty {
            it.onNext(upstream.toSuccess())
        }
    }
}

fun <T> Observable<Result<T>>.doOnSuccess(mapper: (Result<T>) -> Unit): Observable<Result<T>> {
    return doOnNext {
        if (it.isSuccess) {
            mapper(it)
        }
    }
}

fun <T> Observable<Result<T>>.doOnFailure(mapper: (Result<T>) -> Unit): Observable<Result<T>> {
    return doOnNext {
        if (it.isFailure) {
            mapper(it)
        }
    }
}

fun <T> Observable<Result<T>>.doOnLoading(mapper: (Result<T>) -> Unit): Observable<Result<T>> {
    return doOnNext {
        if (it.isLoading) {
            mapper(it)
        }
    }
}

fun <T> Observable<T>.result(): Observable<Result<T>> {
    return map { Result.success(it) }
        .switchIfEmpty { it.onNext(Result.success()) }
        .onErrorReturn { Result.error(it) }
        .map { it }
}