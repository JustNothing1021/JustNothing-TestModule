package com.justnothing.testmodule.command.framework.output;

/**
 * 客户端能力需求。
 *
 * <p>包含客户端的能力和需求信息：
 * <ul>
 *   <li>supportsInput - 是否支持交互输入</li>
 *   <li>isJsonMode - 是否使用JSON模式输出</li>
 *   <li>width - 终端宽度（列数）</li>
 *   <li>height - 终端高度（行数）</li>
 *   <li>supportsAnsi - 是否支持 ANSI 转义序列</li>
 *   <li>colorSystem - 颜色系统类型（0=NONE, 1=STANDARD, 2=EIGHT_BIT, 3=TRUECOLOR）</li>
 * </ul>
 * </p>
 *
 * <p>注意：如果 isJsonMode 为 true，则 supportsInput 会被强制设置为 false，
 * 因为JSON模式不支持交互输入。</p>
 */
public class ClientRequirements {

    /** 颜色系统常量 */
    public static final byte COLOR_NONE = 0;
    public static final byte COLOR_STANDARD = 1;
    public static final byte COLOR_EIGHT_BIT = 2;
    public static final byte COLOR_TRUECOLOR = 3;

    private boolean supportsInput;
    private boolean isJsonMode;
    private int width;
    private int height;
    private boolean supportsAnsi;
    private byte colorSystem;

    public ClientRequirements() {
        this(false, false);
    }

    public ClientRequirements(boolean supportsInput, boolean isJsonMode) {
        this.isJsonMode = isJsonMode;
        this.supportsInput = !isJsonMode && supportsInput;
    }

    // ─── 基础能力 ──────────────────────────────────────────

    public boolean isSupportsInput() {
        return supportsInput;
    }

    public void setSupportsInput(boolean supportsInput) {
        if (!isJsonMode) {
            this.supportsInput = supportsInput;
        }
    }

    public boolean isJsonMode() {
        return isJsonMode;
    }

    public void setJsonMode(boolean jsonMode) {
        this.isJsonMode = jsonMode;
        if (jsonMode) {
            this.supportsInput = false;
        }
    }

    // ─── 终端尺寸 ──────────────────────────────────────────

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    // ─── ANSI 和颜色 ───────────────────────────────────────

    public boolean isSupportsAnsi() {
        return supportsAnsi;
    }

    public void setSupportsAnsi(boolean supportsAnsi) {
        this.supportsAnsi = supportsAnsi;
    }

    public byte getColorSystem() {
        return colorSystem;
    }

    public void setColorSystem(byte colorSystem) {
        this.colorSystem = colorSystem;
    }

    @Override
    public String toString() {
        return "ClientRequirements[" +
                "supportsInput=" + supportsInput +
                ", isJsonMode=" + isJsonMode +
                ", width=" + width +
                ", height=" + height +
                ", supportsAnsi=" + supportsAnsi +
                ", colorSystem=" + colorSystem +
                ']';
    }
}
