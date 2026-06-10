package com.studymentor.app.util;

import com.studymentor.app.R;

/**
 * Maps the subject string returned by the backend (see api-contract)
 * onto a drawable icon resource id.
 */
public final class SubjectIcons {

    private SubjectIcons() {}

    public static int forSubject(String subject) {
        if (subject == null) return R.drawable.ic_sparkles;
        switch (subject) {
            case "math":      return R.drawable.ic_target;
            case "science":   return R.drawable.ic_flame;
            case "code":      return R.drawable.ic_target;
            case "history":   return R.drawable.ic_history;
            case "language":  return R.drawable.ic_book;
            case "geography": return R.drawable.ic_target;
            default:          return R.drawable.ic_sparkles;
        }
    }
}
