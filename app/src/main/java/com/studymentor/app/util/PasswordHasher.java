package com.studymentor.app.util;

import java.util.Base64;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;

    private PasswordHasher() {}

    public static Credentials hashNew(char[] password) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return new Credentials(encode(salt), encode(derive(password, salt)));
    }

    public static boolean verify(char[] password, String saltBase64, String expectedHashBase64)
            throws GeneralSecurityException {
        byte[] salt = decode(saltBase64);
        byte[] expected = decode(expectedHashBase64);
        byte[] actual = derive(password, salt);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] derive(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static String encode(byte[] value) {
        return Base64.getEncoder().withoutPadding().encodeToString(value);
    }

    private static byte[] decode(String value) {
        return Base64.getDecoder().decode(value);
    }

    public static final class Credentials {
        public final String salt;
        public final String hash;

        private Credentials(String salt, String hash) {
            this.salt = salt;
            this.hash = hash;
        }
    }
}

