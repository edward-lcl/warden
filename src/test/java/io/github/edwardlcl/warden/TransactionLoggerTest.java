package io.github.edwardlcl.warden;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.edwardlcl.warden.log.TransactionLogger;
import org.junit.jupiter.api.Test;

class TransactionLoggerTest {

    @Test
    void jsonLineContainsExpectedFields() {
        String line = TransactionLogger.toJsonLine("item_pickup", "Steve", "DIAMOND", 5, null);
        assertTrue(line.startsWith("{"), "line should be a JSON object");
        assertTrue(line.contains("\"player\":\"Steve\""));
        assertTrue(line.contains("\"event\":\"item_pickup\""));
        assertTrue(line.contains("\"item\":\"DIAMOND\""));
        assertTrue(line.contains("\"qty\":5"));
        assertTrue(line.contains("\"location\":null"));
    }

    @Test
    void nullItemSerializesAsNull() {
        String line = TransactionLogger.toJsonLine("container_open", "Alex", null, 0, null);
        assertTrue(line.contains("\"item\":null"));
    }

    @Test
    void escapesQuotesAndBackslashes() {
        assertEquals("a\\\"b", TransactionLogger.escape("a\"b"));
        assertEquals("a\\\\b", TransactionLogger.escape("a\\b"));
    }
}
