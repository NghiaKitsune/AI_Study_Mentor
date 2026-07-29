package com.studymentor.app.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QuizTimerMathTest {
    @Test public void remainingUsesPersistedAbsoluteDeadline() {
        assertEquals(4_000L, QuizTimerMath.remainingMillis(10_000L, 6_000L));
    }

    @Test public void expiredDeadlineNeverReturnsNegativeTime() {
        assertEquals(0L, QuizTimerMath.remainingMillis(10_000L, 12_000L));
        assertEquals(0L, QuizTimerMath.remainingMillis(0L, 12_000L));
    }

    @Test public void formatsMinutesAndSecondsWithoutOverflow() {
        assertEquals("0:00", QuizTimerMath.formatRemaining(0L));
        assertEquals("0:01", QuizTimerMath.formatRemaining(1L));
        assertEquals("1:00", QuizTimerMath.formatRemaining(60_000L));
        assertEquals("1:28", QuizTimerMath.formatRemaining(88_000L));
    }
}
