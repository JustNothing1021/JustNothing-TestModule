package com.justnothing.methodsclient.tui.widget;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.Component;
import com.justnothing.testmodule.utils.logging.Logger;

public class TuiProgressBar implements TuiWidget {

    private static final Logger logger = Logger.getLoggerForName("TuiProgressBar");

    private final String widgetId;
    private String title = "";
    private int progress = 0;
    private int total = 100;
    private String label = "";
    private TextColor color = TextColor.ANSI.CYAN;
    private boolean showPercent = true;
    private double speed = 0;
    private long eta = 0;

    public TuiProgressBar(TuiWidgetData data) {
        this.widgetId = data.getId();
        if (data.getTitle() != null) {
            this.title = data.getTitle();
        }
        applyConfig(data.asProgressBarConfig());
        logger.info("TuiProgressBar created: id=" + widgetId);
    }

    @Override
    public String getWidgetId() {
        return widgetId;
    }

    @Override
    public TuiWidgetType getWidgetType() {
        return TuiWidgetType.PROGRESS_BAR;
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
        ProgressBarConfig cfg = data.asProgressBarConfig();
        if (cfg != null) {
            applyConfig(cfg);
        }
        if (data.getTitle() != null) {
            this.title = data.getTitle();
        }
    }

    private void applyConfig(ProgressBarConfig cfg) {
        if (cfg == null) return;
        this.progress = cfg.getProgress();
        this.total = cfg.getTotal();
        this.label = cfg.getLabel();
        this.color = mapColor(cfg.getColor());
        this.showPercent = cfg.isShowPercent();
        this.speed = cfg.getSpeed();
        this.eta = cfg.getEta();
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

    @Override
    public void draw(TextGraphics g, TerminalPosition origin, TerminalSize size) {
        int width = size.getColumns();
        int row = origin.getRow();

        // Clamp progress to valid range
        int currentProgress = Math.max(0, Math.min(progress, total));
        if (total <= 0) total = 1;

        // Build the bar string: [████████░░░░] 75% (120/160) 扫描类文件
        int barInnerWidth = width - 2; // minus [ and ]
        if (barInnerWidth < 4) barInnerWidth = 4;

        int filledWidth = 0;
        if (total > 0) {
            filledWidth = (int) ((long) currentProgress * barInnerWidth / total);
        }
        filledWidth = Math.max(0, Math.min(filledWidth, barInnerWidth));

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < barInnerWidth; i++) {
            if (i < filledWidth) {
                sb.append('\u2588'); // █
            } else {
                sb.append('\u2592'); // ░
            }
        }
        sb.append(']');

        // Append percentage and details
        int percent = total > 0 ? (int) ((long) currentProgress * 100 / total) : 0;
        String detailText;
        if (showPercent && !label.isEmpty()) {
            detailText = String.format(" %d%% (%d/%d) %s", percent, currentProgress, total, label);
        } else if (showPercent) {
            detailText = String.format(" %d%% (%d/%d)", percent, currentProgress, total);
        } else if (!label.isEmpty()) {
            detailText = " " + label;
        } else {
            detailText = "";
        }

        sb.append(detailText);

        // Draw the full string, clamped to available width
        String fullText = sb.toString();
        if (fullText.length() > width) {
            fullText = fullText.substring(0, width);
        }

        g.setForegroundColor(color);
        g.putString(origin.getColumn(), row, fullText);
    }

    @Override
    public boolean needsRefresh() {
        return progress < total;
    }

    @Override
    public TerminalSize getPreferredSize() {
        return new TerminalSize(50, 1);
    }

    @Override
    public void dispose() {
        logger.info("TuiProgressBar disposed: id=" + widgetId);
    }
}
