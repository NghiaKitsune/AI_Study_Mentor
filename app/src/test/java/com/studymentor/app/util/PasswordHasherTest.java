package com.studymentor.app.util;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PasswordHasherTest {
    @Test
    public void hashAndVerifyUsesPerUserSalt() throws Exception {
        char[] password = "StrongPass9!".toCharArray();
        PasswordHasher.Credentials first = PasswordHasher.hashNew(password);
        PasswordHasher.Credentials second = PasswordHasher.hashNew(password);

        assertTrue(PasswordHasher.verify(password, first.salt, first.hash));
        assertFalse(PasswordHasher.verify("WrongPass9!".toCharArray(), first.salt, first.hash));
        assertNotEquals(first.salt, second.salt);
        assertNotEquals(first.hash, second.hash);
        Arrays.fill(password, '\0');
    }
}

