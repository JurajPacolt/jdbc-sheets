/* Created on 14.12.2024 */
package org.javerland.jdbcsheets;

/**
 * @author juraj.pacolt
 */
public class JdbcSheetsException extends RuntimeException {

    public JdbcSheetsException() {
        super();
    }

    public JdbcSheetsException(String msg) {
        super(msg);
    }

    public JdbcSheetsException(Throwable th) {
        super(th);
    }

    public JdbcSheetsException(String msg, Throwable th) {
        super(msg, th);
    }

}
