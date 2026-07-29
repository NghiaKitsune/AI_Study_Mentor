package com.studymentor.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface XpEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertOnce(XpEvent event);

    @Query("SELECT COALESCE(SUM(amount),0) FROM xp_events WHERE user_id = :userId")
    int totalXp(long userId);

    @Query("SELECT * FROM xp_events WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    List<XpEvent> recent(long userId, int limit);
}

