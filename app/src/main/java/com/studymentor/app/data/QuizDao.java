package com.studymentor.app.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface QuizDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertQuestion(QuizQuestionEntity question);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    List<Long> insertQuestions(List<QuizQuestionEntity> questions);

    @Query("SELECT * FROM quiz_questions WHERE user_id = :userId AND (:subject = '' OR subject = :subject) ORDER BY created_at DESC LIMIT :limit")
    List<QuizQuestionEntity> questions(long userId, String subject, int limit);

    @Query("SELECT * FROM quiz_questions WHERE id IN (:ids) AND user_id = :userId")
    List<QuizQuestionEntity> questionsByIds(long userId, List<Long> ids);

    @Query("SELECT * FROM quiz_questions WHERE id = :questionId AND user_id = :userId LIMIT 1")
    QuizQuestionEntity question(long questionId, long userId);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertAttempt(QuizAttempt attempt);

    @Update
    void updateAttempt(QuizAttempt attempt);

    @Query("UPDATE quiz_attempts SET current_question_index = :index, " +
            "current_question_started_at = :startedAt, current_question_deadline = :deadline, " +
            "last_updated_at = :updatedAt WHERE id = :attemptId AND user_id = :userId AND completed = 0")
    int updateQuestionProgress(long attemptId, long userId, int index,
                               long startedAt, long deadline, long updatedAt);

    @Delete
    void deleteAttempt(QuizAttempt attempt);

    @Query("SELECT * FROM quiz_attempts WHERE user_id = :userId AND subject = :subject AND completed = 0 ORDER BY started_at DESC LIMIT 1")
    QuizAttempt activeAttempt(long userId, String subject);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertAnswer(QuizAnswer answer);

    @Query("SELECT * FROM quiz_attempts WHERE id = :attemptId AND user_id = :userId LIMIT 1")
    QuizAttempt attempt(long attemptId, long userId);

    @Query("SELECT quiz_answers.* FROM quiz_answers " +
            "INNER JOIN quiz_attempts ON quiz_attempts.id = quiz_answers.attempt_id " +
            "WHERE quiz_answers.attempt_id = :attemptId AND quiz_attempts.user_id = :userId " +
            "ORDER BY quiz_answers.answered_at ASC")
    List<QuizAnswer> answers(long attemptId, long userId);

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE user_id = :userId AND completed = 1")
    int completedAttemptCount(long userId);

    @Query("SELECT COALESCE(SUM(score),0) FROM quiz_attempts WHERE user_id = :userId AND completed = 1")
    int totalCorrect(long userId);

    @Query("SELECT COALESCE(SUM(total),0) FROM quiz_attempts WHERE user_id = :userId AND completed = 1")
    int totalAnswered(long userId);

    @Query("SELECT completed_at FROM quiz_attempts WHERE user_id = :userId AND completed = 1 ORDER BY completed_at DESC")
    List<Long> completedTimestamps(long userId);
}
