package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "user_preferences",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "user_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("user_id")}
)
public class UserPreference {
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    public long userId;

    @NonNull
    @ColumnInfo(name = "education_level")
    public String educationLevel = "";

    @NonNull
    @ColumnInfo(name = "subjects_csv")
    public String subjectsCsv = "";

    @NonNull
    @ColumnInfo(name = "explanation_style")
    public String explanationStyle = "detailed";

    @NonNull
    @ColumnInfo(name = "language")
    public String language = "en";
}

