package com.sukhaikoh.reborn.core.rate

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class RateTest {
    @Test
    fun `timer will return a rate that check with timeout correctly`() {
        val timeout = 2L
        val timeUnit = TimeUnit.MILLISECONDS
        val key = "a key"

        val rate = Rate.timer(timeout / 2, timeUnit).withKey(key)

        assertTrue(rate.shouldProcess())
        assertFalse(rate.shouldProcess())

        Thread.sleep(timeout)

        assertTrue(rate.shouldProcess())
        assertFalse(rate.shouldProcess())

        rate.withKey(123)
        assertTrue(rate.shouldProcess())
        assertFalse(rate.shouldProcess())
    }

    @Test
    fun `count will return a rate that check with count correctly`() {
        val count = 2
        val key = "a key"

        val rate = Rate.count(count).withKey(key)

        assertTrue(rate.shouldProcess())
        assertTrue(rate.shouldProcess())
        assertFalse(rate.shouldProcess())
        assertFalse(rate.shouldProcess())

        rate.withKey(123)
        assertTrue(rate.shouldProcess())
        assertTrue(rate.shouldProcess())
        assertFalse(rate.shouldProcess())
    }

    @Test
    fun `once will return a rate that can only return true once`() {
        val key = "a key"

        val rate = Rate.once().withKey(key)

        assertTrue(rate.shouldProcess())
        assertFalse(rate.shouldProcess())
        assertFalse(rate.shouldProcess())

        rate.withKey(123)
        assertTrue(rate.shouldProcess())
        assertFalse(rate.shouldProcess())
    }

    @Test
    fun `withLimit will return a rate that rely once the given limit`() {
        val limit: Limit = mock()
        val key = "a key"

        val rate = Rate.limitWith(limit).withKey(key)

        rate.shouldProcess()

        verify(limit, times(1)).shouldProcess(key)
    }
}