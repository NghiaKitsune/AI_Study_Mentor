package com.studymentor.app.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface QuestionDao {

    @Insert
    long insert(Question q);

    @Update
    void update(Question q);

    /** Bulk fetch — full list, newest first. */
    @Query("SELECT * FROM questions ORDER BY created_at DESC")
    List<Question> all();

    /** Last N questions, newest first. Used by HomeActivity's "Recent" row. */
    @Query("SELECT * FROM questions ORDER BY created_at DESC LIMIT :limit")
    List<Question> recent(int limit);

    @Query("SELECT * FROM questions WHERE id = :id")
    Question byId(long id);

    @Query("SELECT COUNT(*) FROM questions")
    int count();

    @Query("SELECT COUNT(*) FROM questions WHERE bookmarked = 1")
    int bookmarkedCount();

    @Query("SELECT COUNT(*) FROM questions WHERE subject = :subject")
    int countBySubject(String subject);

    @Delete
    void delete(Question q);

    /** Updates the {@code answer} column without rewriting the whole row. */
    @Query("UPDATE questions SET answer = :answer WHERE id = :id")
    void updateAnswer(long id, String answer);
}
