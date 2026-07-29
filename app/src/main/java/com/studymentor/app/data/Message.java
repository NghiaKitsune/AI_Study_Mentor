package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "messages",
        foreignKeys = @ForeignKey(
                entity = Question.class,
                parentColumns = "id",
                childColumns = "question_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("question_id"), @Index("sent_at")}
)
public class Message {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "question_id")
    public long questionId;

    @NonNull
    @ColumnInfo(name = "role")
    public String role = ROLE_USER;

    @NonNull
    @ColumnInfo(name = "text")
    public String text = "";

    @ColumnInfo(name = "sent_at")
    public long sentAt = System.currentTimeMillis();

    public static Message user(long questionId, String text) {
        Message message = new Message();
        message.questionId = questionId;
        message.role = ROLE_USER;
        message.text = text == null ? "" : text;
        return message;
    }

    public static Message assistant(long questionId, String text) {
        Message message = new Message();
        message.questionId = questionId;
        message.role = ROLE_ASSISTANT;
        message.text = text == null ? "" : text;
        return message;
    }
}
