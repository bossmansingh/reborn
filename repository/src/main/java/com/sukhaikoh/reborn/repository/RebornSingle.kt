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
import io.reactivex.SingleObserver
import io.reactivex.exceptions.CompositeException

class RebornSingle private constructor() {

    companion object {
        /**
         * Load the [data] and return as a [Single]<[Result]<[T]>>.
         *
         * This is a short hand of writing:
         * ```
         * Single.just("my data").result()
         * ```
         *
         * ### Example
         * ```
         * RebornSingle.load(123)
         *     .subscribe { result ->
         *         print(result.data)
         *     }
         *
         * // Print
         * 123
         * ```
         *
         * @param T the type of the data that gets loaded.
         * @param data the data to be returned in a [Single].
         * @return a [Single]<[Result]<[T]>>.
         */
        @JvmStatic
        fun <T> load(data: T): Single<Result<T>> {
            return Single.just(data)
                .result()
        }

        /**
         * Transform the given [single] of type [T] into a [Single]<[Result]<[T]>>.
         *
         * This is a short hand of writing:
         * ```
         * val single = Single.just(123)
         *
         * single.result()
         * ```
         *
         * ### Example
         * ```
         * RebornSingle.load(Single.just(123))
         *     .subscribe { result ->
         *         print(result.data)
         *     }
         *
         * // Print
         * 123
         * ```
         *
         * @param T the type of the data that gets loaded.
         * @param single the [Single].
         * @return a [Single]<[Result]<[T]>>.
         */
        @JvmStatic
        fun <T> load(single: Single<T>): Single<Result<T>> {
            return single.result()
        }
    }
}

/**
 * Returns a [Single] that is based on applying a specified function to the item emitted
 * by the upstream, where that function returns a [Single]<[Result]<[T]>>.
 *
 * If [skip] returns `true`, then the upstream [Single] will be returned. If [skip] returns
 * `false`, result from [mapper] will be returned. If the [mapper] has any error, then
 * [Result.error] will be in the returned [Single], otherwise result from [mapper]
 * will be returned.
 *
 * ### Example
 * ```
 * Single.just(Result.success(1))
 *     .load { Single.just(Result.success(2)) }
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
 * @param mapper a function that return [Single]<[Result]<[T]>>.
 * @return a [Single]<[Result]<[T]>>.
 */
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

/**
 * Calls the shared consumer with the success value sent via [Result.isSuccess]
 * for each [SingleObserver] that subscribes to the current [Single].
 *
 * @param T the type of the data that gets loaded.
 * @param onSuccess the consumer called with the success value of [Result.isSuccess].
 * @return the new [Single] instance.
 */
fun <T> Single<Result<T>>.doOnResultSuccess(onSuccess: (Result<T>) -> Unit): Single<Result<T>> {
    return doOnSuccess {
        if (it.isSuccess) {
            onSuccess(it)
        }
    }
}

/**
 * Calls the shared consumer with the success value sent via [Result.isFailure]
 * for each [SingleObserver] that subscribes to the current [Single].
 *
 * @param T the type of the data that gets loaded.
 * @param onFailure the consumer called with the success value of [Result.isFailure].
 * @return the new [Single] instance.
 */
fun <T> Single<Result<T>>.doOnFailure(onFailure: (Result<T>) -> Unit): Single<Result<T>> {
    return doOnSuccess {
        if (it.isFailure) {
            onFailure(it)
        }
    }
}

/**
 * Calls the shared consumer with the success value sent via [Result.isLoading]
 * for each [SingleObserver] that subscribes to the current [Single].
 *
 * @param T the type of the data that gets loaded.
 * @param onLoading the consumer called with the success value of [Result.isLoading].
 * @return the new [Single] instance.
 */
fun <T> Single<Result<T>>.doOnLoading(onLoading: (Result<T>) -> Unit): Single<Result<T>> {
    return doOnSuccess {
        if (it.isLoading) {
            onLoading(it)
        }
    }
}

/**
 * Wrap the data [T] with [Result].
 *
 * Returns a [Single] that wraps the item emitted by the source [Single] with
 * [Result].
 *
 * If the source [Single] encounters an error, then a [Single] with
 * [Result.error] will be returned, otherwise [Result.success] will be
 * returned with item emitted by the source [Single].
 *
 * @param T the type of the data that gets loaded.
 * @return a [Single] that emits the item from the source Single, transformed
 * by wrapping the item with [Result].
 */
fun <T> Single<T>.result(): Single<Result<T>> {
    return map { Result.success(it) }
        .onErrorReturn { Result.error(it) }
        .map { it }
}