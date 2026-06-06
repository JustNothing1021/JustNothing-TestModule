package com.justnothing.methodsclient.tui.widget;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.justnothing.testmodule.utils.logging.Logger;

public class TuiStatusBar implements TuiWidget {

    private static final Logger logger = Logger.getLoggerForName("TuiStatusBar");

    private final String widgetId;
    private String title = "";
    private final Label label;
    private String statusText = "";
    private String statusType = "info";
    private boolean isRunning = false;

    public TuiStatusBar(TuiWidgetData data) {
        this.widgetId = data.getId();
        if (data.getTitle() != null) {
            this.title = data.getTitle();
        }
        this.label = new Label("");
        applyConfig(data.asStatusBarConfig());
        updateLabel();
        logger.info("TuiStatusBar created: id=" + widgetId);
    }

    @Override
    public String getWidgetId() {
        return widgetId;
    }

    @Override
    public TuiWidgetType getWidgetType() {
        return TuiWidgetType.STATUS_BAR;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Component getComponent() {
        return label;
    }

    @Override
    public void update(TuiWidgetData data) {
        StatusBarConfig cfg = data.asStatusBarConfig();
        if (cfg != null) {
            applyConfig(cfg);
        }
        if (data.getTitle() != null) {
            this.title = data.getTitle();
        }
        updateLabel();
    }

    private void applyConfig(StatusBarConfig cfg) {
        if (cfg == null) return;
        this.statusText = cfg.getStatus();
        this.statusType = cfg.getStatusType();
        this.isRunning = cfg.isRunning();
    }

    private void updateLabel() {
        String prefix;
        TextColor textColor;

        if ("success".equals(statusType)) {
            prefix = "[OK]";
            textColor = TextColor.ANSI.GREEN;
        } else if ("error".equals(statusType)) {
            prefix = "[!!]";
            textColor = TextColor.ANSI.RED;
        } else if ("warning".equals(statusType)) {
            prefix = "[!]";
            textColor = TextColor.ANSI.YELLOW;
        } else if ("info".equals(statusType)) {
            prefix = "[i]";
            textColor = TextColor.ANSI.BLUE;
        } else { // running
            prefix = "[*]";
            textColor = TextColor.ANSI.CYAN;
        }

        String displayText = prefix + " " + statusText;
        label.setText(displayText);
        label.setForegroundColor(textColor);
    }

    @Override
    public boolean needsRefresh() {
        return isRunning;
    }

    @Override
    public TerminalSize getPreferredSize() {
        return new TerminalSize(40, 1);
    }

    @Override
    public void dispose() {
        label.setText("");
        logger.info("TuiStatusBar disposed: id=" + widgetId);
    }
}
