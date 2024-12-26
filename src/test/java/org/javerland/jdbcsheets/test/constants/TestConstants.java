/* Created on 26.12.2024 */
package org.javerland.jdbcsheets.test.constants;

/**
 * @author juraj.pacolt
 */
public class TestConstants {

    public final static String FILE_NAME = "test-data.xlsx";
    public final static String DIRECTORY = ".";
    public final static String FULL_FILE_PATH = String.format("%s/%s", DIRECTORY, FILE_NAME);
    public final static String URL_WITH_PARAMS = String.format("jdbc:sheets://?database=%s&directory=%s", FILE_NAME, DIRECTORY);
    public final static String URL_WITH_FILE = String.format("jdbc:sheets://?file=%s", FULL_FILE_PATH);
    public final static String DRIVER_CLASS = "org.javerland.jdbcsheets.Driver";

}
