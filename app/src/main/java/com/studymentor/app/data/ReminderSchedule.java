package com.studymentor.app.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "reminder_schedules",
        foreignKeys = @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "user_id", onDelete = ForeignKey.CASCADE),
        indices = {@Index("user_id")}
)
public class ReminderSchedule {
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "enabled")
    public boolean enabled = false;

    @ColumnInfo(name = "interval_days")
    public int intervalDays = 1;

    @ColumnInfo(name = "preferred_hour")
    public int preferredHour = 19;
}

