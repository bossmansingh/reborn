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
import io.reactivex.Flowable
import io.reactivex.exceptions.CompositeException

class RebornFlowable private constructor() {

    companion object {
        @JvmStatic
        fun <T> load(data: T): Flowable<Result<T>> {
            return Flowable.just(data)
                .result()
        }

        @JvmStatic
        fun <T> load(flowable: Flowable<T>): Flowable<Result<T>> {
            return flowable.result()
        }
    }
}

fun <T> Flowable<Result<T>>.load(
    skip: (Result<T>) -> Boolean = { false },
    mapper: (Result<T>) -> Flowable<Result<T>>
): Flowable<Result<T>> {
    return flatMap { upstream ->
        if (skip(upstream)) {
            Flowable.just(upstream)
        } else {
            try {
                mapper(upstream)
            } catch (t: Throwable) {
                Flowable.error<Result<T>>(t)
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

fun <T> Flowable<Result<T>>.doOnSuccess(mapper: (Result<T>) -> Unit): Flowable<Result<T>> {
    return doOnNext {
        if (it.isSuccess) {
            mapper(it)
        }
    }
}

fun <T> Flowable<Result<T>>.doOnFailure(mapper: (Result<T>) -> Unit): Flowable<Result<T>> {
    return doOnNext {
        if (it.isFailure) {
            mapper(it)
        }
    }
}

fun <T> Flowable<Result<T>>.doOnLoading(mapper: (Result<T>) -> Unit): Flowable<Result<T>> {
    return doOnNext {
        if (it.isLoading) {
            mapper(it)
        }
    }
}

fun <T> Flowable<T>.result(): Flowable<Result<T>> {
    return map { Result.success(it) }
        .switchIfEmpty { it.onNext(Result.success()) }
        .onErrorReturn { Result.error(it) }
        .map { it }
}