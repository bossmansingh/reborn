package com.sukhaikoh.reborn.core.rate

import java.util.concurrent.TimeUnit

class Rate private constructor(
    private val limit: Limit
) {
    private var key: Any? = null

    fun withKey(key: Any?): Rate {
        this.key = key
        return this
    }

    internal fun shouldProcess() = limit.shouldProcess(key)

    companion object {
        @JvmStatic
        fun once(): Rate {
            return Rate(CountLimit(1))
        }

        @JvmStatic
        fun count(count: Int): Rate {
            return Rate(CountLimit(count))
        }

        @JvmStatic
        fun timer(timeout: Long, timeUnit: TimeUnit): Rate {
            return Rate(TimeLimit(timeout, timeUnit))
        }

        @JvmStatic
        fun limitWith(limit: Limit): Rate {
            return Rate(limit)
        }
    }
}