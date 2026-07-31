package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "user_achievements",
        primaryKeys = {"user_id", "achievement_key"},
        foreignKeys = @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "user_id", onDelete = ForeignKey.CASCADE),
        indices = {@Index("user_id")}
)
public class UserAchievement {
    @ColumnInfo(name = "user_id")
    public long userId;

    @NonNull @ColumnInfo(name = "achievement_key")
    public String achievementKey = "";

    @ColumnInfo(name = "unlocked_at")
    public long unlockedAt = System.currentTimeMillis();
}

