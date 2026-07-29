package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "learning_events",
        foreignKeys = @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "user_id", onDelete = ForeignKey.CASCADE),
        indices = {@Index("user_id"), @Index("created_at"), @Index("event_type")}
)
public class LearningEvent {
    public static final String TYPE_ANSWER_REVIEW = "ANSWER_REVIEW";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    @NonNull
    @ColumnInfo(name = "event_type")
    public String eventType = "";

    @NonNull
    @ColumnInfo(name = "source_key")
    public String sourceKey = "";

    @ColumnInfo(name = "duration_seconds")
    public long durationSeconds;

    @ColumnInfo(name = "created_at")
    public long createdAt = System.currentTimeMillis();
}

