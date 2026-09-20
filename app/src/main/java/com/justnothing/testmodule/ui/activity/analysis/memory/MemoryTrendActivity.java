package com.justnothing.testmodule.ui.activity.analysis.memory;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.databinding.ActivityMemoryTrendBinding;
import com.justnothing.testmodule.ui.activity.BaseActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemoryTrendActivity extends BaseActivity {

    private ActivityMemoryTrendBinding binding;

    private String trendType;
    private List<MemorySnapshot> snapshots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMemoryTrendBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            String title = switch (trendType) {
                case MemoryAnalysisActivity.TREND_JAVA_HEAP -> getString(R.string.analysis_memory_java_runtime);
                case MemoryAnalysisActivity.TREND_NATIVE_HEAP -> getString(R.string.analysis_memory_native_heap);
                case MemoryAnalysisActivity.TREND_SYSTEM -> getString(R.string.analysis_memory_system_status);
                default -> getString(R.string.analysis_memory_trend_title);
            };
            getSupportActionBar().setTitle(title);
            binding.tvTrendTitle.setText(title);
        }
    }

    private void displayData() {
        binding.progressBar.setVisibility(View.GONE);

        if (snapshots.isEmpty()) {
            binding.tvEmptyHint.setVisibility(View.VISIBLE);
            binding.tvEmptyHint.setText(R.string.analysis_memory_trend_empty);
            binding.lineChart.setVisibility(View.GONE);
            return;
        }

        binding.tvEmptyHint.setVisibility(View.GONE);
        binding.lineChart.setVisibility(View.VISIBLE);
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

        XAxis xAxis = binding.lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setTextSize(10f);
        xAxis.setValueFormatter(new ValueFormatter() {
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

        YAxis leftAxis = binding.lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setTextSize(10f);
        leftAxis.setAxisMinimum(0);
        leftAxis.setAxisMaximum(100);
        leftAxis.setGranularity(5f);

        YAxis rightAxis = binding.lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        binding.lineChart.getDescription().setEnabled(false);
        binding.lineChart.getLegend().setTextColor(Color.GRAY);
        binding.lineChart.getLegend().setTextSize(11f);

        LineData lineData = new LineData(dataSet);
        binding.lineChart.setData(lineData);
        binding.lineChart.invalidate();

        if (!entries.isEmpty()) {
            float lastValue = entries.get(entries.size() - 1).getY();
            updateStatusText(lastValue);
        }
    }

    private void updateStatusText(float currentValue) {
        String statusText = getString(R.string.analysis_memory_trend_current,
                (double) currentValue,
                getUsageLabel(currentValue));
        binding.tvTrendTitle.setText(statusText);
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
