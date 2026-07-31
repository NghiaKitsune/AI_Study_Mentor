package com.studymentor.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(NotificationEntity notification);

    @Query("SELECT * FROM notifications WHERE user_id = :userId AND (:type = '' OR type = :type) ORDER BY created_at DESC")
    List<NotificationEntity> list(long userId, String type);

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = 0")
    int unreadCount(long userId);

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :notificationId AND user_id = :userId")
    int markRead(long notificationId, long userId);

    @Query("UPDATE notifications SET is_read = 1 WHERE user_id = :userId")
    void markAllRead(long userId);
}

