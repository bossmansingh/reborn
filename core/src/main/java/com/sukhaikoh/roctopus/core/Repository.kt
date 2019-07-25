package com.sukhaikoh.roctopus.core

import io.reactivex.Flowable
import io.reactivex.Scheduler

class Repository<T> private constructor(
    private var subscribeScheduler: Scheduler?,
    private var stream: Flowable<StreamObject<T>>,
    private var isEmptyStream: Boolean = true
) {
    companion object {
        private var globalMainThreadScheduler: Scheduler? = null
        private var globalSubscribeOnScheduler: Scheduler? = null

        @JvmStatic
        @Synchronized
        fun setGlobalMainThreadScheduler(scheduler: Scheduler?) {
            globalMainThreadScheduler = scheduler
        }

        @JvmStatic
        @Synchronized
        fun setGlobalSubscribeOnScheduler(scheduler: Scheduler?) {
            globalSubscribeOnScheduler = scheduler
        }

        @JvmStatic
        @JvmOverloads
        fun <T> prepare(subscribeScheduler: Scheduler? = null) = Repository<T>(
            subscribeScheduler,
            Flowable.just(StreamObject(Result.loading()))
        )
    }

    var hasSetSubscribeScheduler: Boolean = false
        private set

    var hasSetObserveOnScheduler: Boolean = false
        private set

    fun execute(): Flowable<Result<T>> = if (isEmptyStream) {
        Flowable.empty()
    } else {
        if (subscribeScheduler == null) {
            subscribeScheduler = globalSubscribeOnScheduler
        }
        stream = if (subscribeScheduler != null) {
            hasSetSubscribeScheduler = true
            stream.subscribeOn(globalSubscribeOnScheduler!!)
        } else {
            stream
        }

        stream = if (globalMainThreadScheduler != null) {
            hasSetObserveOnScheduler = true
            stream.observeOn(globalMainThreadScheduler!!)
        } else {
            stream
        }

        stream.map { it.result }
            .startWith(Result.loading())
    }

    fun save(block: (Result<T>) -> Unit) = save(block, {})

    fun save(
        block: (Result<T>) -> Unit,
        options: (ExecutionOption<T>) -> Unit
    ): Repository<T> {
        return apply(options) { upstreamStreamObject ->
            Flowable.just(block(upstreamStreamObject.result))
                .map {
                    upstreamStreamObject.copy(
                        result = upstreamStreamObject.result.toSuccess()
                    )
                }
        }
    }

    fun load(block: (Result<T>) -> Flowable<T>) = load(block, {})

    fun load(
        block: (Result<T>) -> Flowable<T>,
        options: (ExecutionOption<T>) -> Unit
    ): Repository<T> {
        return apply(options) { upstreamStreamObject ->
            block(upstreamStreamObject.result)
                .map { StreamObject(Result.success(it)) }
        }
    }

    @Suppress("RemoveExplicitTypeArguments")
    private fun apply(
        options: (ExecutionOption<T>) -> Unit,
        block: (StreamObject<T>) -> Flowable<StreamObject<T>>
    ): Repository<T> {
        stream = stream
            .flatMap { upstreamStreamObject ->
                if (!upstreamStreamObject.shouldSkipDownstream) {
                    val option = ExecutionOption(upstreamStreamObject.result)
                    options(option)

                    if (!option.skip) {
                        var flowable = try {
                            block(upstreamStreamObject)
                        } catch (t: Throwable) {
                            Flowable.error<StreamObject<T>>(t)
                        }.onErrorResumeNext { t: Throwable ->
                            Flowable.just(
                                option.onErrorReturn(
                                    t,
                                    upstreamStreamObject.result
                                ).toStreamObject()
                            )
                        }.switchIfEmpty {
                            it.onNext(StreamObject(Result.success()))
                        }.doOnNext {
                            if (it.result.isSuccess) {
                                option.onSuccess?.invoke(it.result)
                            } else if (it.result.isFailure) {
                                option.onError?.invoke(it.result.cause!!)
                            }
                        }

                        if (option.startWithUpstreamResult) {
                            flowable = flowable.startWith(
                                upstreamStreamObject.copy(
                                    result = upstreamStreamObject.result.toLoading(),
                                    shouldSkipDownstream = true
                                )
                            )
                        }

                        return@flatMap flowable
                    }
                }

                Flowable.just(upstreamStreamObject)
            }

        isEmptyStream = false

        return this
    }
}