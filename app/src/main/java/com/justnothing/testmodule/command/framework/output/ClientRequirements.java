package com.justnothing.testmodule.command.framework.output;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
 *   <li>language - 客户端界面语言（语言码如 zh/en），服务端据此决定命令输出的语言</li>
 * </ul>
 * </p>
 *
 * <p>注意：如果 isJsonMode 为 true，则 supportsInput 会被强制设置为 false，
 * 因为JSON模式不支持交互输入。</p>
 */
public class ClientRequirements {

    /** 颜色系统常量 */
    public static final byte COLOR_STANDARD = 1;
    public static final byte COLOR_EIGHT_BIT = 2;
    public static final byte COLOR_TRUECOLOR = 3;

    private boolean supportsInput;
    private boolean isJsonMode;
    private int width;
    private int height;
    private boolean supportsAnsi;
    private byte colorSystem;
    private String language;

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

    // ─── 界面语言 ──────────────────────────────────────────

    /**
     * 客户端界面语言（语言码，如 {@code "zh"} / {@code "en"}）。
     *
     * <p>为什么不让服务端自己读 {@code Locale.getDefault()}：命令是客户端发起的、
     * 文案却是服务端渲染的，而两边常常不在一个进程里 —— GUI 的界面语言来自 Android 资源，
     * 服务端的 Locale 则是它所在进程的。所以由客户端显式声明。</p>
     *
     * <p>{@code null} 表示客户端没声明（老客户端、agent 内部调用），
     * 此时服务端回落到自己的 Locale，即引入本字段之前的行为。</p>
     */
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    // ─── RPC 参数序列化（sys.hello）──────────────────────────

    /**
     * 序列化为 RPC 参数（sys.hello）。
     *
     * <p>手写 JsonObject：本类字段无 {@code @Expose} 注解，
     * 走 GsonFactory 会被过滤掉。params 只携带能力字段。</p>
     */
    public static JsonObject toRpcParams(ClientRequirements req) {
        JsonObject params = new JsonObject();
        params.addProperty("supportsInput", req.isSupportsInput());
        params.addProperty("isJsonMode", req.isJsonMode());
        params.addProperty("width", req.getWidth());
        params.addProperty("height", req.getHeight());
        params.addProperty("supportsAnsi", req.isSupportsAnsi());
        params.addProperty("colorSystem", req.getColorSystem());
        // language 可以缺省（表示「没声明」）。Gson 的 addProperty(String, String) 传 null 会写成
        // JsonNull，读回时 getAsString() 会抛；所以缺省时干脆不放这个键。
        if (req.getLanguage() != null) {
            params.addProperty("language", req.getLanguage());
        }
        return params;
    }

    /**
     * 从 RPC 参数（sys.hello）还原客户端能力。
     */
    public static ClientRequirements fromRpcParams(JsonObject params) {
        ClientRequirements req = new ClientRequirements();
        if (params == null) {
            return req;
        }
        if (params.has("supportsInput")) req.setSupportsInput(params.get("supportsInput").getAsBoolean());
        if (params.has("isJsonMode")) req.setJsonMode(params.get("isJsonMode").getAsBoolean());
        if (params.has("width")) req.setWidth(params.get("width").getAsInt());
        if (params.has("height")) req.setHeight(params.get("height").getAsInt());
        if (params.has("supportsAnsi")) req.setSupportsAnsi(params.get("supportsAnsi").getAsBoolean());
        if (params.has("colorSystem")) req.setColorSystem(params.get("colorSystem").getAsByte());
        if (params.has("language") && !params.get("language").isJsonNull()) {
            req.setLanguage(params.get("language").getAsString());
        }
        return req;
    }

    /**
     * {@link #toRpcParams} 的字符串形式。
     */
    public static String toRpcParamsJson(ClientRequirements req) {
        return toRpcParams(req).toString();
    }

    /** 从 {@link #toRpcParamsJson} 的产物还原；解析不了时返回 {@code null}。 */
    public static ClientRequirements fromRpcParamsJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JsonObject params = JsonParser.parseString(json).getAsJsonObject();
            return fromRpcParams(params);
        } catch (Exception e) {
            return null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "ClientRequirements[" +
                "supportsInput=" + supportsInput +
                ", isJsonMode=" + isJsonMode +
                ", width=" + width +
                ", height=" + height +
                ", supportsAnsi=" + supportsAnsi +
                ", colorSystem=" + colorSystem +
                ", language=" + language +
                ']';
    }
}
