package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "answer_details",
        foreignKeys = @ForeignKey(
                entity = Question.class,
                parentColumns = "id",
                childColumns = "question_id",
                onDelete = ForeignKey.CASCADE
        )
)
public class AnswerDetail {
    @PrimaryKey
    @ColumnInfo(name = "question_id")
    public long questionId;

    @NonNull @ColumnInfo(name = "steps_json")
    public String stepsJson = "[]";

    @NonNull @ColumnInfo(name = "key_concepts_json")
    public String keyConceptsJson = "[]";

    @NonNull @ColumnInfo(name = "common_mistakes_json")
    public String commonMistakesJson = "[]";

    @NonNull @ColumnInfo(name = "alternative_approach")
    public String alternativeApproach = "";

    @NonNull @ColumnInfo(name = "examples_json")
    public String examplesJson = "[]";

    @NonNull @ColumnInfo(name = "follow_ups_json")
    public String followUpsJson = "[]";
}

