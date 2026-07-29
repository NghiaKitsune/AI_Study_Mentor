package com.studymentor.app.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Question question);

    @Update
    void update(Question question);

    @Delete
    void delete(Question question);

    @Query("SELECT * FROM questions WHERE id = :id AND user_id = :userId LIMIT 1")
    Question byId(long id, long userId);

    @Query("SELECT * FROM questions WHERE user_id = :userId ORDER BY created_at DESC")
    List<Question> all(long userId);

    @Query("SELECT * FROM questions WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    List<Question> recent(long userId, int limit);

    @Query("SELECT * FROM questions WHERE user_id = :userId AND status = 'COMPLETED' ORDER BY created_at DESC LIMIT :limit")
    List<Question> recentCompleted(long userId, int limit);

    @Query("SELECT * FROM questions WHERE user_id = :userId AND " +
            "(:bookmarkedOnly = 0 OR bookmarked = 1) AND " +
            "(:subject = '' OR subject = :subject) AND " +
            "(:search = '' OR prompt LIKE '%' || :search || '%' OR IFNULL(answer, '') LIKE '%' || :search || '%') " +
            "ORDER BY created_at DESC")
    List<Question> search(long userId, String search, String subject, boolean bookmarkedOnly);

    @Query("SELECT COUNT(*) FROM questions WHERE user_id = :userId")
    int count(long userId);

    @Query("SELECT COUNT(*) FROM questions WHERE user_id = :userId AND bookmarked = 1")
    int bookmarkedCount(long userId);

    @Query("SELECT COUNT(*) FROM questions WHERE user_id = :userId AND subject = :subject")
    int countBySubject(long userId, String subject);

    @Query("SELECT * FROM questions WHERE user_id = :userId AND status = 'PENDING' ORDER BY created_at ASC")
    List<Question> pending(long userId);

    @Query("SELECT * FROM questions WHERE user_id = :userId AND normalized_prompt = :normalizedPrompt " +
            "AND subject = :subject AND education_level = :educationLevel " +
            "AND explanation_style = :explanationStyle AND language = :language " +
            "AND status = 'COMPLETED' AND answer IS NOT NULL ORDER BY updated_at DESC LIMIT 1")
    Question findCache(long userId, String normalizedPrompt, String subject,
                       String educationLevel, String explanationStyle, String language);

    @Query("UPDATE questions SET status = :status, error_code = :errorCode, error_message = :errorMessage, updated_at = :updatedAt " +
            "WHERE id = :questionId AND user_id = :userId")
    int updateStatus(long questionId, long userId, String status, String errorCode,
                     String errorMessage, long updatedAt);

    @Query("UPDATE questions SET status = 'PROCESSING', error_code = NULL, error_message = NULL, updated_at = :updatedAt " +
            "WHERE id = :questionId AND user_id = :userId AND " +
            "(status IN ('PENDING','FAILED') OR (status = 'PROCESSING' AND updated_at < :staleBefore))")
    int claimForProcessing(long questionId, long userId, long staleBefore, long updatedAt);

    @Query("UPDATE questions SET status = 'PENDING', error_code = 'RECOVERED', " +
            "error_message = 'Recovered after an interrupted request.', updated_at = :updatedAt " +
            "WHERE user_id = :userId AND status = 'PROCESSING' AND updated_at < :staleBefore")
    int recoverStaleProcessing(long userId, long staleBefore, long updatedAt);

    @Query("UPDATE questions SET status = :status, error_code = :errorCode, error_message = :errorMessage, updated_at = :updatedAt " +
            "WHERE id = :questionId AND user_id = :userId AND status = 'PROCESSING'")
    int failProcessing(long questionId, long userId, String status, String errorCode,
                       String errorMessage, long updatedAt);

    @Query("UPDATE questions SET answer = :answer, status = 'COMPLETED', error_code = NULL, error_message = NULL, " +
            "from_cache = :fromCache, updated_at = :updatedAt WHERE id = :questionId AND user_id = :userId " +
            "AND status = 'PROCESSING'")
    int complete(long questionId, long userId, String answer, boolean fromCache, long updatedAt);

    @Query("UPDATE questions SET bookmarked = :bookmarked, updated_at = :updatedAt WHERE id = :questionId AND user_id = :userId")
    int setBookmarked(long questionId, long userId, boolean bookmarked, long updatedAt);

    @Query("UPDATE questions SET user_id = :newUserId WHERE user_id = :oldUserId")
    int reassignOwner(long oldUserId, long newUserId);

    @Query("SELECT subject, COUNT(*) AS count FROM questions WHERE user_id = :userId AND status = 'COMPLETED' GROUP BY subject ORDER BY count DESC")
    List<SubjectCount> subjectCounts(long userId);

    @Query("SELECT created_at FROM questions WHERE user_id = :userId AND status = 'COMPLETED' ORDER BY created_at DESC")
    List<Long> completedTimestamps(long userId);

}
