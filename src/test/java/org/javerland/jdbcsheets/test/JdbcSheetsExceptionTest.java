/* Created on 26.10.2025 */
package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.exception.JdbcSheetsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author juraj.pacolt
 */
public class JdbcSheetsExceptionTest {

    @Test
    public void testDefaultConstructor() {
        JdbcSheetsException exception = new JdbcSheetsException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testMessageConstructor() {
        String message = "Test error message";
        JdbcSheetsException exception = new JdbcSheetsException(message);
        
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testThrowableConstructor() {
        Throwable cause = new RuntimeException("Root cause");
        JdbcSheetsException exception = new JdbcSheetsException(cause);
        
        assertNotNull(exception);
        assertEquals(cause, exception.getCause());
        assertNotNull(exception.getMessage());
    }

    @Test
    public void testMessageAndThrowableConstructor() {
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        JdbcSheetsException exception = new JdbcSheetsException(message, cause);
        
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testThrowException() {
        assertThrows(JdbcSheetsException.class, () -> {
            throw new JdbcSheetsException("Test exception");
        });
    }

    @Test
    public void testExceptionIsRuntimeException() {
        JdbcSheetsException exception = new JdbcSheetsException();
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testExceptionMessage() {
        String expectedMessage = "Database connection failed";
        JdbcSheetsException exception = new JdbcSheetsException(expectedMessage);
        
        assertEquals(expectedMessage, exception.getMessage());
    }
}
