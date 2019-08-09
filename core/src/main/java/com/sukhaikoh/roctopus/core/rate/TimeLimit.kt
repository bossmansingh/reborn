package com.sukhaikoh.roctopus.core.rate

import java.util.concurrent.TimeUnit

class TimeLimit(
    timeout: Long,
    timeUnit: TimeUnit
) : Limit {
    private val timeoutInMillis = timeUnit.toMillis(timeout)
    private val map = mutableMapOf<Any, Long>()
    private var nullKeyLastCheckedTime = -1L

    @Synchronized
    override fun shouldProcess(key: Any?): Boolean {
        val now = System.currentTimeMillis()

        return if (key == null) {
            if (now - nullKeyLastCheckedTime > timeoutInMillis ||
                nullKeyLastCheckedTime == -1L) {
                nullKeyLastCheckedTime = now
                true
            } else {
                false
            }
        } else {
            if (map.containsKey(key)) {
                val lastCheckedTime = map[key]!!
                if (now - lastCheckedTime > timeoutInMillis) {
                    map[key] = now
                    true
                } else {
                    false
                }
            } else {
                map[key] = now
                true
            }
        }
    }
}