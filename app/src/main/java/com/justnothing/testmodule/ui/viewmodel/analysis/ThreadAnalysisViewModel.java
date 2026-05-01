package com.justnothing.testmodule.ui.viewmodel.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.justnothing.testmodule.ui.viewmodel.BaseViewModel;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.threads.DeadlockDetectRequest;
import com.justnothing.testmodule.command.functions.threads.ThreadInfoRequest;
import com.justnothing.testmodule.command.functions.threads.DeadlockDetectResult;
import com.justnothing.testmodule.command.functions.threads.ThreadInfoResult;
import com.justnothing.testmodule.ui.activity.analysis.thread.ThreadSnapshot;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class ThreadAnalysisViewModel extends BaseViewModel<ThreadInfoRequest, ThreadInfoResult> {

    private static final int MAX_HISTORY_SIZE = 60;
    private static final int DEFAULT_REFRESH_INTERVAL_SEC = 3;

    private final LinkedList<ThreadSnapshot> historySamples = new LinkedList<>();

    private final MutableLiveData<ThreadSnapshot> threadData = new MutableLiveData<>();
    private final MutableLiveData<String> lastUpdateTime = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> autoRefresh = new MutableLiveData<>(true);
    private final MutableLiveData<DeadlockDetectResult> deadlockResult = new MutableLiveData<>();

    public LiveData<ThreadSnapshot> getThreadData() { return threadData; }
    public LiveData<String> getLastUpdateTime() { return lastUpdateTime; }
    public LiveData<Boolean> isAutoRefresh() { return autoRefresh; }
    public LiveData<DeadlockDetectResult> getDeadlockResult() { return deadlockResult; }

    public List<ThreadSnapshot> getHistorySamples() { return new LinkedList<>(historySamples); }

    public ThreadAnalysisViewModel(@NonNull Application application) {
        super(application, ThreadInfoResult.class);
    }

    public void queryThreadInfo(boolean detailed) {
        String level = detailed ? ThreadInfoRequest.LEVEL_FULL : ThreadInfoRequest.LEVEL_BASIC;
        queryThreadInfo(level);
    }

    private void queryThreadInfo(String detailLevel) {
        isLoading.setValue(true);
        error.setValue(null);

        getExecutor().execute(() -> {
            ThreadInfoResult result = execute(new ThreadInfoRequest(detailLevel));
            if (result != null && result.isSuccess()) {
                ThreadSnapshot snapshot = ThreadSnapshot.fromResult(result);
                threadData.postValue(snapshot);
                addToHistory(snapshot);
                updateLastUpdateTime(snapshot.timestamp());
            } else if (result != null) {
                postError(result.getError(), R.string.analysis_thread_error_query_failed);
            }
        });
    }

    public void detectDeadlock() {
        isLoading.setValue(true);
        error.setValue(null);

        getExecutor().execute(() -> {
            DeadlockDetectResult result = executeAny(new DeadlockDetectRequest(), DeadlockDetectResult.class);
            if (result != null && result.isSuccess()) {
                deadlockResult.postValue(result);
            } else if (result != null) {
                postError(result.getError(), R.string.analysis_thread_error_deadlock_failed);
            }
            queryThreadInfo(ThreadInfoRequest.LEVEL_FULL);
        });
    }

    public void setAutoRefresh(boolean enabled) {
        autoRefresh.postValue(enabled);
        if (enabled) {
            startAutoRefresh(DEFAULT_REFRESH_INTERVAL_SEC, () -> queryThreadInfo(ThreadInfoRequest.LEVEL_FULL));
        } else {
            stopAutoRefresh();
        }
    }

    private void addToHistory(ThreadSnapshot snapshot) {
        synchronized (historySamples) {
            historySamples.addLast(snapshot);
            while (historySamples.size() > MAX_HISTORY_SIZE) {
                historySamples.removeFirst();
            }
        }
    }

    private void updateLastUpdateTime(long timestamp) {
        if (timestamp <= 0) timestamp = System.currentTimeMillis();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        lastUpdateTime.postValue(sdf.format(new java.util.Date(timestamp)));
    }
}
