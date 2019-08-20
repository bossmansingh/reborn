package com.sukhaikoh.reborn.core.rate

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CountLimitTest {

    private lateinit var limit: CountLimit

    @Test
    fun `shouldProcess with null key and zero count then return false`() {
        limit = CountLimit(0)

        assertFalse(limit.shouldProcess(null))
    }

    @Test
    fun `shouldProcess with null key and negative count then return false`() {
        limit = CountLimit(-1)

        assertFalse(limit.shouldProcess(null))
    }

    @Test
    fun `shouldProcess with null key and one count then return true`() {
        limit = CountLimit(1)

        assertTrue(limit.shouldProcess(null))
    }

    @Test
    fun `shouldProcess with null key and positive count then return true`() {
        limit = CountLimit(2)

        assertTrue(limit.shouldProcess(null))
    }

    @Test
    fun `multiple shouldProcess with null key and positive count then return correct result`() {
        limit = CountLimit(3)

        assertTrue(limit.shouldProcess(null))
        assertTrue(limit.shouldProcess(null))
        assertTrue(limit.shouldProcess(null))
        assertFalse(limit.shouldProcess(null))
        assertFalse(limit.shouldProcess(null))
    }

    @Test
    fun `shouldProcess with a key and zero count then return false`() {
        limit = CountLimit(0)

        assertFalse(limit.shouldProcess("a key"))
    }

    @Test
    fun `shouldProcess with a key and negative count then return false`() {
        limit = CountLimit(-1)

        assertFalse(limit.shouldProcess("a key"))
    }

    @Test
    fun `shouldProcess with a key and one count then return true`() {
        limit = CountLimit(1)

        assertTrue(limit.shouldProcess("a key"))
    }

    @Test
    fun `shouldProcess with a key and positive count then return true`() {
        limit = CountLimit(2)

        assertTrue(limit.shouldProcess("a key"))
    }

    @Test
    fun `multiple shouldProcess with a key and positive count then return correct result`() {
        limit = CountLimit(3)
        val key = "a key"

        assertTrue(limit.shouldProcess(key))
        assertTrue(limit.shouldProcess(key))
        assertTrue(limit.shouldProcess(key))
        assertFalse(limit.shouldProcess(key))
        assertFalse(limit.shouldProcess(key))
    }

    @Test
    fun `multiple shouldProcess with different keys and positive count then return correct result`() {
        limit = CountLimit(3)
        val key1 = "123"
        val key2 = 123

        assertTrue(limit.shouldProcess(key1))
        assertTrue(limit.shouldProcess(key1))
        assertTrue(limit.shouldProcess(key1))
        assertTrue(limit.shouldProcess(key2))
        assertTrue(limit.shouldProcess(key2))
        assertTrue(limit.shouldProcess(key2))
        assertFalse(limit.shouldProcess(key2))
        assertFalse(limit.shouldProcess(key1))
    }
}