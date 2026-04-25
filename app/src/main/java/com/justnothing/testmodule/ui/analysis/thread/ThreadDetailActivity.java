package com.justnothing.testmodule.ui.analysis.thread;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.ui.analysis.classanalysis.ClassDetailActivity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreadDetailActivity extends AppCompatActivity {

    public static final String EXTRA_THREAD_ITEM = "thread_item";

    private static final Pattern STACK_FRAME_PATTERN =
            Pattern.compile("at\\s+([\\w$]+(?:\\.[\\w$]+)+)\\.([\\w$<>-]+)\\(([^:]+)(?::(\\d+))?\\)");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ThreadSnapshot.ThreadItem item = (ThreadSnapshot.ThreadItem) getIntent().getSerializableExtra(EXTRA_THREAD_ITEM);
        if (item == null) {
            finish();
            return;
        }

        setupToolbar(item.name());
        displayThreadInfo(item);
        buildStackFrames(item.stackTrace());
    }

    private void setupToolbar(String threadName) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(threadName);
        }
    }

    private void displayThreadInfo(ThreadSnapshot.ThreadItem item) {
        TextView tvName = findViewById(R.id.tv_detail_name);
        TextView tvId = findViewById(R.id.tv_detail_id);
        TextView tvState = findViewById(R.id.tv_detail_state);
        TextView tvPriority = findViewById(R.id.tv_detail_priority);
        TextView tvDaemon = findViewById(R.id.tv_detail_daemon);
        TextView tvInterrupted = findViewById(R.id.tv_detail_interrupted);
        TextView tvAlive = findViewById(R.id.tv_detail_alive);

        if (tvName != null) {
            tvName.setText(item.name());
            tvName.setTextColor(getStateColor(item.state()));
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

        LinearLayout layoutStack = findViewById(R.id.layout_stack_trace);
        if (layoutStack != null && item.stackTrace().isEmpty()) {
            findViewById(R.id.label_stack).setVisibility(View.GONE);
            layoutStack.setVisibility(View.GONE);
        }
    }

    private int getStateColor(String state) {
        if (state == null) return Color.GRAY;
        return switch (state) {
            case "RUNNABLE" -> ContextCompat.getColor(this, R.color.light_green);
            case "BLOCKED" -> ContextCompat.getColor(this, R.color.red);
            case "WAITING" -> ContextCompat.getColor(this, R.color.yellow);
            case "TIMED_WAITING" -> ContextCompat.getColor(this, R.color.magenta);
            default -> Color.GRAY;
        };
    }

    private void buildStackFrames(List<String> frames) {
        LinearLayout parent = findViewById(R.id.layout_stack_trace);
        if (parent == null || frames.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(this);

        for (String frame : frames) {
            Matcher matcher = STACK_FRAME_PATTERN.matcher(frame.trim());

            MaterialCardView card = new MaterialCardView(this);
            card.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            card.setUseCompatPadding(true);

            LinearLayout innerLayout = new LinearLayout(this);
            innerLayout.setOrientation(LinearLayout.VERTICAL);
            innerLayout.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));

            TextView frameTv = new TextView(this);
            frameTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f);
            frameTv.setTypeface(android.graphics.Typeface.MONOSPACE);
            frameTv.setTextColor(0xFF000000);

            String className = null;

            if (matcher.find()) {
                className = matcher.group(1);
                String methodName = matcher.group(2);
                String fileName = matcher.group(3);
                String lineNum = matcher.group(4) != null ? matcher.group(4) : "";

                StringBuilder displayText = new StringBuilder();
                displayText.append(className).append(".").append(methodName).append("(");
                if (!fileName.equals("Native Method")) {
                    displayText.append(fileName);
                    if (!lineNum.isEmpty()) {
                        displayText.append(":").append(lineNum);
                    }
                } else {
                    displayText.append("Native Method");
                }
                displayText.append(")");
                frameTv.setText(displayText.toString());
            } else {
                frameTv.setText(frame.trim());
            }

            innerLayout.addView(frameTv);

            if (className != null) {
                card.setClickable(true);
                card.setFocusable(true);
                card.setCardBackgroundColor(0x0DFFFFFF);
                card.setRippleColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.cyan)));
                String finalClassName = className;
                card.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ClassDetailActivity.class);
                    intent.putExtra(ClassDetailActivity.EXTRA_CLASS_NAME, finalClassName);
                    startActivity(intent);
                });

                TextView hintTv = new TextView(this);
                hintTv.setText(R.string.analysis_thread_stack_click_hint);
                hintTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 9f);
                hintTv.setTextColor(ContextCompat.getColor(this, R.color.cyan));
                hintTv.setPadding(0, dpToPx(4), 0, 0);
                innerLayout.addView(hintTv);
            }

            card.addView(innerLayout);
            parent.addView(card);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
