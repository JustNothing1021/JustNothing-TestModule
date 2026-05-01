package com.justnothing.testmodule.ui.activity.analysis.hook;

import com.justnothing.testmodule.command.functions.hook.HookListResult;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public record HookSnapshot(
        long timestamp,
        int totalHookCount,
        int activeCount,
        List<HookItem> hooks
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static HookSnapshot fromResult(HookListResult result) {
        List<HookItem> items = new ArrayList<>();
        if (result.getHooks() != null) {
            for (HookListResult.HookItem d : result.getHooks()) {
                items.add(new HookItem(
                        d.getId(), d.getClassName(), d.getMethodName(),
                        d.getSignature(), d.isHasBefore(), d.isHasAfter(),
                        d.isHasReplace(), d.getCallCount(), d.isActive(),
                        d.isEnabled(), d.getCreateTime(),
                        d.getBeforeCodePreview(), d.getAfterCodePreview(), d.getReplaceCodePreview()
                ));
            }
        }
        return new HookSnapshot(
                result.getTimestamp(),
                result.getTotalHookCount(),
                result.getActiveCount(),
                items
        );
    }

    public record HookItem(
            String id, String className, String methodName,
            String signature, boolean hasBefore, boolean hasAfter,
            boolean hasReplace, int callCount, boolean active,
            boolean enabled, long createTime,
            String beforeCodePreview, String afterCodePreview, String replaceCodePreview
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        public String targetDisplay() {
            return className + "." + methodName;
        }

        public String phaseLabel() {
            StringBuilder sb = new StringBuilder();
            if (hasBefore) sb.append("B");
            if (hasAfter) sb.append("A");
            if (hasReplace) sb.append("R");
            return sb.toString();
        }

        public String statusKey() {
            if (!active) return "INACTIVE";
            return enabled ? "ENABLED" : "DISABLED";
        }

        public String shortInfo() {
            return "[" + statusKey() + "] " +
                    targetDisplay() +
                    " calls=" + callCount +
                    " phases=" + phaseLabel();
        }
    }
}
