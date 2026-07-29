package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "users",
        indices = {@Index(value = "email", unique = true)}
)
public class User {
    /** Reserved owner used only while migrating v1 data before the first real login. */
    public static final long MIGRATED_USER_ID = 1L;
    public static final String MIGRATED_EMAIL = "__migrated_local__";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "email")
    public String email = "";

    @NonNull
    @ColumnInfo(name = "display_name")
    public String displayName = "";

    @NonNull
    @ColumnInfo(name = "password_hash")
    public String passwordHash = "";

    @NonNull
    @ColumnInfo(name = "password_salt")
    public String passwordSalt = "";

    @ColumnInfo(name = "onboarding_complete")
    public boolean onboardingComplete;

    @ColumnInfo(name = "created_at")
    public long createdAt = System.currentTimeMillis();

    @ColumnInfo(name = "is_migrated_placeholder")
    public boolean migratedPlaceholder;
}

