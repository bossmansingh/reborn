package com.sukhaikoh.reborn.core.rate

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class TimeLimitTest {

    private lateinit var limit: TimeLimit

    private val timeout = 2L
    private val timeUnit = TimeUnit.MILLISECONDS

    @BeforeEach
    fun setup() {
        limit = TimeLimit(timeout / 2, timeUnit)
    }

    @Test
    fun `shouldProcess with null key then return true`() {
        assertTrue(limit.shouldProcess(null))
    }

    @Test
    fun `shouldProcess with null key and call again within timeout then return false`() {
        assertTrue(limit.shouldProcess(null))
        assertFalse(limit.shouldProcess(null))
    }

    @Test
    fun `shouldProcess with null key and call again after timeout then return true`() {
        assertTrue(limit.shouldProcess(null))

        Thread.sleep(timeout)

        assertTrue(limit.shouldProcess(null))
    }

    @Test
    fun `multiple shouldProcess with null key and timeout then return correct result`() {
        assertTrue(limit.shouldProcess(null))

        Thread.sleep(timeout)
        assertTrue(limit.shouldProcess(null))
        assertFalse(limit.shouldProcess(null))

        Thread.sleep(timeout)
        assertTrue(limit.shouldProcess(null))
    }

    @Test
    fun `shouldProcess with a key then return true`() {
        assertTrue(limit.shouldProcess("a key"))
    }

    @Test
    fun `shouldProcess with a key and call again within timeout then return false`() {
        assertTrue(limit.shouldProcess("a key"))
        assertFalse(limit.shouldProcess("a key"))
    }

    @Test
    fun `shouldProcess with a key and call again after timeout then return true`() {
        assertTrue(limit.shouldProcess("a key"))

        Thread.sleep(timeout)

        assertTrue(limit.shouldProcess("a key"))
    }

    @Test
    fun `multiple shouldProcess with a key and timeout then return correct result`() {
        val key = "a key"

        assertTrue(limit.shouldProcess(key))

        Thread.sleep(timeout)
        assertTrue(limit.shouldProcess(key))
        assertFalse(limit.shouldProcess(key))

        Thread.sleep(timeout)
        assertTrue(limit.shouldProcess(key))
        assertFalse(limit.shouldProcess(key))
    }

    @Test
    fun `multiple shouldProcess with different keys and timeout then return correct result`() {
        val key1 = "123"
        val key2 = 123

        assertTrue(limit.shouldProcess(key1))
        assertFalse(limit.shouldProcess(key1))
        assertTrue(limit.shouldProcess(key2))
        assertFalse(limit.shouldProcess(key2))

        Thread.sleep(timeout)
        assertTrue(limit.shouldProcess(key2))
        assertFalse(limit.shouldProcess(key2))

        Thread.sleep(timeout)
        assertTrue(limit.shouldProcess(key2))
        assertTrue(limit.shouldProcess(key1))
    }
}