/* Created on 14.12.2024 */
package org.javerland.jdbcsheets;

/**
 * Public driver properties and version constants.
 *
 * @author juraj.pacolt
 */
public final class DriverInfo {

    private DriverInfo() {
    }

    /**
     * Source directory.
     */
    public final static String PROP_DIRECTORY = "directory";
    /**
     * File name in directory.
     */
    public final static String PROP_DATABASE = "database";
    /**
     * Absolute/Relative path of the file.
     */
    public final static String PROP_FILE = "file";
    /** Use the first physical worksheet row as JDBC column names. */
    public static final String PROP_HEADER = "header";

    /** Major driver version. */
    public static final int MAJOR_VERSION = 26;
    /** Minor driver version. */
    public static final int MINOR_VERSION = 3;
    /** Patch driver version. */
    public static final int PATCH_VERSION = 2;

    /** Human-readable driver name. */
    public final static String DRIVER_NAME = "JDBC Sheets Driver for XLSX";
    /** Short driver identifier. */
    public final static String DRIVER_SHORT_NAME = "jdbc-sheets";
    /** Driver version in semantic-version form. */
    public final static String DRIVER_VERSION = String.format("%d.%d.%d", MAJOR_VERSION, MINOR_VERSION, PATCH_VERSION);
    /** Human-readable driver name including its version. */
    public final static String DRIVER_FULL_NAME = DRIVER_NAME + " " + DRIVER_VERSION;

    /** Implemented JDBC specification version. */
    public static final String JDBC_VERSION = "4.2";
    private static final int JDBC_INTVERSION = 42;
    /** JDBC specification major version. */
    public static final int JDBC_MAJOR_VERSION = JDBC_INTVERSION / 10;
    /** JDBC specification minor version. */
    public static final int JDBC_MINOR_VERSION = JDBC_INTVERSION % 10;

}
