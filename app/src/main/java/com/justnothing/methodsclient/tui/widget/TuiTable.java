package com.justnothing.methodsclient.tui.widget;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.Component;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.ArrayList;
import java.util.List;

public class TuiTable implements TuiWidget {

    private static final Logger logger = Logger.getLoggerForName("TuiTable");

    private static final int MAX_COLUMNS = 10;

    private final String widgetId;
    private String title = "";
    private String[] headers = new String[0];
    private final List<Object[]> rows = new ArrayList<>();
    private int[] columnWidths;

    // Box-drawing characters (single-line style)
    private static final char HORZ = '\u2500';      // ─
    private static final char VERT = '\u2502';       // │
    private static final char TL_CORNER = '\u250c';  // ┌
    private static final char TR_CORNER = '\u2510';  // ┐
    private static final char BL_CORNER = '\u2514';  // └
    private static final char BR_CORNER = '\u2518';  // ┘
    private static final char T_JUNCTION = '\u252c'; // ┬
    private static final char B_JUNCTION = '\u2534'; // ┴
    private static final char L_JUNCTION = '\u251c'; // ├
    private static final char R_JUNCTION = '\u2524'; // ┤
    private static final char CROSS = '\u253c';     // ┼

    public TuiTable(TuiWidgetData data) {
        this.widgetId = data.getId();
        if (data.getTitle() != null) {
            this.title = data.getTitle();
        }
        applyConfig(data.asTableConfig());
        recalcColumnWidths();
        logger.info("TuiTable created: id=" + widgetId);
    }

    @Override
    public String getWidgetId() {
        return widgetId;
    }

