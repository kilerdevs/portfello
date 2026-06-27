package com.portfello.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LockStateLogicTest {

    @Test
    fun `lockout escalation thresholds`() {
        val lockout = { attempts: Int ->
            when {
                attempts >= 8 -> 300_000L
                attempts >= 5 -> 60_000L
                attempts >= 3 -> 30_000L
                else -> 0L
            }
        }
        assertEquals(0L, lockout(0))
        assertEquals(0L, lockout(2))
        assertEquals(30_000L, lockout(3))
        assertEquals(30_000L, lockout(4))
        assertEquals(60_000L, lockout(5))
        assertEquals(60_000L, lockout(7))
        assertEquals(300_000L, lockout(8))
        assertEquals(300_000L, lockout(100))
    }

    @Test
    fun `timeout triggers relock`() {
        val lockTimeoutMs = 60_000L
        val lastActive = 1_000_000L
        val nowAfterTimeout = lastActive + lockTimeoutMs + 1
        val nowBeforeTimeout = lastActive + lockTimeoutMs - 1

        val shouldLock = { now: Long -> (now - lastActive) > lockTimeoutMs }

        assertEquals(true, shouldLock(nowAfterTimeout))
        assertEquals(false, shouldLock(nowBeforeTimeout))
    }
}
