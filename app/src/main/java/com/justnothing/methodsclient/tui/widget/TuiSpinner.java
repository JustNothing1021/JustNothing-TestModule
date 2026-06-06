package com.justnothing.methodsclient.tui.widget;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.Component;
import com.justnothing.testmodule.utils.logging.Logger;

public class TuiSpinner implements TuiWidget {

    private static final Logger logger = Logger.getLoggerForName("TuiSpinner");

    /** Braille spinner animation frames */
    private static final String[] FRAMES = {
            "\u28cb", "\u2899", "\u28b9", "\u28b8",
            "\u28bc", "\u2834", "\u2826", "\u28e7",
            "\u28c7", "\u280f"
    };

    private final String widgetId;
    private String title = "";
    private String text = "处理中...";
    private TextColor color = TextColor.ANSI.CYAN;
    private int frameIndex = 0;

    public TuiSpinner(TuiWidgetData data) {
        this.widgetId = data.getId();
        if (data.getTitle() != null) {
            this.title = data.getTitle();
        }
        applyConfig(data.asSpinnerConfig());
        logger.info("TuiSpinner created: id=" + widgetId);
    }

    @Override
    public String getWidgetId() {
        return widgetId;
    }

    @Override
    public TuiWidgetType getWidgetType() {
        return TuiWidgetType.SPINNER;
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
        SpinnerConfig cfg = data.asSpinnerConfig();
        if (cfg != null) {
            applyConfig(cfg);
        }
        if (data.getTitle() != null) {
            this.title = data.getTitle();
        }
        advanceFrame();
    }

    private void applyConfig(SpinnerConfig cfg) {
        if (cfg == null) return;
        this.text = cfg.getText();
        this.color = mapColor(cfg.getColor());
    }

    private TextColor mapColor(String colorName) {
        if (colorName == null || "cyan".equalsIgnoreCase(colorName)) return TextColor.ANSI.CYAN;
        if ("red".equalsIgnoreCase(colorName)) return TextColor.ANSI.RED;
        if ("green".equalsIgnoreCase(colorName)) return TextColor.ANSI.GREEN;
        if ("blue".equalsIgnoreCase(colorName)) return TextColor.ANSI.BLUE;
        if ("yellow".equalsIgnoreCase(colorName)) return TextColor.ANSI.YELLOW;
        if ("magenta".equalsIgnoreCase(colorName)) return TextColor.ANSI.MAGENTA;
        return TextColor.ANSI.CYAN;
    }

    /**
     * Advance the spinner frame index by one position.
     */
    private void advanceFrame() {
        frameIndex = (frameIndex + 1) % FRAMES.length;
    }

    @Override
    public void draw(TextGraphics g, TerminalPosition origin, TerminalSize size) {
        int col = origin.getColumn();
        int row = origin.getRow();

        String frameChar = FRAMES[frameIndex];
        String displayText = frameChar + " " + text;

        int width = size.getColumns();
        if (displayText.length() > width) {
            displayText = displayText.substring(0, width);
        }

        g.setForegroundColor(color);
        g.putString(col, row, displayText);
    }

    @Override
    public boolean needsRefresh() {
        return true;
    }

    @Override
    public TerminalSize getPreferredSize() {
        return new TerminalSize(20, 1);
    }

    @Override
    public void dispose() {
        frameIndex = 0;
        logger.info("TuiSpinner disposed: id=" + widgetId);
    }
}