    @Override
    public TuiWidgetType getWidgetType() {
        return TuiWidgetType.TABLE;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Component getComponent() {
        return null;
    }

    @Override
    public void update(TuiWidgetData data) {
        TableConfig cfg = data.asTableConfig();
        if (cfg != null) {
            applyConfig(cfg);
        }
        if (data.getTitle() != null) {
            this.title = data.getTitle();
        }
        recalcColumnWidths();
    }

    private void applyConfig(TableConfig cfg) {
        if (cfg == null) return;

        // Handle headers
        String[] h = cfg.getHeaders();
        if (h != null && h.length > 0) {
            setHeaders(h);
        }

        // Handle addRow
        Object[] rowValues = cfg.getAddRow();
        if (rowValues != null && rowValues.length > 0) {
            addRow(rowValues);
        }

        // Handle clearRows
        if (cfg.isClearRows()) {
            clearRows();
        }
    }

    private void setHeaders(String[] h) {
        int count = Math.min(h.length, MAX_COLUMNS);
        this.headers = new String[count];
        System.arraycopy(h, 0, this.headers, 0, count);
    }

    private void addRow(Object[] rowValues) {
        int count = Math.min(rowValues.length, MAX_COLUMNS);
        Object[] trimmed = new Object[count];
        System.arraycopy(rowValues, 0, trimmed, 0, count);
        rows.add(trimmed);
    }

    private void clearRows() {
        rows.clear();
    }

    private void recalcColumnWidths() {
        int colCount = headers.length;
        if (colCount == 0 && !rows.isEmpty()) {
            // Infer column count from first row
            colCount = Math.min(rows.get(0).length, MAX_COLUMNS);
        }
        if (colCount == 0) {
            columnWidths = new int[0];
            return;
        }

        columnWidths = new int[colCount];
        // Initialize with header widths
        for (int c = 0; c < colCount; c++) {
            if (c < headers.length && headers[c] != null) {
                columnWidths[c] = headers[c].length();
            } else {
                columnWidths[c] = 4; // default min width
            }
        }
        // Expand based on row data
        for (Object[] row : rows) {
            for (int c = 0; c < colCount && c < row.length; c++) {
                String cellText = row[c] != null ? row[c].toString() : "";
                if (cellText.length() > columnWidths[c]) {
                    columnWidths[c] = cellText.length();
                }
            }
        }
        // Minimum width of 3 per column
        for (int c = 0; c < colCount; c++) {
            if (columnWidths[c] < 3) columnWidths[c] = 3;
        }
    }

    @Override
    public void draw(TextGraphics g, TerminalPosition origin, TerminalSize size) {
        int width = size.getColumns();
        int height = size.getRows();
        int col = origin.getColumn();
        int startRow = origin.getRow();

        if (columnWidths == null || columnWidths.length == 0) {
            g.setForegroundColor(TextColor.ANSI.DEFAULT);
            g.putString(col, startRow, "(empty table)");
            return;
        }

        int colCount = columnWidths.length;

        List<String> outputLines = new ArrayList<>();

        // Top border
        StringBuilder topBorder = buildBorderLine(colCount, TL_CORNER, HORZ, T_JUNCTION, TR_CORNER);
        outputLines.add(topBorder.toString());

        // Header row
        StringBuilder headerSb = new StringBuilder();
        headerSb.append(VERT);
        for (int c = 0; c < colCount; c++) {
            headerSb.append(' ');
            String headerText = c < headers.length && headers[c] != null ? headers[c] : "";
            headerSb.append(padOrTruncate(headerText, columnWidths[c]));
            headerSb.append(' ');
            headerSb.append(VERT);
        }
        outputLines.add(headerSb.toString());

        // Separator line after header
        StringBuilder sepBorder = buildBorderLine(colCount, L_JUNCTION, HORZ, CROSS, R_JUNCTION);
        outputLines.add(sepBorder.toString());

        // Data rows
        for (Object[] rowData : rows) {
            StringBuilder rowSb = new StringBuilder();
            rowSb.append(VERT);
            for (int c = 0; c < colCount; c++) {
                rowSb.append(' ');
                String cellText = c < rowData.length && rowData[c] != null ? rowData[c].toString() : "";
                rowSb.append(padOrTruncate(cellText, columnWidths[c]));
                rowSb.append(' ');
                rowSb.append(VERT);
            }
            outputLines.add(rowSb.toString());
        }

        // Bottom border
        StringBuilder bottomBorder = buildBorderLine(colCount, BL_CORNER, HORZ, B_JUNCTION, BR_CORNER);
        outputLines.add(bottomBorder.toString());

        // Draw all lines within available area
        g.setForegroundColor(TextColor.ANSI.DEFAULT);
        int lineCount = Math.min(outputLines.size(), height);
        for (int i = 0; i < lineCount; i++) {
            String line = outputLines.get(i);
            if (line.length() > width) {
                line = line.substring(0, width);
            }
            g.putString(col, startRow + i, line);
        }
    }

    private StringBuilder buildBorderLine(int colCount, char left, char fill, char mid, char right) {
        StringBuilder sb = new StringBuilder();
        sb.append(left);
        for (int c = 0; c < colCount; c++) {
            for (int i = 0; i < columnWidths[c] + 2; i++) {
                sb.append(fill);
            }
            if (c < colCount - 1) {
                sb.append(mid);
            }
        }
        sb.append(right);
        return sb;
    }

    private String padOrTruncate(String text, int width) {
        if (text.length() > width) {
            return text.substring(0, width);
        }
        StringBuilder sb = new StringBuilder(text);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    @Override
    public TerminalSize getPreferredSize() {
        int colCount = columnWidths != null ? columnWidths.length : 0;
        int totalWidth = 0;
        if (colCount > 0) {
            totalWidth = 1; // left border
            for (int w : columnWidths) {
                totalWidth += w + 3; // content + 2 spaces + 1 border
            }
        }
        int rowCount = 3 + rows.size(); // top border + header + separator + rows + bottom border
        return new TerminalSize(Math.max(totalWidth, 30), Math.max(rowCount, 5));
    }

    @Override
    public void dispose() {
        rows.clear();
        headers = new String[0];
        columnWidths = new int[0];
        logger.info("TuiTable disposed: id=" + widgetId);
    }
}
