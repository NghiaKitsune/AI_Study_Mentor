package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "notifications",
        foreignKeys = @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "user_id", onDelete = ForeignKey.CASCADE),
        indices = {@Index("user_id"), @Index("created_at"), @Index("is_read")}
)
public class NotificationEntity {
    public static final String TYPE_REMINDER = "REMINDER";
    public static final String TYPE_PROGRESS = "PROGRESS";
    public static final String TYPE_REVIEW = "REVIEW";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    @NonNull @ColumnInfo(name = "title")
    public String title = "";

    @NonNull @ColumnInfo(name = "body")
    public String body = "";

    @NonNull @ColumnInfo(name = "type")
    public String type = TYPE_REMINDER;

    @ColumnInfo(name = "created_at")
    public long createdAt = System.currentTimeMillis();

    @ColumnInfo(name = "is_read")
    public boolean read;
}

