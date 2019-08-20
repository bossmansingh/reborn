package com.sukhaikoh.roctopus.core

import com.sukhaikoh.roctopus.core.Result.Companion
import io.reactivex.BackpressureStrategy
import io.reactivex.BackpressureStrategy.LATEST
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Flowable.error
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.exceptions.CompositeException
import org.reactivestreams.Subscriber

class Repository2<T> private constructor(
    private val stream: Flowable<Result<T>>
) : Flowable<Result<T>>() {

    override fun subscribeActual(s: Subscriber<in Result<T>>) {
        stream.subscribe(s)
    }

    companion object {
        fun <T> load(data: Flowable<T>): Repository2<T> = Repository2(data.mapWithResult())

        fun <T> load(data: Observable<T>, backpressureStrategy: BackpressureStrategy = LATEST): Repository2<T> =
            load(data.toFlowable(backpressureStrategy))

        fun <T> load(data: Single<T>): Repository2<T> = load(data.toFlowable())

        fun <T> load(data: Maybe<T>): Repository2<T> = load(data.toFlowable())

        fun <T> load(data: T): Repository2<T> = load(Flowable.just(data))
    }

    fun asFlowable(): Flowable<Result<T>> {
        return stream
            .switchIfEmpty { it.onNext(Result.success()) }
            .startWith(Result.loading())
    }

    fun asSingle(): Single<Result<T>> {
        return stream
            .filter { !it.isLoading }
            .first(Result.success())
    }

    fun asMaybe(): Maybe<Result<T>> {
        return stream
            .filter { !it.isLoading }
            .firstElement()
    }

    fun asObservable(): Observable<Result<T>> {
        return asFlowable()
            .toObservable()
    }

    fun asCompletable(): Completable {
        return stream
            .filter { !it.isLoading }
            .flatMap {
                if (it.isSuccess) {
                    just(it)
                } else {
                    error(it.cause)
                }
            }
            .ignoreElements()
    }

    fun load(
        predicate: (Result<T>) -> Flowable<Boolean> = { Flowable.just(false) },
        mapper: Repository2<T>.(Result<T>) -> Flowable<Result<T>>
    ): Repository2<T> {
        return Repository2(
            stream.flatMap { upstream ->
                try {
                    predicate(upstream)
                        .flatMap { skip ->
                            if (skip) {
                                just(upstream)
                            } else {
                                try {
                                    mapper(this, upstream)
                                } catch (t: Throwable) {
                                    error<Result<T>>(t)
                                }
                            }
                        }
                } catch (t: Throwable) {
                    error<Result<T>>(t)
                }.onErrorResumeNext { t: Throwable ->
                    just(if (upstream.cause != null) {
                        Result.error(CompositeException(upstream.cause, t), upstream.data)
                    } else {
                        upstream.toError(t)
                    })
                }.switchIfEmpty {
                    it.onNext(upstream.toSuccess())
                }
            }
        )
    }

    fun Flowable<Result<T>>.asNonPrimarySource(upstream: Result<T>): Flowable<Result<T>> {
        return map { upstream.toLoading() }
    }
}

fun <T> Flowable<Result<T>>.load(
    predicate: (Result<T>) -> Flowable<Boolean> = { Flowable.just(false) },
    mapper: (Result<T>) -> Flowable<Result<T>>
): Flowable<Result<T>> {
    return flatMap { upstream ->
        try {
            predicate(upstream)
                .switchIfEmpty { it.onNext(false) }
                .flatMap { skip ->
                    if (skip) {
                        Flowable.just(upstream)
                    } else {
                        try {
                            mapper(upstream)
                        } catch (t: Throwable) {
                            Flowable.error<Result<T>>(t)
                        }
                    }
                }
        } catch (t: Throwable) {
            Flowable.error<Result<T>>(t)
        }.onErrorResumeNext { t: Throwable ->
            Flowable.just(if (upstream.cause != null) {
                Result.error(CompositeException(upstream.cause, t), upstream.data)
            } else {
                upstream.toError(t)
            })
        }.switchIfEmpty {
            it.onNext(upstream.toSuccess())
        }
    }
}

inline fun <T> Flowable<Result<T>>.doOnSuccess(crossinline mapper: (Result<T>) -> Unit): Flowable<Result<T>> {
    return doOnNext {
        if (it.isSuccess) {
            mapper(it)
        }
    }
}

inline fun <T> Flowable<Result<T>>.doOnFailure(crossinline mapper: (Result<T>) -> Unit): Flowable<Result<T>> {
    return doOnNext {
        if (it.isFailure) {
            mapper(it)
        }
    }
}

inline fun <T> Flowable<Result<T>>.doOnLoading(crossinline mapper: (Result<T>) -> Unit): Flowable<Result<T>> {
    return doOnNext {
        if (it.isLoading) {
            mapper(it)
        }
    }
}

fun <T> Flowable<Result<T>>.asRepository(): Repository2<T> = this as Repository2<T>

fun <T> Flowable<T>.result(): Flowable<Result<T>> {
    return mapWithResult()
}

private fun <T> Flowable<T>.mapWithResult(): Flowable<Result<T>> {
    return map { Result.success(it) }
        .switchIfEmpty { Result.success<T>() }
        .onErrorReturn { Result.error(it) }
        .map { it }
}