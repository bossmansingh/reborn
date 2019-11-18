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
import io.reactivex.Scheduler
import io.reactivex.exceptions.CompositeException
import io.reactivex.schedulers.Schedulers

class RebornFlowable private constructor() {

    companion object {
        /**
         * Load the [data] and return as a [Flowable]<[Result]<[T]>>.
         *
         * This is a short hand of writing:
         * ```
         * Flowable.just("my data").result()
         * ```
         *
         * ### Example
         * ```
         * RebornFlowable.load(123)
         *     .subscribe { result ->
         *         print(result.data)
         *     }
         *
         * // Print
         * 123
         * ```
         *
         * @param T the type of the data that gets loaded.
         * @param data the data to be returned in a [Flowable].
         * @return a [Flowable]<[Result]<[T]>>.
         */
        @JvmStatic
        fun <T> load(data: T): Flowable<Result<T>> {
            return Flowable.just(data)
                .result()
        }

        /**
         * Transform the given [flowable] of type [T] into a [Flowable]<[Result]<[T]>>.
         *
         * This is a short hand of writing:
         * ```
         * val flowable = Flowable.just(123)
         *
         * flowable.result()
         * ```
         *
         * ### Example
         * ```
         * RebornFlowable.load(Flowable.just(123))
         *     .subscribe { result ->
         *         print(result.data)
         *     }
         *
         * // Print
         * 123
         * ```
         *
         * @param T the type of the data that gets loaded.
         * @param flowable the [Flowable].
         * @return a [Flowable]<[Result]<[T]>>.
         */
        @JvmStatic
        fun <T> load(flowable: Flowable<T>): Flowable<Result<T>> {
            return flowable.result()
        }
    }
}

/**
 * Returns a [Flowable] that is based on applying a specified function to the item emitted
 * by the upstream, where that function returns a [Flowable]<[Result]<[T]>>.
 *
 * If [skip] returns `true`, then the upstream [Flowable] will be returned. If [skip] returns
 * `false`, result from [mapper] will be returned. If the [mapper] has any error, then
 * [Result.error] will be in the returned [Flowable]. If the [mapper] is empty, upstream
 * data with [Result.success] will be returned.
 *
 * ### Example
 * ```
 * Flowable.just(Result.success(1))
 *     .load { Flowable.just(Result.success(2)) }
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
 * @param mapper a function that return [Flowable]<[Result]<[T]>>.
 * @return a [Flowable]<[Result]<[T]>>.
 */
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

/**
 * Modifies the source Publisher so that it invokes an action when it calls onNext with
 * [Result.isSuccess].
 *
 * @param T the type of the data that gets loaded.
 * @param onSuccess the consumer called with the success value of [Result.isSuccess].
 * @return the new [Flowable] instance.
 */
fun <T> Flowable<Result<T>>.doOnSuccess(onSuccess: (Result<T>) -> Unit): Flowable<Result<T>> {
    return doOnNext {
        if (it.isSuccess) {
            onSuccess(it)
        }
    }
}

/**
 * Modifies the source Publisher so that it invokes an action when it calls onNext with
 * [Result.isFailure].
 *
 * @param T the type of the data that gets loaded.
 * @param onFailure the consumer called with the success value of [Result.isFailure].
 * @return the new [Flowable] instance.
 */
fun <T> Flowable<Result<T>>.doOnFailure(onFailure: (Result<T>) -> Unit): Flowable<Result<T>> {
    return doOnNext {
        if (it.isFailure) {
            onFailure(it)
        }
    }
}

/**
 * Modifies the source Publisher so that it invokes an action when it calls onNext with
 * [Result.isLoading].
 *
 * @param T the type of the data that gets loaded.
 * @param onLoading the consumer called with the success value of [Result.isLoading].
 * @return the new [Flowable] instance.
 */
fun <T> Flowable<Result<T>>.doOnLoading(onLoading: (Result<T>) -> Unit): Flowable<Result<T>> {
    return doOnNext {
        if (it.isLoading) {
            onLoading(it)
        }
    }
}

/**
 * Wrap the data [T] with [Result].
 *
 * Returns a [Flowable] that wraps the item emitted by the source [Flowable] with
 * [Result].
 *
 * If the source [Flowable] encounters an error, then a [Flowable] with
 * [Result.error] will be returned. If the source [Flowable] is empty, then
 * a [Flowable] with [Result.success] with `null` data will be returned,
 * otherwise [Result.success] will be returned with item emitted by
 * the source [Flowable].
 *
 * @param T the type of the data that gets loaded.
 * @return a [Flowable] that emits the item from the source Flowable, transformed
 * by wrapping the item with [Result].
 */
fun <T> Flowable<T>.result(): Flowable<Result<T>> {
    return map { Result.success(it) }
        .switchIfEmpty { it.onNext(Result.success()) }
        .onErrorReturn { Result.error(it) }
        .map { it }
}

/**
 * Wrap the data [T] with [Result] and start the stream with [Result.loading].
 *
 * @param T the type of the data that gets loaded.
 * @param scheduler the [Scheduler] the source [Flowable] will subscribe on. This [scheduler]
 * will not be set if the source [Flowable] has already subscribed on to another [Scheduler].
 * @return a [Flowable] that emits [Result.loading], then emits the item from the source
 * [Flowable], transformed by wrapping the item with [Result].
 */
fun <T> Flowable<T>.execute(scheduler: Scheduler = Schedulers.io()): Flowable<Result<T>> {
    return subscribeOn(scheduler)
        .result()
        .startWith(Result.loading())
}