package com.sukhaikoh.roctopus.core.rate

interface Limit {
    fun shouldProcess(key: Any? = null): Boolean
}