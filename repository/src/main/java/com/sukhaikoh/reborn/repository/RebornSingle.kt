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