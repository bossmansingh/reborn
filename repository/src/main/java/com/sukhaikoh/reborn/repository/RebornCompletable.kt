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
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.schedulers.Schedulers

class RebornCompletable private constructor()

/**
 * Returns a [Flowable] which will subscribe to this [Completable] and once that is
 * completed then will subscribe to the next [Flowable] from [mapper].
 * An error event from this [Completable] will result in [Flowable] with [Result.error]
 * being returned.
 *
 * If [skip] returns `true`, then the [Flowable] will contains [Result.success]
 * with `null` data. If [skip] returns `false`, then the [Flowable] will contains
 * [Result] returning from [mapper]. If the [mapper] has any error, then
 * [Result.error] will be in the returned [Flowable].
 *
 * ### Example
 * ```
 * Completable.complete()
 *     .load<Int> {
 *         Flowable.just(
 *             Result.success(1),
 *             Result.success(2),
 *             Result.success(3)
 *         )
 *     }
 *     .subscribe { result ->
 *         print("${result.data} ")
 *     }
 *
 * // Print
 * 1 2 3
 * ```
 *
 * @param T the type of the data that gets loaded.
 * @param skip a function that return `true` to skip executing [mapper], which means the upstream
 * [Result] will be returned, or `false` to execute [mapper] and the [Result] from this [mapper]
 * will be returned.
 * @param mapper a function that return [Flowable]<[Result]<[T]>>.
 * @return a [Flowable]<[Result]<[T]>>.
 */
fun <T> Completable.load(
    skip: (Result<T>) -> Boolean = { false },
    mapper: (Result<T>) -> Flowable<Result<T>>
): Flowable<Result<T>> {
    return andThen(Flowable.just(Result.success<T>()))
        .onErrorReturn { Result.error(it) }
        .load(skip, mapper)
}

/**
 * Returns a [Single] which will subscribe to this [Completable] and once that
 * is completed then will subscribe to the next [SingleSource] that emits
 * [Result.success]. An error event from this [Completable] will be propagated
 * to the downstream subscriber and will result in receiving [Result.error].
 *
 * @return Single that composes this Completable and next.
 */
fun Completable.result(): Single<Result<Unit>> {
    return andThen(Single.just(Result.success<Unit>()))
        .onErrorResumeNext {
            Single.just(Result.error(it))
        }
}

/**
 * Returns a [Single] which will subscribe to this [Completable] and once that
 * is completed then will subscribe to the next [SingleSource] that emits
 * [Result.success]. An error event from this [Completable] will be propagated
 * to the downstream subscriber and will result in receiving [Result.error].
 *
 * This [Completable] will also gets set [Completable.subscribeOn] with the
 * [scheduler] if it hasn't done so.
 *
 * @param scheduler the [Scheduler] that source [Completable] will subscribe on.
 * @return Single that composes this Completable and next.
 */
fun Completable.execute(scheduler: Scheduler = Schedulers.io()): Single<Result<Unit>> {
    return subscribeOn(scheduler)
        .result()
}