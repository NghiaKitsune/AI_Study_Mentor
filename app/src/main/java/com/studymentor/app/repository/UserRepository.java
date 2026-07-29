package com.studymentor.app.repository;

import android.content.Context;

import com.studymentor.app.StudyMentorApp;
import com.studymentor.app.data.User;
import com.studymentor.app.data.UserPreference;

public class UserRepository {
    private final StudyMentorApp app;

    public UserRepository(Context context) { app = StudyMentorApp.get(); }

    public void profile(long userId, RepositoryCallback<UserProfile> callback) {
        app.executor().execute(() -> {
            try {
                User user = app.db().userDao().byId(userId);
                UserPreference preference = app.db().userPreferenceDao().byUser(userId);
                if (user == null) {
                    app.postToMain(() -> callback.onError("Current account was not found.", null));
                    return;
                }
                UserProfile result = new UserProfile(user, preference);
                app.postToMain(() -> callback.onSuccess(result));
            } catch (Exception e) {
                app.postToMain(() -> callback.onError("Unable to load the account.", e));
            }
        });
    }


    public void setLanguage(long userId, String language, RepositoryCallback<UserProfile> callback) {
        app.executor().execute(() -> {
            try {
                User user = app.db().userDao().byId(userId);
                UserPreference preference = app.db().userPreferenceDao().byUser(userId);
                if (user == null) { app.postToMain(() -> callback.onError("Current account was not found.", null)); return; }
                if (preference == null) { preference = new UserPreference(); preference.userId = userId; }
                preference.language = "vi".equals(language) ? "vi" : "en";
                app.db().userPreferenceDao().upsert(preference);
                UserPreference saved = preference;
                app.postToMain(() -> callback.onSuccess(new UserProfile(user, saved)));
            } catch (Exception e) { app.postToMain(() -> callback.onError("Language preference could not be saved.", e)); }
        });
    }

    public static final class UserProfile {
        public final User user;
        public final UserPreference preference;
        public UserProfile(User user, UserPreference preference) {
            this.user = user; this.preference = preference;
        }
    }
}

