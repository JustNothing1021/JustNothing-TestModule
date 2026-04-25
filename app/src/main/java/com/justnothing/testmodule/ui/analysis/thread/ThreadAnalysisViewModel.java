package com.justnothing.testmodule.ui.analysis.thread;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.methodsclient.UiClient;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.JsonProtocol;
import com.justnothing.testmodule.protocol.json.request.DeadlockDetectRequest;
import com.justnothing.testmodule.protocol.json.request.ThreadInfoRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.DeadlockDetectResult;
import com.justnothing.testmodule.protocol.json.response.ThreadInfoResult;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ThreadAnalysisViewModel extends AndroidViewModel {

    private static final String TAG = "ThreadAnalysisVM";
    private final Logger logger = new Logger() {
        @Override
        public String getTag() { return TAG; }
    };

    private final UiClient client = UiClient.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;

    private static final int MAX_HISTORY_SIZE = 60;
    private final LinkedList<ThreadSnapshot> historySamples = new LinkedList<>();

    private final MutableLiveData<ThreadSnapshot> threadData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> lastUpdateTime = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> autoRefresh = new MutableLiveData<>(true);
    private final MutableLiveData<DeadlockDetectResult> deadlockResult = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<ThreadSnapshot> getThreadData() { return threadData; }
    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<String> getLastUpdateTime() { return lastUpdateTime; }
    public LiveData<Boolean> isAutoRefresh() { return autoRefresh; }
    public LiveData<DeadlockDetectResult> getDeadlockResult() { return deadlockResult; }
    public LiveData<String> getError() { return error; }

    public List<ThreadSnapshot> getHistorySamples() { return new LinkedList<>(historySamples); }

    private static final int DEFAULT_REFRESH_INTERVAL_SEC = 3;

    public ThreadAnalysisViewModel(@NonNull Application application) {
        super(application);
    }

    public void queryThreadInfo(boolean detailed) {
        String level = detailed ? ThreadInfoRequest.LEVEL_FULL : ThreadInfoRequest.LEVEL_BASIC;
        queryThreadInfo(level);
    }

    private void queryThreadInfo(String detailLevel) {
        isLoading.postValue(true);
        error.postValue(null);

        executor.execute(() -> {
            try {
                ThreadInfoRequest request = new ThreadInfoRequest(detailLevel);
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);

                if (parsedResult instanceof ThreadInfoResult result) {
                    if (result.isSuccess()) {
                        ThreadSnapshot snapshot = ThreadSnapshot.fromResult(result);
                        threadData.postValue(snapshot);
                        addToHistory(snapshot);
                        updateLastUpdateTime(snapshot.timestamp());
                    } else {
                        String msg = result.getError() != null ? result.getError().getMessage()
                                : getApplication().getString(R.string.analysis_thread_error_query_failed);
                        error.postValue(msg);
                    }
                } else {
                    error.postValue(getApplication().getString(
                            R.string.analysis_thread_error_response_type, parsedResult.getClass().getSimpleName()));
                }
            } catch (Exception e) {
                logger.error("线程信息查询异常", e);
                error.postValue(getApplication().getString(
                        R.string.analysis_thread_error_query_format, e.getMessage()));
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    private void addToHistory(ThreadSnapshot snapshot) {
        synchronized (historySamples) {
            historySamples.addLast(snapshot);
            while (historySamples.size() > MAX_HISTORY_SIZE) {
                historySamples.removeFirst();
            }
        }
    }

    public void detectDeadlock() {
        isLoading.postValue(true);
        error.postValue(null);

        executor.execute(() -> {
            try {
                DeadlockDetectRequest request = new DeadlockDetectRequest();
                String jsonResponse = client.executeCommandRequest(JsonProtocol.toJson(request));
                CommandResult parsedResult = JsonProtocol.parseResponse(jsonResponse);

                if (parsedResult instanceof DeadlockDetectResult result) {
                    if (result.isSuccess()) {
                        deadlockResult.postValue(result);
                    } else {
                        String msg = result.getError() != null ? result.getError().getMessage()
                                : getApplication().getString(R.string.analysis_thread_error_deadlock_failed);
                        error.postValue(msg);
                    }
                } else {
                    error.postValue(getApplication().getString(
                            R.string.analysis_thread_error_deadlock_response_type));
                }

                queryThreadInfo(ThreadInfoRequest.LEVEL_FULL);
            } catch (Exception e) {
                logger.error("死锁检测异常", e);
                error.postValue(getApplication().getString(
                        R.string.analysis_thread_error_deadlock_execute_failed, e.getMessage()));
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
        scheduledTask = scheduler.scheduleAtFixedRate(() ->
                queryThreadInfo(ThreadInfoRequest.LEVEL_FULL),
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
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        lastUpdateTime.postValue(sdf.format(new java.util.Date(timestamp)));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopScheduler();
        executor.shutdown();
    }
}
