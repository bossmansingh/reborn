package com.sukhaikoh.reborn.core

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
        configure: (Options<T>) -> Unit
    ): Repository<T> {
        return apply(configure) { upstreamStreamObject ->
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
        configure: (Options<T>) -> Unit
    ): Repository<T> {
        return apply(configure) { upstreamStreamObject ->
            block(upstreamStreamObject.result)
                .map { StreamObject(Result.success(it)) }
        }
    }

    @Suppress("RemoveExplicitTypeArguments")
    private fun apply(
        configure: (Options<T>) -> Unit,
        block: (StreamObject<T>) -> Flowable<StreamObject<T>>
    ): Repository<T> {

        val options = Options<T>()

        stream = stream
            .flatMap { upstreamStreamObject ->
                if (!upstreamStreamObject.shouldSkipDownstream) {
                    options.upstreamResult = upstreamStreamObject.result
                    configure(options)

                    if (!options.skip) {
                        val shouldProcess = options.rate?.shouldProcess() ?: true

                        if (shouldProcess) {
                            var flowable = try {
                                block(upstreamStreamObject)
                            } catch (t: Throwable) {
                                Flowable.error<StreamObject<T>>(t)
                            }.onErrorResumeNext { t: Throwable ->
                                Flowable.just(
                                    options.onErrorReturn(
                                        t,
                                        upstreamStreamObject.result
                                    ).toStreamObject()
                                )
                            }.switchIfEmpty {
                                it.onNext(StreamObject(Result.success()))
                            }.doOnNext {
                                if (it.result.isSuccess) {
                                    options.onSuccess?.invoke(it.result)
                                } else if (it.result.isFailure) {
                                    options.onError?.invoke(it.result.cause!!)
                                }
                            }.filter { !options.ignore }

                            if (options.startWithUpstreamResult) {
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
                }

                Flowable.just(upstreamStreamObject)
            }

        isEmptyStream = false

        return this
    }
}