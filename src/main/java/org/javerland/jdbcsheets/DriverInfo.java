/* Created on 14.12.2024 */
package org.javerland.jdbcsheets;

/**
 * @author juraj.pacolt
 */
public class DriverInfo {

    /** Source directory. */
    public final static String PROP_DIRECTORY = "directory";
    /** File name in directory. */
    public final static String PROP_DATABASE = "database";
    /** Absolute/Relative path of the file. */
    public final static String PROP_FILE = "file";

    // Driver version
    public static int MAJOR_VERSION = 0;
    public static int MINOR_VERSION = 0;
    public static int PATCH_VERSION = 1;

    // Driver name
    public final static String DRIVER_NAME = "JDBC Driver for XLSX and others sheets reading";
    public final static String DRIVER_SHORT_NAME = "jdbc-sheets";
    public final static String DRIVER_VERSION = String.format("%d.%d.%d", MAJOR_VERSION, MINOR_VERSION, PATCH_VERSION);
    public final static String DRIVER_FULL_NAME = DRIVER_NAME + " " + DRIVER_VERSION;

    // JDBC specification
    public static final String JDBC_VERSION = "4.2";
    private static final int JDBC_INTVERSION = 42;
    public static final int JDBC_MAJOR_VERSION = JDBC_INTVERSION / 10;
    public static final int JDBC_MINOR_VERSION = JDBC_INTVERSION % 10;

}
