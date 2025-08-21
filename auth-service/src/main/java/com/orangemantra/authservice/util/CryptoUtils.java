package com.orangemantra.authservice.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class CryptoUtils {
    private static volatile SecretKey secretKey;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int GCM_TAG_BITS = 128;

    private CryptoUtils() {}

    public static void init(String secret) {
        if (secret == null || secret.isBlank()) return;
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
            secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            // fallback: zero-padded bytes to 32
            byte[] raw = new byte[32];
            byte[] src = secret.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(src, 0, raw, 0, Math.min(src.length, raw.length));
            secretKey = new SecretKeySpec(raw, "AES");
        }
    }

    public static String encrypt(String plain) {
        if (plain == null) return null;
        SecretKey key = secretKey;
        if (key == null || plain.isEmpty()) return plain;
        try {
            byte[] iv = new byte[12];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return "ENC::" + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(ct);
        } catch (Exception e) {
            return plain;
        }
    }

    public static String decrypt(String enc) {
        if (enc == null) return null;
        SecretKey key = secretKey;
        if (key == null) return enc;
        try {
            if (!enc.startsWith("ENC::")) return enc;
            String data = enc.substring(5);
            int idx = data.indexOf(':');
            if (idx <= 0) return enc;
            byte[] iv = Base64.getDecoder().decode(data.substring(0, idx));
            byte[] ct = Base64.getDecoder().decode(data.substring(idx + 1));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return enc;
        }
    }
}
