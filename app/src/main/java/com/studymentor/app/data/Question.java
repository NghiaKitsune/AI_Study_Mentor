package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(
        tableName = "questions",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "user_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("user_id"),
                @Index("created_at"),
                @Index("status"),
                @Index(value = "request_id", unique = true),
                @Index(value = {"user_id", "normalized_prompt", "subject", "education_level", "explanation_style", "language"})
        }
)
public class Question {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId = User.MIGRATED_USER_ID;

    @NonNull
    @ColumnInfo(name = "request_id")
    public String requestId = UUID.randomUUID().toString();

    @NonNull
    @ColumnInfo(name = "prompt")
    public String prompt = "";

    @NonNull
    @ColumnInfo(name = "normalized_prompt")
    public String normalizedPrompt = "";

    @Nullable
    @ColumnInfo(name = "answer")
    public String answer;

    @NonNull
    @ColumnInfo(name = "subject")
    public String subject = "general";

    @NonNull
    @ColumnInfo(name = "education_level")
    public String educationLevel = "";

    @NonNull
    @ColumnInfo(name = "explanation_style")
    public String explanationStyle = "detailed";

    @NonNull
    @ColumnInfo(name = "language")
    public String language = "en";

    @NonNull
    @ColumnInfo(name = "status")
    public String status = STATUS_PENDING;

    @Nullable
    @ColumnInfo(name = "error_code")
    public String errorCode;

    @Nullable
    @ColumnInfo(name = "error_message")
    public String errorMessage;

    @ColumnInfo(name = "created_at")
    public long createdAt = System.currentTimeMillis();

    @ColumnInfo(name = "updated_at")
    public long updatedAt = System.currentTimeMillis();

    @ColumnInfo(name = "bookmarked")
    public boolean bookmarked;

    @ColumnInfo(name = "from_cache")
    public boolean fromCache;
}
