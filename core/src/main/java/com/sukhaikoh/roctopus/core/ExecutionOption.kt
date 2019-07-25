package com.sukhaikoh.roctopus.core

data class ExecutionOption<T> internal constructor(
    val upstreamResult: Result<T>
) {
    internal var onSuccess: OnSuccess<T>? = null
    internal var onError: OnError? = null
    internal val skip: Boolean
        get() = !filterPredicate.invoke(upstreamResult)
    internal val startWithUpstreamResult: Boolean
        get() = startWithUpstreamResultPredicate.invoke(upstreamResult)
    internal var onErrorReturn: OnErrorReturn<T> =
        this::defaultOnErrorReturnHandling
    private var filterPredicate: Predicate<T> =
        this::defaultFilterHandling
    private var startWithUpstreamResultPredicate: Predicate<T> = { false }

    fun filter(predicate: Predicate<T>): ExecutionOption<T> {
        filterPredicate = predicate
        return this
    }

    fun startWithUpstreamResult(): ExecutionOption<T> {
        return startWithUpstreamResult { true }
    }

    fun startWithUpstreamResult(predicate: Predicate<T>): ExecutionOption<T> {
        startWithUpstreamResultPredicate = predicate
        return this
    }

    fun onSuccess(handler: OnSuccess<T>): ExecutionOption<T> {
        this.onSuccess = handler
        return this
    }

    fun onError(handler: OnError): ExecutionOption<T> {
        this.onError = handler
        return this
    }

    fun onComplete(
        onSuccess: OnSuccess<T> = {},
        onError: OnError = {}
    ): ExecutionOption<T> {
        this.onSuccess = onSuccess
        this.onError = onError
        return this
    }

    fun onErrorReturn(block: OnErrorReturn<T>): ExecutionOption<T> {
        this.onErrorReturn = block
        return this
    }

    private fun defaultOnErrorReturnHandling(
        t: Throwable,
        upstreamResult: Result<T>
    ) = Result.error(t, upstreamResult.data)

    private fun defaultFilterHandling(upstreamResult: Result<T>) =
        !upstreamResult.isFailure
}