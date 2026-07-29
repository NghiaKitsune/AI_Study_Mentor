package com.studymentor.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(User user);

    @Update
    void update(User user);

    @Query("SELECT * FROM users WHERE lower(email) = lower(:email) LIMIT 1")
    User byEmail(String email);

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    User byId(long userId);

    @Query("UPDATE users SET onboarding_complete = :complete WHERE id = :userId")
    void setOnboardingComplete(long userId, boolean complete);
}

