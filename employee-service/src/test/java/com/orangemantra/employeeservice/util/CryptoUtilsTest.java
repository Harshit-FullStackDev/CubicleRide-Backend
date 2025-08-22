package com.orangemantra.employeeservice.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilsTest {

    @AfterEach
    void reset() {
        // ensure key cleared between tests
        ReflectionTestUtils.setField(CryptoUtils.class, "secretKey", null);
    }

    @Test
    void encryptDecrypt_roundTrip_whenKeyInitialized() {
        CryptoUtils.init("this_is_a_very_long_secret_key_32_chars!");
        String plain = "hello world";
        String enc = CryptoUtils.encrypt(plain);
        assertNotNull(enc);
        assertTrue(enc.startsWith("ENC::"));
        String dec = CryptoUtils.decrypt(enc);
        assertEquals(plain, dec);
    }

    @Test
    void encrypt_returnsPlain_whenNoKey() {
        ReflectionTestUtils.setField(CryptoUtils.class, "secretKey", null);
        String plain = "hello";
        String enc = CryptoUtils.encrypt(plain);
        assertEquals(plain, enc);
    }

    @Test
    void decrypt_returnsInput_whenNotEncryptedOrNoKey() {
        ReflectionTestUtils.setField(CryptoUtils.class, "secretKey", null);
        assertEquals("abc", CryptoUtils.decrypt("abc"));
        assertEquals("INVALID_FORMAT", CryptoUtils.decrypt("INVALID_FORMAT"));
    }
}

