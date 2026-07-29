package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "quiz_attempts",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "user_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("user_id"), @Index("completed_at")}
)
public class QuizAttempt {
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    @NonNull
    @ColumnInfo(name = "subject")
    public String subject = "general";

    @NonNull
    @ColumnInfo(name = "question_ids_json")
    public String questionIdsJson = "[]";

    @ColumnInfo(name = "started_at")
    public long startedAt = System.currentTimeMillis();

    @ColumnInfo(name = "completed_at")
    public long completedAt;

    @NonNull
    @ColumnInfo(name = "status")
    public String status = STATUS_ACTIVE;

    @ColumnInfo(name = "current_question_index")
    public int currentQuestionIndex;

    @ColumnInfo(name = "current_question_started_at")
    public long currentQuestionStartedAt;

    @ColumnInfo(name = "current_question_deadline")
    public long currentQuestionDeadline;

    @ColumnInfo(name = "last_updated_at")
    public long lastUpdatedAt = System.currentTimeMillis();

    @ColumnInfo(name = "score")
    public int score;

    @ColumnInfo(name = "total")
    public int total;

    @ColumnInfo(name = "completed")
    public boolean completed;
}
