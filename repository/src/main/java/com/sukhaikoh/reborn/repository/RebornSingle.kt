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
import io.reactivex.Single
import io.reactivex.exceptions.CompositeException

class RebornSingle private constructor() {

    companion object {
        @JvmStatic
        fun <T> load(data: T): Single<Result<T>> {
            return Single.just(data)
                .result()
        }

        @JvmStatic
        fun <T> load(single: Single<T>): Single<Result<T>> {
            return single.result()
        }
    }
}

fun <T> Single<Result<T>>.load(
    skip: (Result<T>) -> Boolean = { false },
    mapper: (Result<T>) -> Single<Result<T>>
): Single<Result<T>> {
    return flatMap { upstream ->
        if (skip(upstream)) {
            Single.just(upstream)
        } else {
            try {
                mapper(upstream)
            } catch (t: Throwable) {
                Single.error<Result<T>>(t)
            }
        }.onErrorReturn { t: Throwable ->
            if (upstream.cause != null) {
                Result.error(CompositeException(upstream.cause, t), upstream.data)
            } else {
                upstream.toError(t)
            }
        }
    }
}

fun <T> Single<Result<T>>.doOnResultSuccess(mapper: (Result<T>) -> Unit): Single<Result<T>> {
    return doOnSuccess {
        if (it.isSuccess) {
            mapper(it)
        }
    }
}

fun <T> Single<Result<T>>.doOnFailure(mapper: (Result<T>) -> Unit): Single<Result<T>> {
    return doOnSuccess {
        if (it.isFailure) {
            mapper(it)
        }
    }
}

fun <T> Single<Result<T>>.doOnLoading(mapper: (Result<T>) -> Unit): Single<Result<T>> {
    return doOnSuccess {
        if (it.isLoading) {
            mapper(it)
        }
    }
}

fun <T> Single<T>.result(): Single<Result<T>> {
    return map { Result.success(it) }
        .onErrorReturn { Result.error(it) }
        .map { it }
}