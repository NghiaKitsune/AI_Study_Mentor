package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * A single question the user asked, plus its final assistant answer.
 * Conversation turns live in {@link Message}, keyed by {@code question_id}.
 *
 * Public fields (not getters/setters) because Room handles those for us
 * and the rest of the app already reads them as fields.
 */
@Entity(tableName = "questions")
public class Question {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "prompt")
    public String prompt = "";

    /** Assistant's final reply text. Updated by {@code QuestionDao.updateAnswer}. */
    @Nullable
    @ColumnInfo(name = "answer")
    public String answer;

    @NonNull
    @ColumnInfo(name = "subject")
    public String subject = "general";

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "bookmarked")
    public boolean bookmarked;

    public Question() {
        this.createdAt = System.currentTimeMillis();
    }
}
