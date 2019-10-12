package com.sukhaikoh.reborn.repository

import com.sukhaikoh.reborn.result.Result
import io.reactivex.Completable
import io.reactivex.Flowable

class RebornCompletable private constructor()

fun <T> Completable.load(
    skip: (Result<T>) -> Boolean = { false },
    mapper: (Result<T>) -> Flowable<Result<T>>
): Flowable<Result<T>> {
    return andThen(Flowable.just(Result.success<T>()))
        .onErrorReturn { Result.error(it) }
        .load(skip, mapper)
}