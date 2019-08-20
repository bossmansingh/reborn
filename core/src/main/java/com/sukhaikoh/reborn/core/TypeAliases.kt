package com.sukhaikoh.reborn.core

typealias Predicate<T> = (Result<T>) -> Boolean
typealias OnErrorReturn<T> = (Throwable, Result<T>) -> Result<T>
typealias OnSuccess<T> = (Result<T>) -> Unit
typealias OnError = (Throwable) -> Unit