package com.sukhaikoh.reborn.core.rate

class CountLimit(
    private val count: Int
) : Limit {
    private val map = mutableMapOf<Any, Int>()
    private var nullKeyCount = 0

    override fun shouldProcess(key: Any?): Boolean {
        return if (key == null) {
            nullKeyCount++ < count
        } else {
            if (map.containsKey(key)) {
                val currentCount = map[key]!!
                val shouldProcess = currentCount < count
                map[key] = currentCount + 1
                shouldProcess
            } else {
                map[key] = 1
                count > 0
            }
        }
    }
}