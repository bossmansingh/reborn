package com.sukhaikoh.roctopus.core

import io.reactivex.Flowable
import org.reactivestreams.Subscriber

internal abstract class ApplyFlowable<T>(
    private val upstream: Flowable<StreamObject<T>>
) : Flowable<StreamObject<T>>() {

    private val options = Options<T>()

    abstract fun configure(options: Options<T>)

    abstract fun process(upstreamStreamObject: StreamObject<T>): Flowable<StreamObject<T>>

    open fun onComplete() {}

    override fun subscribeActual(s: Subscriber<in StreamObject<T>>) {

        upstream.flatMap { upstreamStreamObject ->
            if (!upstreamStreamObject.shouldSkipDownstream) {
                options.upstreamResult = upstreamStreamObject.result
                configure(options)

                if (!options.skip) {
                    val shouldProcess = options.rate?.shouldProcess() ?: true

                    if (shouldProcess) {
                        var flowable = try {
                            process(upstreamStreamObject)
                        } catch (t: Throwable) {
                            error<StreamObject<T>>(t)
                        }.onErrorResumeNext { t: Throwable ->
                            just(
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
            just(upstreamStreamObject)

        }.doOnComplete {
            onComplete()
        }.subscribe(s)
    }
}