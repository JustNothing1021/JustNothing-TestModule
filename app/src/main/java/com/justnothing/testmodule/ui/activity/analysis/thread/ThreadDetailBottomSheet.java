package com.justnothing.testmodule.ui.activity.analysis.thread;

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
import com.justnothing.testmodule.databinding.BottomSheetThreadDetailBinding;
import com.justnothing.testmodule.ui.activity.analysis.classanalysis.ClassDetailActivity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreadDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_THREAD_ITEM = "thread_item";

    private static final Pattern STACK_FRAME_PATTERN =
            Pattern.compile("at\\s+([\\w$]+(?:\\.[\\w$]+)+)\\.([\\w$]+)\\(([^:]+)(?::(\\d+))?\\)");

    private BottomSheetThreadDetailBinding binding;

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
        binding = BottomSheetThreadDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;

        ThreadSnapshot.ThreadItem item = (ThreadSnapshot.ThreadItem) args.getSerializable(ARG_THREAD_ITEM);
        if (item == null) return;

        TextView tvName = binding.tvDetailName;
        TextView tvId = binding.tvDetailId;
        TextView tvState = binding.tvDetailState;
        TextView tvPriority = binding.tvDetailPriority;
        TextView tvDaemon = binding.tvDetailDaemon;
        TextView tvInterrupted = binding.tvDetailInterrupted;
        TextView tvAlive = binding.tvDetailAlive;
        LinearLayout layoutStack = binding.layoutStackTrace;

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
            binding.labelStack.setVisibility(View.GONE);
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
            frameTv.setTextAppearance(R.style.TextAppearance_App_Label);
            frameTv.setTypeface(android.graphics.Typeface.MONOSPACE);
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
                hintTv.setTextAppearance(R.style.TextAppearance_App_Label);
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
