package com.studymentor.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface AnswerDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(AnswerDetail detail);

    @Query("SELECT answer_details.* FROM answer_details INNER JOIN questions ON questions.id = answer_details.question_id " +
            "WHERE answer_details.question_id = :questionId AND questions.user_id = :userId LIMIT 1")
    AnswerDetail byQuestion(long questionId, long userId);

}

