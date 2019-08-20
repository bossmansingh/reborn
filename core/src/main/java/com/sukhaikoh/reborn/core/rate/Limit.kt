package com.sukhaikoh.reborn.core.rate

interface Limit {
    fun shouldProcess(key: Any? = null): Boolean
}