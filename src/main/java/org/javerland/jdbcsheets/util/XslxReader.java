package org.javerland.jdbcsheets.util;

import java.io.File;

/**
 * @deprecated Use {@link XlsxReader}. The former class name contained a typo.
 */
@Deprecated
public class XslxReader extends XlsxReader {

    /**
     * Creates a reader using Excel coordinate column names.
     *
     * @param xlsxFile workbook file
     * @deprecated Use {@link XlsxReader#XlsxReader(File)}.
     */
    @Deprecated
    public XslxReader(File xlsxFile) {
        super(xlsxFile);
    }

    /**
     * Creates a reader with optional first-row column names.
     *
     * @param xlsxFile workbook file
     * @param headerRow whether the first physical row contains column names
     * @deprecated Use {@link XlsxReader#XlsxReader(File, boolean)}.
     */
    @Deprecated
    public XslxReader(File xlsxFile, boolean headerRow) {
        super(xlsxFile, headerRow);
    }
}
