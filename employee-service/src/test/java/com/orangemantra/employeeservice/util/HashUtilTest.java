package com.orangemantra.employeeservice.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashUtilTest {

    @Test
    void sha256Hex_null_returnsNull() {
        assertNull(HashUtil.sha256Hex(null));
    }

    @Test
    void sha256Hex_knownValue_matches() {
        String in = "abc";
        String expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        assertEquals(expected, HashUtil.sha256Hex(in));
    }
}

