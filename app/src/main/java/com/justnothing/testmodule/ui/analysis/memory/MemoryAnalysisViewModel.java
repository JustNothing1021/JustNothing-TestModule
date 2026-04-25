package com.justnothing.testmodule.ui.analysis.memory;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.methodsclient.UiClient;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.JsonProtocol;
import com.justnothing.testmodule.protocol.json.request.GcRequest;
import com.justnothing.testmodule.protocol.json.request.MemoryInfoRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.GcResult;
import com.justnothing.testmodule.protocol.json.response.MemoryInfoResult;
import com.justnothing.testmodule.utils.logging.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MemoryAnalysisViewModel extends AndroidViewModel {

    private static final String TAG = "MemoryAnalysisVM";
    private final Logger logger = new Logger() {
        @Override
        public String getTag() { return TAG; }
    };

    private final UiClient client = UiClient.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;

    private static final int MAX_HISTORY_SIZE = 60;
    private final LinkedList<MemorySnapshot> historySamples = new LinkedList<>();

    private final MutableLiveData<MemorySnapshot> memoryData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> lastUpdateTime = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> autoRefresh = new MutableLiveData<>(true);
    private final MutableLiveData<GcResult> gcResult = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<MemorySnapshot>> trendData = new MutableLiveData<>();

    public LiveData<MemorySnapshot> getMemoryData() { return memoryData; }
    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<String> getLastUpdateTime() { return lastUpdateTime; }
    public LiveData<Boolean> isAutoRefresh() { return autoRefresh; }
    public LiveData<GcResult> getGcResult() { return gcResult; }
    public LiveData<String> getError() { return error; }
    public LiveData<List<MemorySnapshot>> getTrendData() { return trendData; }

    public List<MemorySnapshot> getHistorySamples() { return new LinkedList<>(historySamples); }

    private static final int DEFAULT_REFRESH_INTERVAL_SEC = 3;

    public MemoryAnalysisViewModel(@NonNull Application application) {
        super(application);
    }

    public void queryMemoryInfo(boolean detailed) {
        String level = detailed ? MemoryInfoRequest.LEVEL_FULL : MemoryInfoRequest.LEVEL_BASIC;
        queryMemoryInfo(level);
    }

    private void queryMemoryInfo(String detailLevel) {
        isLoading.postValue(true);
        error.postValue(null);

        executor.execute(() -> {
            try {
                MemoryInfoRequest request = new MemoryInfoRequest(detailLevel);
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);

                if (parsedResult instanceof MemoryInfoResult result) {
                    if (result.isSuccess()) {
                        MemorySnapshot snapshot = MemorySnapshot.fromResult(result);
                        memoryData.postValue(snapshot);
                        addToHistory(snapshot);
                        updateLastUpdateTime(snapshot.timestamp());
                    } else {
                        String msg = result.getError() != null ? result.getError().getMessage()
                                : getApplication().getString(R.string.analysis_memory_error_query_failed);
                        error.postValue(msg);
                    }
                } else {
                    error.postValue(getApplication().getString(
                            R.string.analysis_memory_error_response_type, parsedResult.getClass().getSimpleName()));
                }
            } catch (Exception e) {
                logger.error("内存信息查询异常", e);
                error.postValue(getApplication().getString(
                        R.string.analysis_memory_error_query_format, e.getMessage()));
            } finally {
                isLoading.postValue(false);
            }
        });
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

    public void triggerGc(boolean fullGc) {
        isLoading.postValue(true);
        error.postValue(null);

        executor.execute(() -> {
            try {
                GcRequest request = new GcRequest(fullGc);
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);

                if (parsedResult instanceof GcResult result) {
                    if (result.isSuccess()) {
                        gcResult.postValue(result);
                    } else {
                        String msg = result.getError() != null ? result.getError().getMessage()
                                : getApplication().getString(R.string.analysis_memory_error_gc_failed);
                        error.postValue(msg);
                    }
                } else {
                    error.postValue(getApplication().getString(
                            R.string.analysis_memory_error_gc_response_type));
                }

                queryMemoryInfo(MemoryInfoRequest.LEVEL_FULL);
            } catch (Exception e) {
                logger.error("GC执行异常", e);
                error.postValue(getApplication().getString(
                        R.string.analysis_memory_error_gc_execute_failed, e.getMessage()));
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void setAutoRefresh(boolean enabled) {
        autoRefresh.postValue(enabled);
        if (enabled) {
            startScheduler();
        } else {
            stopScheduler();
        }
    }

    private void startScheduler() {
        stopScheduler();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduledTask = scheduler.scheduleWithFixedDelay(() ->
                queryMemoryInfo(MemoryInfoRequest.LEVEL_FULL),
                0, DEFAULT_REFRESH_INTERVAL_SEC, TimeUnit.SECONDS
        );
        logger.info("自动刷新已启动, 间隔: " + DEFAULT_REFRESH_INTERVAL_SEC + "秒");
    }

    private void stopScheduler() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduledTask = null;
        scheduler = null;
    }

    private void updateLastUpdateTime(long timestamp) {
        if (timestamp <= 0) timestamp = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        lastUpdateTime.postValue(sdf.format(new Date(timestamp)));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopScheduler();
        executor.shutdown();
    }
}
