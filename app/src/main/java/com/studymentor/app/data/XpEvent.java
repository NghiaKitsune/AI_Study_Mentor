package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "xp_events",
        foreignKeys = @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "user_id", onDelete = ForeignKey.CASCADE),
        indices = {
                @Index("user_id"),
                @Index("created_at"),
                @Index(value = {"user_id", "event_type", "source_key"}, unique = true)
        }
)
public class XpEvent {
    public static final String TYPE_QUESTION_COMPLETED = "QUESTION_COMPLETED";
    public static final String TYPE_ANSWER_REVIEWED = "ANSWER_REVIEWED";
    public static final String TYPE_QUIZ_COMPLETED = "QUIZ_COMPLETED";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    @NonNull @ColumnInfo(name = "event_type")
    public String eventType = "";

    @NonNull @ColumnInfo(name = "source_key")
    public String sourceKey = "";

    @ColumnInfo(name = "amount")
    public int amount;

    @ColumnInfo(name = "created_at")
    public long createdAt = System.currentTimeMillis();
}

