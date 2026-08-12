package com.studymentor.app.security;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Runtime checks for security claims used in the P6/M5 report evaluation.
 */
@RunWith(AndroidJUnit4.class)
public class ManifestSecurityTest {

    @Test
    public void applicationBackupIsDisabled() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        ApplicationInfo info = context.getPackageManager()
                .getApplicationInfo(context.getPackageName(), 0);

        assertEquals(0, info.flags & ApplicationInfo.FLAG_ALLOW_BACKUP);
    }

    @Test
    public void onlyLauncherSplashActivityIsExported() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PackageInfo info = context.getPackageManager().getPackageInfo(
                context.getPackageName(), PackageManager.GET_ACTIVITIES);
        assertNotNull(info.activities);

        List<String> exported = new ArrayList<>();
        for (ActivityInfo activity : info.activities) {
            if (activity.exported) exported.add(activity.name);
        }

        assertEquals(1, exported.size());
        assertEquals("com.studymentor.app.ui.SplashActivity", exported.get(0));
    }

    @Test
    public void scanFileProviderIsPrivateAndUsesTemporaryUriGrants() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PackageInfo info = context.getPackageManager().getPackageInfo(
                context.getPackageName(), PackageManager.GET_PROVIDERS);
        assertNotNull(info.providers);

        ProviderInfo fileProvider = null;
        String expectedAuthority = context.getPackageName() + ".fileprovider";
        for (ProviderInfo provider : info.providers) {
            if (expectedAuthority.equals(provider.authority)) {
                fileProvider = provider;
                break;
            }
        }

        assertNotNull(fileProvider);
        assertFalse(fileProvider.exported);
        assertTrue(fileProvider.grantUriPermissions);
    }
}
