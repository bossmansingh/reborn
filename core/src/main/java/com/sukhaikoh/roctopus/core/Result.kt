package com.sukhaikoh.roctopus.core

/**
 * A class that holds a value representing the result from a request. You
 * can create an instance of this class by its factory methods
 * [Result.loading], [Result.success], or [Result.error].
 *
 * @param data The value hold by this class if any, `null` if no value is held
 * by this class.
 */
sealed class Result<out T>(open val data: T? = null) {
    /**
     * Return `true` if this result is in loading state, `false` otherwise.
     * In this case [isSuccess] and [isFailure] will return `false`.
     */
    val isLoading: Boolean get() = this is Loading

    /**
     * Return `true` if this is a successful result, `false` otherwise.
     * In this case [isFailure] and [isLoading] will return `false`.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Return `true` if this is a failure result, meaning it failed to get
     * the expected result, `false` otherwise.
     * In this case [isSuccess] and [isLoading] will return `false`.
     */
    val isFailure: Boolean get() = this is Error

    /**
     * Return the cause of [isFailure] when [isFailure] return `true`, `null`
     * otherwise.
     */
    val cause: Throwable? get() = if (this is Error) throwable else null

    /**
     * A handy method to handle when [isLoading] return `true`.
     *
     * ### Example
     * ```
     * val result = Result.loading("my data")
     * result.onLoading {
     *     Log.d("Data: ${data}.")
     * }
     * ```
     *
     * @param handler The handler that will get called when [isLoading] is
     * `true`, otherwise this handler will not get called.
     */
    fun onLoading(handler: Result<T>.() -> Unit) {
        if (isLoading) {
            handler(this)
        }
    }

    /**
     * A handy method to handle result loading state, whether [isLoading] is
     * returning `true` or `false`.
     *
     * ### Example
     * ```
     * val result = Result.loading("my data")
     * result.onLoading({
     *     Log.d("On loading data: ${data}.")
     * }, {
     *     Log.d("On not loading data:${data}.")
     * })
     * ```
     *
     * @param onLoading The handler that will get called when [isLoading] is
     * `true`, otherwise this handler will not get called.
     * @param onNotLoading The handler that will get called when [isLoading] is
     * `false`, otherwise this handler will not get called.
     */
    fun onLoading(
        onLoading: Result<T>.() -> Unit,
        onNotLoading: Result<T>.() -> Unit
    ) {
        if (isLoading) {
            onLoading(this)
        } else {
            onNotLoading(this)
        }
    }

    /**
     * A handy method to handle when [isSuccess] return `true`.
     *
     * ### Example
     * ```
     * val result = Result.success("my data")
     * result.onSuccess {
     *     Log.d("Data: ${data}.")
     * }
     * ```
     *
     * @param handler The handler that will get called when [isSuccess] is
     * `true`, otherwise this handler will not get called.
     */
    fun onSuccess(handler: Result<T>.() -> Unit) {
        if (isSuccess) {
            handler(this)
        }
    }

    /**
     * A handy method to handle when [isFailure] return `true`.
     *
     * ### Example
     * ```
     * val result = Result.error(Throwable())
     * result.onFailure { throwable ->
     *     Log.d("Data: ${data}.")
     *     Log.e(throwable)
     * }
     * ```
     *
     * @param handler The handler that will get called when [isSuccess] is
     * `true`, otherwise this handler will not get called.
     */
    fun onFailure(handler: Result<T>.(Throwable) -> Unit) {
        if (this is Error) {
            handler(this, throwable)
        }
    }

    /**
     * Convert this result to a loading result. In other words, the returned
     * result will have [isLoading] return `true`.
     *
     * @return An instance of [Result] with [isLoading] return `true`.
     */
    fun toLoading(): Result<T> = loading(data)

    /**
     * Convert this result to a successful result. In other words, the returned
     * result will have [isSuccess] return `true`.
     *
     * @return An instance of [Result] with [isSuccess] return `true`.
     */
    fun toSuccess(): Result<T> = success(data)

    /**
     * Convert this result to a failure result. In other words, the returned
     * result will have [isFailure] return `true`.
     *
     * @return An instance of [Result] with [isFailure] return `true`.
     */
    fun toError(throwable: Throwable): Result<T> = error(throwable, data)

    companion object {
        /**
         * Create a [Result] that represents the request is still in progress.
         *
         * Note that calling [Result.isLoading] with the returned [Result]
         * object will always return `true`.
         *
         * @param data The value hold by this class if any, `null` if no value
         * is held by this class.
         */
        @JvmStatic
        fun <T> loading(data: T? = null): Result<T> = Loading(data)

        /**
         * Create a [Result] that represents the request was successful.
         *
         * Note that calling [Result.isSuccess] with the returned [Result]
         * object will always return `true`.
         *
         * @param data The value hold by this class if any, `null` if no value
         * is held by this class.
         */
        @JvmStatic
        fun <T> success(data: T? = null): Result<T> = Success(data)

        /**
         * Create a [Result] that represents the request has failed.
         *
         * Note that calling [Result.isFailure] with the returned [Result]
         * object will always return `true`.
         *
         * @param throwable The error that caused the request to fail if any.
         * @param data The value hold by this class if any, `null` if no value
         * is held by this class.
         */
        @JvmStatic
        fun <T> error(
            throwable: Throwable,
            data: T? = null
        ): Result<T> = Error(throwable, data)
    }

    /**
     * A [Result] type that represents the request was successful.
     *
     * @param data The value hold by this class if any, `null` if no value is
     * held by this class.
     */
    private data class Success<out T>(
        override val data: T? = null
    ) : Result<T>(data)

    /**
     * A [Result] type that represents the request is still in progress.
     *
     * @param data The value hold by this class if any, `null` if no value is
     * held by this class.
     */
    private data class Loading<out T>(
        override val data: T? = null
    ) : Result<T>(data)

    /**
     * A [Result] type that represents the request was not successful.
     *
     * @param throwable The error that caused the request to fail if any.
     * @param data The value hold by this class if any, `null` if no value is
     * held by this class.
     */
    private data class Error<out T>(
        val throwable: Throwable,
        override val data: T? = null
    ) : Result<T>(data)
}