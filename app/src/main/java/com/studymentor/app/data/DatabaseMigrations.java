package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/** Explicit migrations. Version 1 data is preserved and temporarily owned by a reserved local user. */
public final class DatabaseMigrations {
    private DatabaseMigrations() {}

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `users` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`email` TEXT NOT NULL, `display_name` TEXT NOT NULL, " +
                    "`password_hash` TEXT NOT NULL, `password_salt` TEXT NOT NULL, " +
                    "`onboarding_complete` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, " +
                    "`is_migrated_placeholder` INTEGER NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)");
            db.execSQL("INSERT OR IGNORE INTO `users` (`id`,`email`,`display_name`,`password_hash`,`password_salt`,`onboarding_complete`,`created_at`,`is_migrated_placeholder`) " +
                    "VALUES (1, '__migrated_local__', '', '', '', 0, 0, 1)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `user_preferences` (" +
                    "`user_id` INTEGER NOT NULL, `education_level` TEXT NOT NULL, " +
                    "`subjects_csv` TEXT NOT NULL, `explanation_style` TEXT NOT NULL, `language` TEXT NOT NULL, " +
                    "PRIMARY KEY(`user_id`), FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_preferences_user_id` ON `user_preferences` (`user_id`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `questions_new` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, " +
                    "`request_id` TEXT NOT NULL, `prompt` TEXT NOT NULL, `normalized_prompt` TEXT NOT NULL, " +
                    "`answer` TEXT, `subject` TEXT NOT NULL, `education_level` TEXT NOT NULL, " +
                    "`explanation_style` TEXT NOT NULL, `language` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                    "`error_code` TEXT, `error_message` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
                    "`bookmarked` INTEGER NOT NULL, `from_cache` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("INSERT INTO `questions_new` (`id`,`user_id`,`request_id`,`prompt`,`normalized_prompt`,`answer`,`subject`,`education_level`,`explanation_style`,`language`,`status`,`error_code`,`error_message`,`created_at`,`updated_at`,`bookmarked`,`from_cache`) " +
                    "SELECT `id`, 1, 'migrated-' || `id`, `prompt`, lower(trim(`prompt`)), `answer`, `subject`, '', 'detailed', 'en', " +
                    "CASE WHEN `answer` IS NULL OR trim(`answer`) = '' THEN 'FAILED' ELSE 'COMPLETED' END, " +
                    "CASE WHEN `answer` IS NULL OR trim(`answer`) = '' THEN 'MIGRATED_INCOMPLETE' ELSE NULL END, NULL, " +
                    "`created_at`, `created_at`, `bookmarked`, 0 FROM `questions`");

            db.execSQL("CREATE TABLE IF NOT EXISTS `messages_backup` (" +
                    "`id` INTEGER PRIMARY KEY NOT NULL, `question_id` INTEGER NOT NULL, " +
                    "`role` TEXT NOT NULL, `text` TEXT NOT NULL, `sent_at` INTEGER NOT NULL)");
            db.execSQL("INSERT INTO `messages_backup` (`id`,`question_id`,`role`,`text`,`sent_at`) " +
                    "SELECT m.`id`,m.`question_id`,m.`role`,m.`text`,m.`sent_at` FROM `messages` m " +
                    "INNER JOIN `questions_new` q ON q.`id` = m.`question_id`");

            db.execSQL("DROP TABLE `messages`");
            db.execSQL("DROP TABLE `questions`");
            db.execSQL("ALTER TABLE `questions_new` RENAME TO `questions`");
            db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `question_id` INTEGER NOT NULL, " +
                    "`role` TEXT NOT NULL, `text` TEXT NOT NULL, `sent_at` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`question_id`) REFERENCES `questions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("INSERT INTO `messages` (`id`,`question_id`,`role`,`text`,`sent_at`) " +
                    "SELECT `id`,`question_id`,`role`,`text`,`sent_at` FROM `messages_backup`");
            db.execSQL("DROP TABLE `messages_backup`");

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_user_id` ON `questions` (`user_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_created_at` ON `questions` (`created_at`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_status` ON `questions` (`status`)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_questions_request_id` ON `questions` (`request_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_user_id_normalized_prompt_subject_education_level_explanation_style_language` " +
                    "ON `questions` (`user_id`,`normalized_prompt`,`subject`,`education_level`,`explanation_style`,`language`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_question_id` ON `messages` (`question_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_sent_at` ON `messages` (`sent_at`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `answer_details` (" +
                    "`question_id` INTEGER NOT NULL, `steps_json` TEXT NOT NULL, `key_concepts_json` TEXT NOT NULL, " +
                    "`common_mistakes_json` TEXT NOT NULL, `alternative_approach` TEXT NOT NULL, `examples_json` TEXT NOT NULL, " +
                    "`follow_ups_json` TEXT NOT NULL, PRIMARY KEY(`question_id`), " +
                    "FOREIGN KEY(`question_id`) REFERENCES `questions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `quiz_questions` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `source_question_id` INTEGER, " +
                    "`subject` TEXT NOT NULL, `type` TEXT NOT NULL, `question_text` TEXT NOT NULL, `options_json` TEXT NOT NULL, " +
                    "`correct_answer` TEXT NOT NULL, `acceptable_answers_json` TEXT NOT NULL, " +
                    "`time_limit_seconds` INTEGER NOT NULL, `case_sensitive` INTEGER NOT NULL, `numeric_tolerance` REAL NOT NULL, " +
                    "`explanation` TEXT NOT NULL, `created_at` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_questions_user_id` ON `quiz_questions` (`user_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_questions_subject` ON `quiz_questions` (`subject`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_questions_created_at` ON `quiz_questions` (`created_at`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `quiz_attempts` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `subject` TEXT NOT NULL, " +
                    "`question_ids_json` TEXT NOT NULL, `started_at` INTEGER NOT NULL, `completed_at` INTEGER NOT NULL, " +
                    "`status` TEXT NOT NULL, `current_question_index` INTEGER NOT NULL, " +
                    "`current_question_started_at` INTEGER NOT NULL, `current_question_deadline` INTEGER NOT NULL, " +
                    "`last_updated_at` INTEGER NOT NULL, " +
                    "`score` INTEGER NOT NULL, `total` INTEGER NOT NULL, `completed` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_attempts_user_id` ON `quiz_attempts` (`user_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_attempts_completed_at` ON `quiz_attempts` (`completed_at`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `quiz_answers` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `attempt_id` INTEGER NOT NULL, `quiz_question_id` INTEGER NOT NULL, " +
                    "`user_answer` TEXT NOT NULL, `is_correct` INTEGER NOT NULL, `timed_out` INTEGER NOT NULL, " +
                    "`answer_type` TEXT NOT NULL, `response_time_ms` INTEGER NOT NULL, " +
                    "`feedback` TEXT NOT NULL, `answered_at` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`attempt_id`) REFERENCES `quiz_attempts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`quiz_question_id`) REFERENCES `quiz_questions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_answers_attempt_id` ON `quiz_answers` (`attempt_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_answers_quiz_question_id` ON `quiz_answers` (`quiz_question_id`)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_quiz_answers_attempt_id_quiz_question_id` ON `quiz_answers` (`attempt_id`,`quiz_question_id`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `learning_events` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `event_type` TEXT NOT NULL, " +
                    "`source_key` TEXT NOT NULL, `duration_seconds` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_events_user_id` ON `learning_events` (`user_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_events_created_at` ON `learning_events` (`created_at`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_events_event_type` ON `learning_events` (`event_type`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `xp_events` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `event_type` TEXT NOT NULL, " +
                    "`source_key` TEXT NOT NULL, `amount` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_xp_events_user_id` ON `xp_events` (`user_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_xp_events_created_at` ON `xp_events` (`created_at`)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_xp_events_user_id_event_type_source_key` ON `xp_events` (`user_id`,`event_type`,`source_key`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `user_achievements` (" +
                    "`user_id` INTEGER NOT NULL, `achievement_key` TEXT NOT NULL, `unlocked_at` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`user_id`,`achievement_key`), " +
                    "FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_achievements_user_id` ON `user_achievements` (`user_id`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `notifications` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `title` TEXT NOT NULL, " +
                    "`body` TEXT NOT NULL, `type` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `is_read` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_user_id` ON `notifications` (`user_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_created_at` ON `notifications` (`created_at`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_is_read` ON `notifications` (`is_read`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `reminder_schedules` (" +
                    "`user_id` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `interval_days` INTEGER NOT NULL, `preferred_hour` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`user_id`), FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_schedules_user_id` ON `reminder_schedules` (`user_id`)");
        }
    };
}
