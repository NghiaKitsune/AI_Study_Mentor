package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "quiz_questions",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "user_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("user_id"), @Index("subject"), @Index("created_at")}
)
public class QuizQuestionEntity {
    public static final String TYPE_MULTIPLE_CHOICE = "MULTIPLE_CHOICE";
    public static final String TYPE_SHORT_ANSWER = "SHORT_ANSWER";
    public static final String TYPE_FILL_BLANK = "FILL_BLANK";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    @Nullable
    @ColumnInfo(name = "source_question_id")
    public Long sourceQuestionId;

    @NonNull @ColumnInfo(name = "subject")
    public String subject = "general";

    @NonNull @ColumnInfo(name = "type")
    public String type = TYPE_MULTIPLE_CHOICE;

    @NonNull @ColumnInfo(name = "question_text")
    public String questionText = "";

    @NonNull @ColumnInfo(name = "options_json")
    public String optionsJson = "[]";

    @NonNull @ColumnInfo(name = "correct_answer")
    public String correctAnswer = "";

    @NonNull @ColumnInfo(name = "acceptable_answers_json")
    public String acceptableAnswersJson = "[]";

    @ColumnInfo(name = "time_limit_seconds")
    public int timeLimitSeconds = 24;

    @ColumnInfo(name = "case_sensitive")
    public boolean caseSensitive;

    @ColumnInfo(name = "numeric_tolerance")
    public double numericTolerance;

    @NonNull @ColumnInfo(name = "explanation")
    public String explanation = "";

    @ColumnInfo(name = "created_at")
    public long createdAt = System.currentTimeMillis();
}
