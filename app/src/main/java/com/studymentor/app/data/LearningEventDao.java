package com.studymentor.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LearningEventDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(LearningEvent event);

    @Query("SELECT COALESCE(SUM(duration_seconds),0) FROM learning_events WHERE user_id = :userId AND event_type = 'ANSWER_REVIEW'")
    long totalReviewSeconds(long userId);

    @Query("SELECT * FROM learning_events WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    List<LearningEvent> recent(long userId, int limit);
}

