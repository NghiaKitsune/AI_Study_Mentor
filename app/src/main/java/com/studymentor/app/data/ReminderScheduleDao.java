package com.studymentor.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface ReminderScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ReminderSchedule schedule);

    @Query("SELECT * FROM reminder_schedules WHERE user_id = :userId LIMIT 1")
    ReminderSchedule byUser(long userId);
}

