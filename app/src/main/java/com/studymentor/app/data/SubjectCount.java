package com.studymentor.app.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

public class SubjectCount {
    @NonNull public String subject = "general";
    @ColumnInfo(name = "count") public int count;
}

