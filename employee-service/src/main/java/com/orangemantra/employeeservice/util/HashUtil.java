package com.orangemantra.employeeservice.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class HashUtil {
    private HashUtil() {}
    public static String sha256Hex(String input) {
        if (input == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return null; }
    }
}

