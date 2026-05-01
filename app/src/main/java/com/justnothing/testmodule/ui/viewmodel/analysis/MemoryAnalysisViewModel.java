package com.justnothing.testmodule.ui.viewmodel.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.testmodule.ui.viewmodel.BaseViewModel;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.memory.GcRequest;
import com.justnothing.testmodule.command.functions.memory.MemoryInfoRequest;
import com.justnothing.testmodule.command.functions.memory.GcResult;
import com.justnothing.testmodule.command.functions.memory.MemoryInfoResult;
import com.justnothing.testmodule.ui.activity.analysis.memory.MemorySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MemoryAnalysisViewModel extends BaseViewModel<MemoryInfoRequest, MemoryInfoResult> {

    private static final int MAX_HISTORY_SIZE = 60;
    private static final int DEFAULT_REFRESH_INTERVAL_SEC = 3;

    private final LinkedList<MemorySnapshot> historySamples = new LinkedList<>();

    private final MutableLiveData<MemorySnapshot> memoryData = new MutableLiveData<>();
    private final MutableLiveData<String> lastUpdateTime = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> autoRefresh = new MutableLiveData<>(true);
    private final MutableLiveData<GcResult> gcResult = new MutableLiveData<>();
    private final MutableLiveData<List<MemorySnapshot>> trendData = new MutableLiveData<>();

    public LiveData<MemorySnapshot> getMemoryData() { return memoryData; }
    public LiveData<String> getLastUpdateTime() { return lastUpdateTime; }
    public LiveData<Boolean> isAutoRefresh() { return autoRefresh; }
    public LiveData<GcResult> getGcResult() { return gcResult; }
    public LiveData<List<MemorySnapshot>> getTrendData() { return trendData; }

    public List<MemorySnapshot> getHistorySamples() { return new LinkedList<>(historySamples); }

    public MemoryAnalysisViewModel(@NonNull Application application) {
        super(application, MemoryInfoResult.class);
    }

    public void queryMemoryInfo(boolean detailed) {
        String level = detailed ? MemoryInfoRequest.LEVEL_FULL : MemoryInfoRequest.LEVEL_BASIC;
        queryMemoryInfo(level);
    }

    private void queryMemoryInfo(String detailLevel) {
        isLoading.setValue(true);
        error.setValue(null);

        getExecutor().execute(() -> {
            MemoryInfoResult result = execute(new MemoryInfoRequest(detailLevel));
            if (result != null && result.isSuccess()) {
                MemorySnapshot snapshot = MemorySnapshot.fromResult(result);
                memoryData.postValue(snapshot);
                addToHistory(snapshot);
                updateLastUpdateTime(snapshot.timestamp());
            } else if (result != null) {
                postError(result.getError(), R.string.analysis_memory_error_query_failed);
            }
        });
    }

    public void triggerGc(boolean fullGc) {
        isLoading.setValue(true);
        error.setValue(null);

        getExecutor().execute(() -> {
            GcResult result = executeAny(new GcRequest(fullGc), GcResult.class);
            if (result != null && result.isSuccess()) {
                gcResult.postValue(result);
            } else if (result != null) {
                postError(result.getError(), R.string.analysis_memory_error_gc_failed);
            }
            queryMemoryInfo(MemoryInfoRequest.LEVEL_FULL);
        });
    }

    public void setAutoRefresh(boolean enabled) {
        autoRefresh.postValue(enabled);
        if (enabled) {
            startAutoRefresh(DEFAULT_REFRESH_INTERVAL_SEC, () -> queryMemoryInfo(MemoryInfoRequest.LEVEL_FULL));
        } else {
            stopAutoRefresh();
        }
    }

    private void addToHistory(MemorySnapshot snapshot) {
        synchronized (historySamples) {
            historySamples.addLast(snapshot);
            while (historySamples.size() > MAX_HISTORY_SIZE) {
                historySamples.removeFirst();
            }
            trendData.postValue(new LinkedList<>(historySamples));
        }
    }

    private void updateLastUpdateTime(long timestamp) {
        if (timestamp <= 0) timestamp = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        lastUpdateTime.postValue(sdf.format(new Date(timestamp)));
    }
}
