package com.studymentor.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserPreference preference);

    @Query("SELECT * FROM user_preferences WHERE user_id = :userId LIMIT 1")
    UserPreference byUser(long userId);
}

