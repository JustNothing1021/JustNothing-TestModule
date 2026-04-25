package com.justnothing.testmodule.ui.analysis.memory;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.justnothing.testmodule.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemoryTrendActivity extends AppCompatActivity {

    private LineChart chart;
    private TextView tvTitle, tvEmptyHint;
    private ProgressBar progressBar;

    private String trendType;
    private List<MemorySnapshot> snapshots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_trend);

        trendType = getIntent().getStringExtra(MemoryAnalysisActivity.EXTRA_TREND_TYPE);
        if (trendType == null) {
            finish();
            return;
        }

        @SuppressWarnings("unchecked")
        ArrayList<MemorySnapshot> data = (ArrayList<MemorySnapshot>) getIntent().getSerializableExtra(
                MemoryAnalysisActivity.EXTRA_TREND_DATA);
        snapshots = data != null ? data : new ArrayList<>();

        initViews();
        displayData();
    }

    private void initViews() {
        chart = findViewById(R.id.line_chart);
        tvTitle = findViewById(R.id.tv_trend_title);
        tvEmptyHint = findViewById(R.id.tv_empty_hint);
        progressBar = findViewById(R.id.progress_bar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            String title = switch (trendType) {
                case MemoryAnalysisActivity.TREND_JAVA_HEAP -> getString(R.string.analysis_memory_java_runtime);
                case MemoryAnalysisActivity.TREND_NATIVE_HEAP -> getString(R.string.analysis_memory_native_heap);
                case MemoryAnalysisActivity.TREND_SYSTEM -> getString(R.string.analysis_memory_system_status);
                default -> getString(R.string.analysis_memory_trend_title);
            };
            getSupportActionBar().setTitle(title);
            tvTitle.setText(title);
        }
    }

    private void displayData() {
        progressBar.setVisibility(View.GONE);

        if (snapshots.isEmpty()) {
            tvEmptyHint.setVisibility(View.VISIBLE);
            tvEmptyHint.setText(R.string.analysis_memory_trend_empty);
            chart.setVisibility(View.GONE);
            return;
        }

        tvEmptyHint.setVisibility(View.GONE);
        chart.setVisibility(View.VISIBLE);
        setupChart(snapshots);
    }

    private void setupChart(List<MemorySnapshot> snapshots) {
        List<Entry> entries = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        for (int i = 0; i < snapshots.size(); i++) {
            MemorySnapshot snapshot = snapshots.get(i);
            float value = switch (trendType) {
                case MemoryAnalysisActivity.TREND_JAVA_HEAP -> (float) snapshot.javaHeap().usagePercent();
                case MemoryAnalysisActivity.TREND_NATIVE_HEAP ->
                        snapshot.nativeHeap().totalBytes() > 0
                                ? (float) (snapshot.nativeHeap().usedBytes() * 100.0 / snapshot.nativeHeap().totalBytes())
                                : 0f;
                case MemoryAnalysisActivity.TREND_SYSTEM -> (float) snapshot.systemMemory().availPercent();
                default -> 0f;
            };

            entries.add(new Entry(i, value));
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.analysis_memory_trend_label));

        int lineColor = switch (trendType) {
            case MemoryAnalysisActivity.TREND_JAVA_HEAP -> ContextCompat.getColor(this, R.color.cyan);
            case MemoryAnalysisActivity.TREND_NATIVE_HEAP -> ContextCompat.getColor(this, R.color.magenta);
            case MemoryAnalysisActivity.TREND_SYSTEM -> ContextCompat.getColor(this, R.color.blue);
            default -> Color.WHITE;
        };

        dataSet.setColor(lineColor);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(lineColor);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#20FFFFFF"));
        dataSet.setFillAlpha(80);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setTextSize(10f);
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < snapshots.size()) {
                    return sdf.format(new Date(snapshots.get(index).timestamp()));
                }
                return "";
            }
        });
        xAxis.setLabelCount(Math.min(snapshots.size(), 12), false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setTextSize(10f);
        leftAxis.setAxisMinimum(0);
        leftAxis.setAxisMaximum(100);
        leftAxis.setGranularity(5f);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setTextColor(Color.GRAY);
        chart.getLegend().setTextSize(11f);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();

        if (!entries.isEmpty()) {
            float lastValue = entries.get(entries.size() - 1).getY();
            updateStatusText(lastValue);
        }
    }

    private void updateStatusText(float currentValue) {
        String statusText = getString(R.string.analysis_memory_trend_current,
                (double) currentValue,
                getUsageLabel(currentValue));
        tvTitle.setText(statusText);
    }

    private String getUsageLabel(float percent) {
        if (percent < 50) return getString(R.string.analysis_memory_usage_good);
        if (percent < 80) return getString(R.string.analysis_memory_usage_warning);
        return getString(R.string.analysis_memory_usage_critical);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
