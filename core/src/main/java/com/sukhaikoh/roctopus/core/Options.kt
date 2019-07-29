package com.sukhaikoh.roctopus.core

data class Options<T> internal constructor(
    internal var startWithUpstreamResult: Boolean = false
) {
    var upstreamResult: Result<T> = Result.loading()
        internal set
    internal var onSuccess: OnSuccess<T>? = null
    internal var onError: OnError? = null
    internal val skip: Boolean
        get() = !filterPredicate.invoke(upstreamResult)
    internal var onErrorReturn: OnErrorReturn<T> = this::defaultOnErrorReturnHandling
    private var filterPredicate: Predicate<T> = this::defaultFilterHandling

    fun filter(predicate: Predicate<T>): Options<T> {
        filterPredicate = predicate
        return this
    }

    fun startWithUpstreamResult(): Options<T> {
        return startWithUpstreamResult { true }
    }

    fun startWithUpstreamResult(predicate: Predicate<T>): Options<T> {
        startWithUpstreamResult = predicate.invoke(upstreamResult)
        return this
    }

    fun onSuccess(handler: OnSuccess<T>): Options<T> {
        this.onSuccess = handler
        return this
    }

    fun onError(handler: OnError): Options<T> {
        this.onError = handler
        return this
    }

    fun onComplete(
        onSuccess: OnSuccess<T> = {},
        onError: OnError = {}
    ): Options<T> {
        this.onSuccess = onSuccess
        this.onError = onError
        return this
    }

    fun onErrorReturn(block: OnErrorReturn<T>): Options<T> {
        this.onErrorReturn = block
        return this
    }

    internal fun dispose() {

    }

    private fun defaultOnErrorReturnHandling(
        t: Throwable,
        upstreamResult: Result<T>
    ) = Result.error(t, upstreamResult.data)

    private fun defaultFilterHandling(upstreamResult: Result<T>) =
        !upstreamResult.isFailure
}