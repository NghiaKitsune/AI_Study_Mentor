package com.studymentor.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Message message);

    @Query("SELECT messages.* FROM messages INNER JOIN questions ON questions.id = messages.question_id " +
            "WHERE messages.question_id = :questionId AND questions.user_id = :userId ORDER BY messages.sent_at ASC")
    List<Message> forQuestion(long questionId, long userId);
}
