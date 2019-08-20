package com.sukhaikoh.roctopus.core

import io.reactivex.Flowable

class FlowableRepository<T>(
    private val stream: Flowable<Result<T>>
) {
}