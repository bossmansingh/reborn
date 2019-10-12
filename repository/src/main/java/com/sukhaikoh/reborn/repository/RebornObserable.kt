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
import io.reactivex.ObservableSource
import io.reactivex.exceptions.CompositeException

class RebornObserable private constructor() {

    companion object {
        /**
         * Load the [data] and return as an [Observable]<[Result]<[T]>>.
         *
         * This is a short hand of writing:
         * ```
         * Observable.just("my data").result()
         * ```
         *
         * ### Example
         * ```
         * RebornObservable.load(123)
         *     .subscribe { result ->
         *         print(result.data)
         *     }
         *
         * // Print
         * 123
         * ```
         *
         * @param T the type of the data that gets loaded.
         * @param data the data to be returned in a [Observable].
         * @return an [Observable]<[Result]<[T]>>.
         */
        @JvmStatic
        fun <T> load(data: T): Observable<Result<T>> {
            return Observable.just(data)
                .result()
        }

        /**
         * Transform the given [Observable] of type [T] into an [Observable]<[Result]<[T]>>.
         *
         * This is a short hand of writing:
         * ```
         * val observable = Observable.just(123)
         *
         * observable.result()
         * ```
         *
         * ### Example
         * ```
         * RebornObservable.load(Observable.just(123))
         *     .subscribe { result ->
         *         print(result.data)
         *     }
         *
         * // Print
         * 123
         * ```
         *
         * @param T the type of the data that gets loaded.
         * @param observable the [Observable].
         * @return an [Observable]<[Result]<[T]>>.
         */
        @JvmStatic
        fun <T> load(observable: Observable<T>): Observable<Result<T>> {
            return observable.result()
        }
    }
}

/**
 * Returns an [Observable] that is based on applying a specified function to the item emitted
 * by the upstream, where that function returns an [Observable]<[Result]<[T]>>.
 *
 * If [skip] returns `true`, then the upstream [Observable] will be returned. If [skip] returns
 * `false`, result from [mapper] will be returned. If the [mapper] has any error, then
 * [Result.error] will be in the returned [Observable]. If the [mapper] is empty, upstream
 * data with [Result.success] will be returned.
 *
 * ### Example
 * ```
 * Observable.just(Result.success(1))
 *     .load { Observable.just(Result.success(2)) }
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
 * @param mapper a function that return [Observable]<[Result]<[T]>>.
 * @return an [Observable]<[Result]<[T]>>.
 */
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

/**
 * Modifies the source [ObservableSource] so that it invokes an action when it calls onNext with
 * [Result.isSuccess].
 *
 * @param T the type of the data that gets loaded.
 * @param onSuccess the consumer called with the success value of [Result.isSuccess].
 * @return the new [Observable] instance.
 */
fun <T> Observable<Result<T>>.doOnSuccess(onSuccess: (Result<T>) -> Unit): Observable<Result<T>> {
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
 * @return the new [Observable] instance.
 */
fun <T> Observable<Result<T>>.doOnFailure(onFailure: (Result<T>) -> Unit): Observable<Result<T>> {
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
 * @return the new [Observable] instance.
 */
fun <T> Observable<Result<T>>.doOnLoading(onLoading: (Result<T>) -> Unit): Observable<Result<T>> {
    return doOnNext {
        if (it.isLoading) {
            onLoading(it)
        }
    }
}

/**
 * Wrap the data [T] with [Result].
 *
 * Returns an [Observable] that wraps the item emitted by the source [Observable] with
 * [Result].
 *
 * If the source [Observable] encounters an error, then an [Observable] with
 * [Result.error] will be returned. If the source [Observable] is empty, then
 * an [Observable] with [Result.success] with `null` data will be returned,
 * otherwise [Result.success] will be returned with item emitted by
 * the source [Observable].
 *
 * @param T the type of the data that gets loaded.
 * @return an [Observable] that emits the item from the source Observable, transformed
 * by wrapping the item with [Result].
 */
fun <T> Observable<T>.result(): Observable<Result<T>> {
    return map { Result.success(it) }
        .switchIfEmpty { it.onNext(Result.success()) }
        .onErrorReturn { Result.error(it) }
        .map { it }
}