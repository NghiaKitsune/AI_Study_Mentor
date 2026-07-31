package com.studymentor.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserAchievementDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long unlock(UserAchievement achievement);

    @Query("SELECT * FROM user_achievements WHERE user_id = :userId ORDER BY unlocked_at DESC")
    List<UserAchievement> all(long userId);

    @Query("SELECT COUNT(*) FROM user_achievements WHERE user_id = :userId")
    int count(long userId);
}

