package com.studymentor.app.repository;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ProgressRulesTest {
    @Test
    public void levelUsesSingleXpThresholdRule() {
        assertEquals(1, ProgressRepository.levelForXp(0));
        assertEquals(1, ProgressRepository.levelForXp(249));
        assertEquals(2, ProgressRepository.levelForXp(250));
    }

    @Test
    public void streakCombinesQuestionAndQuizActivity() {
        ZoneId zone = ZoneId.systemDefault();
        long today = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli();
        long yesterday = LocalDate.now(zone).minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        long twoDaysAgo = LocalDate.now(zone).minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli();
        assertEquals(3, ProgressRepository.calculateStreak(
                Arrays.asList(today, twoDaysAgo), Collections.singletonList(yesterday)));
    }
}

