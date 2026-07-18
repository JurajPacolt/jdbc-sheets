/* Created on 14.12.2024 */
package org.javerland.jdbcsheets.exception;

/**
 * Unchecked exception used by reader operations that cannot declare a JDBC exception.
 *
 * @author juraj.pacolt
 */
public class JdbcSheetsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Creates an exception without a detail message. */
    public JdbcSheetsException() {
        super();
    }

    /**
     * Creates an exception with a detail message.
     *
     * @param msg detail message
     */
    public JdbcSheetsException(String msg) {
        super(msg);
    }

    /**
     * Creates an exception with a cause.
     *
     * @param th underlying cause
     */
    public JdbcSheetsException(Throwable th) {
        super(th);
    }

    /**
     * Creates an exception with a detail message and cause.
     *
     * @param msg detail message
     * @param th underlying cause
     */
    public JdbcSheetsException(String msg, Throwable th) {
        super(msg, th);
    }

}
