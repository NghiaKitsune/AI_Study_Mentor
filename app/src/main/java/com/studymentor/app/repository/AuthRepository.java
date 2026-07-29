package com.studymentor.app.repository;

import android.content.Context;

import com.studymentor.app.StudyMentorApp;
import com.studymentor.app.data.AppDatabase;
import com.studymentor.app.data.ReminderSchedule;
import com.studymentor.app.data.User;
import com.studymentor.app.data.UserPreference;
import com.studymentor.app.util.PasswordHasher;
import com.studymentor.app.util.Session;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Locale;

public class AuthRepository {
    private final Context appContext;
    private final StudyMentorApp app;
    private final AppDatabase db;

    public AuthRepository(Context context) {
        appContext = context.getApplicationContext();
        app = StudyMentorApp.get();
        db = app.db();
    }

    public void signUp(String rawEmail, char[] password, RepositoryCallback<AuthResult> callback) {
        String email = normalizeEmail(rawEmail);
        char[] safePassword = password == null ? new char[0] : Arrays.copyOf(password, password.length);
        app.executor().execute(() -> {
            try {
                if (db.userDao().byEmail(email) != null) {
                    postError(callback, "An account with this email already exists.", null);
                    return;
                }
                PasswordHasher.Credentials credentials = PasswordHasher.hashNew(safePassword);
                User user = new User();
                user.email = email;
                user.displayName = displayNameFromEmail(email);
                user.passwordHash = credentials.hash;
                user.passwordSalt = credentials.salt;
                user.onboardingComplete = false;
                long userId = db.userDao().insert(user);
                claimMigratedData(userId);
                ensureDefaults(userId);
                Session.start(appContext, userId, false);
                user.id = userId;
                postSuccess(callback, new AuthResult(userId, user.displayName, false));
            } catch (GeneralSecurityException e) {
                postError(callback, "Unable to secure the password on this device.", e);
            } catch (Exception e) {
                postError(callback, "Unable to create the local account.", e);
            } finally {
                Arrays.fill(safePassword, '\0');
            }
        });
    }

    public void login(String rawEmail, char[] password, RepositoryCallback<AuthResult> callback) {
        String email = normalizeEmail(rawEmail);
        char[] safePassword = password == null ? new char[0] : Arrays.copyOf(password, password.length);
        app.executor().execute(() -> {
            try {
                User user = db.userDao().byEmail(email);
                boolean valid = user != null
                        && !user.migratedPlaceholder
                        && PasswordHasher.verify(safePassword, user.passwordSalt, user.passwordHash);
                if (!valid) {
                    postError(callback, "Email or password is incorrect.", null);
                    return;
                }
                claimMigratedData(user.id);
                ensureDefaults(user.id);
                Session.start(appContext, user.id, user.onboardingComplete);
                postSuccess(callback, new AuthResult(user.id, user.displayName, user.onboardingComplete));
            } catch (GeneralSecurityException e) {
                postError(callback, "Unable to verify this local account.", e);
            } catch (Exception e) {
                postError(callback, "Unable to sign in to the local account.", e);
            } finally {
                Arrays.fill(safePassword, '\0');
            }
        });
    }

    public void savePersonalization(long userId, String educationLevel, String subjectsCsv,
                                    String explanationStyle, String language,
                                    RepositoryCallback<Void> callback) {
        app.executor().execute(() -> {
            try {
                User user = db.userDao().byId(userId);
                if (user == null || user.migratedPlaceholder) {
                    postError(callback, "The active local account is unavailable.", null);
                    return;
                }
                UserPreference preference = new UserPreference();
                preference.userId = userId;
                preference.educationLevel = educationLevel == null ? "" : educationLevel;
                preference.subjectsCsv = subjectsCsv == null ? "" : subjectsCsv;
                preference.explanationStyle = explanationStyle == null || explanationStyle.isEmpty()
                        ? "detailed" : explanationStyle;
                preference.language = language == null || language.isEmpty() ? "en" : language;
                db.runInTransaction(() -> {
                    db.userPreferenceDao().upsert(preference);
                    db.userDao().setOnboardingComplete(userId, true);
                });
                Session.setOnboarded(appContext, true);
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, "Unable to save learning preferences.", e);
            }
        });
    }

    public void loadCurrentUser(RepositoryCallback<UserProfile> callback) {
        long userId = Session.userId(appContext);
        if (userId <= 0) {
            postError(callback, "No local account is signed in.", null);
            return;
        }
        app.executor().execute(() -> {
            User user = db.userDao().byId(userId);
            UserPreference preference = db.userPreferenceDao().byUser(userId);
            if (user == null || user.migratedPlaceholder) {
                postError(callback, "The active local account is unavailable.", null);
                return;
            }
            postSuccess(callback, new UserProfile(user, preference));
        });
    }

    public void logout() {
        Session.clearAuth(appContext);
    }

    private void ensureDefaults(long userId) {
        if (db.userPreferenceDao().byUser(userId) == null) {
            UserPreference preference = new UserPreference();
            preference.userId = userId;
            db.userPreferenceDao().upsert(preference);
        }
        if (db.reminderScheduleDao().byUser(userId) == null) {
            ReminderSchedule schedule = new ReminderSchedule();
            schedule.userId = userId;
            db.reminderScheduleDao().upsert(schedule);
        }
    }

    private void claimMigratedData(long userId) {
        if (userId != User.MIGRATED_USER_ID) {
            db.questionDao().reassignOwner(User.MIGRATED_USER_ID, userId);
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String displayNameFromEmail(String email) {
        int at = email.indexOf('@');
        String local = at > 0 ? email.substring(0, at) : email;
        String cleaned = local.replace('.', ' ').replace('_', ' ').replace('-', ' ').trim();
        if (cleaned.isEmpty()) return "Student";
        StringBuilder result = new StringBuilder(cleaned.length());
        boolean upper = true;
        for (char c : cleaned.toCharArray()) {
            if (Character.isWhitespace(c)) {
                upper = true;
                result.append(c);
            } else {
                result.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return result.toString();
    }

    private <T> void postSuccess(RepositoryCallback<T> callback, T value) {
        app.postToMain(() -> callback.onSuccess(value));
    }

    private <T> void postError(RepositoryCallback<T> callback, String message, Throwable error) {
        app.postToMain(() -> callback.onError(message, error));
    }

    public static final class AuthResult {
        public final long userId;
        public final String displayName;
        public final boolean onboardingComplete;

        public AuthResult(long userId, String displayName, boolean onboardingComplete) {
            this.userId = userId;
            this.displayName = displayName;
            this.onboardingComplete = onboardingComplete;
        }
    }

    public static final class UserProfile {
        public final User user;
        public final UserPreference preference;

        public UserProfile(User user, UserPreference preference) {
            this.user = user;
            this.preference = preference;
        }
    }
}

