package com.justnothing.methodsclient.tui.widget;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.Component;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.ArrayList;
import java.util.List;

public class TuiLogPanel implements TuiWidget {

    private static final Logger logger = Logger.getLoggerForName("TuiLogPanel");

    private static final int DEFAULT_MAX_LINES = 100;

    private final String widgetId;
    private String title = "";
    private final List<String> lines = new ArrayList<>();
    private int maxLines = DEFAULT_MAX_LINES;
    private TextColor lineColor = null; // null means default color
    private int scrollOffset = 0;

    public TuiLogPanel(TuiWidgetData data) {
        this.widgetId = data.getId();
        if (data.getTitle() != null) {
            this.title = data.getTitle();
        }
        applyConfig(data.asLogPanelConfig());
        logger.info("TuiLogPanel created: id=" + widgetId);
    }

    @Override
    public String getWidgetId() {
        return widgetId;
    }

    @Override
    public TuiWidgetType getWidgetType() {
        return TuiWidgetType.LOG_PANEL;
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
        LogPanelConfig cfg = data.asLogPanelConfig();
        if (cfg != null) {
            applyConfig(cfg);
        }
        if (data.getTitle() != null) {
            this.title = data.getTitle();
        }
    }

    private void applyConfig(LogPanelConfig cfg) {
        if (cfg == null) return;

        // Handle append line
        if (cfg.getAppend() != null) {
            appendLine(cfg.getAppend());
        }

        // Handle append multiple lines
        List<String> appendLines = cfg.getAppendLines();
        if (appendLines != null) {
            for (String line : appendLines) {
                appendLine(line);
            }
        }

        // Handle clear
        if (cfg.isClear()) {
            clearLines();
        }

        // Handle lineColor
        this.lineColor = mapColor(cfg.getLineColor());

        // Handle maxLines
        int ml = cfg.getMaxLines();
        if (ml >= 1) {
            this.maxLines = ml;
        }
    }

    private void appendLine(String line) {
        synchronized (lines) {
            lines.add(line);
            while (lines.size() > maxLines) {
                lines.remove(0);
            }
            autoScroll();
        }
    }

    private void clearLines() {
        synchronized (lines) {
            lines.clear();
            scrollOffset = 0;
        }
    }

    private void autoScroll() {
        scrollOffset = Math.max(0, lines.size() - getVisibleLineCount());
    }

    private int getVisibleLineCount() {
        return 10; // default visible area height, will be overridden by actual draw size
    }

    private TextColor mapColor(String colorName) {
        if (colorName == null) return null;
        if ("red".equalsIgnoreCase(colorName)) return TextColor.ANSI.RED;
        if ("green".equalsIgnoreCase(colorName)) return TextColor.ANSI.GREEN;
        if ("blue".equalsIgnoreCase(colorName)) return TextColor.ANSI.BLUE;
        if ("yellow".equalsIgnoreCase(colorName)) return TextColor.ANSI.YELLOW;
        if ("cyan".equalsIgnoreCase(colorName)) return TextColor.ANSI.CYAN;
        if ("magenta".equalsIgnoreCase(colorName)) return TextColor.ANSI.MAGENTA;
        if ("white".equalsIgnoreCase(colorName)) return TextColor.ANSI.WHITE;
        return null;
    }

    @Override
    public void draw(TextGraphics g, TerminalPosition origin, TerminalSize size) {
        int width = size.getColumns();
        int height = size.getRows();
        int col = origin.getColumn();
        int startRow = origin.getRow();

        synchronized (lines) {
            // Auto-scroll to show latest content
            int visibleCount = height;
            int totalLines = lines.size();

            if (totalLines > 0) {
                int startIndex = Math.max(0, totalLines - visibleCount);
                for (int i = 0; i < visibleCount && (startIndex + i) < totalLines; i++) {
                    String rawLine = lines.get(startIndex + i);
                    String displayLine = "> " + rawLine;

                    // Truncate to fit available width
                    if (displayLine.length() > width) {
                        displayLine = displayLine.substring(0, width);
                    }

                    if (lineColor != null) {
                        g.setForegroundColor(lineColor);
                    } else {
                        g.setForegroundColor(TextColor.ANSI.DEFAULT);
                    }
                    g.putString(col, startRow + i, displayLine);
                }
            }
        }
    }

    @Override
    public TerminalSize getPreferredSize() {
        return new TerminalSize(60, 12);
    }

    @Override
    public void dispose() {
        synchronized (lines) {
            lines.clear();
        }
        logger.info("TuiLogPanel disposed: id=" + widgetId);
    }
}
