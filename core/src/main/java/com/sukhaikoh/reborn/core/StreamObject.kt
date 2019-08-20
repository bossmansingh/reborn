package com.sukhaikoh.roctopus.core

internal data class StreamObject<T>(
    val result: Result<T>,
    val shouldSkipDownstream: Boolean = false
)

internal fun <T> Result<T>.toStreamObject(shouldSkipDownstream: Boolean = false) =
    StreamObject(this, shouldSkipDownstream)