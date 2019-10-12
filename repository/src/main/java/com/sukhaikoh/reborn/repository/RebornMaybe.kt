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
import io.reactivex.Maybe
import io.reactivex.exceptions.CompositeException

class RebornMaybe private constructor() {

    companion object {
        @JvmStatic
        fun <T> load(data: T): Maybe<Result<T>> {
            return Maybe.just(data)
                .result()
        }

        @JvmStatic
        fun <T> load(maybe: Maybe<T>): Maybe<Result<T>> {
            return maybe.result()
        }
    }
}

fun <T> Maybe<Result<T>>.load(
    skip: (Result<T>) -> Boolean = { false },
    mapper: (Result<T>) -> Maybe<Result<T>>
): Maybe<Result<T>> {
    return flatMap { upstream ->
        if (skip(upstream)) {
            Maybe.just(upstream)
        } else {
            try {
                mapper(upstream)
            } catch (t: Throwable) {
                Maybe.error<Result<T>>(t)
            }
        }.onErrorReturn { t: Throwable ->
            if (upstream.cause != null) {
                Result.error(CompositeException(upstream.cause, t), upstream.data)
            } else {
                upstream.toError(t)
            }
        }.defaultIfEmpty(upstream.toSuccess())
    }
}

fun <T> Maybe<Result<T>>.doOnResultSuccess(mapper: (Result<T>) -> Unit): Maybe<Result<T>> {
    return doOnSuccess {
        if (it.isSuccess) {
            mapper(it)
        }
    }
}

fun <T> Maybe<Result<T>>.doOnFailure(mapper: (Result<T>) -> Unit): Maybe<Result<T>> {
    return doOnSuccess {
        if (it.isFailure) {
            mapper(it)
        }
    }
}

fun <T> Maybe<Result<T>>.doOnLoading(mapper: (Result<T>) -> Unit): Maybe<Result<T>> {
    return doOnSuccess {
        if (it.isLoading) {
            mapper(it)
        }
    }
}

fun <T> Maybe<T>.result(): Maybe<Result<T>> {
    return map { Result.success(it) }
        .defaultIfEmpty(Result.success())
        .onErrorReturn { Result.error(it) }
        .map { it }
}
