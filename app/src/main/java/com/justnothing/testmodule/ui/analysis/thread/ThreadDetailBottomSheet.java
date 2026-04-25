package com.justnothing.testmodule.ui.analysis.thread;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.ui.analysis.classanalysis.ClassDetailActivity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreadDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_THREAD_ITEM = "thread_item";

    private static final Pattern STACK_FRAME_PATTERN =
            Pattern.compile("at\\s+([\\w$]+(?:\\.[\\w$]+)+)\\.([\\w$]+)\\(([^:]+)(?::(\\d+))?\\)");

    public static ThreadDetailBottomSheet newInstance(ThreadSnapshot.ThreadItem item) {
        ThreadDetailBottomSheet fragment = new ThreadDetailBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_THREAD_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_thread_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;

        ThreadSnapshot.ThreadItem item = (ThreadSnapshot.ThreadItem) args.getSerializable(ARG_THREAD_ITEM);
        if (item == null) return;

        TextView tvName = view.findViewById(R.id.tv_detail_name);
        TextView tvId = view.findViewById(R.id.tv_detail_id);
        TextView tvState = view.findViewById(R.id.tv_detail_state);
        TextView tvPriority = view.findViewById(R.id.tv_detail_priority);
        TextView tvDaemon = view.findViewById(R.id.tv_detail_daemon);
        TextView tvInterrupted = view.findViewById(R.id.tv_detail_interrupted);
        TextView tvAlive = view.findViewById(R.id.tv_detail_alive);
        LinearLayout layoutStack = view.findViewById(R.id.layout_stack_trace);

        if (tvName != null) {
            tvName.setText(item.name());
            int stateColor = getStateColor(item.state());
            tvName.setTextColor(stateColor);
        }
        if (tvId != null) tvId.setText(String.valueOf(item.threadId()));
        if (tvState != null) {
            tvState.setText(item.state());
            tvState.setTextColor(getStateColor(item.state()));
        }
        if (tvPriority != null) tvPriority.setText(String.valueOf(item.priority()));
        if (tvDaemon != null) tvDaemon.setText(item.daemon() ? "Y" : "N");
        if (tvInterrupted != null) tvInterrupted.setText(item.interrupted() ? "Y" : "N");
        if (tvAlive != null) tvAlive.setText(item.alive() ? "Y" : "N");

        if (layoutStack != null && !item.stackTrace().isEmpty()) {
            buildStackFrames(layoutStack, item.stackTrace(), requireContext());
        } else if (layoutStack != null) {
            view.findViewById(R.id.label_stack).setVisibility(View.GONE);
            layoutStack.setVisibility(View.GONE);
        }
    }

    private int getStateColor(String state) {
        if (state == null) return Color.GRAY;
        return switch (state) {
            case "RUNNABLE" -> ContextCompat.getColor(requireContext(), R.color.light_green);
            case "BLOCKED" -> ContextCompat.getColor(requireContext(), R.color.red);
            case "WAITING" -> ContextCompat.getColor(requireContext(), R.color.yellow);
            case "TIMED_WAITING" -> ContextCompat.getColor(requireContext(), R.color.magenta);
            default -> Color.GRAY;
        };
    }

    private void buildStackFrames(LinearLayout parent, List<String> frames, Context context) {
        for (String frame : frames) {
            Matcher matcher = STACK_FRAME_PATTERN.matcher(frame.trim());

            MaterialCardView card = new MaterialCardView(context);
            card.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            card.setUseCompatPadding(true);

            LinearLayout innerLayout = new LinearLayout(context);
            innerLayout.setOrientation(LinearLayout.VERTICAL);
            innerLayout.setPadding(dpToPx(10, context), dpToPx(8, context), dpToPx(10, context), dpToPx(8, context));

            TextView frameTv = new TextView(context);
            frameTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f);
            frameTv.setTypeface(android.graphics.Typeface.MONOSPACE);
            frameTv.setTextColor(0xFFCCCCCC);
            frameTv.setTextIsSelectable(true);

            String className;

            if (matcher.find()) {
                className = matcher.group(1);
                String methodName = matcher.group(2);
                String fileName = matcher.group(3);
                String lineNum = matcher.group(4) != null ? matcher.group(4) : "";

                StringBuilder displayText = new StringBuilder();
                displayText.append(className).append(".").append(methodName).append("(");

                if (fileName != null && !fileName.equals("Native Method")) {
                    displayText.append(fileName);
                    if (lineNum != null && !lineNum.isEmpty()) {
                        displayText.append(":").append(lineNum);
                    }
                } else {
                    displayText.append("Native Method");
                }
                displayText.append(")");
                frameTv.setText(displayText.toString());
            } else {
                className = null;
                frameTv.setText(frame.trim());
            }

            innerLayout.addView(frameTv);

            if (className != null) {
                card.setForeground(context.getDrawable(android.R.drawable.menuitem_background));
                card.setClickable(true);
                card.setFocusable(true);
                card.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ClassDetailActivity.class);
                    intent.putExtra(ClassDetailActivity.EXTRA_CLASS_NAME, className);
                    context.startActivity(intent);
                });

                TextView hintTv = new TextView(context);
                hintTv.setText(R.string.analysis_thread_stack_click_hint);
                hintTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 9f);
                hintTv.setTextColor(ContextCompat.getColor(context, R.color.cyan));
                hintTv.setPadding(0, dpToPx(4, context), 0, 0);
                innerLayout.addView(hintTv);
            }

            card.addView(innerLayout);
            parent.addView(card);
        }
    }

    private static int dpToPx(int dp, Context ctx) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density);
    }
}
