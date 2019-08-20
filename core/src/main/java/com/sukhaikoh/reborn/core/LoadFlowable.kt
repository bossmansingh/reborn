package com.sukhaikoh.roctopus.core

import io.reactivex.Flowable

internal class LoadFlowable<T>(
    upstream: Flowable<StreamObject<T>>,
    private var block: ((Result<T>) -> Flowable<T>)? = null,
    private var configure: ((Options<T>) -> Unit)? = null
) : ApplyFlowable<T>(upstream) {

    override fun process(upstreamStreamObject: StreamObject<T>): Flowable<StreamObject<T>> {
        return if (block != null) {
            block!!.invoke(upstreamStreamObject.result)
                .map { StreamObject(Result.success(it)) }
        } else {
            empty()
        }
    }

    override fun configure(options: Options<T>) {
        this.configure?.invoke(options)
    }

    override fun onComplete() {
        super.onComplete()

        block = null
        configure = null
    }
}