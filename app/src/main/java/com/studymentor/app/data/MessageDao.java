package com.studymentor.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert
    long insert(Message m);

    /** All messages for a single conversation, oldest first. */
    @Query("SELECT * FROM messages WHERE question_id = :questionId ORDER BY sent_at ASC")
    List<Message> forQuestion(long questionId);
}
