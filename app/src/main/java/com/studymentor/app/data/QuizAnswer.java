package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "quiz_answers",
        foreignKeys = {
                @ForeignKey(entity = QuizAttempt.class, parentColumns = "id", childColumns = "attempt_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = QuizQuestionEntity.class, parentColumns = "id", childColumns = "quiz_question_id", onDelete = ForeignKey.CASCADE)
        },
        indices = {
                @Index("attempt_id"),
                @Index("quiz_question_id"),
                @Index(value = {"attempt_id", "quiz_question_id"}, unique = true)
        }
)
public class QuizAnswer {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "attempt_id")
    public long attemptId;

    @ColumnInfo(name = "quiz_question_id")
    public long quizQuestionId;

    @NonNull @ColumnInfo(name = "user_answer")
    public String userAnswer = "";

    @ColumnInfo(name = "is_correct")
    public boolean correct;

    @ColumnInfo(name = "timed_out")
    public boolean timedOut;

    @NonNull @ColumnInfo(name = "answer_type")
    public String answerType = QuizQuestionEntity.TYPE_MULTIPLE_CHOICE;

    @ColumnInfo(name = "response_time_ms")
    public long responseTimeMs;

    @NonNull @ColumnInfo(name = "feedback")
    public String feedback = "";

    @ColumnInfo(name = "answered_at")
    public long answeredAt = System.currentTimeMillis();
}
