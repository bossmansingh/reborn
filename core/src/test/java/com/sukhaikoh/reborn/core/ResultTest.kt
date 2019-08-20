package com.sukhaikoh.reborn.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResultTest {

    private companion object {
        const val data = "my data"
    }

    @Test
    fun `when create instance with Result_success then isSuccess return true`() {
        val result = Result.success(data)
        assertTrue(result.isSuccess)
        assertEquals(data, result.data)
    }

    @Test
    fun `when create instance with Result_success then isLoading return false`() {
        val result = Result.success(data)
        assertFalse(result.isLoading)
        assertEquals(data, result.data)
    }

    @Test
    fun `when create instance with Result_success then isFailure return false`() {
        val result = Result.success(data)
        assertFalse(result.isFailure)
        assertEquals(data, result.data)
    }

    @Test
    fun `when create instance with Result_loading then isSuccess return false`() {
        val result = Result.loading(data)
        assertFalse(result.isSuccess)
        assertEquals(data, result.data)
    }

    @Test
    fun `when create instance with Result_loading then isLoading return true`() {
        val result = Result.loading(data)
        assertTrue(result.isLoading)
        assertEquals(data, result.data)
    }

    @Test
    fun `when create instance with Result_loading then isFailure return false`() {
        val result = Result.loading(data)
        assertFalse(result.isFailure)
        assertEquals(data, result.data)
    }

    @Test
    fun `when create instance with Result_error then isSuccess return false`() {
        val result = Result.error(Throwable(), data)
        assertFalse(result.isSuccess)
        assertEquals(data, result.data)
    }

    @Test
    fun `when create instance with Result_error then isLoading return false`() {
        val result = Result.error(Throwable(), data)
        assertFalse(result.isLoading)
        assertEquals(data, result.data)
    }

    @Test
    fun `when create instance with Result_error then isFailure return true`() {
        val result = Result.error(Throwable(), data)
        assertTrue(result.isFailure)
        assertEquals(data, result.data)
    }

    @Test
    fun `when isFailure is true then cause is not null`() {
        val expected = Throwable()
        val result = Result.error(expected, data)

        val actual = result.cause

        assertTrue(result.isFailure)
        assertEquals(expected, actual)
        assertEquals(data, result.data)
    }

    @Test
    fun `when isSuccess is true then cause is null`() {
        val result = Result.success(data)

        assertTrue(result.isSuccess)
        assertNull(result.cause)
        assertEquals(data, result.data)
    }

    @Test
    fun `when isLoading is true then cause is null`() {
        val result = Result.loading(data)

        assertTrue(result.isLoading)
        assertNull(result.cause)
        assertEquals(data, result.data)
    }

    @Test
    fun `when isLoading is true then onLoading will get called`() {
        val result = Result.loading(data)

        assertTrue(result.isLoading)

        var called = false
        result.onLoading {
            called = true
        }

        assertTrue(called)
    }

    @Test
    fun `when isLoading is true then onNotLoading will not get called`() {
        val result = Result.loading(data)

        assertTrue(result.isLoading)

        var called = false
        result.onNotLoading {
            called = true
        }

        assertFalse(called)
    }

    @Test
    fun `when isLoading is false then onLoading will not get called`() {
        val result = Result.success(data)

        assertFalse(result.isLoading)

        var called = false
        result.onLoading {
            called = true
        }

        assertFalse(called)
    }

    @Test
    fun `when isLoading is false then onNotLoading will get called`() {
        val result = Result.success(data)

        assertFalse(result.isLoading)

        var called = false
        result.onNotLoading {
            called = true
        }

        assertTrue(called)
    }

    @Test
    fun `when isSuccess is true then onSuccess will get called`() {
        val result = Result.success(data)

        assertTrue(result.isSuccess)

        var called = false
        result.onSuccess {
            called = true
        }

        assertTrue(called)
    }

    @Test
    fun `when isSuccess is false then onSuccess will not get called`() {
        val result = Result.loading(data)

        assertFalse(result.isSuccess)

        var called = false
        result.onSuccess {
            called = true
        }

        assertFalse(called)
    }

    @Test
    fun `when isFailure is true then onFailure will get called`() {
        val expected = Throwable()
        val result = Result.error(expected, data)

        assertTrue(result.isFailure)

        var called = false
        result.onFailure { _, throwable ->
            assertEquals(expected, throwable)
            called = true
        }

        assertTrue(called)
    }

    @Test
    fun `when isFailure is false then onFailure will not get called`() {
        val result = Result.success(data)

        assertFalse(result.isFailure)

        var called = false
        result.onFailure { _, _ ->
            called = true
        }

        assertFalse(called)
    }

    @Test
    fun `toLoading will convert success result to loading result`() {
        val result = Result.success(data)

        assertTrue(result.isSuccess)
        assertFalse(result.isLoading)

        val actual = result.toLoading()

        assertTrue(actual.isLoading)
        assertFalse(actual.isSuccess)

        assertEquals(result.data, actual.data)
    }

    @Test
    fun `toLoading will convert failure result to loading result`() {
        val throwable = Throwable()
        val result = Result.error(throwable, data)

        assertTrue(result.isFailure)
        assertFalse(result.isLoading)

        val actual = result.toLoading()

        assertTrue(actual.isLoading)
        assertFalse(actual.isFailure)

        assertEquals(result.data, actual.data)
    }

    @Test
    fun `toSuccess will convert loading result to success result`() {
        val result = Result.loading(data)

        assertTrue(result.isLoading)
        assertFalse(result.isSuccess)

        val actual = result.toSuccess()

        assertTrue(actual.isSuccess)
        assertFalse(actual.isLoading)

        assertEquals(result.data, actual.data)
    }

    @Test
    fun `toSuccess will convert failure result to success result`() {
        val throwable = Throwable()
        val result = Result.error(throwable, data)

        assertTrue(result.isFailure)
        assertFalse(result.isSuccess)

        val actual = result.toSuccess()

        assertTrue(actual.isSuccess)
        assertFalse(actual.isFailure)

        assertEquals(result.data, actual.data)
    }

    @Test
    fun `toError will convert loading result to failure result`() {
        val expectedThrowable = Throwable()
        val result = Result.loading(data)

        assertTrue(result.isLoading)
        assertFalse(result.isFailure)

        val actual = result.toError(expectedThrowable)

        assertTrue(actual.isFailure)
        assertFalse(actual.isLoading)

        assertEquals(result.data, actual.data)
    }

    @Test
    fun `toError will convert success result to failure result`() {
        val expectedThrowable = Throwable()
        val result = Result.success(data)

        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)

        val actual = result.toError(expectedThrowable)

        assertTrue(actual.isFailure)
        assertFalse(actual.isSuccess)

        assertEquals(result.data, actual.data)
    }
}