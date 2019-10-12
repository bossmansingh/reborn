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
import io.reactivex.MaybeObserver
import io.reactivex.exceptions.CompositeException

class RebornMaybe private constructor() {

    companion object {
        /**
         * Load the [data] and return as a [Maybe]<[Result]<[T]>>.
         *
         * This is a short hand of writing:
         * ```
         * Maybe.just("my data").result()
         * ```
         *
         * ### Example
         * ```
         * RebornMaybe.load(123)
         *     .subscribe { result ->
         *         print(result.data)
         *     }
         *
         * // Print
         * 123
         * ```
         *
         * @param T the type of the data that gets loaded.
         * @param data the data to be returned in a [Maybe].
         * @return a [Maybe]<[Result]<[T]>>.
         */
        @JvmStatic
        fun <T> load(data: T): Maybe<Result<T>> {
            return Maybe.just(data)
                .result()
        }

        /**
         * Transform the given [maybe] of type [T] into a [Maybe]<[Result]<[T]>>.
         *
         * This is a short hand of writing:
         * ```
         * val maybe = Maybe.just(123)
         *
         * maybe.result()
         * ```
         *
         * ### Example
         * ```
         * RebornMaybe.load(Maybe.just(123))
         *     .subscribe { result ->
         *         print(result.data)
         *     }
         *
         * // Print
         * 123
         * ```
         *
         * @param T the type of the data that gets loaded.
         * @param maybe the [Maybe].
         * @return a [Maybe]<[Result]<[T]>>.
         */
        @JvmStatic
        fun <T> load(maybe: Maybe<T>): Maybe<Result<T>> {
            return maybe.result()
        }
    }
}

/**
 * Returns a [Maybe] that is based on applying a specified function to the item emitted
 * by the upstream, where that function returns a [Maybe]<[Result]<[T]>>.
 *
 * If [skip] returns `true`, then the upstream [Maybe] will be returned. If [skip] returns
 * `false`, result from [mapper] will be returned. If the [mapper] has any error, then
 * [Result.error] will be in the returned [Maybe]. If the [mapper] is empty, upstream
 * data with [Result.success] will be returned.
 *
 * ### Example
 * ```
 * Maybe.just(Result.success(1))
 *     .load { Maybe.just(Result.success(2)) }
 *     .subscribe { result ->
 *         print("${result.data}")
 *     }
 *
 * // Print
 * 2
 * ```
 *
 * @param T the type of the data that gets loaded.
 * @param skip a function that return `true` to skip executing [mapper], which means the upstream
 * [Result] will be returned, or `false` to execute [mapper] and the [Result] from this [mapper]
 * will be returned.
 * @param mapper a function that return [Maybe]<[Result]<[T]>>.
 * @return a [Maybe]<[Result]<[T]>>.
 */
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

/**
 * Calls the shared consumer with the success value sent via [Result.isSuccess]
 * for each [MaybeObserver] that subscribes to the current [Maybe].
 *
 * @param T the type of the data that gets loaded.
 * @param onSuccess the consumer called with the success value of [Result.isSuccess].
 * @return the new [Maybe] instance.
 */
fun <T> Maybe<Result<T>>.doOnResultSuccess(onSuccess: (Result<T>) -> Unit): Maybe<Result<T>> {
    return doOnSuccess {
        if (it.isSuccess) {
            onSuccess(it)
        }
    }
}

/**
 * Calls the shared consumer with the success value sent via [Result.isFailure]
 * for each [MaybeObserver] that subscribes to the current [Maybe].
 *
 * @param T the type of the data that gets loaded.
 * @param onFailure the consumer called with the success value of [Result.isFailure].
 * @return the new [Maybe] instance.
 */
fun <T> Maybe<Result<T>>.doOnFailure(onFailure: (Result<T>) -> Unit): Maybe<Result<T>> {
    return doOnSuccess {
        if (it.isFailure) {
            onFailure(it)
        }
    }
}

/**
 * Calls the shared consumer with the success value sent via [Result.isLoading]
 * for each [MaybeObserver] that subscribes to the current [Maybe].
 *
 * @param T the type of the data that gets loaded.
 * @param onLoading the consumer called with the success value of [Result.isLoading].
 * @return the new [Maybe] instance.
 */
fun <T> Maybe<Result<T>>.doOnLoading(onLoading: (Result<T>) -> Unit): Maybe<Result<T>> {
    return doOnSuccess {
        if (it.isLoading) {
            onLoading(it)
        }
    }
}

/**
 * Wrap the data [T] with [Result].
 *
 * Returns a [Maybe] that wraps the item emitted by the source [Maybe] with
 * [Result].
 *
 * If the source [Maybe] encounters an error, then a [Maybe] with
 * [Result.error] will be returned. If the source [Maybe] is empty, then
 * a [Maybe] with [Result.success] with `null` data will be returned,
 * otherwise [Result.success] will be returned with item emitted by
 * the source [Maybe].
 *
 * @param T the type of the data that gets loaded.
 * @return a [Maybe] that emits the item from the source Maybe, transformed
 * by wrapping the item with [Result].
 */
fun <T> Maybe<T>.result(): Maybe<Result<T>> {
    return map { Result.success(it) }
        .defaultIfEmpty(Result.success())
        .onErrorReturn { Result.error(it) }
        .map { it }
}
