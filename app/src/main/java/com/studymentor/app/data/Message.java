package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * One turn in a chat conversation. {@code role} is either
 * {@link #ROLE_USER} or {@link #ROLE_ASSISTANT}.
 */
@Entity(tableName = "messages")
public class Message {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Foreign key onto {@link Question#id}. */
    @ColumnInfo(name = "question_id")
    public long questionId;

    @NonNull
    @ColumnInfo(name = "role")
    public String role = ROLE_USER;

    @NonNull
    @ColumnInfo(name = "text")
    public String text = "";

    @ColumnInfo(name = "sent_at")
    public long sentAt;

    public Message() {
        this.sentAt = System.currentTimeMillis();
    }

    public static Message user(long questionId, String text) {
        Message m = new Message();
        m.role = ROLE_USER;
        m.text = text;
        m.questionId = questionId;
        return m;
    }

    public static Message assistant(long questionId, String text) {
        Message m = new Message();
        m.role = ROLE_ASSISTANT;
        m.text = text;
        m.questionId = questionId;
        return m;
    }
}
